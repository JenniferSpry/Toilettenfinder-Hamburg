package de.bfhh.stilleoertchenhamburg;

import java.util.Properties;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import de.bfhh.stilleoertchenhamburg.helpers.AssetsPropertyReader;

import android.app.Application;
import android.location.Location;
import android.text.TextUtils;

public class AppController extends Application {
	
	public static final String TAG = AppController.class.getSimpleName();  
	
	// to load config.properties
	private AssetsPropertyReader assetsPropertyReader;
	private Properties properties;
	
    private RequestQueue mRequestQueue;
    private static AppController mInstance;
  
    @Override
    public void onCreate() {
        super.onCreate();
        assetsPropertyReader = new AssetsPropertyReader(this);
        properties = assetsPropertyReader.getProperties("config.properties");
        
        mInstance = this;
    }
  
    public static synchronized AppController getInstance() {
        return mInstance;
    }
  
    
    /** volley methods */
    
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
  
        return mRequestQueue;
    }
  
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }
  
    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }
  
    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
    
    
    /** getter for global variables */
    
    public String getToiletsURL(){
    	return properties.getProperty("URLToilets");
    }
    
    public String getMailURL(){
    	return properties.getProperty("URLMail");
    }
    
    public String getAuthKey(){
    	return properties.getProperty("AuthKey");
    }
    
    public Location getStandardLocation(){
    	Location hamburg = new Location("");
    	if (properties.getProperty("Hamburg").contains(",")) {
    		String[] hh = properties.getProperty("Hamburg").split(",");
        	hamburg.setLatitude(Double.valueOf(hh[0]));
        	hamburg.setLongitude(Double.valueOf(hh[1]));
        	return hamburg;
    	} else {
    	    throw new IllegalArgumentException("String " + properties.getProperty("Hamburg") + " does not contain ,");
    	}	
    }
}