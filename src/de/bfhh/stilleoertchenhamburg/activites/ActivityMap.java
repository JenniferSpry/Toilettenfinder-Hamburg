package de.bfhh.stilleoertchenhamburg.activites;

import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.w3c.dom.Document;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.helpers.GoogleDirection.OnDirectionResponseListener;
import de.bfhh.stilleoertchenhamburg.helpers.POIHelper;
import de.bfhh.stilleoertchenhamburg.helpers.GoogleDirection;
import de.bfhh.stilleoertchenhamburg.models.POI;
import de.bfhh.stilleoertchenhamburg.services.SendMailService;

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.Visibility;
import android.os.AsyncTask;
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
 * 1. AUf Leaks testen
 * 2. ActivityResult von GPS check zurückgeben
 */

public class ActivityMap extends ActivityMenuBase {

	private final String TAG = ActivityMap.class.getSimpleName();
	
	//Heights and paddings in dp
	private final int PANEL_HEIGHT = 110;
	private final int MAP_PADDING = 5;
	private final int BUTTONS_PADDING = 10;

	//Google map instance
	private GoogleMap _mMap;
	//Visible mapBounds
	private LatLngBounds _mapBounds; //bounds of visible map, updated onCameraChange
	private LatLngBounds.Builder _builder;
	
	//Buttons on map
	private RelativeLayout _buttonsContainer; //Zoom and my location button
	private ImageButton _myLocationButton; //Button to get back to current user location on map
	private ImageButton _zoomInButton;
	private ImageButton _zoomOutButton;
	
	private Button _routeText; //show route as text button
	private Button _routeMap; //show route on map as polyline
	private GoogleDirection _gd;//Direction class from https://github.com/akexorcist/Android-GoogleDirectionAndPlaceLibrary/blob/master/library/src/main/java/app/akexorcist/gdaplibrary/GoogleDirection.java
	private Polyline _direction; //Polyline on map representing direction
	
	private Button _buttonSendComment;
	
	//Slider plus content
	private SlidingUpPanelLayout _slidingUpPanel;
	TextView txtName;   
    TextView txtDistance;    
    TextView txtAddress;   
    TextView txtDescription;            
    TextView txtWebsite;
    
    //Marker maps and user marker
	private Marker _personInNeedOfToilette; //person's marker
	private HashMap<Integer, Marker> _markerMap;//
	private HashMap<String, Integer> _markerPOIIdMap; // marker ids mapped to poi ids
	//List of all POI in bfhh database
	private ArrayList<POI> _allPOIList; //POI received from POIUpdateService
	
	//User location
	private Location _userLocation;
	private double _userLat;
	private double _userLng;
	private boolean _hasUserLocation;

	//Location Receiver and service
	private LocationUpdateService _locUpdateService;
	private LocationUpdateReceiver _locUpdReceiver;
	private boolean _locUpdReceiverRegistered;
	public boolean _isLowUpdateInterval; //location updates less often
	protected boolean _firstLocationPull; //first time location was received?

	//Display dimensions
	private int _displayHeight;
	private float _logicalDensity;

	//User selected POI (by Pin click or list item click)
	private POI _selectedPoi;
	protected Marker _selectedMarker;

	//Display orientation
	private int orientation;

	protected boolean _showRoute;

	/**
	 * Initiate variables
	 * fetch bundle contents
	 * register receivers
	 * 
	 * Check google play store availability before anything else
	 * on first opening go to user location and draw POI markers
	 */
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.currentThread().setContextClassLoader(ActivityMap.class.getClassLoader());
		Log.d(TAG, "onCreate");
		//Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show();
		
		setContentView(R.layout.activity_main);
		
