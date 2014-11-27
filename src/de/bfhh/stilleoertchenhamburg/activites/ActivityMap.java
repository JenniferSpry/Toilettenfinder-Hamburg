package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;

import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;


/*
 * This activity is started by the ActivitySpashScreen after the IntentService 
 * has finished its task of retrieving a list of POI from the bf-hh server via JSON.
 * This list is also sent to this activity via the starting Intent, as well as
 * the user's current position (retrieved by LocationUpdateService).
 */

public class ActivityMap extends ActivityMenuBase {
	
	private static final String TAG = ActivityMap.class.getSimpleName();

    private static GoogleMap _mMap;
    private Location _userLocation;
    
    private static POIController _poiController; //class that handles Broadcast, methods return List<POI>
    
    private ImageButton _myLocationButton; //Button to get back to current user location on map
    
    private static Marker _personInNeedOfToilette; //person's marker
	private ArrayList<POI> _allPOIList; //POI received from POIUpdateService
	private static HashMap<Integer, MarkerOptions> _markerMap;
	private static HashMap<Integer, Marker> _visiblePOIMap;
	private LatLngBounds _mapBounds; //bounds of visible map, updated onCameraChange
	//private List<LatLng> cornersLatLng; // contains two LatLng decribing northeastern and southwestern points on visible map
	
	private static double _userLat;
	private static double _userLng;
	private boolean _hasUserLocation;
	
	private LatLngBounds.Builder _builder;
	private LocationUpdateReceiver _locUpdReceiver;
	private boolean _locUpdReceiverRegistered;
	
	private LocationUpdateService _locUpdateService;
	
	//camera position is saved onPause() so we can restore old view
	private CameraPosition _cameraPosition;
	private CameraUpdate _cameraUpdate; //the CameraUpdate created by saving northeast and southwest corner in savedInstanceState
	private static ActivityMap _instance;
	
	private SlidingUpPanelLayout _slidingUpPanel;
	
	
    /* get instance of this in case of static shit */
    private static ActivityMap getInstance(){
		if (_instance == null){
			_instance = new ActivityMap();
		}
		return _instance;
	}

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Log.d(TAG, "onCreate");
	    setContentView(R.layout.activity_main);
	    	
	    //Button that will animate camera back to user position
	    _myLocationButton = (ImageButton) findViewById(R.id.mylocation);  
	    //holds the SlidingUpLayout which is wrapper of our layout
	    _slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
	    _slidingUpPanel.setAnchorPoint(0.75f);
	                    
	    //LocUpdateReceiver
	    _locUpdReceiver = new LocationUpdateReceiver(new Handler());
	    _locUpdReceiver.setMainActivityHandler(this);
	    
	    //list of POI that are currently visible on the map
	    _visiblePOIMap = new HashMap<Integer, Marker>();
	    _markerMap = new HashMap<Integer, MarkerOptions>();
	    
