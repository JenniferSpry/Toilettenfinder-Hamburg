package de.bfhh.stilleoertchenhamburg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.internal.cu;
import com.google.android.gms.maps.model.LatLng;

import de.bfhh.stilleoertchenhamburg.activites.ActivityMain;
import de.bfhh.stilleoertchenhamburg.activites.ActivitySplashScreen;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LocationUpdateService extends Service {
	
	private static final String TAG = LocationUpdateService.class.getSimpleName();
	private int result = Activity.RESULT_CANCELED;
	public static final String USERACTION = "userLocation";
	public static final String POIACTION = "toiletLocation";
	
	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String ADDRESS = "address";
	public static final String DESCR = "description";
	public static final String LNG = "longitude"; //user's lat and long
	public static final String LAT = "latitude";
	public static final String RESULT = "result";
	
	private LocationManager mlocationManager;
	
	final private LatLng HAMBURG = new LatLng(53.558, 9.927);
	private Location userLocation;
		
	JSONArray toilets = null;// products JSONArray
	
	private ArrayList<HashMap<String, String>> poiList;
	private long SPLASH_TIME_OUT = 1000; 

	
	public LocationUpdateService() {
		super();
	}
	
	//*****What if bestLocation is null??? ****
	
	private Location getLastKnownLocation() {   
		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	    List<String> providers = mlocationManager.getProviders(true);
	    Location bestLocation = null;
	    for (String provider : providers) {
	        Location l = mlocationManager.getLastKnownLocation(provider);
	        if (l == null) {
	            continue;
	        }
	        if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
	            // Found best last known location: %s", l);
	            bestLocation = l;
	        }
	    }
	    return bestLocation;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		// In this method the 
		String action = intent.getStringExtra(USERACTION); //@TODO: add action static string in activities
		if(action.equals("userLocation")){//a user's location update was requested from an activity
					
			//call getLastLocation in an AsyncTask and
			//publish results once location is received
			new LocationUpdateTask().execute();
									
		}//other action for location update maybe?			
		return super.onStartCommand(intent, flags, startId);
	}


	private void publishUserLocation(int result, Location userLocation){
			//TODO: optimize code
		if(userLocation != null){//you can never check enough!
			//Send Broadcast back to ActivitySplashScreen
			Intent intent = new Intent(USERACTION);
			//location and result based on whether user location received or not
			intent.putExtra(LAT, userLocation.getLatitude());
			intent.putExtra(LNG, userLocation.getLongitude());				
			intent.putExtra(RESULT, result);
			sendBroadcast(intent);
	  	  }//what else?? :)
	}
	
	private void setCurrentUserLocation(Location loc){
		userLocation = loc;
	}
	
	private Location getCurrentUserLocation(){
		return userLocation;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    Log.e(TAG, "in onDestroy in LocationService class");
	    //mlocManager.removeUpdates(mlocListener);
	    stopSelf();
	}
	
	/*
	 * The three types used by an asynchronous task are the following:

		Params: the type of the parameters sent to the task upon execution.
		Progress: the type of the progress units published during the background computation.
		Result: the type of the result of the background computation.
	 */
	private class LocationUpdateTask extends AsyncTask<Void, Void, Location> {

        @Override
        protected Location doInBackground(Void... params) {
            Location currentLocation = getLastKnownLocation();
            return currentLocation;
        }

        @Override
        protected void onPostExecute(Location currentLocation) { //parameter is result passed from doInBackground()
        	
        	
        	//if the location set through LocationUpdateTask is not null publish it
			if(currentLocation != null){
				//set current Location in class
	        	setCurrentUserLocation(currentLocation);
	        	
				result = Activity.RESULT_OK;
				publishUserLocation(result, currentLocation); //send an intent broadcast with the current location
			} else { //if the location is null, set location to standard					
				Location standardLocation = new Location("");
				standardLocation.setLatitude(HAMBURG.latitude);
				standardLocation.setLongitude(HAMBURG.longitude);
				
				//set current Location to standard location
	        	setCurrentUserLocation(standardLocation);
				
				result = Activity.RESULT_CANCELED;
				publishUserLocation(result, standardLocation); //send an intent broadcast with invalid location
			}
        	setCurrentUserLocation(currentLocation);
        	//TODO: Toast that location was received
        	Toast.makeText(getApplicationContext(),
	                  "Location successfully received.  LAT: " + Double.valueOf(currentLocation.getLatitude()) + ", LNG: " + Double.valueOf(currentLocation.getLongitude()),
	                  Toast.LENGTH_LONG).show();
        	
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}

