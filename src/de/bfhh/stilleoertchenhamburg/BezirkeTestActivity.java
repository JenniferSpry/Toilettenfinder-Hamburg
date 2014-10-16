package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * TODO: Vernünftige Fehlermeldung, wenn die Daten nicht kommen
 * @author Jenne
 *
 */

public class BezirkeTestActivity extends ListActivity {
	
	// to load config.properties
	private AssetsPropertyReader assetsPropertyReader;
    private Context context;
    private Properties properties;

	// Progress Dialog
	private ProgressDialog pDialog;
	
	//Comment Steffi from http://developer.android.com/guide/topics/ui/dialogs.html 
	/*Avoid ProgressDialog

	Android includes another dialog class called ProgressDialog that shows a 
	dialog with a progress bar. However, if you need to indicate loading or 
	indeterminate progress, you should instead follow the design guidelines 
	for Progress & Activity and use a ProgressBar in your layout.
	*/

	ArrayList<HashMap<String, String>> bezirkeList;
	
	private static String TAG = BezirkeTestActivity.class.getSimpleName();
	private String jsonResponse;


	// JSON Node names
	private static final String TAG_BEZIRK_ID = "bezirk_id";
	private static final String TAG_NAME = "name";

	// products JSONArray
	JSONArray bezirke = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bezirketest);
		
		Log.d(TAG, "loading bezirketest");
		
		// load properties
		context = this;
        assetsPropertyReader = new AssetsPropertyReader(context);
        properties = assetsPropertyReader.getProperties("config.properties");
        
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);

		// Hashmap for ListView
		bezirkeList = new ArrayList<HashMap<String, String>>();

		makeJsonArrayRequest();
	}
	
	/**
	 * Method to make json array request where response starts with [
	 * */
	private void makeJsonArrayRequest() {

		showpDialog();
	
		String url = properties.getProperty("BasePath") + properties.getProperty("URLBezirke");
		
		JsonArrayRequest req = new JsonArrayRequest(url,
			new Response.Listener<JSONArray>() {
				@Override
				public void onResponse(JSONArray response) {
					Log.d(TAG, response.toString());
				
				try {
					// Parsing json array response
					// loop through each json object
					jsonResponse = "";
					for (int i = 0; i < response.length(); i++) {
					
						JSONObject bezirk = (JSONObject) response.get(i);
						
						String id = bezirk.getString(TAG_BEZIRK_ID);
						String name = bezirk.getString(TAG_NAME);

						// creating new HashMap
						HashMap<String, String> map = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						map.put(TAG_BEZIRK_ID, id);
						map.put(TAG_NAME, name);

						// adding HashList to ArrayList
						bezirkeList.add(map);
					}
					
					ListAdapter adapter = new SimpleAdapter(
							BezirkeTestActivity.this, bezirkeList,
							R.layout.bezirk, new String[] { TAG_BEZIRK_ID,
									TAG_NAME},
							new int[] { R.id.bezirk_id, R.id.bezirk });
					// updating listview
					setListAdapter(adapter);
					
				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(),
						"Error: " + e.getMessage(),
						Toast.LENGTH_LONG).show();
				}
				
				hidepDialog();
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				VolleyLog.d(TAG, "Error: " + error.getMessage());
				Toast.makeText(getApplicationContext(),
					error.getMessage(), Toast.LENGTH_SHORT).show();
					hidepDialog();
				}
			}
		);
	
		// Adding request to request queue
		AppController.getInstance().addToRequestQueue(req);
	}

	private void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }
 
    private void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

}