	    Bundle bundle = null;
	    //check the saved instance state:
	    if(savedInstanceState != null){ //activity was destroyed and recreated
	    	bundle = savedInstanceState;
	    } else { //activity was started from scratch
	    	 Intent i = getIntent();
	         bundle = i.getExtras();
	    }
        if(bundle != null){
        	if(bundle.getDouble(TagNames.EXTRA_LAT) != 0.0 && bundle.getDouble(TagNames.EXTRA_LONG) != 0.0){
        		//set user Position (might be standard location)
        		_userLat = bundle.getDouble(TagNames.EXTRA_LAT);
        		_userLng = bundle.getDouble(TagNames.EXTRA_LONG);
        		_hasUserLocation = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT) == -1;

        		setUserLocation(_userLat, _userLng);
        		//if the location is the standard location, show settings dialog
        		if(!_hasUserLocation){
        			buildAlertMessageGPSSettings();
        			Log.d("MainActivity:", "Activity.RESULT_CANCELED: standard lat and lng");
        		}
        	}
        	//get the toiletList
        	_allPOIList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
        }   

	    //set up POIController with list of toilets received from POIUpdateService
	    if(_allPOIList != null){
	    	_poiController = new POIController(_allPOIList);
	    }else{
	    	Log.d("MainActivity.oncreate():", "_allPOIList is null");
	    }
	} // end onCreate

	
	@Override
	protected void onStart(){
		super.onStart();
		
		setUpMapIfNeeded();
		
		//hide the sliding up Panel
		_slidingUpPanel.hidePanel();
		
		//set Listener for different sliding events
		_slidingUpPanel.setPanelSlideListener(new PanelSlideListener(){
			@Override
			public void onPanelSlide(View panel, float slideOffset) {
				/*if the panel is slid within close vicinity of the anchorPoint 
				 * (at 0.75f), expand the panel to that anchorPoint (for some reason
				 * this doesn't work by only setting slidingUpPanel.setAnchorPoint(0.75f);)
				*/
				if( Math.abs(slideOffset - 0.75f) < 0.02f ) {
					_slidingUpPanel.expandPanel(0.75f);
				}	
			}
	
			@Override
			public void onPanelCollapsed(View panel) {}
	
			@Override
			public void onPanelExpanded(View panel) {}
	
			@Override
			public void onPanelAnchored(View panel) {}
	
			@Override
			public void onPanelHidden(View panel) {}    	
	    });
	}

	@Override
	protected void onResume() {
	    super.onResume();
	    
	    if (checkPlayServices()){
	    	
	    	setUpMapIfNeeded();
	    	setPeeerOnMap();
	    	//only called after first setup of application 
	    	if(_mapBounds == null){
	    		_mMap.clear();
	    		//set/update map markers
	    		deleteOldMarkersFromList();
	        	//updateClosestXMarkers(poiController, userLat, userLng, 10);
	        	//move camera to user location  
	    		CameraUpdate cu = getClosestPOIBoundsOnMap(10);
	        	moveToLocation(cu);
	    	}
	    	
		    if(!_locUpdReceiverRegistered){
		    	registerReceiver(_locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
		    	_locUpdReceiverRegistered = true;
		    }
		    //bind LocationUpdateService
		    Intent intent= new Intent(this, LocationUpdateService.class);
		    bindService(intent, mConnection,  Context.BIND_AUTO_CREATE);
		        
		    _mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
		    	@Override
		    	public void onCameraChange(CameraPosition pos) {
		    		// Move camera.	
		    		if(_mMap != null){
		    			//get LatLngBounds of current camera position
		    			_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
		    			if (_poiController != null){
		    				addPOIToMap(_poiController.getAllPOI());
		    				Log.d("Contained poi size:", String.valueOf(getContainedPOI().size()));
		    				Log.d("markerlist size:", String.valueOf(_markerMap.size()));
		    				Log.d("visible poi size:", String.valueOf(_visiblePOIMap.size()));
		    			} else {
		    				if(_allPOIList != null){
		    					_poiController = new POIController(_allPOIList);
		    				}    		
		    			}
		    		}
		    	}			
		    });
		    
		    //TODO: make transition of map between slidinUpPanel hide / show nicer
			//hide the panel if user clicks somewhere on the map where there is no marker
			_mMap.setOnMapClickListener(new OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					_slidingUpPanel.hidePanel();
				}
			});
			
	        
			//Location Button Listener
		     _myLocationButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	//Only move to user location if it isn't the standardLocation
	            	if(!_hasUserLocation){
	            		CameraUpdate cu = getClosestPOIBoundsOnMap(10);
	            		moveToLocation(cu); //pass user location and amount of POI to display close to user
	            	}else{//if userLocation == standardLocation (-> no userLoc found), show GPS Settings dialog
	            		//
	            		buildAlertMessageGPSSettings();
	            	}
	            }
	         });
	    	
	    	
	    }
	}

	//not really used right now, but will be needed later
    public static class LocationUpdateReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread
		private ActivityMap main;

        public LocationUpdateReceiver(Handler handler) {
            this.handler = handler;
        }
        
        void setMainActivityHandler(ActivityMap main){
            this.main = main;
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
        	Bundle bundle = intent.getExtras();
            String action = intent.getAction();
            //Location has been updated (by provider in LocService)
            if(action.equals(TagNames.BROADC_LOCATION_UPDATED)){
            	if(bundle != null){
            		double lat = bundle.getDouble(TagNames.EXTRA_LAT);
            		double lng = bundle.getDouble(TagNames.EXTRA_LONG);
            		String provider = bundle.getString(TagNames.EXTRA_PROVIDER);
            		Log.d("ActivityMap LocationUpdateReceiver", "Location received from "+ provider + ": " + lat  + ", " + lng);
            		//Toast to show updates
            		Toast.makeText(context, "Location Update from provider: " + provider + ", Location: LAT " + lat + ", LNG " + lng,
            				Toast.LENGTH_LONG).show();
            		//update the user location
            		Location nLocation = new Location("");
            		nLocation.setLatitude(lat);
            		nLocation.setLongitude(lng);
            		//to call non static methods we need instance of ActivityMap
            		_instance = getInstance();
            		_instance.setUserLocation(lat, lng );            		
            		//TODO: Testing
            		// if the old userLocation is same as standardLocation and distance to newly received location is greater 10m -> move camera
            		if(!_instance._hasUserLocation){
            			_instance.buildAlertMessageGPSSettings();
            			if( _instance._userLocation.distanceTo(nLocation) > 10.0){
            				CameraUpdate cu = _instance.getClosestPOIBoundsOnMap(10);
                     		//CameraPosition cameraPosition = new CameraPosition.Builder().target(
                                   // new LatLng(nLocation.getLatitude(), nLocation.getLongitude())).zoom(12).build();
                     		_mMap.animateCamera(cu);
            			}
                 	}
            		
            		_instance.deleteOldMarkersFromList();
            		updateUserAndPOIOnMap(lat, lng);

            		if(_poiController == null){
            			Log.d("ActivityMap LocUpdRec" , "poiController is null");
            		}         
            		
            		
            	}
            }        
        }
    }
    
    
    //is called before activity is destroyed
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putSerializable(TagNames.EXTRA_LAT, _allPOIList);
        savedInstanceState.putDouble(TagNames.EXTRA_LONG, _userLat);
        savedInstanceState.putDouble(TagNames.EXTRA_POI_LIST, _userLng);
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    //set user marker and closest POI markers on map
    public static void updateUserAndPOIOnMap(double lat, double lng) {
		// update Markers first
    	updateClosestXMarkers(_poiController, lat, lng, 10);
		//set user position TODO: method out of that
    	getInstance().setPeeerOnMap();
	}

	private void moveToLocation(final CameraUpdate cu){
    	//somehow set zoom dynamically, so that all toilets are seen
    	//CameraUpdate cu = getClosestPOIBoundsOnMap(poiAmount);
        //this block has to be here because the map layout might not
    	//have initialized yet, therefore we can't get bounding box with padding when our map
    	//has width = 0. In that case we wait until the Fragment holding our
    	//map has been initialized, and when it's time call animateCamera() again.
    	/*
    	 * More info here: http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
    	 * and here: http://stackoverflow.com/questions/13692579/movecamera-with-cameraupdatefactory-newlatlngbounds-crashes
    	 */
    	try{
    		_mMap.animateCamera(cu);
    	} catch (IllegalStateException e) {
    	    // fragment layout with map not yet initialized
    	    final View mapView = getSupportFragmentManager()
    	       .findFragmentById(R.id.map).getView();
    	    if (mapView.getViewTreeObserver().isAlive()) {
    	        mapView.getViewTreeObserver().addOnGlobalLayoutListener(
    	        new OnGlobalLayoutListener() {
    	            @SuppressWarnings("deprecation")
    	            @SuppressLint("NewApi")
    	            // We check which build version we are using.
    	            @Override
    	            public void onGlobalLayout() {
    	                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //below API Level 16 ?
    	                    mapView.getViewTreeObserver()
    	                        .removeGlobalOnLayoutListener(this);
    	                } else {
    	                    mapView.getViewTreeObserver()
    	                        .removeOnGlobalLayoutListener(this);
    	                }
    	                _mMap.animateCamera(cu);
    	            }
    	        });
    	    }
    	}
    	
        
    }
    
	//returns the LatLngBounds around the user and the ten nearest POI
    private CameraUpdate getClosestPOIBoundsOnMap(int poiAmount){
    	_builder = new LatLngBounds.Builder();
    	//update markerList with ten nearest POI to user position
    	/*if(poiController != null){
    		if(mMap != null){
    			mMap.clear();
    			deleteOldMarkersFromList();//delete markers in markerList and visiblePOI
    			updateUserAndPOIOnMap(userLat, userLng); //put ten closest in markerList and add to map
    		}
    	}*/
    	for(MarkerOptions moValue : _markerMap.values()){
    		_builder.include(moValue.getPosition());
    	}
        //also add peeer, in case there is only toilets on one side, 
        //he / she doesn't get left out of the map view :)
        //builder.include(personInNeedOfToilette.getPosition());
        _builder.include(new LatLng(_userLat, _userLng));
        LatLngBounds bounds = _builder.build();

        return CameraUpdateFactory.newLatLngBounds(bounds, 30);
    }
    
    //List of POI that are contained within the LatLngBounds of the map or NULL
    private List<POI> getContainedPOI(){
		List<POI> containedPOI = null;
		List<POI> allPOI = null;
		//get a list of all POI
		if(_poiController != null){
			allPOI = _poiController.getAllPOI();
			if(allPOI != null && _mapBounds != null){
				containedPOI = new ArrayList<POI>();
				//check all of the POI as to whether they're in the LatLngBounds
				for(int i = 0; i < allPOI.size(); i++){
					//if the POI's LatLng lies within the bounds, add it to containedPOI
					if(_mapBounds.contains(allPOI.get(i).getLatLng())){
						containedPOI.add(allPOI.get(i));
					}
				}
			}
		}
		return containedPOI;// may be null
    }
    
    //delete old markers on the map
    private void deleteOldMarkersFromList(){
    	if(!_markerMap.isEmpty() && !_visiblePOIMap.isEmpty()){
        	_markerMap.clear(); //clear old markers first
        	_visiblePOIMap.clear();
    	}	
    }   
    
    private void addPOIToMap(List<POI> pois){
        if(_mMap != null){
        	Log.d("Contained poi size:", String.valueOf(getContainedPOI().size()));
        	Log.d("markerlist size:", String.valueOf(_markerMap.size()));
        	Log.d("visible poi size:", String.valueOf(_visiblePOIMap.size()));
            //mapBounds is the current user-viewable region of the map
            //Loop through all the items that are available to be placed on the map
            for(POI poi : pois){
                //If the item is within the the bounds of the screen
                if(_mapBounds.contains(new LatLng(poi.getLat(), poi.getLng()))){
                    //If the item isn't already being displayed
                    if(!_visiblePOIMap.containsKey(poi.getId())){
                        //Add the Marker to the Map and keep track of it with the HashMap
                        //getMarkerOptionsForPOI just returns a MarkerOptions object
                    	MarkerOptions mo = _poiController.getMarkerOptionsForPOI(poi);
                    	Marker m = _mMap.addMarker(mo);
                    	//the slidingUpPanel is shown if the user clicks on a marker
                    	_mMap.setOnMarkerClickListener(new OnMarkerClickListener()
                        {
                            @Override
                            public boolean onMarkerClick(Marker arg0) { 
                                _slidingUpPanel.showPanel();
                                return true;
                            }
                        });
                    	int id = poi.getId();
                        _visiblePOIMap.put(id, m);
                        if(!_markerMap.containsKey(id)){
                        	_markerMap.put(id, mo);
                        }
                    }
                }
                //If the marker is off screen
                else{
                    //If the course was previously on screen
                    if(_visiblePOIMap.containsKey(poi.getId()))
                    {
                    	int id = poi.getId();
                        //1. Remove the Marker from the GoogleMap
                        _visiblePOIMap.get(id).remove();
                     
                        //2. Remove the reference to the Marker from the HashMap
                        _visiblePOIMap.remove(id);
                        
                        if(_markerMap.containsKey(id)){
                        	_markerMap.remove(id);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	_locUpdateService.stopLocationUpdates();
    	unbindService(mConnection);//unbind service
    	if(_locUpdReceiverRegistered){
    		unregisterReceiver(_locUpdReceiver);
    		_locUpdReceiverRegistered = false;
    	}	
    }
    
    @Override
	protected void onStop(){
    	super.onStop();
    	if(_locUpdReceiverRegistered){
    		unregisterReceiver(_locUpdReceiver);
    		_locUpdReceiverRegistered = false;
    	}
    	
    }
    
    @Override
	protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "onDestroy");
    	if(_locUpdReceiverRegistered){
    		unregisterReceiver(_locUpdReceiver);
    		_locUpdReceiverRegistered = false;
    	}
    	//fix the map at restart of application, so that user and markers can be seen again
    	//TODO: nullpointer after disabling network in running app and then trying to pause it (back  button)
    	if (_mMap != null){
    		_cameraPosition = _mMap.getCameraPosition();		    		
            _mMap = null;
    	}
    }
    
    private void setUserLocation(double lat, double lng){
    	_userLat = lat;
    	_userLng = lng;
    	//set user Location
        _userLocation = new Location("");//empty provider string
        _userLocation.setLatitude(lat);
        _userLocation.setLongitude(lng);
    }
    
    
    // Update closest X markers on the map
    private static void updateClosestXMarkers(POIController poiController, double currLat, double currLng, int x){
    	List<POI> closestX = new ArrayList<POI>(); //list with closest ten POI to user position
    	_markerMap = new HashMap<Integer, MarkerOptions>(); //markers for those POI
    	//check whether the broadcast was received
    	if(poiController != null ){
	        if(poiController.poiReceived() ){
	        	//propagate the user position to all POI and set their distance to user in meters
	        	poiController.setDistancePOIToUser(currLat, currLng);
	        	//get closest ten POI
	        	closestX = poiController.getClosestPOI(x); // get ten closest toilets
	        	if(_mMap != null){
	        		//add markers to the map  for the ten closest POI
		        	for(int i = 0; i < closestX.size(); i++){
		        		POI poi = closestX.get(i);
		        		//get MarkerOptions for this POI from poiController
		        		MarkerOptions marker = poiController.getMarkerOptionsForPOI(poi);
		                // adding marker
		                _visiblePOIMap.put(poi.getId(), _mMap.addMarker(marker));
		                _markerMap.put(poi.getId(), marker); 
		        	}
	        	}
	        } else {
	        	//TODO: Toast
	        	Log.d("ActivityMain.onStart()", "POIController has not received Broadcast");
	        }
    	}
    }

    /**
     * Sets up the map if it is possible to do so 
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (_mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            _mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        }
    }

    /**
     * Set user icon on map, if there is no user icon yet, otherwise change its position
     * */
    private void setPeeerOnMap() {
    	// check whether user position is standard position, if not set icon
        if(_hasUserLocation && _mMap != null){
        	if(_personInNeedOfToilette != null){
        		_personInNeedOfToilette.setPosition(new LatLng(_userLat, _userLng));
        	}else{
        		_personInNeedOfToilette = _mMap.addMarker(new MarkerOptions()
	    		.position(new LatLng(_userLat, _userLng))
	    		.icon(BitmapDescriptorFactory.fromResource(R.drawable.peeer)));
        	}
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          _locUpdateService = b.getLocService();
          Toast.makeText(ActivityMap.this, "LocService Connected", Toast.LENGTH_SHORT).show();
          Location loc = _locUpdateService.getCurrentUserLocation();
          if(!_hasUserLocation || loc == null){
        	  _locUpdateService.updateLocation();//is this called multiple times??
          }
        }

        public void onServiceDisconnected(ComponentName className) {
          _locUpdateService = null;
          Toast.makeText(ActivityMap.this, "LocService Disconnected", Toast.LENGTH_SHORT).show();
        }
      };
      
      
      /* ------- Dialog Methods -------- */
      
	DialogInterface.OnClickListener onOkListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int id) {
			startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		}
	};
      
	DialogInterface.OnClickListener onCancelListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int id) {
			
		}
	};

  	/* show alert dialog with option to change gps settings */
  	private void buildAlertMessageGPSSettings() {
  		final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
  	    builder.setMessage("Dein GPS ist ausgestellt, möchtest du es jetzt anstellen?")
  	           .setCancelable(false)
  	           .setPositiveButton("Ja", onOkListener)
  	           .setNegativeButton("Nein", onCancelListener);
  	    final AlertDialog alert = builder.create();
  	    alert.show();
  	    //TODO: check if user has turned location services on (onActivityResult)
  	}
}