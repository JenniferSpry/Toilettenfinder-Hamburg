package de.bfhh.stilleoertchenhamburg.activites;

import com.google.android.gms.maps.model.LatLng;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.helpers.JSONParser;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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

public class ActivitySplashScreen extends ListActivity {
	
	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();

	private static final String TAG_LAT = "latitude";
	private static final String TAG_LONG = "longitude";
	private static final String TAG_RESULT = "result";
	private static final String TAG_USERLOCATION = "userLocation";
	private static final String TAG_POIUPDATE = "POIUpdate";
	private static final String TAG_POIUPDATE_OK = "POIUpdate finished";
	
	final private LatLng HAMBURG = new LatLng(53.558, 9.927); //standard position in HH
	
    // Splash screen timeout
    private static int SPLASH_TIME_OUT = 2000;
    
    // BroadcastReceiver for Broadcasts from LocationUpdateService
    private BroadcastReceiver receiver = new BroadcastReceiver() {
    	private double lat = 0.0;
    	private double lng = 0.0;
    	
        @Override
        public void onReceive(Context context, Intent intent) {
        	//Stop the Service
        	stopLocationUpdateService();
        	
        	//Get Extras
        	Bundle bundle = intent.getExtras();
        	String action = intent.getAction();
        	if(action.equals(TAG_USERLOCATION)){ //action = "userLocation"
        		if (bundle != null) {
		        	//Get resultCode, latitude and longitude sent from LocationUpdateService
		            lat = bundle.getDouble(LocationUpdateService.LAT);
		            lng = bundle.getDouble(LocationUpdateService.LNG);
		            int resultCode = bundle.getInt(LocationUpdateService.RESULT);
		
		            if (resultCode == RESULT_OK) {
			              //Start POIUpdateService with user location 
			              //received from LocationUpdateService
			              startPOIUpdateService(resultCode, lat, lng);
		
			              //TODO: BroadcastReceiver for POIUpdateService to close this activity 
			              //when results are received (two actions in Intentfilter)
			              
			              // Wait to show the logo, otherwise SplashScreen will not be seen.
					      // Then close this activity.
			              /*new Handler().postDelayed(new Runnable() {
			            	  @Override
			            	  public void run() {
				            	  // close this activity
				            	  finish();
			            	  }
			              }, SPLASH_TIME_OUT); */
			              
		            } else { //if RESULT_CANCELLED, then no location was received from locationManager in LocationUpdateService
		            	  Toast.makeText(ActivitySplashScreen.this, "Last user location not received. Standard Location is set",
		            		  Toast.LENGTH_LONG).show();
		            	  //start with Hamburg standard location
		            	  startPOIUpdateService(resultCode, lat, lng);
		            }
	          }
           } else if(action.equals(TAG_POIUPDATE_OK)){//POIUpdateService is finished
        	   //terminate this activity
        	   finish();
           }
        }
    };
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        //TODO: Somehow this doesn't seem to create a thread at all...
        //SOLUTION: create Asynctask in Service to handle Location stuff
        //Start LocationUpdateService in its own thread 
        new Thread(new Runnable(){
		    @Override
        	public void run() {   
		    	startLocationUpdateService();
		    }
		}).start(); 
	  	
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	IntentFilter filter = new IntentFilter(LocationUpdateService.USERACTION);
    	filter.addAction(POIUpdateService.POIACTION_OK);
    	this.registerReceiver(receiver, filter);
        registerReceiver(receiver, new IntentFilter(LocationUpdateService.USERACTION));
    }
    
    @Override
    protected void onPause() {
      super.onPause();
      unregisterReceiver(receiver);
    }
    
    //Start LocationUpdateService with Intent to get user's current position
    private void startLocationUpdateService(){	
     	Intent i1 = new Intent(getApplicationContext(), LocationUpdateService.class);
     	// Add appropriate action
        i1.putExtra(LocationUpdateService.USERACTION, "userLocation");
        startService(i1);
    }
    
    private void stopLocationUpdateService(){
    	//Stop LocationUpdateService using same Intent it was started with
	    Intent i1 = new Intent(getApplicationContext(), LocationUpdateService.class);
	    // add action
	    i1.putExtra(LocationUpdateService.USERACTION, TAG_USERLOCATION);
	    stopService(i1);
      	
    }
    
    //start POIUpdateService with Intent to get list of POI
    private void startPOIUpdateService(int resultCode, double lat, double lng){
    	
     	Intent i2 = new Intent(ActivitySplashScreen.this, POIUpdateService.class);
        // add action			     	
        i2.putExtra(POIUpdateService.POIACTION, TAG_POIUPDATE);
        //add lat and lng
        i2.putExtra(TAG_LAT, lat);
        i2.putExtra(TAG_LONG, lng);
        i2.putExtra(TAG_RESULT, resultCode);
        startService(i2);
    }
 
}

