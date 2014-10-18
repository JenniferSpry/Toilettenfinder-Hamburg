package de.bfhh.stilleoertchenhamburg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

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
		
	JSONArray toilets = null;// products JSONArray
	
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
			makeJsonArrayRequest();
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
	
	private void makeJsonArrayRequest() {
		Location userLocation = getLastKnownLocation();
		String url = AppController.getInstance().getStoresURL(
				String.valueOf(userLocation.getLatitude()), 
				String.valueOf(userLocation.getLongitude()), "70");
		
		JsonArrayRequest req = new JsonArrayRequest(url,
			new Response.Listener<JSONArray>() {
				@Override
				public void onResponse(JSONArray json) {
				
				try {
					// Parsing json array response
					Log.d("JSON", json.toString());
					
					poiList = new ArrayList<HashMap<String, String>>();
					
					// loop through each json object
					for (int i = 0; i < json.length(); i++) {
						JSONObject c = json.getJSONObject(i);

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
					Location userLocation = getLastKnownLocation();
					HashMap<String,String> latLngMap = new HashMap<String,String>();
					latLngMap.put("userLatitude", String.valueOf(userLocation.getLatitude()));
					latLngMap.put("userLongitude", String.valueOf(userLocation.getLongitude()));
					
					poiList.add(latLngMap);
					
					Intent i = new Intent("toiletLocation"); //make it a bit more generic
		            //add ArrayList<HashMap<String, String>> poiList with store info as extra to intent
		            i.putExtra("poiList",(Serializable) poiList);      
		            i.putExtra(RESULT, -1);
		            sendBroadcast(i);
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				VolleyLog.d("Error: " + error.getMessage());
				}
			}) {
//			// lets use this later to make sure our "API" only responds to this app
//			@Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                HashMap<String, String> headers = new HashMap<String, String>();
//                headers.put("Content-Type", "application/json");
//                headers.put("apiKey", "xxxxxxxxxxxxxxx");
//                return headers;
//            }
		};
	
		Log.d("REQUEST", req.toString());
		AppController.getInstance().addToRequestQueue(req);
	}
	
	
}
