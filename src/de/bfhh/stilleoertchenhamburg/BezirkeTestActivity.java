package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class BezirkeTestActivity extends ListActivity {

	// Progress Dialog
	private ProgressDialog pDialog;
	
	//Comment Steffi from http://developer.android.com/guide/topics/ui/dialogs.html 
	/*Avoid ProgressDialog

	Android includes another dialog class called ProgressDialog that shows a 
	dialog with a progress bar. However, if you need to indicate loading or 
	indeterminate progress, you should instead follow the design guidelines 
	for Progress & Activity and use a ProgressBar in your layout.
	*/

	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();

	ArrayList<HashMap<String, String>> bezirkeList;

	// url to get all bezirke list
	private static String url_get_bezirke = "http://bfhhtestapi.jenniferspry.com/get_bezirke.php";

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
			JSONObject json = jParser.makeHttpRequest(url_get_bezirke, "GET", params);
			
			// Check your log cat for JSON reponse
			Log.d("Alle Bezirke: ", json.toString());

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
