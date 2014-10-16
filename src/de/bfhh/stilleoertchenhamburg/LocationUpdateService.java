package de.bfhh.stilleoertchenhamburg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

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
	
	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();
	
	JSONArray toilets = null;// products JSONArray
	
	private static String url_get_toilets = "http://barrierefreieshamburg.de/androidAPI/get_stores.php";
	
	// JSON Node names
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_TOILETS = "toilets";//??
	private static final String TAG_CATID = "kategorie";
	private static final String TAG_NAME = "name";
	private static final String TAG_LAT = "latitude";
	private static final String TAG_LONG = "longitude";
	
	private ArrayList<HashMap<String, String>> poiList; 
	
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
	
	//protected void onHandleIntent(Intent intent) {
	public int onStartCommand(Intent intent, int flags, int startId){
		// In this method the 
		String action = intent.getStringExtra(POIACTION); //@TODO: add action static string in activities
		if(action.equals("userLocation")){//a user's location update was requested from an activity
			Location currentLoc = getLastKnownLocation();
			if(currentLoc != null){
				result = Activity.RESULT_OK;
				publishUserLocation(currentLoc, result); //send an intent broadcast with the current location
				
			}else{
				result = Activity.RESULT_CANCELED;
				Log.d("current user location is null", action);
				publishUserLocation(null, result); //send an intent broadcast with invalid location
			}
		}else if(action.equals("toiletLocation")){
			//send 
			new LoadAllToilets().execute();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void publishUserLocation(Location location, int result){
		Intent intent = new Intent(USERACTION);
		if(location != null){
		    intent.putExtra(LAT, location.getLatitude());
		    intent.putExtra(LNG, location.getLongitude());
		}
	    intent.putExtra(RESULT, result);
	    sendBroadcast(intent);
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

	}
	
	class LoadAllToilets extends AsyncTask<Void, Void, Void> {

		/**
		 * Before starting background thread 
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();			
		}

		/**
		 * getting All toilets from url
		 * */
		protected Void doInBackground(Void... arg0) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
					
			poiList = new ArrayList<HashMap<String, String>>();
			
			//maybe this isn't a good idea:
			Location userLocation = getLastKnownLocation();
			//add all the url parameters
			//?kategorie=26&lat=53.5653&long=10.0014&status=1&radius=0.5
			params.add(new BasicNameValuePair(TAG_CATID, "127"));
			params.add(new BasicNameValuePair("lat", String.valueOf(userLocation.getLatitude())));
			params.add(new BasicNameValuePair("long", String.valueOf(userLocation.getLongitude())));
			params.add(new BasicNameValuePair("status", "1"));
			params.add(new BasicNameValuePair("radius", "1")); //set depending on zoom
			
			// getting JSON string from URL
			JSONObject json = jParser.makeHttpRequest(url_get_toilets, "GET", params);			
			
			
			// Check your log cat for JSON reponse
			Log.d("Alle Bezirke: ", json.toString());

			try {
				// Checking for SUCCESS TAG
				int success = json.getInt(TAG_SUCCESS);

				if (success == 1) {
					// products found
					// Getting Array of toilets
					toilets = json.getJSONArray("data");

					// looping through All toilets
					for (int i = 0; i < toilets.length(); i++) {
						JSONObject c = toilets.getJSONObject(i);

						// Storing each json item in variable
						
						String id = c.getString(ID);
						String name = c.getString(NAME);
						String address = c.getString(ADDRESS);
						String description = c.getString(DESCR);
						String latitude = String.valueOf(c.getDouble(LAT));
						String longitude = String.valueOf(c.getDouble(LNG));
						
						// creating new HashMap
						HashMap<String, String> map = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						map.put(ID, id);
						map.put(NAME, name);
						map.put(ADDRESS, address);
						map.put(DESCR, description);
						map.put(LAT, latitude);
						map.put(LNG, longitude);
						
						// adding HashMap to ArrayList
						poiList.add(map);
					}
					//add userLocation lat and lng to poiList
					HashMap<String,String> latLngMap = new HashMap<String,String>();
					latLngMap.put("userLatitude", String.valueOf(userLocation.getLatitude()));
					latLngMap.put("userLongitude", String.valueOf(userLocation.getLongitude()));
					
					poiList.add(latLngMap);
				} 
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(Void result) {
			/*runOnUiThread(new Runnable() {
				public void run() {
					/**
					 * Updating parsed JSON data into ListView
					 * */
					/* ListAdapter adapter = new SimpleAdapter(
							LocationUpdateService.this, poiList,
							R.layout.toilet, new String[] { 
									TAG_NAME, TAG_LAT },
							new int[] { R.id.toilet_name, R.id.latitude });
					// updating listview
					setListAdapter(adapter);
				}
			});*/
			
			super.onPostExecute(result);
            // After completing http call
            // will close this activity and lauch main activity
            Intent i = new Intent("toiletLocation"); //make it a bit more generic
            //add ArrayList<HashMap<String, String>> poiList with store info as extra to intent
            i.putExtra("poiList",(Serializable) poiList);      
            i.putExtra(RESULT, -1);
            sendBroadcast(i);
			
		}

	}
}
