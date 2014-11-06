package de.bfhh.stilleoertchenhamburg.activites;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.android.gms.maps.model.LatLng;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;

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
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
 
/*
 * This is the Activity that is always shown when one starts the Application.
 * It shows a SplashScreen with the bf-hh Logo and their website URL.
 * In the background, this Activity starts the LocationUpdateService 
 * from it's onCreate() method and registers a receiver for it's broadcasts in onResume().
 * Inside the BroadcastReceiver's onReceive() method, the LocationUpdateService is stopped,
 * and a new IntentService - POIUpdateService - is started with the data (= user location)
 * received from LocationUpdateService.
 * Finally, this activity kills itself after a preset timeout (will be changed to when
 * it receives a broadcast from POIUpdateService that it's done processing)
 */

public class ActivitySplashScreen extends Activity {

	private static final String TAG_LAT = "latitude";
	private static final String TAG_LONG = "longitude";
	private static final String TAG_RAD = "radius";
	private static final String TAG_RESULT = "result";
	private static final String TAG_USERLOCATION = "userLocation";
	private static final String TAG_POIUPDATE = "POIUpdate";
	private static final String TAG_POIUPDATE_OK = "POIUpdate_OK";
	
	// TODO: should be more like 1km what ever fits on our inital sceen in the map activity
	private static final double INITIAL_RADIUS = 1d;
    
    private boolean registered;//is the receiver registered?
    
    private boolean locServiceRegistered;
    
    private IntentFilter filter;
    
    final private LatLng HAMBURG = new LatLng(53.558, 9.927);
    
    private LocationUpdateService service;
    
    private boolean changeGPSSettings;
    
    private double lat = 0.0;
	private double lng = 0.0;
    
    // BroadcastReceiver for Broadcasts from LocationUpdateService and POIUpdateService
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
        	//Get Extras
        	Bundle bundle = intent.getExtras();
        	String action = intent.getAction();
        	
