package de.bfhh.stilleoertchenhamburg.services;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import de.bfhh.stilleoertchenhamburg.AppController;
import de.bfhh.stilleoertchenhamburg.TagNames;

/**
 * This class makes a JSON request to the bf-hh Server, 
 * where an e-mail will be sent to whoever is in charge of comments.
 */
public class SendMailService extends IntentService {
	
	private static final String TAG = SendMailService.class.getSimpleName();
	
	public SendMailService() {
		super("Send mail Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		sendMail(
				(String)bundle.get(TagNames.EXTRA_E_MAIL),
				(String)bundle.get(TagNames.EXTRA_NAME),
				(String)bundle.get(TagNames.EXTRA_COMMENT));
	}
	
	
	private void sendMail(String eMail, String name, String comment) {	
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("name", name);
		params.put("emailaddress", eMail);
		params.put("content", comment);
		
		JsonObjectRequest req = new JsonObjectRequest(AppController.getInstance().getMailURL(), new JSONObject(params), 
			new Response.Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject json) {
					VolleyLog.d("Response received");
					Log.d(TAG, json.toString());
					stopSelf();
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				VolleyLog.d("Error: " + error.getMessage());
				stopSelf();
				}
			}
		){     
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
