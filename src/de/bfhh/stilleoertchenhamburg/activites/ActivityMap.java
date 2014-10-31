package de.bfhh.stilleoertchenhamburg.activites;

import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.R.drawable;
import de.bfhh.stilleoertchenhamburg.R.id;
import de.bfhh.stilleoertchenhamburg.R.layout;
import de.bfhh.stilleoertchenhamburg.models.POI;

import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;


/*
 * This activity is started by POIUpdateService after the IntentService 
 * has finished its task of retrieving a list of POI from the bf-hh server via JSON.
 * This list is also sent to this activity via the starting Intent, as well as
 * the user's current position (retrieved by LocationUpdateService).
 * 
 * 
 */

public class ActivityMap extends ActivityMenuBase {

    private static GoogleMap mMap;
    private static Location userLocation;
    private TextView latitude;
    private TextView longitude;
    
    private static POIController poiController; //class that handles Broadcast, methods return List<POI>
    
    private ImageButton myLocationButton; //Button to get back to current user location on map
    
    final private LatLng HAMBURG = new LatLng(53.558, 9.927); //standard position in HH
    
    private static Marker personInNeedOfToilette; //person's marker
	private ArrayList<HashMap<String, String>> toiletList; //POI received from POIUpdateService
	private static ArrayList<MarkerOptions> markerList;
	
	private ArrayList<String> keyNames;
	
	private static double userLat;
	private static double userLng;
	
	private LatLngBounds.Builder builder;
	private POIReceiver poiReceiver;
	private LocationUpdateReceiver locUpdReceiver;
	private boolean poiReceiverRegistered;
	private boolean locUpdReceiverRegistered;
	
