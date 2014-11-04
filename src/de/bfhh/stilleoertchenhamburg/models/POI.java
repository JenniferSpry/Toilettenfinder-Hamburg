package de.bfhh.stilleoertchenhamburg.models;

import java.util.HashMap;

import com.google.android.gms.maps.model.LatLng;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class POI implements Parcelable{
	
	//können wir die final machen? 
	//-> wenn final, müssen sie im constructor initialisiert werden
	
	private static final String TAG_ID = "id";
	private static final String TAG_NAME = "name";
	private static final String TAG_LAT = "latitude";
	private static final String TAG_LNG = "longitude";
	private static final String TAG_ADR = "address";
	private static final String TAG_DESCR = "description";
	
	private final int id; //ID from DB
	private final String name;
	private final double lat;
	private final double lng;
	private final String address;
	private final String description;
	private final Location location; //location (lat lng)
	private final LatLng latLng;
	
	private float distanceToUser; //distance to user's current position in meters
	

	public POI(int id, String name, double lat, double lng, String address, String description){
		this.id = id;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.address = address;
		this.description = description;
		this.location = new Location("");//empty provider string
		location.setLatitude(this.lat);
        location.setLongitude(this.lng);
        latLng = new LatLng(lat, lng);
	}
	
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public double getLat(){
		return lat;
	}
	
	public double getLng(){
		return lng;
	}
	
	public LatLng getLatLng(){
		return latLng;
	}
	
	public String getAddress() {
		return address;
	}

	public String getDescription() {
		return description;
	}
	
	public Location getLocation(){		
		return location;	
	}
	//so if the user's position changes, every instance of poi/toilet needs to be updated
	//-> isn't that inefficient? 
	//-> Listener?? and save userlocation in here as instance variable?
	public void setDistanceToUser(Location userLocation){
		this.distanceToUser = this.location.distanceTo(userLocation);
	}
	//returns distance to user in meters (float)
	public float getDistanceToUser(){
		return distanceToUser;
	}
	//returns distance to user in meters (int)
	public int getDistanceToUserInt(){
		return (int) distanceToUser;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
	
	public HashMap<String, String> getHashMap(){
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		// adding each child node to HashMap key => value
		// some have to be typecast
		map.put(TAG_ID, String.valueOf(this.id));
		map.put(TAG_NAME, this.name);
		map.put(TAG_LAT, String.valueOf(this.lat));
		map.put(TAG_LNG, String.valueOf(this.lng));
		map.put(TAG_ADR, this.address);
		map.put(TAG_DESCR, this.description);

		return map;		
	}
}
