package de.bfhh.stilleoertchenhamburg;

import java.util.List;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public class LocationUpdateService extends IntentService {
	
	private int result = Activity.RESULT_CANCELED;
	public static final String ACTION = "userLocation";
	public static final String LNG = "longitude";
	public static final String LAT = "latitude";
	public static final String USERLOCATION = "";
	public static final String RESULT = "result";
	private LocationManager mlocationManager;
	
	public LocationUpdateService() {
		super("Location Update Service");
		
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
	protected void onHandleIntent(Intent intent) {
		// In this method the 
		String action = intent.getStringExtra(ACTION); //@TODO: add action static string in activities
		if(action.equals("userLocation")){//a user's location update was requested from an activity
			Location currentLoc = getLastKnownLocation();
			if(currentLoc != null){
				result = Activity.RESULT_OK;
				publishUserLocation(currentLoc, result); //send an intent broadcast with the current location
				
			}else{
				result = Activity.RESULT_CANCELED;
				publishUserLocation(null, result); //send an intent broadcast with invalid location
			}
		}
	}
	
	private void publishUserLocation(Location location, int result){
		Intent intent = new Intent(ACTION);
	    intent.putExtra(LAT, location.getLatitude());
	    intent.putExtra(LNG, location.getLongitude());
	    intent.putExtra(RESULT, result);
	    sendBroadcast(intent);
	}

}
