package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import android.location.Location;
import android.location.LocationManager;
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
 * 2. ActivityResult von GPS check zurückgeben
 * 3. Prüfung, wie weit sich der User vom letzten Standpunkt bewegt hat -> erst ab mind 10m user icon neu zeichnen
 */

public class ActivityMap extends ActivityMenuBase {

	private static final String TAG = ActivityMap.class.getSimpleName();

	private static GoogleMap _mMap;
	
	private ImageButton _myLocationButton; //Button to get back to current user location on map
	private ImageButton _zoomInButton;
	private ImageButton _zoomOutButton;
	
	TextView txtName;   
    TextView txtDistance;    
    TextView txtAddress;   
    TextView txtDescription;            
    TextView txtWebsite;
    
    EditText editName;
    EditText editEmail;
    EditText editComment;
    
	private static Marker _personInNeedOfToilette; //person's marker
	private static HashMap<Integer, Marker> _markerMap;
	private static HashMap<String, Integer> _markerPOIIdMap; // marker ids mapped to poi ids
	
	private LatLngBounds _mapBounds; //bounds of visible map, updated onCameraChange
	private LatLngBounds.Builder _builder;
	private CameraPosition _cameraPosition;
	private CameraUpdate _cameraUpdate;
	
	private ArrayList<POI> _allPOIList; //POI received from POIUpdateService
	
	private Location _userLocation;
	private static double _userLat;
	private static double _userLng;
	private boolean _hasUserLocation;
	public boolean _isLowUpdateInterval;
	protected boolean _firstLocationPull;

	private LocationUpdateService _locUpdateService;
	private LocationUpdateReceiver _locUpdReceiver;
	private boolean _locUpdReceiverRegistered;

	//the CameraUpdate created by saving northeast and southwest corner in savedInstanceState
	private static ActivityMap _instance;

	private SlidingUpPanelLayout _slidingUpPanel;
	private RelativeLayout _buttonsContainer;

	//Display dimensions
	private int _displayWidth;
	private int _displayHeight;
	private float _logicalDensity;

	private POI _selectedPoi;
	protected Marker _selectedMarker;

	private ArrayList<EditText> _editTexts;


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
		
