package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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

public class ActivitySplashScreen extends ActivityBase {
	
	private static final String TAG = ActivitySplashScreen.class.getSimpleName();
    
    private boolean isReceiverRegistered = false;;//is the receiver registered?
    
    private boolean locServiceBound = false;
        
    private LocationUpdateService service;
    
    private double lat = 0.0;
	private double lng = 0.0;
	private int locationResult = Activity.RESULT_CANCELED;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	if (checkPlayServices()) {
    		// Set receiver to listen to actions from both services
        	if(!isReceiverRegistered){
        		IntentFilter filter = new IntentFilter(TagNames.BROADC_LOCATION_NEW);
            	filter.addAction(TagNames.BROADC_POIS);
        		registerReceiver(receiver, filter);
        		isReceiverRegistered = true;
        	}
        	if(!locServiceBound){
        		 //bind to service rather than starting it
                Intent intent = new Intent(this, LocationUpdateService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                locServiceBound = true;
        	}
    	}
    }
    
	
    // BroadcastReceiver for Broadcasts from LocationUpdateService and POIUpdateService
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
        	//Get Extras
        	Bundle bundle = intent.getExtras();
        	String action = intent.getAction();
        	
        	if(action.equals(TagNames.BROADC_LOCATION_NEW)){ //LocationUpdateService is finished
        		Log.d(TAG, "Recieved Broadcast location new");
        		if (bundle != null) {
		        	//Get resultCode, latitude and longitude sent from LocationUpdateService
		            lat = bundle.getDouble(TagNames.EXTRA_LAT);
		            lng = bundle.getDouble(TagNames.EXTRA_LONG);
		            locationResult = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
		            //lat, lng and resultcode received successfully
		            if (locationResult == RESULT_CANCELED) {
		            	//if RESULT_CANCELLED or lat and long are standard location then no location was received from locationManager in LocationUpdateService
		            	  Toast.makeText(ActivitySplashScreen.this, "Last user location not received. Standard Location is set",
		            		  Toast.LENGTH_LONG).show();    
		            } 
		            startPOIUpdateService(lat, lng);
	          }
           } else if(action.equals(TagNames.BROADC_POIS)){
        	   //terminate this activity
        	   Log.d(TAG, "Recieved Broadcast poi list");
        	   if (bundle != null) {
		            ArrayList<POI> poiList = intent.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
		            if(lat != 0.0 && lng != 0.0 && poiList != null){
		            	//start activity which shows map
		            	startMapActivity(lat, lng, poiList, locationResult);
		            	finish();// terminate this activity
		            }	            
        	   }        	   
           }
        }
    };

	private void startMapActivity(double userLat, double userLng, ArrayList<POI> poiList, int result){
		Intent i = new Intent(this, ActivityMap.class);
        i.putParcelableArrayListExtra(TagNames.EXTRA_POI_LIST, poiList);
        i.putExtra(TagNames.EXTRA_LAT, userLat);
        i.putExtra(TagNames.EXTRA_LONG, userLng);
  	  	i.putExtra(TagNames.EXTRA_LOCATION_RESULT, result);
  	  	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
  	  	startActivity(i); //start Main Activity
	}
    
    //start POIUpdateService with Intent to get list of POI
    private void startPOIUpdateService(double userLat, double userLng){
     	Intent i = new Intent(this, POIUpdateService.class);
     	i.putExtra(TagNames.EXTRA_LAT, userLat);
        i.putExtra(TagNames.EXTRA_LONG, userLng);
        startService(i);
    }
    
    
    //handles what happens when this activity binds to LocationUpdateService
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          service = b.getLocService();
          Toast.makeText(ActivitySplashScreen.this, "LocService Connected", Toast.LENGTH_SHORT).show();
          Location loc = service.getCurrentUserLocation();
          if(loc == null){
        	  service.updateLocation();//calls AsyncTask and publishes results
          }
        }

        public void onServiceDisconnected(ComponentName className) {
        	service.stopLocationUpdates();
        	service = null;
          	Toast.makeText(ActivitySplashScreen.this, "LocService Disconnected", Toast.LENGTH_SHORT).show();
        
        }
    };
 
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
    protected void onPause() {
    	super.onPause();
    	if(isReceiverRegistered){
    	  	unregisterReceiver(receiver);
    	  	isReceiverRegistered = false;
      	}
    	if(locServiceBound){
    		unbindService(mConnection);
    		locServiceBound = false;
    	}
    }
    
    @Override
	protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "onDestroy");
    	if(isReceiverRegistered){
    	  	unregisterReceiver(receiver);
    	  	isReceiverRegistered = false;
      	}
    	if(locServiceBound){
    		unbindService(mConnection);
    		locServiceBound = false;
    	}
    }
}

