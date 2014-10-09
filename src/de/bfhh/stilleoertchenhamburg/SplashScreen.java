package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
 
public class SplashScreen extends ListActivity {
 
	ArrayList<String> names;
	ArrayList<String> latitudes, longitudes;
	
	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();

	ArrayList<HashMap<String, String>> toiletList;

	// url to get all bezirke list
	//private static String url_get_toilets = "http://bf-hh.de/httpdocs/androidAPI/db_toilets_new.php";
	private static String url_get_toilets = "http://barrierefreieshamburg.de/androidAPI/db_toilets.php";

	// JSON Node names
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_TOILETS = "toilets";
	//private static final String TAG_BEZIRK_ID = "bezirk_id";
	private static final String TAG_NAME = "name";
	private static final String TAG_LAT = "latitude";
	private static final String TAG_LONG = "longitude";

	 	
	// products JSONArray
	JSONArray toilets = null;
	
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 3000;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        //HashMap for ListView
        toiletList = new ArrayList<HashMap<String, String>>();
        
     // Loading products in Background Thread
     	new LoadAllToilets().execute();
     	
     	
        /*new Handler().postDelayed(new Runnable() {
 
            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */
        /*
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(i);
 
                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
        */
    }
    
    
    
    
 
    
    class LoadAllToilets extends AsyncTask<Void, Void, Void> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}

		/**
		 * getting All toilets from url
		 * */
		protected Void doInBackground(Void... arg0) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			// getting JSON string from URL
			
			/* FEHLER: Der zurückgelieferte String von PHP ist kein JSON sondern die HTML Seite*/
			JSONObject json = jParser.makeHttpRequest(url_get_toilets, "GET", params);
			
			// Check your log cat for JSON reponse
			Log.d("Alle Bezirke: ", json.toString());

			try {
				// Checking for SUCCESS TAG
				int success = json.getInt(TAG_SUCCESS);

				if (success == 1) {
					// products found
					// Getting Array of toilets
					toilets = json.getJSONArray(TAG_TOILETS);

					// looping through All toilets
					for (int i = 0; i < toilets.length(); i++) {
						JSONObject c = toilets.getJSONObject(i);

						// Storing each json item in variable
						//String id = c.getString(TAG_BEZIRK_ID);
						String name = c.getString(TAG_NAME);
						String latitude = String.valueOf(c.getDouble(TAG_LAT));
						String longitude = String.valueOf(c.getDouble(TAG_LONG));
						
						// creating new HashMap
						HashMap<String, String> map = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						//map.put(TAG_BEZIRK_ID, id);
						map.put(TAG_NAME, name);
						map.put(TAG_LAT, latitude);
						//map.put(TAG_LONG, longitude);
						

						// adding HashList to ArrayList
						toiletList.add(map);
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
		protected void onPostExecute(Void result) {
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {
					/**
					 * Updating parsed JSON data into ListView
					 * */
					ListAdapter adapter = new SimpleAdapter(
							SplashScreen.this, toiletList,
							R.layout.toilet, new String[] { 
									TAG_NAME, TAG_LAT },
							new int[] { R.id.toilet_name, R.id.latitude });
					// updating listview
					setListAdapter(adapter);
				}
			});
			
			/*
			super.onPostExecute(result);
            // After completing http call
            // will close this activity and lauch main activity
            Intent i = new Intent(SplashScreen.this, MainActivity.class);
            i.putExtra("now_playing", now_playing);
            i.putExtra("earned", earned);
            startActivity(i);
 
            // close this activity
            finish();
			*/
		}

	}
 
}

