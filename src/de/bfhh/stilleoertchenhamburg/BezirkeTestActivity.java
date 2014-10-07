package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

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

	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();

	ArrayList<HashMap<String, String>> bezirkeList;


	// JSON Node names
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_BEZIRKE = "bezirke";
	private static final String TAG_BEZIRK_ID = "bezirk_id";
	private static final String TAG_NAME = "name";

	// products JSONArray
	JSONArray bezirke = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bezirketest);
		
		// load properties
		context = this;
        assetsPropertyReader = new AssetsPropertyReader(context);
        properties = assetsPropertyReader.getProperties("config.properties");

		// Hashmap for ListView
		bezirkeList = new ArrayList<HashMap<String, String>>();

		// Loading products in Background Thread
		new LoadAllBezirke().execute();
	}

	/**
	 * Background Async Task to Load all product by making HTTP Request
	 * */
	class LoadAllBezirke extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(BezirkeTestActivity.this);
			pDialog.setMessage("Bezirke werden geladen, bitte warten...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting All bezirke from url
		 * */
		protected String doInBackground(String... args) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			// getting JSON string from URL
			String url = properties.getProperty("BasePath") + properties.getProperty("URLBezirke");
			JSONObject json = jParser.makeHttpRequest(url, "GET", params);
			
			//Log.d("Alle Bezirke: ", json.toString());

			try {
				// Checking for SUCCESS TAG
				int success = json.getInt(TAG_SUCCESS);

				if (success == 1) {
					// products found
					// Getting Array of bezirke
					bezirke = json.getJSONArray(TAG_BEZIRKE);

					// looping through All bezirke
					for (int i = 0; i < bezirke.length(); i++) {
						JSONObject c = bezirke.getJSONObject(i);

						// Storing each json item in variable
						String id = c.getString(TAG_BEZIRK_ID);
						String name = c.getString(TAG_NAME);

						// creating new HashMap
						HashMap<String, String> map = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						map.put(TAG_BEZIRK_ID, id);
						map.put(TAG_NAME, name);

						// adding HashList to ArrayList
						bezirkeList.add(map);
					}
				} 
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {
					/**
					 * Updating parsed JSON data into ListView
					 * */
					ListAdapter adapter = new SimpleAdapter(
							BezirkeTestActivity.this, bezirkeList,
							R.layout.bezirk, new String[] { TAG_BEZIRK_ID,
									TAG_NAME},
							new int[] { R.id.bezirk_id, R.id.bezirk });
					// updating listview
					setListAdapter(adapter);
				}
			});

		}

	}
}