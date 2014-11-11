package de.bfhh.stilleoertchenhamburg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

/*
 * This class makes the JSON request to the bf-hh Server and 
 * fills an ArrayList (poiList) with the results of this request. 
 */

public class POIUpdateService extends IntentService{

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String ADDRESS = "address";
	public static final String DESCR = "description";
	public static final String LNG = "longitude"; //user's lat and long
	public static final String LAT = "latitude";
	public static final String RAD = "radius";
	public static final String RESULT = "result";
	
	public static final String POIACTION = "POIUpdate";
	public static final String POIACTION_OK = "POIUpdate_OK";
	public static final String TOILET_LOCATION = "toiletLocation";
	
	private ArrayList<HashMap<String, String>> poiList; //List with POI data from JSON Request

	public POIUpdateService() {
		super("POI Update Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {	
		String action = intent.getStringExtra(POIACTION); //@TODO: add action static string in activities
		Bundle bundle = intent.getExtras();
		if(bundle != null){
			//data passed by SplashScreen Activity
			int result = bundle.getInt(RESULT);
			double userLat = bundle.getDouble(LAT);
			double userLng = bundle.getDouble(LNG);
			double userRadius = bundle.getDouble(RAD);
			if(action.equals("POIUpdate")){
				//request JSON Array and add results to ArrayList, then send Broadcast with ArrayList
				makeJsonArrayRequest(result, userLat, userLng);
			}
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }	
	
	//broadcast results: userLat, userLng, result code and poiList
	private void broadCastToActivity(int result, double userLat, double userLng){      
		
		//send broadcast to all activities, incl. ActivitySplash, so it can terminate itself
  	  	Intent i2 = new Intent(POIACTION_OK);
  	  	i2.putExtra("poiList",(Serializable) poiList);
  	  	i2.putExtra(LAT, userLat);
  	  	i2.putExtra(LNG, userLng);
  	  	//putExtra contentprovider at some point
  	  	i2.putExtra(RESULT, result);
  	  	i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	
  	  	sendBroadcast(i2); 	  	
	}
	
	private void makeJsonArrayRequest(final int result, final double userLat, final double userLng) {
		
		String url = AppController.getInstance().getStoresURL(
				String.valueOf(userLat), 
				String.valueOf(userLng),
				"70");
		
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
		            
					broadCastToActivity(result, userLat, userLng);
					
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
//			// TODO: lets use this later to make sure our "API" only responds to this app
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
