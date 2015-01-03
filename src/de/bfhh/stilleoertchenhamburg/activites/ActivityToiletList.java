package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.fragments.FragmentToiletList;
import de.bfhh.stilleoertchenhamburg.helpers.POIHelper;
import de.bfhh.stilleoertchenhamburg.models.POI;
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
 */

public class ActivityToiletList extends ActivityMenuBase {
	
	private static final String TAG = ActivityToiletList.class.getSimpleName();
        
    private LocationUpdateService _service;
    
    private Double _lat;
	private Double _lng;
	private ArrayList<POI> _poiList;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_list);
        Log.d(TAG, "onCreate");
        
        ActionBar actionBar = getSupportActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);
		
        if (checkPlayServices()) {
        	if(savedInstanceState != null){
        		// use saved 
    			Bundle bundle = savedInstanceState;
    			_lat = bundle.getDouble(TagNames.EXTRA_LAT);
    			_lng = bundle.getDouble(TagNames.EXTRA_LONG);
    			_poiList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
    			fillFragmentList();
    		} 
        	if (_lat == null || _lng == null || _poiList == null){
		        // Set receivers
        		IntentFilter filter = new IntentFilter(TagNames.BROADC_LOCATION_NEW);
        		filter.addAction(TagNames.BROADC_LOCATION_UPDATED);
        		registerReceiver(locationReceiver, filter);
		        registerReceiver(poiReceiver, new IntentFilter(TagNames.BROADC_POIS));
		        
		        Intent intent= new Intent(this, LocationUpdateService.class);
		        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		        startService(new Intent(this, POIUpdateService.class));
    		}
        }
    }
    
	
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
    		Log.d(TAG, "Recieved Broadcast location new");

        	Bundle bundle = intent.getExtras();
    		
    		if (bundle != null) {
	        	//Get resultCode, latitude and longitude sent from LocationUpdateService
	            _lat = bundle.getDouble(TagNames.EXTRA_LAT);
	            _lng = bundle.getDouble(TagNames.EXTRA_LONG);
	            int locationResult = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
	            //lat, lng and resultcode received successfully
	            if (locationResult == RESULT_CANCELED) {
	            	//if RESULT_CANCELLED or lat and long are standard location then no location was received from locationManager in LocationUpdateService
	            	  Toast.makeText(ActivityToiletList.this, "Last user location not received. Standard Location is set",
	            		  Toast.LENGTH_LONG).show();    
	            } 
	            unregisterReceiver(locationReceiver);
            	unbindService(mConnection);
            	fillFragmentList();
    		}
        }
    };
    
    
    private BroadcastReceiver poiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 	
        	Log.d(TAG, "Recieved Broadcast poi list");       	
        	Bundle bundle = intent.getExtras();
        	        	   
        	if (bundle != null) {
	            _poiList = intent.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
            	fillFragmentList();
            	unregisterReceiver(poiReceiver);
    	   }
        }
    };

    
    /** 
     * called on both receive actions
     * only does something once all data is received
     */
	private void fillFragmentList(){
		if (_lat != null && _lng != null && _poiList != null){
			_poiList = POIHelper.setDistancePOIToUser(_poiList, _lat, _lng);
			ArrayList<POI> pois = POIHelper.getClosestPOI(_poiList, 20);
			
			Bundle args = new Bundle();  
			args.putParcelableArrayList(TagNames.EXTRA_POI_LIST, pois);
			FragmentToiletList fragment = new FragmentToiletList();
			fragment.setArguments(args);
	
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragmentToiletList, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
	}
    
    
    /** handles what happens when this activity binds to LocationUpdateService */
    private ServiceConnection mConnection = new ServiceConnection() {

    	public void onServiceConnected(ComponentName className, IBinder binder) {
    		Log.d(TAG, "onServiceConnected");
    		LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
    		_service = b.getLocService();
    		Toast.makeText(ActivityToiletList.this, "LocService Connected", Toast.LENGTH_SHORT).show();
    		Location loc = _service.getCurrentUserLocation();
    		Location oldLoc = null;
    		if (_lat != null & _lng != null){
    			oldLoc = new Location("");
    			oldLoc.setLatitude(_lat);
    			oldLoc.setLongitude(_lng);
    		}
    		if(loc == null || oldLoc == null || _service.isBetterLocation(loc, oldLoc) ){
    			Log.d(TAG, "onServiceDisconnected updateLocation");
    			_service.updateLocation(60000, 3.0f, 5000, 1.0f);//calls AsyncTask and publishes results
    		}
    	}

    	public void onServiceDisconnected(ComponentName className) {
    		Log.d(TAG, "onServiceDisconnected");
    		_service.stopLocationUpdates();
    		_service = null;
    		Toast.makeText(ActivityToiletList.this, "LocService Disconnected", Toast.LENGTH_SHORT).show();
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
    
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (_lat != null && _lng != null && _poiList != null){
			savedInstanceState.putParcelableArrayList(TagNames.EXTRA_POI_LIST, _poiList);
			savedInstanceState.putDouble(TagNames.EXTRA_LAT, _lat);
			savedInstanceState.putDouble(TagNames.EXTRA_LONG, _lng);
			super.onSaveInstanceState(savedInstanceState);
		}
	}

}
