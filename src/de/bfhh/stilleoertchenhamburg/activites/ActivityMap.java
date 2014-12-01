package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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

import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.helpers.POIHelper;
import de.bfhh.stilleoertchenhamburg.models.POI;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;


/**
 * This activity is started by the ActivitySpashScreen after the IntentService 
 * has finished its task of retrieving a list of POI from the bf-hh server via JSON.
 * This list is also sent to this activity via the starting Intent, as well as
 * the user's current position (retrieved by LocationUpdateService).
 */

/* TODO
 * 1. Map nach oben schieben, wenn Slider ge�ffnet wird
 * 2. ActivityResult von GPS check zur�ckgeben
 * 3. Pr�fung, wie weit sich der User vom letzten Standpunkt bewegt hat -> erst ab mind 10m user icon neu zeichnen
 */

public class ActivityMap extends ActivityMenuBase {

	private static final String TAG = ActivityMap.class.getSimpleName();

	private static GoogleMap _mMap;
	
	private ImageButton _myLocationButton; //Button to get back to current user location on map
	private ImageButton _zoomInButton;
	private ImageButton _zoomOutButton;
	
	private static Marker _personInNeedOfToilette; //person's marker
	private static HashMap<Integer, MarkerOptions> _markerMap;
	private static HashMap<Integer, Marker> _visiblePOIMap;
	
	private LatLngBounds _mapBounds; //bounds of visible map, updated onCameraChange
	private LatLngBounds.Builder _builder;
	private CameraPosition _cameraPosition;
	private CameraUpdate _cameraUpdate;
	
	private ArrayList<POI> _allPOIList; //POI received from POIUpdateService
	
	private Location _userLocation;
	private static double _userLat;
	private static double _userLng;
	private boolean _hasUserLocation;

	private LocationUpdateService _locUpdateService;
	private LocationUpdateReceiver _locUpdReceiver;
	private boolean _locUpdReceiverRegistered;

	//the CameraUpdate created by saving northeast and southwest corner in savedInstanceState
	private static ActivityMap _instance;

	private SlidingUpPanelLayout _slidingUpPanel;

	//Display dimensions
	private int _displayWidth;
	private int _displayHeight;

	protected Marker _clickedMarker;

	private float _logicalDensity;

	private int _clickedMarkerId;

