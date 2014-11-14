package de.bfhh.stilleoertchenhamburg.activites;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.provider.Settings;


/*
 * This activity is started by the ActivitySpashScreen after the IntentService 
 * has finished its task of retrieving a list of POI from the bf-hh server via JSON.
 * This list is also sent to this activity via the starting Intent, as well as
 * the user's current position (retrieved by LocationUpdateService).
 */

public class ActivityMap extends ActivityMenuBase {
	
	private static final String TAG = ActivitySplashScreen.class.getSimpleName();

    private static GoogleMap mMap;
    private static Location userLocation;
    
    private static final String BUNDLE_POILIST = "com.bfhh.stilleoertchenhamburg.BUNDLE_POILIST";
    private static final String BUNDLE_LATITUDE = "com.bfhh.stilleoertchenhamburg.BUNDLE_LATITUDE";
    private static final String BUNDLE_LONGITUDE = "com.bfhh.stilleoertchenhamburg.BUNDLE_LONGITUDE";
    
    private static POIController poiController; //class that handles Broadcast, methods return List<POI>
    
    private ImageButton myLocationButton; //Button to get back to current user location on map
    
    private static Marker personInNeedOfToilette; //person's marker
	private ArrayList<POI> toiletList; //POI received from POIUpdateService
	private static ArrayList<MarkerOptions> markerList;
	private static HashMap<Integer, Marker> visiblePOI;
	private LatLngBounds mapBounds; //bounds of visible map, updated onCameraChange
	//private List<LatLng> cornersLatLng; // contains two LatLng decribing northeastern and southwestern points on visible map
	
	private static double userLat;
	private static double userLng;
	
	private LatLngBounds.Builder builder;
	private POIReceiver poiReceiver;
	private LocationUpdateReceiver locUpdReceiver;
	private boolean poiReceiverRegistered;
	private boolean locUpdReceiverRegistered;
	
	private LocationUpdateService service;
	
	//camera position is saved onPause() so we can restore old view
	private CameraPosition cp;
	protected boolean changeGPSSettings;
	private static Location standardLocation;
	private static ActivityMap instance;
	
	private SlidingUpPanelLayout slidingUpPanel;
	
    //not really used right now, but will be needed later
    public static class POIReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread
		private ActivityMap main;

        public POIReceiver(Handler handler) {
            this.handler = handler;
        }
        
