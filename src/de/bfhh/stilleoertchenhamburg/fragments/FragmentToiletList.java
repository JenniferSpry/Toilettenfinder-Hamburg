package de.bfhh.stilleoertchenhamburg.fragments;

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

import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
    private List<POI> pois;
    private ArrayList<HashMap<String, String>> poiList;
    private POIController poiController;
	private double userLat;
	private double userLng;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_toilet_list, null, false);
    }

    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent i = getActivity().getIntent();
        Bundle bundle = i.getExtras();
        if(bundle != null){
        	if(bundle.getDouble(TagNames.EXTRA_LAT) != 0.0 && bundle.getDouble(TagNames.EXTRA_LONG) != 0.0){
        		//set user Position
        		userLat = bundle.getDouble(TagNames.EXTRA_LAT);
        		userLng = bundle.getDouble(TagNames.EXTRA_LONG);
        		poiList = (ArrayList<HashMap<String, String>>) bundle.getSerializable(TagNames.EXTRA_POI_LIST);
        		poiController = new POIController(poiList);
        		poiController.setDistancePOIToUser(userLat, userLng);
        		pois = poiController.getClosestPOI(20);
        		adapter = new AdapterToiletList(getActivity(), pois);
        	    setListAdapter(adapter);
        	}
        }
        
       
		//makeJsonArrayRequest();
    }
    
    
	private void makeJsonArrayRequest() {
	
		String url = AppController.getInstance().getToiletsURL();
		Log.d("URL", url);
		
		JsonArrayRequest req = new JsonArrayRequest(url,
			new Response.Listener<JSONArray>() {

				@Override
				public void onResponse(JSONArray response) {
				
				try {
					// Parsing json array response
					Log.d("JSON", response.toString());
					
					pois = new ArrayList<POI>();
					poiList = new ArrayList<HashMap<String, String>>();
					// loop through each json object
					for (int i = 0; i < response.length(); i++) {
					
						JSONObject poiJSON = (JSONObject) response.get(i);
						
						pois.add(new POI(
								poiJSON.getInt("id"),
								poiJSON.getString("name"),
								poiJSON.getDouble("latitude"),
								poiJSON.getDouble("longitude"),
								poiJSON.getString("address"),
								poiJSON.getString("description")
								));
						
								//add data to hashmap, then hashmap to arraylist
								String id = poiJSON.getString("id");
								String name = poiJSON.getString("name");
								String address = poiJSON.getString("address");
								String description = poiJSON.getString("description");
								String latitude = String.valueOf(poiJSON.getDouble("latitude"));
								String longitude = String.valueOf(poiJSON.getDouble("longitude"));
								
								// creating new HashMap
								HashMap<String, String> map = new HashMap<String, String>();
		
								// adding each child node to HashMap key => value
								map.put("id", id);
								map.put("name", name);
								map.put("address", address);
								map.put("description", description);
								map.put("latitude", latitude);
								map.put("longitude", longitude);
								
								// adding HashMap to ArrayList
								poiList.add(map);
					}
					
					
					adapter = new AdapterToiletList(getActivity(), pois);
			        setListAdapter(adapter);
			        
					
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
			// lets use this later to make sure our "API" only responds to this app
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
