package de.bfhh.stilleoertchenhamburg.activites;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.bfhh.stilleoertchenhamburg.LocationUpdateService;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.R.id;
import de.bfhh.stilleoertchenhamburg.R.layout;
import de.bfhh.stilleoertchenhamburg.helpers.JSONParser;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;
 
public class ActivitySplashScreen extends ListActivity {
 
	ArrayList<String> names;
	ArrayList<String> latitudes, longitudes;
	
	// Creating JSON Parser object
	JSONParser jParser = new JSONParser();

	ArrayList<HashMap<String, String>> toiletList;

	// url to get all bezirke list
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
    private static int SPLASH_TIME_OUT = 1000;
    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
    	private double lat = 0.0;
    	private double lng = 0.0;
    	
        @Override
        public void onReceive(Context context, Intent intent) {
          Bundle bundle = intent.getExtras();
          String action = intent.getAction();
          if(action.equals("toiletLocation")){
        	  //toiletList = intent.getParcelableExtra("poiList");
        	  toiletList = (ArrayList<HashMap<String,String>>) intent.getSerializableExtra("poiList");
        	  int resultCode = bundle.getInt(LocationUpdateService.RESULT);
        	  if (resultCode == RESULT_OK) {
	        	  
        		  runOnUiThread(new Runnable() {
	  				 public void run() {
	  					/**
	  					 * Updating parsed JSON data into ListView
	  					 * */
        		  	ListAdapter adapter = new SimpleAdapter(
	  							ActivitySplashScreen.this, toiletList,
	  							R.layout.toilet, new String[] { 
	  									TAG_NAME, TAG_LAT },
	  							new int[] { R.id.toilet_name, R.id.toilet_description });
	  					// updating listview
	  					setListAdapter(adapter);
	  				 }
	  				 
	  			   });
	        	  
	        	  // After receiving the ArrayList from LocationUpdateService
	              // will close this activity and lauch main activity
	              // wait 3 seconds to show the logo, otherwise SplashScreen will not be seen
	              new Handler().postDelayed(new Runnable() {
	            	  @Override
	            	  public void run() {
		            	  // This method will be executed once the timer is over
		            	  // Start your app main activity
		            	  Intent i = new Intent(ActivitySplashScreen.this, ActivityMain.class);
		            	  i.putExtra("poiList", (Serializable) toiletList); //send lat and long to main activity

		            	  startActivity(i);
		            	  
		            	  // close this activity
		            	  finish();
	            	  }
	              }, SPLASH_TIME_OUT);
        	  }
          }else{
	          if (bundle != null) {
	            lat = bundle.getDouble(LocationUpdateService.LAT);
	            lng = bundle.getDouble(LocationUpdateService.LNG);
	            int resultCode = bundle.getInt(LocationUpdateService.RESULT);
	
	            if (resultCode == RESULT_OK) {
	              Toast.makeText(ActivitySplashScreen.this,
	                  "Location successfully received.  LAT: " + Double.valueOf(lat) + ", LNG: " + Double.valueOf(lng),
	                  Toast.LENGTH_LONG).show();
	              
	
	              //can i send the whole location from locationUpdateService to SplashScreen to MainActivity?
	              //can i send an arraylist of Toilets (extends POI) from LocationUpdateService (via intent.putExtra("ToiletList", ArrayList<Toilet>) http://stackoverflow.com/questions/2736389/how-to-pass-object-from-one-activity-to-another-in-android/2736612#2736612 
	              //public ArrayList<T> getParcelableArrayListExtra (String name)-> make POI implements Parcelable
	
	              
	              // After receiving position from LocationUpdateService
	              // will close this activity and lauch main activity
	              // wait 3 seconds to show the logo, otherwise SplashScreen will not be seen
	              new Handler().postDelayed(new Runnable() {
	            	  @Override
	            	  public void run() {
		            	  // This method will be executed once the timer is over
		            	  // Start your app main activity
		            	  Intent i = new Intent(ActivitySplashScreen.this, ActivityMain.class);
		            	  i.putExtra("lat", lat); //send lat and long to main activity
		                  i.putExtra("lng", lng);
		            	  startActivity(i);
		            	  
		            	  // close this activity
		            	  finish();
	            	  }
	              }, SPLASH_TIME_OUT);
	              
	            } else {
	              Toast.makeText(ActivitySplashScreen.this, "Location not received. Error",
	                  Toast.LENGTH_LONG).show();
	            }
	          }
           }
        }
    };
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        //HashMap for ListView
        toiletList = new ArrayList<HashMap<String, String>>();
        
     // Loading products in Background Thread
     	//new LoadAllToilets().execute();
     	
     	//start service with Intent to get User current position
     	Intent intent = new Intent(this, LocationUpdateService.class);
        // add infos for the service which action to 
        intent.putExtra(LocationUpdateService.POIACTION, "toiletLocation");
        startService(intent);
        
        //Intent intent2 = new Intent(this.LocationUpdateService.class);

    }
    
    @Override
    protected void onResume(){
    	super.onResume();
        registerReceiver(receiver, new IntentFilter(LocationUpdateService.POIACTION));
    }
    
    @Override
    protected void onPause() {
      super.onPause();
      unregisterReceiver(receiver);
    }
    
   /* class LoadAllToilets extends AsyncTask<Void, Void, Void> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
	/*	@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}

		/**
		 * getting All toilets from url
		 * */
	/*	protected Void doInBackground(Void... arg0) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			
			
			//add 
			
			// getting JSON string from URL
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
*/
		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
	/*	protected void onPostExecute(Void result) {
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {
					/**
					 * Updating parsed JSON data into ListView
					 * */
		/*			ListAdapter adapter = new SimpleAdapter(
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
	/*	}

	}*/
 
}

