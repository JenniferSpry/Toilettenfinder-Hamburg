package de.bfhh.stilleoertchenhamburg;

import java.util.Properties;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import de.bfhh.stilleoertchenhamburg.helpers.AssetsPropertyReader;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

public class AppController extends Application {
	
	// to load config.properties
	private AssetsPropertyReader assetsPropertyReader;
	private Properties properties;
    private Context context;

	public static final String TAG = AppController.class.getSimpleName();  
    private RequestQueue mRequestQueue;
    private static AppController mInstance;
  
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        assetsPropertyReader = new AssetsPropertyReader(context);
        properties = assetsPropertyReader.getProperties("config.properties");
        
        mInstance = this;
    }
  
    public static synchronized AppController getInstance() {
        return mInstance;
    }
  
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
    
    public String getBezirkeURL(){
    	return properties.getProperty("BasePath") + properties.getProperty("URLBezirke");
    }
}