		//Display size in pixel
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			_displayWidth = display.getWidth();
			_displayHeight = display.getHeight();
		} else {
			display.getSize(size);
			_displayWidth = size.x;
			_displayHeight = size.y;
		}
		//get display density
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		_logicalDensity = metrics.density;
		
		_instance = this;

		//Buttons
		_myLocationButton = (ImageButton) findViewById(R.id.buttonToLocation); 
		_zoomInButton = (ImageButton) findViewById(R.id.buttonZoomIn); 
		_zoomOutButton = (ImageButton) findViewById(R.id.buttonZoomOut); 
		
		_buttonsContainer = (RelativeLayout) findViewById(R.id.mapButtons);
				
		//holds the SlidingUpLayout which is wrapper of our layout
		_slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		_slidingUpPanel.setAnchorPoint(0.45f);
		_slidingUpPanel.setOverlayed(true);
		_slidingUpPanel.setPanelHeight((int) Math.ceil(110 * _logicalDensity));//dp to px
		_slidingUpPanel.hidePanel();
		
		//text input fields in slider
		_editTexts = new ArrayList<EditText>();
		editName = (EditText) findViewById(R.id.name_field);
		editEmail = (EditText) findViewById(R.id.email_field);
		editComment = (EditText) findViewById(R.id.comment_field);
		_editTexts.add(editName);
		_editTexts.add(editEmail);
		_editTexts.add(editComment);

		//LocationUpdateReceiver
		_locUpdReceiver = new LocationUpdateReceiver(new Handler());
		_locUpdReceiver.setMainActivityHandler(this);
		_isLowUpdateInterval = false; 

		//list of POI markers that are currently visible on the map
		_markerMap = new HashMap<Integer, Marker>();
		_markerPOIIdMap = new HashMap<String, Integer>();

		Bundle bundle = null;
		//check the saved instance state:
		if(savedInstanceState != null){ //activity was destroyed and recreated
			bundle = savedInstanceState;
			Log.d(TAG, "onCreate from savedInstanceState");
		} else { 						//activity was started from scratch
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

			setUserLocation(_userLat, _userLng);
			//if the location is the standard location, show settings dialog
			if(!_hasUserLocation){
				buildAlertMessageGPSSettings();
				Log.d("MainActivity:", "Activity.RESULT_CANCELED: standard lat and lng");
			}
			//get the poiList
			_allPOIList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
			
			//get clicked marker if it's set
			_selectedPoi = bundle.getParcelable(TagNames.EXTRA_POI);
			updateSliderContent(_selectedPoi);
		}   

		//set up POIController with list of toilets received from POIUpdateService
		if(_allPOIList != null){
			//set distance to user position for all POI and return updated list
			_allPOIList = POIHelper.setDistancePOIToUser(_allPOIList, _userLat, _userLng);
		} else {
			Log.d("MainActivity.oncreate():", "_allPOIList is null");
		}
	} // end onCreate
	

	/** Override onNewIntent() so that intents sent from other activities
	 *  are processed when ActivityMap is already running, but in background.
	 */
	@Override
	protected void onNewIntent(Intent intent) {
	    super.onNewIntent(intent);
	    setIntent(intent);
	    
	    //intent from FragmentToiletList on list item click
		String action = intent.getAction();
		Bundle bundle = intent.getExtras();
		Log.d("onNewIntent Intent received? ", "intent: "+ intent);
		Log.d("action ",  ""+ action);
		
		if(action != null && action.equals(TagNames.ACTION_SHOW_SLIDER)){
			Log.d("###**** intent from FragmentToiletList", "receeeeived");
			//center map on marker, show slider
			_selectedPoi = bundle.getParcelable(TagNames.EXTRA_POI);
		}
	}
	
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
			setPaddingBottom(0);
			
			//only called after first setup of application 
			//_clickedMarkerId check so that if a marker was clicked before screen rotation,
			//it 
			if(_mapBounds == null && _selectedPoi == null){
				CameraUpdate cu = getCameraUpdateForClosestPOIBounds(10);
				moveToLocation(cu);
				updatePOIMarkers();
			}
			setPeeerOnMap();
			
			updateAllMarkerColor();

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
					if(_mMap != null){
						refreshMapBounds();
						updatePOIMarkers();
					}
				}			
			});

			_mMap.setOnMapClickListener(new OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					_slidingUpPanel.hidePanel();
					if (_selectedMarker != null){
						_selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getIcon()));
					}
					_selectedMarker = null;
					_selectedPoi = null;
				}
			});
			
			//the slidingUpPanel is shown if the user clicks on a marker
			_mMap.setOnMarkerClickListener(new OnMarkerClickListener(){
				@Override
				public boolean onMarkerClick(Marker marker) { 
					//center map on clicked marker
					LatLng center = marker.getPosition();
					moveToLocation(CameraUpdateFactory.newLatLng(center));
					refreshMapBounds();
					// set last selected marker back to unselected
					if (_selectedMarker != null & _selectedPoi != null) {
						_selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getIcon()));
					}

					if(_markerPOIIdMap.get(marker.getId()) != null){//is this markers id in the list (if not it is user marker)
						_selectedPoi = POIHelper.getPoiByIdReference(_markerPOIIdMap, _allPOIList, marker.getId());
						marker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getActiveIcon()));
						updateSliderContent(_selectedPoi);
						_slidingUpPanel.showPanel();
						_selectedMarker = marker;
					} else {
						_slidingUpPanel.hidePanel();
					}
					return true;
				}
			});

			//Location Button Listener
			_myLocationButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					//Only move to user location if it isn't the standardLocation
					if (_hasUserLocation){
						moveToLocation(getCameraUpdateForClosestPOIBounds(10));
						updatePOIMarkers();
					} else {//show GPS Settings dialog
						buildAlertMessageGPSSettings();
					}
				}
			});
			
			_zoomInButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					moveToLocation(CameraUpdateFactory.zoomIn());
				}
			});  
			
			_zoomOutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					moveToLocation(CameraUpdateFactory.zoomOut());
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
					if( Math.abs(slideOffset - 0.45f) < 0.02f ) {
						_slidingUpPanel.expandPanel(0.45f);
					}	
				}

				@Override
				public void onPanelCollapsed(View panel) {
					//get height of panel in pixels (68dp)
	                //set padding to map so that map controls are moved
	                setPaddingBottom(110);
					
					refreshMapBounds();
					
					//clickedMarker may be null
					if(_selectedMarker == null && _selectedPoi != null && _markerMap != null){
						_selectedMarker = _markerMap.get(_selectedPoi.getId());
					}
					if(_selectedMarker != null){
						//get distance between map center (gmaps) and clicked marker position
						float d = getDistanceBewteen(getMapCenter(_mapBounds), _selectedMarker.getPosition());
						//if panel collapsed for first time (== first shown) and d > 10 meters
						if(!firstCollapse && d > 10.0f ){
							moveToLocation(CameraUpdateFactory.newLatLng(_selectedMarker.getPosition()));
						}
					}
					firstCollapse = false;
					firstAnchored = false;
					_buttonsContainer.setVisibility(View.VISIBLE);
				}

				@Override
				public void onPanelExpanded(View panel) {}

				@Override
				public void onPanelAnchored(View panel) {
					//move the map up so that clicked marker is visible
					if(!firstAnchored ){
						//set bottom padding
						setPaddingBottom((int) (_displayHeight/2));
						
						if(_selectedMarker == null && _selectedPoi != null && _markerMap != null){
							_selectedMarker = _markerMap.get(_selectedPoi.getId());
						}
						if(_selectedMarker != null){
							LatLng markerPos = _selectedMarker.getPosition();
							//move position to center map to down a bit
							LatLng mPlusOffset = new LatLng(markerPos.latitude+0.001f, markerPos.longitude);
							moveToLocation(CameraUpdateFactory.newLatLng(mPlusOffset));
						}
						firstAnchored = true;
					}
					_buttonsContainer.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onPanelHidden(View panel) {
					firstCollapse = true;
					firstAnchored = true;
					setPaddingBottom(0);
				}    	
			});			
			
			if ((_selectedPoi != null) && (_mMap != null)) {
				// 
				//center camera on marker
				moveToLocation(CameraUpdateFactory.newLatLng(_selectedPoi.getLatLng()));
				updateSliderContent(_selectedPoi);
				Marker marker = null;
				//marker not on map (not one of the closest ten markers?)
				if(!_markerMap.containsKey(_selectedPoi.getId())){
					Log.d("3454564653w4r onResume _clickedPOI", "adding marker to map and showing panel");
					//add marker to mMap and to lists
					marker = _mMap.addMarker(POIHelper.getMarkerOptionsForPOI(_selectedPoi));	
					_markerMap.put(_selectedPoi.getId(), marker);
					_markerPOIIdMap.put(marker.getId(), _selectedPoi.getId());
				} else { //marker already set
					Log.d("{{{{{{7{ onResume _clickedPOI", "marker already on map");
					marker = _markerMap.get(_selectedPoi.getId());
				}
				_selectedMarker = marker;
				_slidingUpPanel.showPanel();
			}	
				
		}
	}//end onResume
	

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
					//Retrieve latitude, longitude, provider and result
					double lat = bundle.getDouble(TagNames.EXTRA_LAT);
					double lng = bundle.getDouble(TagNames.EXTRA_LONG);
					String provider = bundle.getString(TagNames.EXTRA_PROVIDER);
					int result = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
					
					Log.d("ActivityMap LocationUpdateReceiver", "Location received from "+ provider + ": " + lat  + ", " + lng);
					//Toast to show updates
					//Toast.makeText(context, "Location Update from provider: " + provider + ", Location: LAT " + lat + ", LNG " + lng,
						//	Toast.LENGTH_LONG).show();
					
					//Result_ok -> true, Result_cancelled -> false
					_instance._hasUserLocation =  result == -1;

					//get previous user Location (might be standardLocation)
					Location oldLocation = new Location("");
					oldLocation.setLatitude(_userLat);
					oldLocation.setLongitude(_userLng);

					Log.d("_hasUserLocation =", ""+ _instance._hasUserLocation);
					Log.d("userlocation 1", ""+_instance._userLocation.getLatitude());
					
					//update the user location
					_instance.setUserLocation(lat, lng);
					_instance._allPOIList = POIHelper.setDistancePOIToUser(_instance._allPOIList, lat, lng);
					
					Log.d("userlocation 2", ""+_instance._userLocation.getLatitude());
					//TODO: Testing
					//old userLocation == standardLocation and distance to newly received location is greater 10m -> move camera
					if(_instance._hasUserLocation){					
						if(_instance._userLocation.distanceTo(oldLocation) < 10.0 ){
							if(!_instance._isLowUpdateInterval){//high update interval
								Log.d("-------- ", "lower update interval frequency");
								_instance.callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
								_instance._isLowUpdateInterval = true;
							}
						}else{//distance > 10 meters (because user moved or because old loc is standard loc)
							Log.d("======= distance to old loc", ""+_instance._userLocation.distanceTo(oldLocation));
							
							Location s = AppController.getInstance().getStandardLocation();
							//oldLocation == standardLocation -> we have jumped quite far
							if(oldLocation.getLatitude() == s.getLatitude() && oldLocation.getLongitude() == s.getLongitude()){
								Log.d("-------- ", "lower update interval frequency");
								_instance.callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
								_instance._isLowUpdateInterval = true;
								Toast.makeText(context, "Position wird bestimmt...",
										Toast.LENGTH_LONG).show();
								_instance.moveToLocation(_instance.getCameraUpdateForClosestPOIBounds(10));
								_instance.updatePOIMarkers();
								_instance.setPeeerOnMap();
							}else if(_instance._userLocation.distanceTo(oldLocation) > 84.0 && _instance._isLowUpdateInterval){
								Log.d("+++++++++ ", "higher update interval frequency");
								_instance.callForLocationUpdates(60000, 3.0f, 5000, 1.0f );
								_instance._isLowUpdateInterval = false;
								_instance.moveToLocation(_instance.getCameraUpdateForClosestPOIBounds(10));
								_instance.updatePOIMarkers();
								_instance.setPeeerOnMap();
							}
						}
					}    
				}
			}        
		}
	}//end LocationUpdateReceiver


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
		if (_mMap != null){
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
			refreshMapBounds();
		}
	}//end moveToLocation
	
	
	private void refreshMapBounds(){
		try {
			_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
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
								_mapBounds = _mMap.getProjection().getVisibleRegion().latLngBounds;
							}
						});
			}
		}
	}//end getMapBounds


	/**
	 * Returns the CameraUpdate around the user and the X nearest POI
	 */
	private CameraUpdate getCameraUpdateForClosestPOIBounds(int poiAmount){
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
			//Loop through all the items that are available to be placed on the map
			for(POI poi : _allPOIList){
				//If the item is within the the bounds of the screen
				if(_mapBounds.contains(new LatLng(poi.getLat(), poi.getLng()))){
					//If the item isn't already being displayed
					if(!_markerMap.containsKey(poi.getId())){
						//Add the Marker to the Map and keep track of it with the HashMap
						//getMarkerOptionsForPOI just returns a MarkerOptions object
						Marker marker = _mMap.addMarker(POIHelper.getMarkerOptionsForPOI(poi));
						
						_markerMap.put(poi.getId(), marker);
						_markerPOIIdMap.put(marker.getId(), poi.getId());
					} 
				} else if(_markerMap.containsKey(poi.getId()) && _selectedPoi != null) { // delete marker outside of bounds
					if (_selectedPoi.getId() != poi.getId()){ // do not remove selected marker 
						//1. Remove the Marker from the GoogleMap
						_markerMap.get(poi.getId()).remove();
						//2. Remove the reference to the Marker from the HashMap
						_markerMap.remove(poi.getId());
						// remove marker id to poi id mapping
						for(Iterator<Map.Entry<String, Integer>> it = _markerPOIIdMap.entrySet().iterator(); it.hasNext(); ) {
							Map.Entry<String, Integer> entry = it.next();
							if (poi.getId() == entry.getValue()) {
						        it.remove();
						    }
						}
					}
				}
			}
		}
	}
	
	// TODO
	private void updateAllMarkerColor(){
		if (_allPOIList != null && _markerMap != null){
			for(POI poi : _allPOIList){
				if(_markerMap.containsKey(poi.getId())){
					if (_selectedPoi != null && poi.getId() == _selectedPoi.getId()){
						_markerMap.get(poi.getId()).setIcon(
				    			BitmapDescriptorFactory.fromResource(poi.getActiveIcon()));
					} else {
						_markerMap.get(poi.getId()).setIcon(
				    			BitmapDescriptorFactory.fromResource(poi.getIcon()));
					}
				}
			}
		}
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
	
	/** ----- SLIDING PANEL ----**/		
	
	//Update info displayed in sliding panel
	protected void updateSliderContent(POI poi) {
		if (poi != null){
			txtName = (TextView) findViewById(R.id.name_detail);
	        txtName.setText(poi.getName());
	        
	        int d = (int) Math.round(poi.getDistanceToUser());
	        String distance = String.valueOf(d) + " m";  
	        txtDistance = (TextView) findViewById(R.id.distance_detail);
	        txtDistance.setText(distance);
	        
	        txtAddress = (TextView) findViewById(R.id.address_detail);
	        txtAddress.setText(poi.getAddress());
	        
	        txtDescription = (TextView) findViewById(R.id.description_detail);
	        txtDescription.setText(poi.getDescription());
	                
	        txtWebsite = (TextView) findViewById(R.id.url_detail);
	        txtWebsite.setText(poi.getWebsite());
	        if (!poi.getWebsite().equals("")){
	        	txtWebsite.setVisibility(View.VISIBLE);
	        	txtWebsite.setMovementMethod(LinkMovementMethod.getInstance());
	        } else {
	        	txtWebsite.setVisibility(View.GONE);
	        }
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
	
	private void setPaddingBottom(int height){
		height = (int) Math.ceil(height * _logicalDensity);
		if (_mMap != null) {
			_mMap.setPadding(5, 5, 5, height+5);
		}
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)_buttonsContainer.getLayoutParams();
	    params.setMargins(10, 10, 10, height+10); 
	    _buttonsContainer.setLayoutParams(params);
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
		savedInstanceState.putParcelable(TagNames.EXTRA_POI, _selectedPoi);		 

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

	private void callForLocationUpdates(int gpsInterval, float gpsDistanceChange, int networkInterval, float networkDistanceChange){
		if(_locUpdateService != null){
			_locUpdateService.stopLocationUpdates();
			_locUpdateService.updateLocation(gpsInterval, gpsDistanceChange, networkInterval, networkDistanceChange );
		}
	}
	
	/**
	 * Manage connection to LocationUpdateService
	 */
	private ServiceConnection _mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			//boolean to see whether the location is pulled for the first time after service reconnect or not
			_firstLocationPull = true;
			LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
			_locUpdateService = b.getLocService();
			//Toast.makeText(ActivityMap.this, "LocService Connected", Toast.LENGTH_SHORT).show();
			Location loc = _locUpdateService.getCurrentUserLocation();
			Log.d("##### userLocation", ""+_userLat + ", "+ _userLng);
			//beim ersten mal öfter location updates fordern
			if(_locUpdateService.isProviderEnabled(LocationManager.GPS_PROVIDER) || _locUpdateService.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
				loc = _locUpdateService.getLastKnownLocation();
			}
			if(_userLocation.getLatitude() == AppController.getInstance().getStandardLocation().getLatitude() && _userLocation.getLongitude() == AppController.getInstance().getStandardLocation().getLongitude() ) {
				callForLocationUpdates(60000, 3.0f, 5000, 1.0f );
				Log.d("******onServiceConnected", "updateLocation call, HIGH update interval");
				_instance._isLowUpdateInterval = false;
			}else{
				Log.d("******onServiceConnected", "updateLocation call, LOW update interval");
				//request new location updates less frequently
				callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
				_instance._isLowUpdateInterval = true;
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			_locUpdateService.stopLocationUpdates();
			_locUpdateService = null;
			//Toast.makeText(ActivityMap.this, "LocService Disconnected", Toast.LENGTH_SHORT).show();
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
		builder.setMessage("Dein GPS ist ausgestellt, möchtest du es jetzt anstellen?")
		.setCancelable(false)
		.setPositiveButton("Ja", onOkListener)
		.setNegativeButton("Nein", onCancelListener);
		final AlertDialog alert = builder.create();
		alert.show();
		//TODO: check if user has turned location services on (onActivityResult)
	}
	
	//overwrite back key behavior
	@Override
	public void onBackPressed() {		
	   if(_slidingUpPanel.isPanelHidden()){
		   ActivityMap.super.onBackPressed();
	   } else {
		   _slidingUpPanel.hidePanel();
		   _buttonsContainer.setVisibility(View.VISIBLE);
	   }
	}

}