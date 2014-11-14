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

import de.bfhh.stilleoertchenhamburg.helpers.DatabaseHelper;
import de.bfhh.stilleoertchenhamburg.models.POI;

/*
 * This class makes the JSON request to the bf-hh Server and 
 * fills an ArrayList (poiList) with the results of this request. 
 */

public class POIUpdateService extends IntentService {
	
	private static final String TAG = POIUpdateService.class.getSimpleName();
	
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
			if (DatabaseHelper.getInstance(getApplicationContext()).isDataStillFresh()) {
				getPOIFromDatabase(userLat, userLng);
			} else {
				refreshDatabaseAndBroadcast(userLat, userLng);
			}
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }	
	
	//broadcast results: userLat, userLng and poiList
	private void broadcastPoiList(double userLat, double userLng, ArrayList<POI> poiList){      
		
  	  	Intent i2 = new Intent(TagNames.BROADC_POIS);
  	  	i2.putParcelableArrayListExtra(TagNames.EXTRA_POI_LIST, poiList);
  	  	i2.putExtra(TagNames.EXTRA_LAT, userLat);
  	  	i2.putExtra(TagNames.EXTRA_LONG, userLng);
  	  	i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	
  	  	sendBroadcast(i2);
	}
	
	private void getPOIFromDatabase(final double userLat, final double userLng){
		// TODO: If the data is still fresh just fetch it from the database
		// will not jet happen
	}
	
	private void refreshDatabaseAndBroadcast(final double userLat, final double userLng) {
		
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
					final String WEBSITE = "website";
					final String LNG = "longitude";
					final String LAT = "latitude";
				
				try {
					// Parsing json array response
					//Log.d("JSON", json.toString());
					
					ArrayList<POI> poiList = new ArrayList<POI>();
					
					// loop through each json object
					for (int i = 0; i < json.length(); i++) {
						JSONObject c = json.getJSONObject(i);

						POI poi = new POI(
								c.getInt(ID),
								c.getString(NAME),
								c.getString(ADDRESS),
								c.getString(DESCR),
								c.getString(WEBSITE),
								c.getDouble(LAT),
								c.getDouble(LNG));
						
						// adding HashMap to ArrayList
						poiList.add(poi);
					}					
		            
					DatabaseHelper.getInstance(getApplicationContext()).refreshAllPOI(poiList);
					// TODO: fetch from database only nearest POIs
					broadcastPoiList(userLat, userLng, poiList);
					
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