		//Display size in pixel
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			_displayHeight = display.getHeight();
		} else {
			display.getSize(size);
			_displayHeight = size.y;
		}
		//get display density
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		_logicalDensity = metrics.density;

		//get view Elements
		_buttonsContainer = (RelativeLayout) findViewById(R.id.mapButtons);
		_myLocationButton = (ImageButton) findViewById(R.id.buttonToLocation); 
		_zoomInButton = (ImageButton) findViewById(R.id.buttonZoomIn); 
		_zoomOutButton = (ImageButton) findViewById(R.id.buttonZoomOut);
		
		_routeText = (Button) findViewById(R.id.route_text);
		_routeMap = (Button) findViewById(R.id.route_map);
		_showRoute = false; //determines whether other markers apart from user and destination should be shown (false -> yes, true -> no)
		
		_buttonSendComment = (Button) findViewById(R.id.button_send_comment);
		
		//if landscape mode, hide zoom buttons entirely
		orientation = getResources().getConfiguration().orientation;
		adjustZoomVisibility(orientation);
		
		_slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		_slidingUpPanel.setAnchorPoint(0.45f);
		_slidingUpPanel.setOverlayed(true);
		_slidingUpPanel.setPanelHeight((int) Math.ceil(PANEL_HEIGHT * _logicalDensity));//dp to px
		_slidingUpPanel.hidePanel();

		//LocationUpdateReceiver
		_locUpdReceiver = new LocationUpdateReceiver(new Handler());
		_locUpdReceiver.setMainActivityHandler(this);
		_isLowUpdateInterval = false;

		//list of POI markers that are currently visible on the map
		_markerMap = new HashMap<Integer, Marker>();
		_markerPOIIdMap = new HashMap<String, Integer>();
		
		//Get bundle contents
		Bundle bundle = null;
		CameraPosition cameraPosition = null;
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
			//poi selected by user 
			_selectedPoi = bundle.getParcelable(TagNames.EXTRA_POI); // may be null
			cameraPosition = bundle.getParcelable(TagNames.EXTRA_CAMERA_POS); // may be null
			_showRoute = bundle.getBoolean(TagNames.EXTRA_SHOW_ROUTE);
		}   

		if(_allPOIList != null){
			_allPOIList = POIHelper.setDistancePOIToUser(_allPOIList, _userLat, _userLng);
		} else {
			Log.d("MainActivity.oncreate():", "_allPOIList is null");
		}
		
		// set map related things
		if (checkPlayServices()){

			setUpMapIfNeeded();
			
			if (cameraPosition != null) {
				moveToLocation(CameraUpdateFactory.newCameraPosition(cameraPosition));
				_mMap.getUiSettings().setZoomControlsEnabled(false);
			} else {
				moveToLocation(getCameraUpdateForClosestPOIBounds(10));
			}
			
			updatePOIMarkers();
			if (_selectedPoi != null){
				updateSliderContent(_selectedPoi);
				selectMarker(_selectedPoi);
			}
			
			setPeeerOnMap();

			_mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition pos) {
					if(_mMap != null){
						refreshMapBounds();
						//only refresh markers on map if no route is shown
						if(!_showRoute){
							updatePOIMarkers();
						}
						Log.d(TAG + " camera changed to pos", pos.toString());
					}
				}			
			});

			_mMap.setOnMapClickListener(new OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					//hide slider and set formerly selected marker back to normal
					_slidingUpPanel.hidePanel();
					if (_selectedMarker != null){
						_selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getIcon()));
					}
					_selectedMarker = null;
					_selectedPoi = null;
					//remove polyline if it's set
					if(_direction != null){
						_direction.remove();
						_direction = null;
					}
					if(_zoomInButton.getVisibility() == View.INVISIBLE && _zoomOutButton.getVisibility() == View.INVISIBLE){
						_zoomInButton.setVisibility(View.VISIBLE);
						_zoomOutButton.setVisibility(View.VISIBLE);
						showMarkersExceptUserAndDestination();
						_showRoute = false;
					}
				}
			});
			
			//the slidingUpPanel is shown if the user clicks on a marker
			_mMap.setOnMarkerClickListener(new OnMarkerClickListener(){
				@Override
				public boolean onMarkerClick(Marker marker) { 
					//if the buttonsContainer was hidden, show it again
					if(_zoomInButton.getVisibility() == View.INVISIBLE && _zoomOutButton.getVisibility() == View.INVISIBLE
							&& _selectedMarker != marker){
						_zoomInButton.setVisibility(View.VISIBLE);
						_zoomOutButton.setVisibility(View.VISIBLE);
						showMarkersExceptUserAndDestination();
						_showRoute = false;
					}
					//remove polyline if it's set
					if(_direction != null){
						_direction.remove();
						_direction = null;
					}
					
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
					
					//center map on clicked marker
					refreshMapBounds();	
					LatLng markerLatLng = marker.getPosition();	
					moveToLocation(CameraUpdateFactory.newLatLng(markerLatLng));
					return true;
				}
			});

			//Location Button Listener
			_myLocationButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					//Only move to user location if it isn't the standardLocation
					if (_hasUserLocation){
						if(_showRoute){ //show user and selected marker
							showUserLocationAndDestination();
						}else{
							moveToLocation(getCameraUpdateForClosestPOIBounds(10));
							updatePOIMarkers();
							showMarkersExceptUserAndDestination();	
						}
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
			
			
			_gd = new GoogleDirection(this);
			//Called when we get a response with the direction from google
	        _gd.setOnDirectionResponseListener(new OnDirectionResponseListener() {
	        	@Override
	        	public void onResponse(String status, Document doc, GoogleDirection gd) {	
					Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
					
					showUserLocationAndDestination();
					
					//update slider top to show start and destination plus distance in meters
					updateSliderContentRoute(gd, doc);
					
					//remove directions polyline if it's already there
					if(_direction != null){
						_direction.remove();
					}
					//draw route
					_direction = _mMap.addPolyline(gd.getPolyline(doc, 6, Color.rgb(105,127,188)));

					_showRoute = true;
					_zoomInButton.setVisibility(View.INVISIBLE);
					_zoomOutButton.setVisibility(View.INVISIBLE);
					hideMarkersExceptUserAndDestination();
	        	}

			});
	        	
			_routeText.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String requestUrl = _gd.request(new LatLng(_userLat, _userLng), _selectedMarker.getPosition(), GoogleDirection.MODE_WALKING);
					_slidingUpPanel.expandPanel();
				}
			});
			
			_routeMap.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					_slidingUpPanel.collapsePanel();
					//request direction from userPosition to selectedMarker position
					String requestUrl = _gd.request(new LatLng(_userLat, _userLng), _selectedMarker.getPosition(), GoogleDirection.MODE_WALKING);
				}
			});
			
			// TODO
			_buttonSendComment.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (_selectedPoi != null) {
						String emailText = ((EditText) findViewById(R.id.email_field)).getText().toString();
						String nameText = ((EditText) findViewById(R.id.name_field)).getText().toString();
						String commentText = ((EditText) findViewById(R.id.comment_field)).getText().toString();
						
						if (commentText != null && !commentText.equals("")){
							Intent i = new Intent(ActivityMap.this, SendMailService.class);
					        i.putExtra(TagNames.EXTRA_NAME, nameText);
					        i.putExtra(TagNames.EXTRA_E_MAIL, emailText);
					  	  	i.putExtra(TagNames.EXTRA_COMMENT, commentText);
					  	  	i.putExtra(TagNames.EXTRA_POI, _selectedPoi);
					  	    ActivityMap.this.startService(i);
						} else {
							Toast.makeText(getApplicationContext(), "Bitte füllen Sie das Kommentar-Feld aus.", Toast.LENGTH_LONG).show();
						}
					}
				}
			});
			
			_slidingUpPanel.setPanelSlideListener(new PanelSlideListener(){
				private boolean firstCollapse = true;
				private boolean firstAnchored = true;

				@Override
				public void onPanelSlide(View panel, float slideOffset) {
					/*if the panel is slid within close vicinity of the anchorPoint 
					 * (at 0.75f), expand the panel to that anchorPoint (for some reason
					 * this doesn't work by only setting slidingUpPanel.setAnchorPoint(0.75f);)
					 */
					// TODO: noch mal testen, bei JS geht's auch ohne diesen code
					if( Math.abs(slideOffset - 0.45f) < 0.02f ) {
						_slidingUpPanel.expandPanel(0.45f);
					}
				}

				@Override
				public void onPanelCollapsed(View panel) {
					refreshMapBounds();
					adjustLayoutToPanel();
					//clickedMarker may be null
					if(_selectedMarker == null && _selectedPoi != null && _markerMap != null){
						_selectedMarker = _markerMap.get(_selectedPoi.getId());
					}
					if(_selectedMarker != null){
						//get distance between map center (gmaps) and clicked marker position
						float d = getDistanceBewteen(getMapCenter(_mapBounds), _selectedMarker.getPosition());
						if(!_showRoute){ //only center on selected marker if no route is shown
							moveToLocation(CameraUpdateFactory.newLatLng(_selectedMarker.getPosition()));
						}else{
							showUserLocationAndDestination();
						}
					}
					firstCollapse = false;
					firstAnchored = false;
					
				}

				@Override
				public void onPanelExpanded(View panel) {}

				@Override
				public void onPanelAnchored(View panel) {
					adjustLayoutToPanel();
					//move the map up so that clicked marker is visible
					if(!firstAnchored ){	
						if(_selectedMarker == null && _selectedPoi != null && _markerMap != null){
							_selectedMarker = _markerMap.get(_selectedPoi.getId());
						}
						if(_selectedMarker != null){
							LatLng markerPos = _selectedMarker.getPosition();
							//move position to center map to down a bit
							if(!_showRoute){ //only center on selected marker if no route is shown
								//LatLng mPlusOffset = new LatLng(markerPos.latitude, markerPos.longitude);
								moveToLocation(CameraUpdateFactory.newLatLng(_selectedMarker.getPosition()));
							}else{
								showUserLocationAndDestination();
							}

						}
						firstAnchored = true;
					}
					
				}

				@Override
				public void onPanelHidden(View panel) {
					firstCollapse = true;
					firstAnchored = true;
					adjustLayoutToPanel();
				}    	
			});			
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
			_selectedMarker = _markerMap.get(_selectedPoi.getId());
			//if route was shown, then list view opened and list item clicked 
			//-> go back to map but don't show route and restore all other markers
			if(_showRoute && _direction != null){
				_showRoute = false;
				_direction.remove();
				_direction = null;
				showMarkersExceptUserAndDestination();
			}
			if ((_selectedPoi != null) && (_mMap != null)) {
				moveToLocation(CameraUpdateFactory.newLatLng(_selectedPoi.getLatLng()));
				updateSliderContent(_selectedPoi);
				selectMarker(_selectedPoi);
				_slidingUpPanel.showPanel();
			}
			moveToLocation(CameraUpdateFactory.newLatLng(_selectedMarker.getPosition()));
		} else {
			if ((_selectedPoi != null) && (_mMap != null)) {
				selectMarker(_selectedPoi);
			}
		}
	}
	
	/**
	 * use this method to create or highlight the marker of a poi
	 */
	private void selectMarker(POI poi){
		Marker marker = null;
		//marker not on map (not one of the closest ten markers?)
		if(!_markerMap.containsKey(poi.getId())){
			//add marker to mMap and to lists
			marker = _mMap.addMarker(POIHelper.getMarkerOptionsForPOI(poi, true));	
			_markerMap.put(poi.getId(), marker);
			_markerPOIIdMap.put(marker.getId(), poi.getId());
		} else { //marker already set
			marker = _markerMap.get(poi.getId());
			marker.setIcon(BitmapDescriptorFactory.fromResource(poi.getActiveIcon()));
		}
		_selectedMarker = marker;
	}
	
	/**
	 * Check google play store availability before resuming
	 */
	@Override
	protected void onResume() {
		super.onResume();
		//Toast.makeText(this, "onResume", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onResume");

		if (checkPlayServices()){
			// register/bind recievers
			if(!_locUpdReceiverRegistered){
				registerReceiver(_locUpdReceiver, new IntentFilter(TagNames.BROADC_LOCATION_UPDATED));
				_locUpdReceiverRegistered = true;
			}
			Intent intent= new Intent(this, LocationUpdateService.class);
			bindService(intent, _mConnection,  Context.BIND_AUTO_CREATE);
			Log.d("Map onResume", "Service bound");	
			
			adjustLayoutToPanel(); // needed here because of strange slider behavior			
						
			//check whether Activity was started by onclick in List, if so _clickedPoi is set (see onNewIntent())
            if(_selectedPoi != null){                
                if (_mMap != null){
                	 if(!_showRoute){
                     	_mMap.animateCamera(CameraUpdateFactory.newLatLng(_selectedPoi.getLatLng()));
                     	updateSliderContent(_selectedPoi);
                	 }
    				selectMarker(_selectedPoi);
                    //set distance to user, otherwise slider will show 0 m
                    if(_userLat != 0.0d && _userLng != 0.0d){
                    	_selectedPoi = POIHelper.setDistanceSinglePOIToUser(_selectedPoi, _userLat, _userLng);
                    }
                    //center camera on marker if route was not shown before (-> _showRoute = false)
                   
                    //_mMap.animateCamera(CameraUpdateFactory.newLatLng(_selectedPoi.getLatLng()));
                    //updateSliderContent(_selectedPoi);
                    Marker marker = null;
                    //marker not on map 
                    if(!_markerMap.containsKey(_selectedPoi.getId())){
                        Log.d("3454564653w4r onResume _clickedPOI", "adding marker to map and showing panel");
                        //add marker to mMap and to lists
                        marker = _mMap.addMarker(POIHelper.getMarkerOptionsForPOI(_selectedPoi, true));    
                        _markerMap.put(_selectedPoi.getId(), marker);
                        _markerPOIIdMap.put(marker.getId(), _selectedPoi.getId());
                        _slidingUpPanel.showPanel();
                    }else{ //marker already set
                        Log.d("{{{{{{7{ onResume _clickedPOI", "marker already on map");
                        marker = _markerMap.get(_selectedPoi.getId());
                        _selectedMarker = marker;
                    }
                    
                }
            }else{//selectedPoi = null
            	_slidingUpPanel.hidePanel();
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
			Log.d("ActivityMap", "onReceive location");
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
					main._hasUserLocation =  result == -1;

					//get previous user Location (might be standardLocation)
					Location oldLocation = new Location("");
					oldLocation.setLatitude(main._userLat);
					oldLocation.setLongitude(main._userLng);

					Log.d("_hasUserLocation =", ""+ main._hasUserLocation);
					Log.d("userlocation 1", ""+ main._userLocation.getLatitude());
					
					//update the user location
					main.setUserLocation(lat, lng);
					main._allPOIList = POIHelper.setDistancePOIToUser(main._allPOIList, lat, lng);
					
					Log.d("userlocation 2", ""+main._userLocation.getLatitude());
					//TODO: Testing
					//old userLocation == standardLocation and distance to newly received location is greater 10m -> move camera
					if(main._hasUserLocation){					
						if(main._userLocation.distanceTo(oldLocation) < 10.0 ){
							if(!main._isLowUpdateInterval){//high update interval
								Log.d("-------- ", "lower update interval frequency");
								main.callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
								main._isLowUpdateInterval = true;
							}
						}else{//distance > 10 meters (because user moved or because old loc is standard loc)
							Log.d("======= distance to old loc", ""+main._userLocation.distanceTo(oldLocation));
							
							Location s = AppController.getInstance().getStandardLocation();
							//oldLocation == standardLocation -> we have jumped quite far
							if(oldLocation.getLatitude() == s.getLatitude() && oldLocation.getLongitude() == s.getLongitude()){
								Log.d("-------- ", "lower update interval frequency");
								main.callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
								main._isLowUpdateInterval = true;
								Toast.makeText(context, "Position wird bestimmt...",
										Toast.LENGTH_LONG).show();
								main.moveToLocation(main.getCameraUpdateForClosestPOIBounds(10));
								main.updatePOIMarkers();
								main.setPeeerOnMap();
							}else if(main._userLocation.distanceTo(oldLocation) > 84.0 && main._isLowUpdateInterval){
								Log.d("+++++++++ ", "higher update interval frequency");
								main.callForLocationUpdates(60000, 3.0f, 5000, 1.0f );
								main._isLowUpdateInterval = false;
								main.moveToLocation(main.getCameraUpdateForClosestPOIBounds(10));
								main.updatePOIMarkers();
								main.setPeeerOnMap();
							}
							//redraw route if it's set
							if(main._showRoute){
								if(main._gd == null){
									main._gd = new GoogleDirection(context);
								}
								if(main._selectedMarker != null){
									String requestUrl = main._gd.request(new LatLng(main._userLat, main._userLng), main._selectedMarker.getPosition(), GoogleDirection.MODE_WALKING);
								}
							}
						}
					}    
				}
			}        
		}
	}//end LocationUpdateReceiver

	
	private void showUserLocationAndDestination(){
		if(_selectedMarker != null && _mMap != null){
			LatLngBounds bounds = new LatLngBounds.Builder()
	    	.include(new LatLng(_userLat,_userLng))
	    	.include(_selectedMarker.getPosition())
	    	.build();
		_mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
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
		}
		refreshMapBounds();
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
		Log.d(TAG, "getCameraUpdateForClosestPOIBounds");
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
						Marker marker = _mMap.addMarker(POIHelper.getMarkerOptionsForPOI(poi, false));
						
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
	
	private void hideMarkersExceptUserAndDestination(){
		if(_mMap != null){
			//Loop through all poi
			for(POI poi : _allPOIList){
				if(_markerMap.containsKey(poi.getId())){
					//poi is not destination (or user)
					if(poi.getId() != _selectedPoi.getId()){
						_markerMap.get(poi.getId()).setVisible(false);
						//_markerMap.remove(poi.getId());
					}
				}
			}
		}
	}
	
	private void showMarkersExceptUserAndDestination(){
		if(_mMap != null){
			//Loop through all poi
			for(POI poi : _allPOIList){
				if(_markerMap.containsKey(poi.getId())){
					_markerMap.get(poi.getId()).setVisible(true);
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
	        
	        float d = poi.getDistanceToUser();
	        NumberFormat numberFormat = new DecimalFormat("0.0");
			numberFormat.setRoundingMode(RoundingMode.DOWN);
			//show distance rounded in kilometers if greater than 999 meters
			String distance = d > 999 ? String.valueOf(numberFormat.format(d*0.001)) + " km" : String.valueOf((int) Math.round(d)) + " m";  
	        //String distance = String.valueOf(d) + " m";  
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
	
	/*
	 * Updates the slider with data about the route direction.
	 */
	protected void updateSliderContentRoute(GoogleDirection gd, Document doc){
		//set duration and distance from user to destination
		int d = gd.getTotalDistanceValue(doc);
		String duration = gd.getTotalDurationText(doc).replace("Minuten", "min").replace("Stunden", "h").replace("Stunde", "h");
		
		NumberFormat numberFormat = new DecimalFormat("0.0");
		numberFormat.setRoundingMode(RoundingMode.DOWN);
		//show distance rounded in kilometers if greater than 999 meters
		String distance = d > 999 ? String.valueOf(numberFormat.format(d*0.001)) + " km" : String.valueOf(d) + " m";  
        txtDistance = (TextView) findViewById(R.id.distance_detail);
        txtDistance.setText(duration.trim() + "\n");
        txtDistance.append(Html.fromHtml("(" + distance + ")"));
       
        //change name to start and destination, like: "Von: Aktuelle Position \n Nach: xxx"
        txtName = (TextView) findViewById(R.id.name_detail);
        txtName.setText("Von:   Aktuelle Position" + " \nNach: " +  gd.getEndAddress(doc));
		//delete address field contents
        txtAddress = (TextView) findViewById(R.id.address_detail);
        txtAddress.setText("");
        
        //get the text direction instructions and distance values
        ArrayList<String> htmlInstructions = gd.getHTMLInstructions(doc);
        int[] distances = gd.getDistanceValue(doc);

        txtDescription = (TextView) findViewById(R.id.description_detail);
        //Set scrollView back to top after textView contents have changed, so that user can see start of direction description
        //Doesn't work the first time
        txtDescription.addTextChangedListener(new TextWatcher() {	
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				//get ScrollView to scroll to top
		      	ScrollView scrollView = (ScrollView) findViewById(R.id.scroll);
		      	scrollView.scrollTo(0,0);
			}
		});
        
        txtDescription.setText(Html.fromHtml("<b>Wegbeschreibung: </b> <br><br>"));
        for(int i = 0; i < htmlInstructions.size(); i++){
        	if(i > 0){
        		txtDescription.append(Html.fromHtml("<b>Nach " + String.valueOf(distances[i-1]) + " m: </b>"));
        	}
        	if(i < htmlInstructions.size()-1){
        		txtDescription.append(htmlInstructions.get(i) + ".\n");
        	}else{
        		txtDescription.append(htmlInstructions.get(i).trim());
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
	
	/**
	 * change padding on map and the button containing layout
	 * depending on the state of the slidingUpPanel
	 */
	private void adjustLayoutToPanel(){
		int height = 0;
		if (_slidingUpPanel.isPanelAnchored()){
			height = (int) (_displayHeight/2);//height in pixels
			_buttonsContainer.setVisibility(View.INVISIBLE);
		} else if (!_slidingUpPanel.isPanelHidden()){
			height = PANEL_HEIGHT;
			_buttonsContainer.setVisibility(View.VISIBLE);
			//NOTE: this is to convert dp to pixels
			height = (int) Math.ceil(height * _logicalDensity);
		}else if(_slidingUpPanel.isPanelHidden()){
			_buttonsContainer.setVisibility(View.VISIBLE);
		}
		
		if (_mMap != null) {
			_mMap.setPadding(MAP_PADDING, MAP_PADDING, MAP_PADDING, height+MAP_PADDING);
		}
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)_buttonsContainer.getLayoutParams();
	    params.setMargins(BUTTONS_PADDING, BUTTONS_PADDING, BUTTONS_PADDING, height+BUTTONS_PADDING); 
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
		savedInstanceState.putParcelable(TagNames.EXTRA_CAMERA_POS, _mMap.getCameraPosition());
		savedInstanceState.putBoolean(TagNames.EXTRA_SHOW_ROUTE, _showRoute);
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause(){
		super.onPause();
		Log.d(TAG, "onPause");
		//NOTE: if service is not set to null here onLocationChanged() is called twice in a row
		_locUpdateService = null;

		unbindService(_mConnection);//unbind service
		Log.d("Map onPause", "service unbound");
		
		if(_locUpdReceiverRegistered){
			unregisterReceiver(_locUpdReceiver);
			_locUpdReceiverRegistered = false;
		}
	}

	@Override
	protected void onStop(){
		super.onStop();
		Log.d(TAG, "onStop");
		if(_locUpdReceiverRegistered){
			unregisterReceiver(_locUpdReceiver);
			_locUpdReceiverRegistered = false;
		}
		if (_selectedMarker != null & _selectedPoi != null) {
			_selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getIcon()));
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
		// hide sliding panel
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
				_isLowUpdateInterval = false;
			}else{
				Log.d("******onServiceConnected", "updateLocation call, LOW update interval");
				//request new location updates less frequently
				callForLocationUpdates(120000, 8.0f, 60000, 5.0f );
				_isLowUpdateInterval = true;
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
	   if(_slidingUpPanel.isPanelHidden() || _selectedPoi == null){
		   ActivityMap.super.onBackPressed();
	   } else {
		   //hide slider and put selected pin back to normal
		   _slidingUpPanel.hidePanel();
		   	if (_selectedMarker != null){
				_selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(_selectedPoi.getIcon()));
			}
			_selectedMarker = null;
			_selectedPoi = null;
	   }
	}
	
	private void adjustZoomVisibility(int orientation){
		if(orientation == Configuration.ORIENTATION_LANDSCAPE){
			_zoomInButton.setVisibility(View.GONE);
			_zoomOutButton.setVisibility(View.GONE);
		}else{
			_zoomInButton.setVisibility(View.VISIBLE);
			_zoomOutButton.setVisibility(View.VISIBLE);
		}
	}


}