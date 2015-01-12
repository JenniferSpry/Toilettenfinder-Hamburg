package de.bfhh.stilleoertchenhamburg.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.helpers.DatabaseHelper;
import de.bfhh.stilleoertchenhamburg.helpers.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;

/**
 * This class makes the JSON request to the bf-hh Server, fills an ArrayList
 * (poiList) with the results of this request and broadcasts that list.
 */
public class POIUpdateService extends IntentService {

	private static final String TAG = POIUpdateService.class.getSimpleName();

	public POIUpdateService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (DatabaseHelper.getInstance(getApplicationContext())
				.isDataStillFresh(getApplicationContext())) {
			ArrayList<POI> poiList = DatabaseHelper.getInstance(
					getApplicationContext()).getAllPOI();
			Log.i(TAG, "got Data from Database");
			broadcastPoiList(poiList);
		} else {
			refreshDatabaseAndBroadcast();
		}
	}

	/** broadcast poiList */
	private void broadcastPoiList(ArrayList<POI> poiList) {
		Intent i2 = new Intent(TagNames.BROADC_POIS);
		i2.putParcelableArrayListExtra(TagNames.EXTRA_POI_LIST, poiList);
		i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		sendBroadcast(i2);
	}

	private void refreshDatabaseAndBroadcast() {

		JsonArrayRequest req = new JsonArrayRequest(AppController.getInstance()
				.getToiletsURL(), new Response.Listener<JSONArray>() {
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
					Log.d(TAG, json.toString());

					ArrayList<POI> poiList = new ArrayList<POI>();

					for (int i = 0; i < json.length(); i++) {
						JSONObject c = json.getJSONObject(i);

						POI poi = new POI(c.getInt(ID), c.getString(NAME), c
								.getString(ADDRESS).trim(), c.getString(DESCR)
								.trim(), c.getString(WEBSITE),
								c.getDouble(LAT), c.getDouble(LNG));

						poiList.add(poi);
					}

					DatabaseHelper.getInstance(getApplicationContext())
							.refreshAllPOI(poiList, getApplicationContext());
					broadcastPoiList(poiList);

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
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Content-Type", "application/json");
				headers.put("authKey", AppController.getInstance().getAuthKey());
				return headers;
			};
		};

		Log.d("REQUEST", req.toString());
		AppController.getInstance().addToRequestQueue(req);
	}

}