        void setMainActivityHandler(ActivityMap main){
            this.main = main;
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            /*String action = intent.getAction();
            if(action.equals(TagNames.BROADC_POIS)){//action sent by POIUpdateService
            	
            	//TODO: How to check whether this typecast worked?
          	  	//get POI List
	            ArrayList<HashMap<String, String>> poiList = (ArrayList<HashMap<String,String>>) intent.getSerializableExtra("poiList");
          	  	if (poiList != null){
          	  		//create poiController Object
          		  	final POIController pc = new POIController(poiList);
          		  	main.setPOIController(pc);
          		  
          		  	// Post the UI updating code to our Handler
          		  	handler.post(new Runnable() {
          		  		@Override
          		  		public void run() {
          		  			//TODO see if userLat and userLng are set
          		  			updateClosestXMarkers(pc, userLat, userLng, 10);
          		  		}
          		  	});
          	  	}
            }      */
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
            		instance = getInstance();
            		standardLocation = AppController.getInstance().getStandardLocation();
            		
            		if(lat == standardLocation.getLatitude() && lng == standardLocation.getLongitude()){
						instance.buildAlertMessageGPSSettings();	
                 	}//TODO: Testing
            		// if the old userLocation is same as standardLocation and distance to newly received location is greater 10m -> move camera
            		if(userLocation.getLatitude() == standardLocation.getLatitude() && userLocation.getLongitude() == standardLocation.getLongitude()
            				&& userLocation.distanceTo(nLocation) > 10.0){
                 		//instance.moveToLocation(nLocation, 10);
                 		CameraUpdate cu = instance.getClosestPOIBoundsOnMap(10);
                 		//CameraPosition cameraPosition = new CameraPosition.Builder().target(
                               // new LatLng(nLocation.getLatitude(), nLocation.getLongitude())).zoom(12).build();
                 		mMap.animateCamera(cu);

                 	}
            		setUserLocation(lat, lng );
            		instance.deleteOldMarkersFromList();
            		updateUserAndPOIOnMap(lat, lng);

            		if(poiController == null){
            			Log.d("ActivityMap LocUpdRec" , "poiController is null");
            		}           		
            	}
            }        
        }
    }
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Button that will animate camera back to user position
        myLocationButton = (ImageButton) findViewById(R.id.mylocation);  
        //holds the SlidingUpLayout which is wrapper of our layout
        slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        
        //TODO: is it good to register receiver in oncreate() ??
        //Register Receiver for POI Updates
        poiReceiver = new POIReceiver(new Handler());
        poiReceiver.setMainActivityHandler(this);
        registerReceiver(poiReceiver, new IntentFilter(TagNames.BROADC_POIS));
        poiReceiverRegistered = true;
        
        //LocUpdateReceiver
        locUpdReceiver = new LocationUpdateReceiver(new Handler());
        locUpdReceiver.setMainActivityHandler(this);
        registerReceiver(locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
        locUpdReceiverRegistered = true;
        
        //list of POI that are currently visible on the map
        visiblePOI = new HashMap<Integer, Marker>();
        
        //check the saved instance state:
        /*
         * if the activity is closed by pressing back button it is destroyed, but
         * the application might still be run again by the user.
         * in that case we have to save the location and poiList in savedInstanceState bundle
         * and check whether this bundle holds any information here.
         */
        if(savedInstanceState != null){ //activity was destroyed and recreated
        	// Restore value of members from saved state
            userLat = savedInstanceState.getDouble(BUNDLE_LATITUDE);
            userLng = savedInstanceState.getDouble(BUNDLE_LONGITUDE);
            standardLocation = AppController.getInstance().getStandardLocation();
            setUserLocation(userLat, userLng);
         	if(userLat == standardLocation.getLatitude() && userLng == standardLocation.getLongitude()){
         		buildAlertMessageGPSSettings();
         	}else{
         		//TODO: check whether location is standardlocation, if yes dont set userlocation
             	
         	}
            toiletList = savedInstanceState.getParcelableArrayList(BUNDLE_POILIST);
            setUpMapIfNeeded();

        } else { //activity was started from scratch
        	
        	 Intent i = getIntent();
             Bundle bundle = i.getExtras();
             if(bundle != null){
             	if(bundle.getDouble(TagNames.EXTRA_LAT) != 0.0 && bundle.getDouble(TagNames.EXTRA_LONG) != 0.0){
             		//set user Position
             		userLat = bundle.getDouble(TagNames.EXTRA_LAT);
             		userLng = bundle.getDouble(TagNames.EXTRA_LONG);
             		standardLocation = AppController.getInstance().getStandardLocation();
             		setUserLocation(userLat, userLng);
             		//if the location is the standard location, show settings dialog
                 	if(userLat == standardLocation.getLatitude() && userLng == standardLocation.getLongitude()){
                 		buildAlertMessageGPSSettings();
                 	}else{
                 		//TODO: check whether location is standardlocation, if yes dont set userlocation
                     	
                 	}
                 	setUpMapIfNeeded();
             	}
             	
                int result = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
                if(result == Activity.RESULT_CANCELED){
                	Log.d("MainActivity:", "Activity.RESULT_CANCELED: standard lat and lng");
                }
                //get the toiletList
                toiletList = i.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
             }   
        }
        //set up POIController with list of toilets received from POIUpdateService
        if(toiletList != null){
        	setPOIController(new POIController(toiletList));
        }else{
        	Log.d("MainActivity.oncreate():", "toiletList is null");
        }
        //set up Google Map with current user position
        setUpMapIfNeeded();
        
        //setPeeerOnMap();//needed????
        
        //set/update map markers
        updateClosestXMarkers(poiController, userLat, userLng, 10);
        //move camera to user location      
        moveToLocation(userLocation, 10);
        
        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition pos) {
                // Move camera.
            	CameraUpdate cu = getClosestPOIBoundsOnMap(10);
            	if(mMap != null){
            		mMap.moveCamera(cu);
                    // Remove listener to prevent position reset on camera move.
                	mMap.setOnCameraChangeListener(null);
                	//get LatLngBounds of current camera position
                	mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    //setLatLngCornersFromBounds(mapBounds);
            	}
            }			
        });
		
		//Location Button Listener
		myLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//Only move to user location if it isn't the standardLocation
            	if( userLocation.getLatitude() != standardLocation.getLatitude()  &&  userLocation.getLongitude() != standardLocation.getLongitude() ){
            		moveToLocation(userLocation, 10); //pass user location and amount of POI to display close to user
            	}else{//if userLocation == standardLocation (-> no userLoc found), show GPS Settings dialog
            		//
            		buildAlertMessageGPSSettings();
            	}
            }
        });
		
		//TODO: put the settings action check somewhere else (LocationUpdateService)
		if (userLocation == null) {
			//mylistener.onLocationChanged(userLocation);
			// leads to the settings because there is no last known location
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
    }
    
	DialogInterface.OnClickListener onOkListener = new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            changeGPSSettings = true;
            
        }
    };
    
    DialogInterface.OnClickListener onCancelListener = new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
            //startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            changeGPSSettings = true;
           
        }
    };
    
	//show alert dialog with option to change gps settings
	private void buildAlertMessageGPSSettings() {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityMap.this);
	    builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
	           .setCancelable(false)
	           .setPositiveButton("Yes", onOkListener)
	           .setNegativeButton("No", onCancelListener);
	    final AlertDialog alert = builder.create();
	    alert.show();
	}
    
	private static ActivityMap getInstance(){
		if (instance == null){
			instance = new ActivityMap();
		}
		return instance;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
    	switch (item.getItemId()) {
    		case R.id.menu_toi_list:
    			Intent activityToiletList = new Intent(this, ActivityToiletList.class);
    	    	activityToiletList.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	    	activityToiletList.putExtra(TagNames.EXTRA_POI_LIST,(Serializable) toiletList);
    	    	activityToiletList.putExtra(TagNames.EXTRA_LAT, userLat);
    	    	activityToiletList.putExtra(TagNames.EXTRA_LONG, userLng);
    	    	startActivity(activityToiletList);
    	    	return true;
    		default:
	            return super.onOptionsItemSelected(item);
    	}
		
	}
	
    //is called before activity is destroyed
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putSerializable(BUNDLE_POILIST, toiletList);
        savedInstanceState.putDouble(BUNDLE_LATITUDE, userLat);
        savedInstanceState.putDouble(BUNDLE_LONGITUDE, userLng);
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    //set user marker and closest POI markers on map
    public static void updateUserAndPOIOnMap(double lat, double lng) {
		// update Markers first
    	updateClosestXMarkers(poiController, lat, lng, 10);
		//set user position TODO: method out of that
    	getInstance().setPeeerOnMap();
    	//personInNeedOfToilette.setPosition(new LatLng(lat, lng));
	}

	private void moveToLocation(Location loc, final int poiAmount){
    	LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
    	//personInNeedOfToilette.setPosition(pos);
    	//somehow set zoom dynamically, so that all toilets are seen
    	CameraUpdate cu = getClosestPOIBoundsOnMap(poiAmount);
    	deleteOldMarkersFromList();
        //this block has to be here because the map layout might not
    	//have initialized yet, therefore we can't get bounding box with padding when our map
    	//has width = 0. In that case we wait until the Fragment holding our
    	//map has been initialized, and when it's time call animateCamera() again.
    	/*
    	 * More info here: http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
    	 * and here: http://stackoverflow.com/questions/13692579/movecamera-with-cameraupdatefactory-newlatlngbounds-crashes
    	 */
    	try{
    		mMap.animateCamera(cu);
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
    	                CameraUpdate c = getClosestPOIBoundsOnMap(poiAmount);
    	                mMap.animateCamera(c);
    	            }
    	        });
    	    }
    	}
    	
        
    }
    
	//returns the LatLngBounds around the user and the ten nearest POI
    private CameraUpdate getClosestPOIBoundsOnMap(int poiAmount){
    	builder = new LatLngBounds.Builder();
    	//update markerList with ten nearest POI to user position
    	if(poiController != null){
    		if(mMap != null){
    			mMap.clear();
    			deleteOldMarkersFromList();//delete markers in markerList and visiblePOI
    			updateUserAndPOIOnMap(userLat, userLng); //put ten closest in markerList and add to map
    		}
    	}
    	for (MarkerOptions m : markerList) {
            builder.include(m.getPosition());
        }
        //also add peeer, in case there is only toilets on one side, 
        //he / she doesn't get left out of the map view :)
        //builder.include(personInNeedOfToilette.getPosition());
        builder.include(new LatLng(userLat, userLng));
        LatLngBounds bounds = builder.build();

        return CameraUpdateFactory.newLatLngBounds(bounds,
                30);
    }
    
    //called every time onCameraChange -> NEEDED?
    private void setLatLngCornersFromBounds(LatLngBounds bounds) {
//    	cornersLatLng = new ArrayList<LatLng>();
//		// add northeastern corner
//    	cornersLatLng.add(bounds.northeast);
//    	//add southwestern corner
//    	cornersLatLng.add(bounds.southwest);	
	}
    
    //returns Location representing center of displayed map or NULL
    private LatLng getMapCenter(){
    	LatLng mapCenter = null;
		if(mapBounds != null){
			mapCenter = mapBounds.getCenter();
		}else{
			Log.d("ActivityMap getMapCenter()", "mapBounds is null");
		}
		return mapCenter; //may be null
    }
    
    //List of POI that are contained within the LatLngBounds of the map or NULL
    private List<POI> getContainedPOI(){
		List<POI> containedPOI = null;
		List<POI> allPOI = null;
		//get a list of all POI
		if(poiController != null){
			allPOI = poiController.getAllPOI();
			if(allPOI != null && mapBounds != null){
				containedPOI = new ArrayList<POI>();
				//check all of the POI as to whether they're in the LatLngBounds
				for(int i = 0; i < allPOI.size(); i++){
					//if the POI's LatLng lies within the bounds, add it to containedPOI
					if(mapBounds.contains(allPOI.get(i).getLatLng())){
						containedPOI.add(allPOI.get(i));
					}
				}
			}
		}
		return containedPOI;// may be null
    }
    
    private void displayContainedPOI(List<POI> contained){
    	if(mMap != null){
    		if(poiController != null){
    			for(int i = 0; i < contained.size(); i++){
            		POI poi = contained.get(i);
            		MarkerOptions marker = poiController.getMarkerOptionsForPOI(poi);
                    // adding marker
                    visiblePOI.put(poi.getId(), mMap.addMarker(marker));
                    markerList.add(marker); 
            	}
    		}
    	}
    }
    
    //delete old markers on the map
    private void deleteOldMarkersFromList(){
    	markerList.clear(); //clear old markers first
    	visiblePOI.clear();
    	//mMap.clear();//clear ALL markers, polylines etc from map	
    }   
    
    private void addPOIToMap(List<POI> pois){
        if(mMap != null){
            //mapBounds is the current user-viewable region of the map
            //Loop through all the items that are available to be placed on the map
            for(POI poi : pois){
                //If the item is within the the bounds of the screen
                if(mapBounds.contains(new LatLng(poi.getLat(), poi.getLng()))){
                    //If the item isn't already being displayed
                    if(!visiblePOI.containsKey(poi.getId())){
                        //Add the Marker to the Map and keep track of it with the HashMap
                        //getMarkerOptionsForPOI just returns a MarkerOptions object
                    	Marker m = mMap.addMarker(poiController.getMarkerOptionsForPOI(poi));
                    	mMap.setOnMarkerClickListener(new OnMarkerClickListener()
                        {
                            @Override
                            public boolean onMarkerClick(Marker arg0) { 
                                slidingUpPanel.showPanel();
                                return true;
                            }
                        });
                        visiblePOI.put(poi.getId(), m);
                    }
                }
                //If the marker is off screen
                else{
                    //If the course was previously on screen
                    if(visiblePOI.containsKey(poi.getId()))
                    {
                        //1. Remove the Marker from the GoogleMap
                        visiblePOI.get(poi.getId()).remove();
                     
                        //2. Remove the reference to the Marker from the HashMap
                        visiblePOI.remove(poi.getId());
                    }
                }
            }
        }
    }
    
    @Override
    protected void onStart(){
    	super.onStart();
    	//set up Google Map with current user position
        setUpMapIfNeeded();
    	if(!poiReceiverRegistered){
    		registerReceiver(poiReceiver, new IntentFilter(TagNames.BROADC_POIS));
    		poiReceiverRegistered = true;
    	}
    	if(!locUpdReceiverRegistered){
    		registerReceiver(locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
    		locUpdReceiverRegistered = true;
    	}
    	
    	mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
             @Override
             public void onCameraChange(CameraPosition pos) {
                 // Move camera.	
             	if(mMap != null){
                 	//get LatLngBounds of current camera position
                 	mapBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                 	setLatLngCornersFromBounds(mapBounds);
                 	if(poiController != null){
                 		addPOIToMap(poiController.getAllPOI());
                 	}else {
                 		if(toiletList != null){
                 			poiController = new POIController(toiletList);
                 		}    		
                 	}
                 	
             	}
             }			
         });
    	 
    	
    	//hide the sliding up Panel
    	slidingUpPanel.hidePanel();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(!poiReceiverRegistered){
    		registerReceiver(poiReceiver, new IntentFilter(TagNames.BROADC_POIS));
    		poiReceiverRegistered = true;
    	} 
        if(!locUpdReceiverRegistered){
    		registerReceiver(locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
    		locUpdReceiverRegistered = true;
    	}
        //bind LocationUpdateService
        Intent intent= new Intent(this, LocationUpdateService.class);
        bindService(intent, mConnection,
            Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	service.stopLocationUpdates();
    	unbindService(mConnection);//unbind service
    	if(poiReceiverRegistered){
    		unregisterReceiver(poiReceiver);
    		poiReceiverRegistered = false;
    	}
    	if(locUpdReceiverRegistered){
    		unregisterReceiver(locUpdReceiver);
    		locUpdReceiverRegistered = false;
    	}
    	//fix the map at restart of application, so that user and markers can be seen again
    	//TODO: nullpointer after disabling network in running app and then trying to pause it (back  button)
    	cp = mMap.getCameraPosition();
        mMap = null;
    }
    
    @Override
	protected void onStop(){
    	super.onStop();
    	if(poiReceiverRegistered){
    		unregisterReceiver(poiReceiver);
    		poiReceiverRegistered = false;
    	}
    	if(locUpdReceiverRegistered){
    		unregisterReceiver(locUpdReceiver);
    		locUpdReceiverRegistered = false;
    	}
    }
    
    @Override
	protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "onDestroy");
    	if(poiReceiverRegistered){
    		unregisterReceiver(poiReceiver);
    		poiReceiverRegistered = false;
    	}
    	if(locUpdReceiverRegistered){
    		unregisterReceiver(locUpdReceiver);
    		locUpdReceiverRegistered = false;
    	}
    }
    
    private static void setUserLocation(double lat, double lng){
    	userLat = lat;
    	userLng = lng;
    	//set user Location
        userLocation = new Location("");//empty provider string
        userLocation.setLatitude(lat);
        userLocation.setLongitude(lng);
    }
    
    private void setPOIController(POIController pc){
    	poiController = pc;
    }
    
    // Update closest X markers on the map
    private static void updateClosestXMarkers(POIController poiController, double currLat, double currLng, int x){
    	List<POI> closestX = new ArrayList<POI>(); //list with closest ten POI to user position
    	markerList = new ArrayList<MarkerOptions>(); //markers for those POI
    	//check whether the broadcast was received
    	if(poiController != null ){
	        if(poiController.poiReceived() ){
	        	//propagate the user position to all POI and set their distance to user in meters
	        	poiController.setDistancePOIToUser(currLat, currLng);
	        	//get closest ten POI
	        	closestX = poiController.getClosestPOI(x); // get ten closest toilets
	        	if(mMap != null){
	        		//add markers to the map  for the ten closest POI
		        	for(int i = 0; i < closestX.size(); i++){
		        		POI poi = closestX.get(i);
		        		//get MarkerOptions for this POI from poiController
		        		MarkerOptions marker = poiController.getMarkerOptionsForPOI(poi);
		                // adding marker
		                visiblePOI.put(poi.getId(), mMap.addMarker(marker));
		                markerList.add(marker); 
		        	}
	        	}
	        } else {
	        	//TODO: Toast
	        	Log.d("ActivityMain.onStart()", "POIController has not received Broadcast");
	        }
    	}
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have nfot already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
           
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
            	setPeeerOnMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setPeeerOnMap() {
    	// check whether user position is standard position, if not set icon
        if(userLat != standardLocation.getLatitude() && userLng != standardLocation.getLongitude()){
        	if(personInNeedOfToilette != null){
        		personInNeedOfToilette.remove();
        	}
        	personInNeedOfToilette = mMap.addMarker(new MarkerOptions()
	    	.position(new LatLng(userLat, userLng))
	    	.icon(BitmapDescriptorFactory.fromResource(R.drawable.peeer)));
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, 
            IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          service = b.getLocService();
          Toast.makeText(ActivityMap.this, "LocService Connected", Toast.LENGTH_SHORT)
              .show();
          Location loc = service.getCurrentUserLocation();
          if(loc == standardLocation || loc == null){
        	  service.updateLocation();//is this called multiple times??
          }
        }

        public void onServiceDisconnected(ComponentName className) {
          service = null;
          Toast.makeText(ActivityMap.this, "LocService Disconnected", Toast.LENGTH_SHORT)
          .show();
        }
      };
}