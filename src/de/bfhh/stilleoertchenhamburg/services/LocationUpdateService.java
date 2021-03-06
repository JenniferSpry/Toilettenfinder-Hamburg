package de.bfhh.stilleoertchenhamburg.services;

import java.util.List;

import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.helpers.TagNames;

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

public class LocationUpdateService extends Service {

	private static final String TAG = LocationUpdateService.class
			.getSimpleName();

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private LocationManager mlocationManager;
	private LocationUpdateListener locUpdListener;
	private Location userLocation;

	// Binder to bind activities to this service
	private final IBinder mBinder = new ServiceBinder();

	public LocationUpdateService() {
		super();
	}

	public Location getLastKnownLocation() {
		// get list of enabled providers
		List<String> providers = mlocationManager.getProviders(true);
		Location bestLocation = null;
		// check whether providers list is empty
		if (!providers.isEmpty()) {
			for (String provider : providers) {
				Location l = mlocationManager.getLastKnownLocation(provider);
				if (l == null) {
					continue;
				}
				if (bestLocation == null
						|| l.getAccuracy() < bestLocation.getAccuracy()) {
					// Found best last known location: %s", l);
					bestLocation = l;
				}
			}
		}
		return bestLocation;
	}

	public boolean isProviderEnabled(String provider) {
		if (mlocationManager != null
				&& mlocationManager.isProviderEnabled(provider)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param gpsInterval
	 *            how often to ask for gps update in milliseconds
	 * @param gpsDistanceChange
	 *            minimum distance between location gps updates, in meters
	 * @param networkInterval
	 *            gpsInterval how often to ask for network update in
	 *            milliseconds
	 * @param networkDistanceChange
	 *            minimum distance between location network updates, in meters
	 */
	public void updateLocation(int gpsInterval, float gpsDistanceChange,
			int networkInterval, float networkDistanceChange) {
		Log.d(TAG, "start polling Location");
		// Starting point for this Service
		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locUpdListener = new LocationUpdateListener();
		// register for location Updates
		requestGPSUpdates(gpsInterval, gpsDistanceChange);
		requestNetworkUpdates(networkInterval, networkDistanceChange);
		// call getLastKnownLocation() from within an AsyncTask and
		// publish results once location is received
		new LocationUpdateTask().execute();
	}

	public void stopLocationUpdates() {
		Log.d(TAG, "stop polling Location");
		if (mlocationManager != null) {
			Log.i("stopLocationUpdates", "Removing location updates");
			mlocationManager.removeUpdates(locUpdListener);
			locUpdListener = null;
		}
	}

	public void requestGPSUpdates(long minTime, float minDistance) {
		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				(long) minTime, minDistance, locUpdListener);
	}

	public void requestNetworkUpdates(long minTime, float minDistance) {
		mlocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, minTime, minDistance,
				locUpdListener);
	}

	public void executeLocationTask() {
		new LocationUpdateTask().execute();
	}

	// broadcast user location; done once
	protected void publishUserLocation(int result, Location userLocation) {
		if (userLocation != null) {// you can never check enough!
			// Send Broadcast to activities
			Intent intent = new Intent(TagNames.BROADC_LOCATION_NEW);
			// location and result based on whether user location received or
			// not
			intent.putExtra(TagNames.EXTRA_LAT, userLocation.getLatitude());
			intent.putExtra(TagNames.EXTRA_LONG, userLocation.getLongitude());
			intent.putExtra(TagNames.EXTRA_LOCATION_RESULT, result);
			sendBroadcast(intent);
		}
	}

	private void setCurrentUserLocation(Location loc) {
		userLocation = loc;
	}

	public Location getCurrentUserLocation() {
		return userLocation;
	}

	// return local binder, through which activity can get service instance
	@Override
	public IBinder onBind(Intent intent) {
		Log.d("LocationUpdateService onBind()", "Service is bound");
		return mBinder;
	}

	// inner class Binder, so that Activities can bind to this service
	public class ServiceBinder extends Binder {
		public LocationUpdateService getLocService() {
			return LocationUpdateService.this;// return service to bind to
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "in onDestroy in LocationService class");
		// unregister from location updates
		stopLocationUpdates();
		stopSelf();
	}

	/**
	 * The three types used by an asynchronous task are the following:
	 * 
	 * Params: the type of the parameters sent to the task upon execution.
	 * Progress: the type of the progress units published during the background
	 * computation. Result: the type of the result of the background
	 * computation.
	 */
	private class LocationUpdateTask extends AsyncTask<Void, Void, Location> {

		// retrieve last known location (may be null)
		// and pass it to onPostExecute()
		@Override
		protected Location doInBackground(Void... params) {
			Location currentLocation = getLastKnownLocation();
			return currentLocation;
		}

		@Override
		protected void onPostExecute(Location currentLocation) { // parameter is
																	// result
																	// passed
																	// from
																	// doInBackground()

			// if the received location is "valid" publish it as RESULT_OK
			if (currentLocation != null) {
				// is it better location ?
				if (isBetterLocation(currentLocation, userLocation)) {
					// set current Location in class
					setCurrentUserLocation(currentLocation);
					publishUserLocation(Activity.RESULT_OK, currentLocation);
				}

			} else { // if the location is null, set location to standard and
						// publish as RESULT_CANCELLED
				Location standardLocation = AppController.getInstance()
						.getStandardLocation();
				if (standardLocation != null) {
					// set current location to standard location
					setCurrentUserLocation(standardLocation);
					publishUserLocation(Activity.RESULT_CANCELED,
							standardLocation);
				} else {
					throw new IllegalArgumentException(
							"Standard Location is null");
				}

			}
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	} // Ansynctask end

	// Returns whether location is a better location than currentBestLocation
	public boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
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
		Location oldLocation = AppController.getInstance()
				.getStandardLocation();

		@Override
		public void onLocationChanged(Location location) {
			// if location is not null and better than current location
			// broadcast it
			// oldLocation = getCurrentUserLocation();
			if (location != null) {
				if (isBetterLocation(location, oldLocation)) {
					Log.i("LocationUpdateListener.onLocationChanged(): ",
							"Update of user location received from "
									+ location.getProvider());
					Intent intent = new Intent(TagNames.BROADC_LOCATION_UPDATED);
					intent.putExtra(TagNames.EXTRA_LAT, location.getLatitude());
					intent.putExtra(TagNames.EXTRA_LONG,
							location.getLongitude());
					intent.putExtra(TagNames.EXTRA_PROVIDER,
							location.getProvider());
					intent.putExtra(TagNames.EXTRA_LOCATION_RESULT,
							Activity.RESULT_OK);
					// broadcast to all activities that want location updates
					sendBroadcast(intent);
					setCurrentUserLocation(location);
					oldLocation = location;
				}
			}

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	}
}
