package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;
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
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
    private List<POI> pois;

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, null, false);
    }

    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
		makeJsonArrayRequest();
    }
    
    
	private void makeJsonArrayRequest() {
	
		String url = AppController.getInstance().getStoresURL("53.5653", "10.0014", "1");
		Log.d("URL", url);
		
		JsonArrayRequest req = new JsonArrayRequest(url,
			new Response.Listener<JSONArray>() {
				@Override
				public void onResponse(JSONArray response) {
				
				try {
					// Parsing json array response
					Log.d("JSON", response.toString());
					
					pois = new ArrayList<POI>();
					
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
