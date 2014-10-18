package de.bfhh.stilleoertchenhamburg.activites;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.R.drawable;
import de.bfhh.stilleoertchenhamburg.R.id;
import de.bfhh.stilleoertchenhamburg.R.layout;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

public class ActivityMain extends ActivityMenuBase {
    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private LocationManager locationManager;
    private MyLocationListener mylistener;
    private Criteria criteria;
    private String provider;
    private Location location;
    
    private TextView latitude;
    private TextView longitude;
    
    private ImageButton myLocationButton;
    
    final private LatLng HAMBURG = new LatLng(53.558, 9.927);
    
    private Marker personInNeedOfToilette;
	private ArrayList<HashMap<String, String>> toiletList;
	private ArrayList<MarkerOptions> markerList;
	
	private ArrayList<String> keyNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        //Button that will animate camera back to user position
        myLocationButton = (ImageButton) findViewById(R.id.mylocation);
        
        
        setUpMapIfNeeded();
        
        /*
	    // Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);	//default
		
		// get the best provider depending on the criteria
		provider = locationManager.getBestProvider(criteria, false);
	    
		// the last known location of this provider
		location = locationManager.getLastKnownLocation(provider); */
        
        //get the Intent that was sent from SplashScreen class
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        
        toiletList = (ArrayList<HashMap<String,String>>) i.getSerializableExtra("poiList");
        markerList = new ArrayList<MarkerOptions>();
        
       //default userLocation
        double userLat = HAMBURG.latitude;
        double userLng = HAMBURG.longitude;
        
        //parse through toiletList and set markers
        for(int j = 0; j < toiletList.size(); j++){
        	HashMap<String,String> hMap = toiletList.get(j);
        	 double lat = HAMBURG.latitude;
             double lng = HAMBURG.longitude;
             int id;
             String name = "name";
             String adr;
             String descr;
             MarkerOptions marker = new MarkerOptions();
            
        	for (Entry<String, String> entry : hMap.entrySet()) {
        		String keyName = entry.getKey();
        		
        		if(j == toiletList.size() - 1){ //the last Hashmap has user Coordinates
        			switch (keyName){
        				case "userLatitude":
        					userLat = Double.valueOf(entry.getValue());
        					break;
        				case "userLongitude":
        					userLng = Double.valueOf(entry.getValue());
        					break;
        			}
        		}               
 
                switch (keyName){
                	case "id":
                		id = Integer.valueOf(entry.getValue());
                		break;
                	case "name":
                		name = entry.getValue();
                		break;
                	case "address":
                		adr = entry.getValue();
                		break;
                	case "description":
                		descr = entry.getValue();
                		break;
                	case "latitude":
                		lat = Double.valueOf(entry.getValue());
                		break;
                	case "longitude":
                		lng = Double.valueOf(entry.getValue());
                		break;
                }
            }//end for
        	marker.position(new LatLng(lat, lng)).title(name);
            // adding marker
            mMap.addMarker(marker);
            markerList.add(marker);   	
        }
        
        
        location = new Location("");//empty provider string
        //double lat = bundle.getDouble("lat");
        //double lng = bundle.getDouble("lng");

        location.setLatitude(userLat);
        location.setLongitude(userLng);
		
		//Location Button Listener
		myLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	moveToLocation(location);
            }
        });
		
		mylistener = new MyLocationListener();
	
		if (location != null) {
			mylistener.onLocationChanged(location);
		} else {
			// leads to the settings because there is no last known location
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
		// location updates: at least 1 meter and 200millsecs change
		
		//WHERE TO UPDATE LOCATIONS FROM?? -> inner class locationListener in LocationUpdateService a la  http://stackoverflow.com/questions/14478179/background-service-with-location-listener-in-android
		//locationManager.requestLocationUpdates(provider, 200, 1, mylistener);
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }
    

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have nfot already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
           
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
    	// need to turn this off so we can use our own icon
        //mMap.setMyLocationEnabled(true); 
    	personInNeedOfToilette = mMap.addMarker(new MarkerOptions()
	    	.position(HAMBURG)
	    	.icon(BitmapDescriptorFactory.fromResource(R.drawable.peeer)));
    }
    
    private void moveToLocation(Location loc){
    	LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
        personInNeedOfToilette.setPosition(pos);
    }
    
    private class MyLocationListener implements LocationListener {
    	
		  @Override
		  public void onLocationChanged(Location location) {
			// Initialize the location fields
			  latitude.setText("Latitude: "+String.valueOf(location.getLatitude()));
			  longitude.setText("Longitude: "+String.valueOf(location.getLongitude()));
			  moveToLocation(location);
		  }
	
		  @Override
		  public void onStatusChanged(String provider, int status, Bundle extras) {
			  Toast.makeText(ActivityMain.this, provider + "'s status changed to "+status +"!",
				        Toast.LENGTH_SHORT).show();
		  }
	
		  @Override
		  public void onProviderEnabled(String provider) {
			  Toast.makeText(ActivityMain.this, "Provider " + provider + " enabled!",
		        Toast.LENGTH_SHORT).show();
	
		  }
	
		  @Override
		  public void onProviderDisabled(String provider) {
			  Toast.makeText(ActivityMain.this, "Provider " + provider + " disabled!",
		        Toast.LENGTH_SHORT).show();
		  }
	  }
}