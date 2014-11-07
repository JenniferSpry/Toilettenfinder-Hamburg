package de.bfhh.stilleoertchenhamburg;

import java.util.List;
import org.json.JSONArray;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LocationUpdateService extends Service {
	
	private static final String TAG = LocationUpdateService.class.getSimpleName();
	private int result = Activity.RESULT_CANCELED;
	//Intent actions (labels/names)
	public static final String USERACTION = "userLocation";
	public static final String POIACTION = "toiletLocation";
	
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	
	//names of the data that is sent back to activities
	//TODO: add package name
	public static final String LNG = "longitude"; 
	public static final String LAT = "latitude";
	public static final String RESULT = "result";
	public static final String PROVIDER = "provider";
	
	private LocationManager mlocationManager;
	private LocationUpdateListener locUpdListener; 
	private Location userLocation;
	
	//TODO: export to config file
	final private LatLng HAMBURG = new LatLng(53.558, 9.927);
		
	JSONArray toilets = null;// POI JSONArray
	
	//Binder to bind activities to this service
	private final IBinder mBinder = new ServiceBinder();
	
	public LocationUpdateService(){
		super();
	}
	
	protected Location getLastKnownLocation() {
		//get list of enabled providers
	    List<String> providers = mlocationManager.getProviders(true);
	    Location bestLocation = null;
	    //check whether providers list is empty
	    if(!providers.isEmpty()){
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
	    }
	    return bestLocation;
	}
	
	
	public void updateLocation(){
		// Starting point for this Service
		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locUpdListener = new LocationUpdateListener();
		//register for location Updates every 20 seconds, minimum distance change: 10 meters
		mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000,  10.0f, locUpdListener);
		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 10.0f, locUpdListener);
		//call getLastKnownLocation() from within an AsyncTask and
		//publish results once location is received
		new LocationUpdateTask().execute();							
	}

	//broadcast user location
	protected void publishUserLocation(int result, Location userLocation){
		if(userLocation != null){//you can never check enough!
			//Send Broadcast to activities
			Intent intent = new Intent(USERACTION);
			//location and result based on whether user location received or not
			intent.putExtra(LAT, userLocation.getLatitude());
			intent.putExtra(LNG, userLocation.getLongitude());				
			intent.putExtra(RESULT, result);
			sendBroadcast(intent);
	  	  }
	}
	
	private void setCurrentUserLocation(Location loc){
		userLocation = loc;
	}
	
	public Location getCurrentUserLocation(){
		return userLocation;
	}

	//return local binder, through which activity can get service instance
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	//inner class Binder, so that Activities can bind to this service
	public class ServiceBinder extends Binder {
	    public LocationUpdateService getLocService() { 
	      return LocationUpdateService.this;//return service to bind to
	    }
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    Log.e(TAG, "in onDestroy in LocationService class");
	    
	    //TODO: unregister from location updates by providers
	    stopSelf();
	}
	
	/*
	 * The three types used by an asynchronous task are the following:

		Params: the type of the parameters sent to the task upon execution.
		Progress: the type of the progress units published during the background computation.
		Result: the type of the result of the background computation.
	 */
	private class LocationUpdateTask extends AsyncTask<Void, Void, Location> {
		
		//retrieve last known location (may be null)
		//and pass it to onPostExecute()
        @Override
        protected Location doInBackground(Void... params) {
            Location currentLocation = getLastKnownLocation();
            return currentLocation;
        }

        @Override
        protected void onPostExecute(Location currentLocation) { //parameter is result passed from doInBackground()
        	       	
        	//if the received location is "valid" publish it as RESULT_OK
			if(currentLocation != null){
				//set current Location in class
	        	setCurrentUserLocation(currentLocation);
	        	
				result = Activity.RESULT_OK;
				publishUserLocation(result, currentLocation);
			} else { //if the location is null, set location to standard and publish as RESULT_CANCELLED					
				
				Location standardLocation = new Location("");
				standardLocation.setLatitude(HAMBURG.latitude);
				standardLocation.setLongitude(HAMBURG.longitude);
				
				//set current location to standard location
	        	setCurrentUserLocation(standardLocation);
				
				result = Activity.RESULT_CANCELED;
				publishUserLocation(result, standardLocation);
			}
        	//Toast that location was received
        	Toast.makeText(getApplicationContext(),
	                  "Location successfully received.  LAT: " + Double.valueOf(userLocation.getLatitude()) + ", LNG: " + Double.valueOf(userLocation.getLongitude()),
	                  Toast.LENGTH_LONG).show();
        	
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    } //Ansynctask end
	
	
	//Returns whether location is a better location than currentBestLocation
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }



	// Checks whether two providers are the same 
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    private class LocationUpdateListener implements LocationListener {
    	
		  @Override
		  public void onLocationChanged(Location location) {
			  //if location is not null and better than current location broadcast it
			  if(location != null){
				  if(isBetterLocation(location, userLocation)) {
					  Log.i("LocationUpdateListener.onLocationChanged(): ", "Update of user location received"); 
					  Intent intent = new Intent("LocationUpdate");
					  intent.putExtra(LAT, location.getLatitude());
					  intent.putExtra(LNG, location.getLongitude());    
		              intent.putExtra(PROVIDER, location.getProvider());
		              //broadcast to all activities that want location updates
		              sendBroadcast(intent);          
		          }   		  
			  }
			  
		  }
	
		  @Override
		  public void onStatusChanged(String provider, int status, Bundle extras) {
			  //int status: 0 -> out of service; 1 -> temporarily unavailable; 2 -> available
			  Toast.makeText(getApplicationContext(), provider + "'s status changed to "+status +"!",
				Toast.LENGTH_SHORT).show();
		  }
	
		  @Override
		  public void onProviderEnabled(String provider) {
			  Toast.makeText(getApplicationContext(), "Provider " + provider + " enabled!",
		        Toast.LENGTH_SHORT).show();	
			  new LocationUpdateTask().execute();	
		  }
	
		  @Override
		  public void onProviderDisabled(String provider) {
			  Toast.makeText(getApplicationContext(), "Provider " + provider + " disabled!",
		        Toast.LENGTH_SHORT).show();
		  }
		  
		  //TODO: Check here, whether providers are being disabled while the app
    }
}


