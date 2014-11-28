package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.fragments.FragmentToiletList;
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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO: Vernünftige Fehlermeldung, wenn die Daten nicht kommen
 * @author Jenne
 *
 */

public class ActivityToiletList extends ActivityMenuBase {
	
	private static final String TAG = ActivityToiletList.class.getSimpleName();
        
    private LocationUpdateService service;
    
    private double lat = 0.0;
	private double lng = 0.0;
	private int locationResult = Activity.RESULT_CANCELED;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_list);
        
        ActionBar actionBar = getSupportActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);
		
        if (checkPlayServices()) {
	        // Set receivers
	        registerReceiver(locationReceiver, new IntentFilter(TagNames.BROADC_LOCATION_NEW));
	        registerReceiver(poiReceiver, new IntentFilter(TagNames.BROADC_POIS));
	        
	        Intent intent= new Intent(this, LocationUpdateService.class);
	        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	        Log.d("ToiletList onCreate", "Service bound");
        }
    }
    
	
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
    		Log.d(TAG, "Recieved Broadcast location new");

        	Bundle bundle = intent.getExtras();
    		
    		if (bundle != null) {
	        	//Get resultCode, latitude and longitude sent from LocationUpdateService
	            lat = bundle.getDouble(TagNames.EXTRA_LAT);
	            lng = bundle.getDouble(TagNames.EXTRA_LONG);
	            locationResult = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
	            //lat, lng and resultcode received successfully
	            if (locationResult == RESULT_CANCELED) {
	            	//if RESULT_CANCELLED or lat and long are standard location then no location was received from locationManager in LocationUpdateService
	            	  Toast.makeText(ActivityToiletList.this, "Last user location not received. Standard Location is set",
	            		  Toast.LENGTH_LONG).show();    
	            } 
	            unregisterReceiver(locationReceiver);
            	unbindService(mConnection);
            	Log.d("ToiletList onReceive", "service unbound");
	            startPOIUpdateService(lat, lng);
	          }
          
        }
    };
    
    
    private BroadcastReceiver poiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
        	Log.d(TAG, "Recieved Broadcast poi list");       	
        	Bundle bundle = intent.getExtras();
        	        	   
        	if (bundle != null) {
	            ArrayList<POI> poiList = intent.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
	            if(lat != 0.0 && lng != 0.0 && poiList != null){
	            	//start activity which shows map
	            	fillFragmentList(lat, lng, poiList, locationResult);
	            	unregisterReceiver(poiReceiver);
	            }	            
    	   }        	   
        }
    };

    
	private void fillFragmentList(double userLat, double userLng, ArrayList<POI> poiList, int result){
		POIController poiController = new POIController(poiList);
		poiController.setDistancePOIToUser(userLat, userLng);
		ArrayList<POI> pois = poiController.getClosestPOI(20);
		
		Bundle args = new Bundle();  
		args.putParcelableArrayList(TagNames.EXTRA_POI_LIST, pois);
		FragmentToiletList fragment = new FragmentToiletList();
		fragment.setArguments(args);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragmentToiletList, fragment);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
    

	private void startPOIUpdateService(double userLat, double userLng){
     	Intent i = new Intent(this, POIUpdateService.class);
     	i.putExtra(TagNames.EXTRA_LAT, userLat);
        i.putExtra(TagNames.EXTRA_LONG, userLng);
        startService(i);
    }
    
    
    //handles what happens when this activity binds to LocationUpdateService
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, 
            IBinder binder) {
          LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
          service = b.getLocService();
          Toast.makeText(ActivityToiletList.this, "LocService Connected", Toast.LENGTH_SHORT)
              .show();
          Location loc = service.getCurrentUserLocation();
          Location oldLoc = new Location("");
          oldLoc.setLatitude(lat);
          oldLoc.setLongitude(lng);
          if(loc == null || service.isBetterLocation(loc, oldLoc) ){
        	  service.updateLocation();//calls AsyncTask and publishes results
          }
        }

        public void onServiceDisconnected(ComponentName className) {
        	service = null;
          	Toast.makeText(ActivityToiletList.this, "LocService Disconnected", Toast.LENGTH_SHORT)
          		.show();
        }
    };
    
    
    @Override
    protected void onResume(){
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
	protected void onDestroy(){
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
    }

}
