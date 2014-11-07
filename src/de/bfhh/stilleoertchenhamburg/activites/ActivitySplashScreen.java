package de.bfhh.stilleoertchenhamburg.activites;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;

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

	// TODO: add package names and outsource to other file 
	private static final String TAG_LAT = "latitude";
	private static final String TAG_LONG = "longitude";
	private static final String TAG_RESULT = "result";
	private static final String TAG_USERLOCATION = "userLocation";
	private static final String TAG_POIUPDATE = "POIUpdate";
	private static final String TAG_POIUPDATE_OK = "POIUpdate_OK";
    
    private boolean isReceiverRegistered;//is the receiver registered?
    
    private boolean locServiceBound;
    
    private IntentFilter filter;
    
    private LocationUpdateService service;
    
    private double lat = 0.0;
	private double lng = 0.0;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Set reciever to listen to actions from both services
        filter = new IntentFilter(LocationUpdateService.USERACTION);
    	filter.addAction(POIUpdateService.POIACTION_OK);
    	registerReceiver(receiver, filter);
        isReceiverRegistered = true; //shows that a receiver is registered
        
        //SOLUTION for Threading: create Asynctask in Service to handle Location stuff	
        //bind to it rather than starting service
        Intent intent= new Intent(this, LocationUpdateService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //set locServiceRegistered to true
        locServiceBound = true;
    }
    
	
    // BroadcastReceiver for Broadcasts from LocationUpdateService and POIUpdateService
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
        	//Get Extras
        	Bundle bundle = intent.getExtras();
        	String action = intent.getAction();
        	
        	if(action.equals(TAG_USERLOCATION)){ //action = "userLocation"
        		if (bundle != null) {
		        	//Get resultCode, latitude and longitude sent from LocationUpdateService
		            lat = bundle.getDouble(LocationUpdateService.LAT);
		            lng = bundle.getDouble(LocationUpdateService.LNG);
		            int resultCode = bundle.getInt(LocationUpdateService.RESULT);
		            //lat, lng and resultcode received successfully
		            if (resultCode == RESULT_CANCELED) {
		            	//if RESULT_CANCELLED or lat and long are standard location then no location was received from locationManager in LocationUpdateService
		            	  Toast.makeText(ActivitySplashScreen.this, "Last user location not received. Standard Location is set",
		            		  Toast.LENGTH_LONG).show();    
		            } 
		            startPOIUpdateService(resultCode, lat, lng);
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
     	Intent i2 = new Intent(this, POIUpdateService.class);
        // add action			     	
        i2.putExtra(POIUpdateService.POIACTION, TAG_POIUPDATE);
        //add lat and lng
        i2.putExtra(TAG_LAT, lat);
        i2.putExtra(TAG_LONG, lng);
        i2.putExtra(TAG_RESULT, resultCode);
        startService(i2);
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
    	if(!isReceiverRegistered){
    		registerReceiver(receiver, filter);
    		isReceiverRegistered = true;
    	}
    	if(!locServiceBound){
    		 //bind to service rather than starting it
            Intent intent= new Intent(this, LocationUpdateService.class);
            bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
            locServiceBound = true;
    	}
    }
    
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

