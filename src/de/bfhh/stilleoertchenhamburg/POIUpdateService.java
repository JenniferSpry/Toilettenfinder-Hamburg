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

public class POIUpdateService extends IntentService {
	
	private static final String TAG = POIUpdateService.class.getSimpleName();
	
	private ArrayList<HashMap<String, String>> poiList; //List with POI data from JSON Request

	public POIUpdateService() {
		super("POI Update Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent");
		// TODO: Get Data from Server and update Database if necessary
		// Fetch Pois from Database depending on lat, long and radius
		Bundle bundle = intent.getExtras();
		if(bundle != null){
			double userLat = bundle.getDouble(TagNames.EXTRA_LAT);
			double userLng = bundle.getDouble(TagNames.EXTRA_LONG);
			//double userRadius = bundle.getDouble(TagNames.EXTRA_RADIUS);
			//request JSON Array and add results to ArrayList, then send Broadcast with ArrayList
			makeJsonArrayRequest(userLat, userLng);
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }	
	
	//broadcast results: userLat, userLng and poiList
	private void broadcastPoiList(double userLat, double userLng){      
		
  	  	Intent i2 = new Intent(TagNames.BROADC_POIS);
  	  	i2.putExtra(TagNames.EXTRA_POI_LIST,(Serializable) poiList);
  	  	i2.putExtra(TagNames.EXTRA_LAT, userLat);
  	  	i2.putExtra(TagNames.EXTRA_LONG, userLng);
  	  	i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	
  	  	sendBroadcast(i2); 	  	
	}
	
	private void makeJsonArrayRequest(final double userLat, final double userLng) {
		
		Log.d(TAG, "makeJsonArrayRequest");
		
		String url = AppController.getInstance().getToiletsURL();
		Log.d("URL", url);
		
		JsonArrayRequest req = new JsonArrayRequest(url,
			new Response.Listener<JSONArray>() {
				@Override
				public void onResponse(JSONArray json) {
					
					final String ID = "id";
					final String NAME = "name";
					final String ADDRESS = "address";
					final String DESCR = "description";
					final String LNG = "longitude";
					final String LAT = "latitude";
				
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
		            
					broadcastPoiList(userLat, userLng);
					
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
