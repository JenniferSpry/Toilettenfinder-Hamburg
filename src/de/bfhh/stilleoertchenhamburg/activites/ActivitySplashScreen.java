package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.POIUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.helpers.NetworkUtil;
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
 * In the background, this Activity starts the LocationUpdateService and POIUpdateService
 * and registers a receiver for their broadcasts.
 * Once both broadcasts are received the activity will kill itself and start the mapActivity.
 */

public class ActivitySplashScreen extends ActivityBase {
	
	private static final String TAG = ActivitySplashScreen.class.getSimpleName();
	
	private Double _lat;
	private Double _lng;
	private int _locationResult = Activity.RESULT_CANCELED;
	private ArrayList<POI> _poiList;

    private boolean _isReceiverRegistered = false;//is the receiver registered?
    private boolean _locServiceBound = false;    
	protected boolean _networkConnected;

	protected boolean _mapActivityStarted; //shows whether MapActivity was started already or not

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        _mapActivityStarted = false;
    }
    
    @Override
    protected void onNetworkConnected(){
    	super.onNetworkConnected();
    	if(!_locServiceBound){
    		//bind to service rather than starting it
            Intent intent = new Intent(this, LocationUpdateService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            _locServiceBound = true;
   		}
    	startService(new Intent(this, POIUpdateService.class));
	}

    @Override
    protected void onResume(){
    	super.onResume();
    	if (checkPlayServices()) {
        	// Set receiver to listen to actions from both services
        	if(!_isReceiverRegistered){
        		IntentFilter filter = new IntentFilter(TagNames.BROADC_LOCATION_NEW);
        		filter.addAction(TagNames.BROADC_LOCATION_UPDATED);
            	filter.addAction(TagNames.BROADC_POIS);
        		registerReceiver(receiver, filter);
        		_isReceiverRegistered = true;
        	}
        	
        	//check network connection
        	int status = NetworkUtil.getConnectivityStatus(this);
        	//No connection at the moment, but connecting
        	if(status == TagNames.TYPE_NOT_CONNECTED && _oldConnecState != _connecState){
        		//show toast that we are connecting to network
        		Toast.makeText(this, "Connecting to network, please wait...", Toast.LENGTH_LONG).show();
        	}else if(status == TagNames.TYPE_MOBILE || status == TagNames.TYPE_WIFI){
        		onNetworkConnected();
        	}
    	}
    }
    
    
	/** 
	 * Receiver for Broadcasts from LocationUpdateService and POIUpdateService 
	 */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
    	
        @Override
        public void onReceive(Context context, Intent intent) { 	
        	//Get Extras
        	Bundle bundle = intent.getExtras();
        	String action = intent.getAction();	

        	if (bundle != null){
        		if(action.equals(TagNames.BROADC_LOCATION_NEW) || action.equals(TagNames.BROADC_LOCATION_UPDATED)){ //LocationUpdateService is finished
        			Log.d(TAG, "Recieved Broadcast location new");
        			//Get resultCode, latitude and longitude sent from LocationUpdateService
        			_lat = bundle.getDouble(TagNames.EXTRA_LAT);
        			_lng = bundle.getDouble(TagNames.EXTRA_LONG);
        			_locationResult = bundle.getInt(TagNames.EXTRA_LOCATION_RESULT);
        		} else if(action.equals(TagNames.BROADC_POIS)){
        			// get PoiList
        			Log.d(TAG, "Recieved Broadcast poi list");
        			_poiList = intent.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
        		}
        		startMapActivity(); //start Map
        	}
        }
    };

    /**
     * will switch to the map activity when provided all the data
     */
	private void startMapActivity(){
		if (_lat != null && _lng != null && _poiList != null) {
			Log.d(TAG, "Starting MapActivity");
			Intent i = new Intent(this, ActivityMap.class);
	        i.putParcelableArrayListExtra(TagNames.EXTRA_POI_LIST, _poiList);
	        i.putExtra(TagNames.EXTRA_LAT, _lat);
	        i.putExtra(TagNames.EXTRA_LONG, _lng);
	  	  	i.putExtra(TagNames.EXTRA_LOCATION_RESULT, _locationResult);
	  	  	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	  	  	i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	  	  	startActivity(i);
	  	  	finish();
		}
	}
    
    /**handles what happens when this activity binds to LocationUpdateService */
    private ServiceConnection mConnection = new ServiceConnection() {
    	
    	LocationUpdateService service = null;

        public void onServiceConnected(ComponentName className, IBinder binder) {
        	LocationUpdateService.ServiceBinder b = (LocationUpdateService.ServiceBinder) binder;
        	service = b.getLocService();
        	//Toast.makeText(ActivitySplashScreen.this, "LocService Connected", Toast.LENGTH_SHORT).show();
        	Location loc = service.getCurrentUserLocation();
        	if (loc == null){
        		service.updateLocation(60000, 3.0f, 5000, 1.0f);//calls AsyncTask and publishes results
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	service.stopLocationUpdates();
        	service = null;
          	//Toast.makeText(ActivitySplashScreen.this, "LocService Disconnected", Toast.LENGTH_SHORT).show();
        }
    };
    
    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause");
    	if(_isReceiverRegistered){
    	  	unregisterReceiver(receiver);
    	  	_isReceiverRegistered = false;
      	}
    	if(_locServiceBound){
    		unbindService(mConnection);
    		_locServiceBound = false;
    	}
    }
    
    @Override
	protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "onDestroy");
    	if(_isReceiverRegistered){
    	  	unregisterReceiver(receiver);
    	  	_isReceiverRegistered = false;
      	}
    	if(_locServiceBound){
    		unbindService(mConnection);
    		_locServiceBound = false;
    	}
    }
}

