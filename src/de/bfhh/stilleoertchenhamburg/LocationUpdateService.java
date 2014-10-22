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
import com.google.android.gms.maps.model.LatLng;

import de.bfhh.stilleoertchenhamburg.activites.ActivityMain;
import de.bfhh.stilleoertchenhamburg.activites.ActivitySplashScreen;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
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
					userLocation = getLastKnownLocation();
					if(userLocation != null){
						result = Activity.RESULT_OK;
						//sends Broadcast with location
						/*new Thread(new Runnable(){
							@Override
						    public void run() { */
								publishUserLocation(); //send an intent broadcast with the current location
						/*	}
						}).start(); */
					}else{
						
						publishUserLocation(); //send an intent broadcast with invalid location
					}
				}//other action for location update maybe?
			
		
		/*
		 else if(action.equals("toiletLocation")){ //KOMMT RAUS -> POIUpdateService
			//send 
			makeJsonArrayRequest();
			//start MainActivity from IntentService POIUpdateService after JSONArrayRequest
		}
		*/
		return super.onStartCommand(intent, flags, startId);
	}


	private void publishUserLocation(){
			final Location location = getCurrentUserLocation();
	        //Start Axtivity main
			if(location != null){
			/*
				Intent i = new Intent(getApplicationContext(), ActivityMain.class);
		  	  	i.putExtra(LAT, location.getLatitude());
		  	  	i.putExtra(LNG, location.getLongitude());
		  	  	//putExtra contentprovider at some point
		  	  	i.putExtra(RESULT, result);
		  	  	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		  	  	getApplicationContext().startActivity(i); //start Main Activity
		  	 */
		  	  	
		  	  	//start POIUpdateService in new Thread
				/*
				new Thread(new Runnable(){
					@Override
				    public void run() {
				   */
				    	int result;
						double lat, lng;
						
						
				// 2)
						//Send Boradcast back to ActivitySplashScreen
						Intent intent = new Intent(USERACTION);
						
						lat = location.getLatitude();
						lng = location.getLongitude();
						result = Activity.RESULT_OK;
						
						intent.putExtra(LAT, lat);
						intent.putExtra(LNG, lng);
						intent.putExtra(RESULT, result);
						sendBroadcast(intent);
						
				/*
				    	Intent poiIntent = new Intent(getApplicationContext(), POIUpdateService.class);
				     	poiIntent.putExtra(LAT, lat);
				     	poiIntent.putExtra(LNG, lng);
			  	  	//putExtra contentprovider at some point
				     	poiIntent.putExtra(RESULT, result);
				        // add action info 
				        poiIntent.putExtra(POIUpdateService.POIACTION, "POIUpdate");//POIACTIOn = null in debugger
				        startService(poiIntent);
				        //WHY IS SERVICE NOT STARTED???
				         
				         */
				 /*   }
				}).start(); */
	  	  	}else{
	  	  		//no location received
		  	  	Intent intent = new Intent(USERACTION);
				
				double lat = HAMBURG.latitude;
				double lng = HAMBURG.longitude;
				result = Activity.RESULT_CANCELED;
				
				intent.putExtra(LAT, lat);
				intent.putExtra(LNG, lng);
				intent.putExtra(RESULT, result);
				sendBroadcast(intent);
	  	  	}

  	  	
		
		//wait till timeout is done, then start main activity with location info
	    /*new Handler().postDelayed(new Runnable() {
	    	
      	  	@Override
      	  	public void run() {
          	  	// This method will be executed once the timer is over
      	  		
      	  	}
        }, SPLASH_TIME_OUT);*/
	    
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
*/
	
}