	private LocationUpdateService service;
	
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
        	Bundle bundle = intent.getExtras();
            String action = intent.getAction();
            if(action.equals("toiletLocation")){//action sent by POIUpdateService
            	
            //TODO: How to check whether this typecast worked?
          	  //get POI List
            ArrayList<HashMap<String, String>> poiList = (ArrayList<HashMap<String,String>>) intent.getSerializableExtra("poiList");
          	  if( poiList != null && bundle.getInt(POIUpdateService.RESULT) == RESULT_OK ){
          		  //create poiController Object
          		  final POIController pc = new POIController(poiList);
          		  main.setPOIController(pc);
          		  
          		  // Post the UI updating code to our Handler
                  handler.post(new Runnable() {
                      @Override
                      public void run() {
                    	  //TODO see if userLat and userLng are set
                         updateMarkers(pc, userLat, userLng);
                      }
                  });
          	  }
            }        
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
            if(action.equals("LocationUpdate")){
            	if(bundle != null){
            		double lat = bundle.getDouble("latitude");
            		double lng = bundle.getDouble("longitude");
            		Log.d("ActivityMap BroadcastReceiver", "Location received: " + lat  + ", " + lng);
            		//update the user location
            		setUserLocation(lat, lng );
            		//the user location has changed, so that there might be different closest ten POI, retrieve them
            		updateUserAndPOIOnMap(lat, lng);
            		String provider = bundle.getString("provider");
            		//Toast to show updates
            		Toast.makeText(context, "Location Update from provider: " + provider + ", Location: LAT " + lat + ", LNG " + lng,
            				Toast.LENGTH_LONG).show();
            	}
            }        
        }
    }
    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        //Button that will animate camera back to user position
        myLocationButton = (ImageButton) findViewById(R.id.mylocation);
        
        //TODO: is it good to register receiver in oncreate() ??
        //Register Receiver for POI Updates
        poiReceiver = new POIReceiver(new Handler());
        poiReceiver.setMainActivityHandler(this);
        registerReceiver(poiReceiver, new IntentFilter("toiletLocation"));
        poiReceiverRegistered = true;
        
        //LocUpdateReceiver
        locUpdReceiver = new LocationUpdateReceiver(new Handler());
        locUpdReceiver.setMainActivityHandler(this);
        registerReceiver(locUpdReceiver, new IntentFilter("LocationUpdate"));
        locUpdReceiverRegistered = true;
        
        //Get the Intent that was sent from ActivitySplashScreen
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        if(bundle != null){
        	if(bundle.getDouble("latitude") != 0.0 && bundle.getDouble("longitude") != 0.0){
        		//set user Position
            	setUserLocation(bundle.getDouble("latitude"), bundle.getDouble("longitude"));
        	}
        	
            int result = bundle.getInt("result");
            if(result == Activity.RESULT_CANCELED){
            	Log.d("MainActivity:", "Activity.RESULT_CANCELED: standard lat and lng");
            }
            //get the toiletList
            toiletList = (ArrayList<HashMap<String,String>>) i.getSerializableExtra("poiList");
            
            //set up POIController with list of toilets received from POIUpdateService
            setPOIController(new POIController(toiletList));
            
            //set up Google Map with current user position
            setUpMapIfNeeded();
            
            //set/update map markers
            updateMarkers(poiController, userLat, userLng);
        }
        
        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                // Move camera.
            	CameraUpdate cu = getBoundsOnMap();
            	mMap.moveCamera(cu);
                // Remove listener to prevent position reset on camera move.
            	mMap.setOnCameraChangeListener(null);
            }

        });
		
		//Location Button Listener
		myLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//If the location is not west of Africa, chances are, it is right.
            	//So move camera to it.
            	if( userLocation.getLatitude() != 0.0  &&  userLocation.getLongitude() != 0.0 ){
            		moveToLocation(userLocation);
            	}
            }
        });
		
		//TODO: put the settings action check somwhere else (LocationUpdateService)
		if (userLocation != null) {
			//mylistener.onLocationChanged(userLocation);
		} else {
			// leads to the settings because there is no last known location
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
    }
    
    //send lat,lng to poiController
    public static void updateUserAndPOIOnMap(double lat, double lng) {
		// update Markers first
		updateMarkers(poiController, lat, lng);
		//set user position TODO: method out of that
    	personInNeedOfToilette.setPosition(new LatLng(lat, lng));
	}

	private void moveToLocation(Location loc){
    	LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
    	personInNeedOfToilette.setPosition(pos);
    	//somehow set zoom dynamically, so that all toilets are seen
    	CameraUpdate cu = getBoundsOnMap();
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
    	                CameraUpdate c = getBoundsOnMap();
    	                mMap.animateCamera(c);
    	            }
    	        });
    	    }
    	}
    	
        
    }
    
    private CameraUpdate getBoundsOnMap(){
    	builder = new LatLngBounds.Builder();
        for (MarkerOptions m : markerList) {
            builder.include(m.getPosition());
        }
        //also add peeer, in case there is only toilets on one side, 
        //he / she doesn't get left out of the map view :)
        builder.include(personInNeedOfToilette.getPosition());
        LatLngBounds bounds = builder.build();

        return CameraUpdateFactory.newLatLngBounds(bounds,
                30);
    }
    
    @Override
    protected void onStart(){
    	super.onStart();
    	if(!poiReceiverRegistered){
    		registerReceiver(poiReceiver, new IntentFilter("toiletLocation"));
    		poiReceiverRegistered = true;
    	}
    	if(!locUpdReceiverRegistered){
    		registerReceiver(locUpdReceiver, new IntentFilter("LocationUpdate"));
    		locUpdReceiverRegistered = true;
    	}
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(!poiReceiverRegistered){
    		registerReceiver(poiReceiver, new IntentFilter("toiletLocation"));
    		poiReceiverRegistered = true;
    	} 
        if(!locUpdReceiverRegistered){
    		registerReceiver(locUpdReceiver, new IntentFilter("LocationUpdate"));
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
    	unbindService(mConnection);//unbind service
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
    
    // Update / set markers on Google Map
    private static void updateMarkers(POIController poiController, double currLat, double currLng){
    	List<POI> closestTen = new ArrayList<POI>(); //list with closest ten POI to user position
    	List<POI> allPOI = new ArrayList<POI>();
    	markerList = new ArrayList<MarkerOptions>(); //markers for those POI
    	//check whether the broadcast was received
    	if(poiController != null ){
	        if(poiController.poiReceived() )
	        {
	        	//propagate the user position to all POI and set their distance to user in meters
	        	poiController.setDistancePOIToUser(currLat, currLng);
	        	//get closest ten POI
	        	closestTen = poiController.getClosestTenPOI();
	        	//get all POI
	        	allPOI = poiController.getAllPOI();
	        	
	        	//add markers to the map  for the ten closest POI
	        	for(int i = 0; i < closestTen.size(); i++){
	        		MarkerOptions marker = new MarkerOptions();
	        		POI poi = closestTen.get(i);
	        		marker.position(new LatLng(poi.getLat(), poi.getLng())).title(poi.getName());
	                // adding marker
	                mMap.addMarker(marker);
	                markerList.add(marker); 
	        	}
	        }
	        else
	        {
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
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
    	// need to turn this off so we can use our own icon
        //mMap.setMyLocationEnabled(true); 
    	personInNeedOfToilette = mMap.addMarker(new MarkerOptions()
	    	.position(new LatLng(userLat, userLng))
	    	.icon(BitmapDescriptorFactory.fromResource(R.drawable.peeer)));
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, 
            IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          service = b.getLocService();
          Toast.makeText(ActivityMap.this, "LocService Connected", Toast.LENGTH_SHORT)
              .show();
          Location loc = service.getCurrentUserLocation();
          if(loc == null){
        	  service.updateLocation();
          }
        }

        public void onServiceDisconnected(ComponentName className) {
          service = null;
          Toast.makeText(ActivityMap.this, "LocService Disconnected", Toast.LENGTH_SHORT)
          .show();
        }
      };
}