	/**
	 * Initiate variables
	 * fetch bundle contents
	 * register receivers
	 */
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_main);
		
		//Display size
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			_displayWidth = display.getWidth();
			_displayHeight = display.getHeight();
		}else{
			display.getSize(size);
			_displayWidth = size.x;
			_displayHeight = size.y;
		}
		//get display density
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		_logicalDensity = metrics.density;
		
		_instance = this;

		//Button that will animate camera back to user position
		_myLocationButton = (ImageButton) findViewById(R.id.buttonToLocation); 
		_zoomInButton = (ImageButton) findViewById(R.id.buttonZoomIn); 
		_zoomOutButton = (ImageButton) findViewById(R.id.buttonZoomOut); 
		
		//holds the SlidingUpLayout which is wrapper of our layout
		_slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		_slidingUpPanel.setAnchorPoint(0.5f);
		_slidingUpPanel.setOverlayed(true);

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
			Log.d(TAG, "onCreate from savedInstanceState");
		} else { //activity was started from scratch
			Log.d(TAG, "onCreate from SplashScreen");
			Intent i = getIntent();
			bundle = i.getExtras();
		}

		if(bundle != null){
			//set user Position (might be standard location)
			_userLat = bundle.getDouble(TagNames.EXTRA_LAT);
			_userLng = bundle.getDouble(TagNames.EXTRA_LONG);
			//userLocation == standardLocation -> 0 -> false; else -> -1 -> true
			_hasUserLocation = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT) == -1;
			Log.d("oncreate _hasUserLocation", ""+_hasUserLocation);
			Log.d("oncreate _userLat", ""+_userLat);
			Log.d("oncreate _userLng", ""+_userLng);

			setUserLocation(_userLat, _userLng);
			//if the location is the standard location, show settings dialog
			if(!_hasUserLocation){
				buildAlertMessageGPSSettings();
				Log.d("MainActivity:", "Activity.RESULT_CANCELED: standard lat and lng");
			}
			//get the poiList
			_allPOIList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
			
			//get clicked marker if it's set
			//if(bundle.getInt(TagNames.EXTRA_MARKER_ID) != 0){
				_clickedMarkerId = bundle.getInt(TagNames.EXTRA_MARKER_ID);
			//}
		}   

		//set up POIController with list of toilets received from POIUpdateService
		if(_allPOIList != null){
			//set distance to user position for all POI and return updated list
			_allPOIList = POIHelper.setDistancePOIToUser(_allPOIList, _userLat, _userLng);
		}else{
			Log.d("MainActivity.oncreate():", "_allPOIList is null");
		}
	} // end onCreate

	@Override
	protected void onStart(){
		super.onStart();
		Log.d(TAG, "onStart");

		//hide the sliding up Panel
		_slidingUpPanel.hidePanel();

		
	}// end onStart


	/**
	 * Check google play store availability before anything else
	 * on first opening go to user location and draw POI markers
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

		if (checkPlayServices()){

			setUpMapIfNeeded(); //initialize map if it isn't already	
			//set padding to map
			_mMap.setPadding(5, 5, 5, 5);
			
			//only called after first setup of application 
			//_clickedMarkerId check so that if a marker was clicked before screen rotation,
			//it 
			if(_mapBounds == null && _clickedMarkerId == 0){
				CameraUpdate cu = getClosestPOIBounds(10);
				moveToLocation(cu);
				updatePOIMarkers();
			}
			setPeeerOnMap(); //user marker

			if(!_locUpdReceiverRegistered){
				registerReceiver(_locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
				_locUpdReceiverRegistered = true;
			}
			//bind LocationUpdateService
			Intent intent= new Intent(this, LocationUpdateService.class);
			bindService(intent, _mConnection,  Context.BIND_AUTO_CREATE);
			Log.d("Map onResume", "Service bound");

			_mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition pos) {
					// Move camera.	
					if(_mMap != null){
						//get LatLngBounds of current camera position
						_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
						updatePOIMarkers();
						Log.d("markerlist size:", String.valueOf(_markerMap.size()));
						Log.d("visible poi size:", String.valueOf(_visiblePOIMap.size()));
					}
				}			
			});

			//TODO: make transition of map between slidinUpPanel hide / show nicer
			//hide the panel if user clicks somewhere on the map where there is no marker
			_mMap.setOnMapClickListener(new OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					_slidingUpPanel.hidePanel();
					
					_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
					LatLng center = getMapCenter(_mapBounds);
					_mMap.animateCamera(CameraUpdateFactory.newLatLng(center));
				}
			});

			//Location Button Listener
			_myLocationButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					//Only move to user location if it isn't the standardLocation
					if (_hasUserLocation){
						CameraUpdate cu = getClosestPOIBounds(10);
						moveToLocation(cu);
						updatePOIMarkers();
					} else {//show GPS Settings dialog
						buildAlertMessageGPSSettings();
					}
				}
			});
			
			_zoomInButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (_mMap != null) {_mMap.animateCamera( CameraUpdateFactory.zoomIn()); }
				}
			});  
			
			_zoomOutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (_mMap != null) {_mMap.animateCamera( CameraUpdateFactory.zoomOut()); }
				}
			});

			//Set Listener for different sliding events for SlidingUpPanel
			_slidingUpPanel.setPanelSlideListener(new PanelSlideListener(){
				private boolean firstCollapse = true;
				private boolean firstAnchored = true;

				@Override
				public void onPanelSlide(View panel, float slideOffset) {
					/*if the panel is slid within close vicinity of the anchorPoint 
					 * (at 0.75f), expand the panel to that anchorPoint (for some reason
					 * this doesn't work by only setting slidingUpPanel.setAnchorPoint(0.75f);)
					 */
					if( Math.abs(slideOffset - 0.5f) < 0.02f ) {
						_slidingUpPanel.expandPanel(0.5f);
					}	
				}

				@Override
				public void onPanelCollapsed(View panel) {
					//get height of panel in pixels (68dp)
	                int px = (int) Math.ceil(68 * _logicalDensity);
	                //set padding to map so that map controls are moved
					_mMap.setPadding(5, 5, 5, px);
					
					_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
					Log.d("onPanelCollapsed _mapBounds", ""+_mapBounds);
					
					//clickedMarker may be null
					if(_clickedMarker == null && _clickedMarkerId != 0 && _visiblePOIMap != null){
						_clickedMarker = getMarkerValueByID(_visiblePOIMap, _clickedMarkerId);
					}
					if(_clickedMarker != null){
						//get distance between map center (gmaps) and clicked marker position
						float d = getDistanceBewteen(getMapCenter(_mapBounds), _clickedMarker.getPosition());
						//if panel collapsed for first time (== first shown) and d > 10 meters
						if(!firstCollapse && d > 10.0f ){
							_mMap.animateCamera(CameraUpdateFactory.newLatLng(_clickedMarker.getPosition()));
						}
					}
					firstCollapse = false;
					firstAnchored = false;
					//show location button
					_myLocationButton.setVisibility(View.VISIBLE);
					_zoomInButton.setVisibility(View.VISIBLE);
					_zoomOutButton.setVisibility(View.VISIBLE);
				}

				@Override
				public void onPanelExpanded(View panel) {}

				@Override
				public void onPanelAnchored(View panel) {
					//move the map up so that clicked marker is visible
					if(!firstAnchored ){
						//set bottom padding
						_mMap.setPadding(5, 5, 5, (int) (_displayHeight/2));
						//TODO: Beim Tablet werden die zoom buttons plus copyright noch verdeckt
						if(_clickedMarker == null && _clickedMarkerId != 0 && _visiblePOIMap != null){
							_clickedMarker = getMarkerValueByID(_visiblePOIMap, _clickedMarkerId);
						}
						if(_clickedMarker != null){
							LatLng markerPos = _clickedMarker.getPosition();
							//move position to center map to down a bit
							LatLng mPlusOffset = new LatLng(markerPos.latitude+0.001f, markerPos.longitude);
							_mMap.animateCamera(CameraUpdateFactory.newLatLng(mPlusOffset));
						}
						firstAnchored = true;
					}
					//hide my location button
					_myLocationButton.setVisibility(View.INVISIBLE);
					_zoomInButton.setVisibility(View.INVISIBLE);
					_zoomOutButton.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onPanelHidden(View panel) {
					firstCollapse = true;
					firstAnchored = true;
					_mMap.setPadding(5, 5, 5, 5);//reverse padding to default
				}    	
			});
		}
	}
	

	/**
	 * receive user location updates
	 * on receive: update person icon
	 * at a certain distance move map
	 */
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
					Location oldLocation = new Location("");
					oldLocation.setLatitude(_userLat);
					oldLocation.setLongitude(_userLng);
					Log.d("_hasUserLocation =", ""+ _instance._hasUserLocation);
					Log.d("userlocation 1", ""+_instance._userLocation);

					_instance.setUserLocation(lat, lng);
					_instance._allPOIList = POIHelper.setDistancePOIToUser(_instance._allPOIList, lat, lng);
					
					Log.d("userlocation 1", ""+_instance._userLocation);
					//TODO: Testing
					//old userLocation == standardLocation and distance to newly received location is greater 10m -> move camera
					if(_instance._hasUserLocation){
						if( _instance._userLocation.distanceTo(oldLocation) > 10.0 || oldLocation == AppController.getInstance().getStandardLocation()){
							CameraUpdate cu = _instance.getClosestPOIBounds(10);
							_mMap.animateCamera(cu);
							_instance.updatePOIMarkers();
							_instance.setPeeerOnMap();
						}
					}       
				}
			}        
		}
	}


	private void moveToLocation(final CameraUpdate cu){
		//this block has to be here because the map layout might not
		//have initialized yet, therefore we can't get bounding box with padding when our map
		//has width = 0. In that case we wait until the Fragment holding our
		//map has been initialized, and when it's time call animateCamera() again.
		/*
		 * More info here: http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
		 * and here: http://stackoverflow.com/questions/13692579/movecamera-with-cameraupdatefactory-newlatlngbounds-crashes
		 */
		Log.d(TAG, "moveToLocation");
		try {
			_mMap.animateCamera(cu);
		} catch (IllegalStateException e) {
			// fragment layout with map not yet initialized
			final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();

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
		_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
	}


	/**
	 * Returns the CameraUpdate around the user and the X nearest POI
	 */
	private CameraUpdate getClosestPOIBounds(int poiAmount){
		Log.d(TAG, "getClosestPOIBounds");
		_builder = new LatLngBounds.Builder();

		List<POI> closestX = new ArrayList<POI>(); //list with closest ten POI to user position
		closestX = POIHelper.getClosestPOI(_allPOIList, poiAmount);
		for(POI poi : closestX){
			_builder.include(poi.getLatLng());
		}
		//also add user location
		if (_hasUserLocation){
			_builder.include(new LatLng(_userLat, _userLng));
		}
		LatLngBounds bounds = _builder.build();

		return CameraUpdateFactory.newLatLngBounds(bounds, 30);
	}


	/**
	 * Update currently visible markers of POIS
	 */
	private void updatePOIMarkers(){
		Log.d(TAG, "updatePOIMarkers");

		if(_mMap != null){
			//mapBounds is the current user-viewable region of the map
			//Loop through all the items that are available to be placed on the map
			for(POI poi : _allPOIList){
				//If the item is within the the bounds of the screen
				if(_mapBounds.contains(new LatLng(poi.getLat(), poi.getLng()))){
					//If the item isn't already being displayed
					if(!_visiblePOIMap.containsKey(poi.getId())){
						//Add the Marker to the Map and keep track of it with the HashMap
						//getMarkerOptionsForPOI just returns a MarkerOptions object
						MarkerOptions mo = POIHelper.getMarkerOptionsForPOI(poi);
						Marker m = _mMap.addMarker(mo);
						
						int id = poi.getId();
						_visiblePOIMap.put(id, m);
						if(!_markerMap.containsKey(id)){
							_markerMap.put(id, mo);
						}
						//the slidingUpPanel is shown if the user clicks on a marker
						_mMap.setOnMarkerClickListener(new OnMarkerClickListener(){
							@Override
							public boolean onMarkerClick(Marker m) { 
								//show panel
								_slidingUpPanel.showPanel();
								
								//center map on clicked marker
								LatLng center = m.getPosition();
								_mMap.animateCamera(CameraUpdateFactory.newLatLng(center));
								_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
								_clickedMarker = m;
								Log.d("_visiblePOIMap", ""+_visiblePOIMap);
								Log.d("marker", ""+m);
								_clickedMarkerId = getMarkerIDByValue(_visiblePOIMap, m);
								return true;
							}
						});
						
					}
				} else { //If the marker is off screen
					//If the course was previously on screen
					if(_visiblePOIMap.containsKey(poi.getId())) {
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

		//Log.d("Contained poi size:", String.valueOf(getContainedPOI().size()));
		Log.d("markerlist size:", String.valueOf(_markerMap.size()));
		Log.d("visible poi size:", String.valueOf(_visiblePOIMap.size()));
	}
	
	/*
	 * Returns the Marker ID for a passed Marker and HashMap containing it.
	 * If HashMap doesn't contain marker null is returned.
	 */
	public static <T, E> int getMarkerIDByValue(HashMap<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (value.equals(entry.getValue())) {
	            return (Integer) entry.getKey();
	        }
	    }
	    return 0;
	}
	
	/*
	 * Returns the Marker for a passed ID and HashMap containing it.
	 * If HashMap doesn't contain ID null is returned.
	 */
	public static <T, E> E getMarkerValueByID(HashMap<T, E> map, T id) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (id.equals(entry.getKey())) {
	            return entry.getValue();
	        }
	    }
	    return null;
	}
	
	private float getDistanceBewteen(LatLng a, LatLng b){
		float[] res = new float[1];
		Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res);
		return res[0];
	}
	
	/**
	 * Sets up the map if it isn't yet
	 */
	private void setUpMapIfNeeded() {
		Log.d(TAG, "setUpMapIfNeeded");
		if (_mMap == null) {
			_mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			_mMap.getUiSettings().setZoomControlsEnabled(false);
		}
	}

	private LatLng getMapCenter(LatLngBounds mapBounds){
		LatLng mapCenter = null;
		if(mapBounds != null){
			mapCenter = mapBounds.getCenter();
		}else{
			Log.d("ActivityMap getMapCenter()", "mapBounds is null");
		}
		return mapCenter; //may be null
	}
	
	/**
	 * Set user icon on map, if there is no user icon yet, otherwise change its position
	 */
	private void setPeeerOnMap() {
		Log.d(TAG, "setPeeerOnMap");
		// check whether user position is standard position, if not set icon

		if (_hasUserLocation && _mMap != null) {
			//NOTE: this is the only way of doing it, because marker.setposition(LatLng) doesn't work!
			if (_personInNeedOfToilette != null) {
				_personInNeedOfToilette.remove();
			}
			_personInNeedOfToilette = _mMap.addMarker(new MarkerOptions()
				.position(new LatLng(_userLat, _userLng))
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.peeer)));
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

	/**
	 * is called before activity is destroyed to save state (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save current map state
		savedInstanceState.putParcelableArrayList(TagNames.EXTRA_POI_LIST, _allPOIList);
		savedInstanceState.putDouble(TagNames.EXTRA_LAT, _userLat);
		savedInstanceState.putDouble(TagNames.EXTRA_LONG, _userLng);
		savedInstanceState.putInt(TagNames.EXTRA_LOCATION_RESULT, _hasUserLocation ? -1 : 1);
		savedInstanceState.putInt(TagNames.EXTRA_MARKER_ID, _clickedMarkerId);		 

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause(){
		super.onPause();
		//NOTE: if service is not set to null here onLocationChanged() is called twice in a row
		_locUpdateService = null;

		unbindService(_mConnection);//unbind service
		Log.d("Map onPause", "service unbound");

		if(_locUpdReceiverRegistered){
			unregisterReceiver(_locUpdReceiver);
			_locUpdReceiverRegistered = false;
		}
		
		_slidingUpPanel.hidePanel();
		//fix the map at restart of application, so that user and markers can be seen again
		//TODO: nullpointer after disabling network in running app and then trying to pause it (back  button)
		if (_mMap != null){
			_cameraPosition = _mMap.getCameraPosition();		    		
			_mMap = null;
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
		// set this to false so that when map is brought back to front a new location fix is received
		_hasUserLocation = false;
		//hide sliding panel
		_slidingUpPanel.hidePanel();
	}


	/**
	 * Manage connection to LocationUpdateService
	 */
	private ServiceConnection _mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
			_locUpdateService = b.getLocService();
			Toast.makeText(ActivityMap.this, "LocService Connected", Toast.LENGTH_SHORT).show();
			Location loc = _locUpdateService.getCurrentUserLocation();
			if(!_hasUserLocation || loc == null || _locUpdateService.isBetterLocation(loc, _userLocation)) {
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

	/** show alert dialog with option to change gps settings */
	private void buildAlertMessageGPSSettings() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityMap.this);
		builder.setMessage("Dein GPS ist ausgestellt, m�chtest du es jetzt anstellen?")
		.setCancelable(false)
		.setPositiveButton("Ja", onOkListener)
		.setNegativeButton("Nein", onCancelListener);
		final AlertDialog alert = builder.create();
		alert.show();
		//TODO: check if user has turned location services on (onActivityResult)
	}
}