        	if(action.equals(TAG_USERLOCATION)){ //action = "userLocation"
        		//Stop the Service
            	//stopLocationUpdateService();
        		if (bundle != null) {
		        	//Get resultCode, latitude and longitude sent from LocationUpdateService
		            lat = bundle.getDouble(LocationUpdateService.LAT);
		            lng = bundle.getDouble(LocationUpdateService.LNG);
		            int resultCode = bundle.getInt(LocationUpdateService.RESULT);
		            //lat, lng and resultcode received successfully
		            if (resultCode == RESULT_OK && lat != HAMBURG.latitude && lng != HAMBURG.longitude) {
			              //Start POIUpdateService with user location 
			              //received from LocationUpdateService
			              startPOIUpdateService(resultCode, lat, lng);
			              
		            } else { //if RESULT_CANCELLED or lat and long are standard location then no location was received from locationManager in LocationUpdateService
		            	  Toast.makeText(ActivitySplashScreen.this, "Last user location not received. Standard Location is set",
		            		  Toast.LENGTH_LONG).show();
		            	  if(changeGPSSettings == false){
		            		 // buildAlertMessageGPSSettings();
		            	  }
		            	  //start with Hamburg standard location
		            	  startPOIUpdateService(resultCode, lat, lng);
		            }
	          }
           } else if(action.equals(TAG_POIUPDATE_OK)){//POIUpdateService is finished
        	   //terminate this activity
        	   if (bundle != null) {
		        	//Get resultCode, latitude and longitude sent from LocationUpdateService
		            lat = bundle.getDouble(LocationUpdateService.LAT);
		            lng = bundle.getDouble(LocationUpdateService.LNG);
		            int resultCode = bundle.getInt(LocationUpdateService.RESULT);
		            ArrayList<HashMap<String,String>> poiList = (ArrayList<HashMap<String,String>>) intent.getSerializableExtra("poiList");
		            if(lat != 0.0 && lng != 0.0 && poiList != null){
		            	//start activity which shows map
		            	startMapActivity(lat, lng, poiList, resultCode);
		            	finish();// terminate this activity
		            }	            
        	   }        	   
           }
        }
    };
    
	DialogInterface.OnClickListener onOkListener = new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            changeGPSSettings = true;
            startPOIUpdateService(0, lat, lng);
        }
    };
    
    DialogInterface.OnClickListener onCancelListener = new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int id) {
            //startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            changeGPSSettings = true;
            startPOIUpdateService(0, lat, lng); //start Service with standard location which is already set
        }
    };
    
	//show alert dialog with option to change gps settings
	private void buildAlertMessageGPSSettings() {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySplashScreen.this);
	    builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
	           .setCancelable(false)
	           .setPositiveButton("Yes", onOkListener)
	           .setNegativeButton("No", onCancelListener);
	    final AlertDialog alert = builder.create();
	    alert.show();
	}
    
	private void startMapActivity(double userLat, double userLng, ArrayList<HashMap<String,String>> poiList, int result){
		Intent i = new Intent(this, ActivityMap.class);
        i.putExtra("poiList",(Serializable) poiList);
        i.putExtra(TAG_LAT, userLat);
        i.putExtra(TAG_LONG, userLng);
  	  	//putExtra contentprovider at some point
  	  	i.putExtra(TAG_RESULT, result);
  	  	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	startActivity(i); //start Main Activity
	}
    
    //start POIUpdateService with Intent to get list of POI
    private void startPOIUpdateService(int resultCode, double lat, double lng){
     	Intent i2 = new Intent(ActivitySplashScreen.this, POIUpdateService.class);
        // add action			     	
        i2.putExtra(POIUpdateService.POIACTION, TAG_POIUPDATE);
        //add lat and lng
        i2.putExtra(TAG_LAT, lat);
        i2.putExtra(TAG_LONG, lng);
        i2.putExtra(TAG_RAD, INITIAL_RADIUS);
        i2.putExtra(TAG_RESULT, resultCode);
        startService(i2);
    }
    
    //Start LocationUpdateService with Intent to get user's current position
    private void startLocationUpdateService(){	
     	Intent i1 = new Intent(getApplicationContext(), LocationUpdateService.class);
     	// Add appropriate action
        i1.putExtra(LocationUpdateService.USERACTION, TAG_USERLOCATION);
        startService(i1);
    }
    
    //Stop LocationUpdateService using same Intent it was started with
    private void stopLocationUpdateService(){
	    Intent i1 = new Intent(getApplicationContext(), LocationUpdateService.class);
	    // add action
	    i1.putExtra(LocationUpdateService.USERACTION, TAG_USERLOCATION);
	    stopService(i1);
      	
    }
    
    //handles what happens when this activity binds to LocationUpdateService
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, 
            IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          service = b.getLocService();
          Toast.makeText(ActivitySplashScreen.this, "LocService Connected", Toast.LENGTH_SHORT)
              .show();
          Location loc = service.getCurrentUserLocation();
          if(loc == null){
        	  service.updateLocation();//calls AsyncTask and publishes results
          }
        }

        public void onServiceDisconnected(ComponentName className) {
        service = null;
          Toast.makeText(ActivitySplashScreen.this, "LocService Disconnected", Toast.LENGTH_SHORT)
          .show();
        }
    };
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        filter = new IntentFilter(LocationUpdateService.USERACTION);
    	filter.addAction(POIUpdateService.POIACTION_OK);
    	registerReceiver(receiver, filter);
        //registerReceiver(receiver, new IntentFilter(LocationUpdateService.USERACTION));
        registered = true; //shows that a receiver is registered
        
        changeGPSSettings = false;
        //SOLUTION for Threading: create Asynctask in Service to handle Location stuff
        //startLocationUpdateService();  	
        //bind to it rather than starting service
        //bind LocationUpdateService
        Intent intent= new Intent(this, LocationUpdateService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //set locServiceRegistered to true
        locServiceRegistered = true;
    }
    
    /*
    @Override
    protected void onStart(){
    	super.onStart();
    	if(!registered){
    		registerReceiver(receiver, filter);
    		registered = true;
    	}
    }*/
    
    
    @Override
    protected void onResume(){
    	super.onResume();
    	if(!registered){
    		registerReceiver(receiver, filter);
    		registered = true;
    	}
    	if(!locServiceRegistered){
    		 //bind to service rather than starting it
            Intent intent= new Intent(this, LocationUpdateService.class);
            //intent.putExtra(LocationUpdateService.USERACTION, TAG_USERLOCATION);
            bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
            locServiceRegistered = true;
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if(registered){
    	  	unregisterReceiver(receiver);
    	  	registered = false;
      	}
    	if(locServiceRegistered){
    		unbindService(mConnection);
    		locServiceRegistered = false;
    	}
    }
    
    @Override
	protected void onDestroy(){
    	super.onDestroy();
    	if(registered){
    	  	unregisterReceiver(receiver);
    	  	registered = false;
      	}
    	if(locServiceRegistered){
    		unbindService(mConnection);
    		locServiceRegistered = false;
    	}
    }
}

