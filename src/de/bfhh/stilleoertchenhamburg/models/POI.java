package de.bfhh.stilleoertchenhamburg.models;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.google.android.gms.maps.model.LatLng;

import de.bfhh.stilleoertchenhamburg.R;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Point of interest
 * (Toilet locations)
 */
public class POI implements Parcelable {
		
	private final int id;
	private final String name;
	private final String address;
	private final String description;
	private final String website;	
	private final double lat;
	private final double lng;
	
	private final Location location;
	private final LatLng latLng;
	
	private int icon;
	private int activeIcon;
	private boolean isWheelchairAccessible;
	
	//distance to user's current position in meters
	private float distanceToUser;
	private String distanceText;
	

	public POI(int id, String name, String address, String description, String website, double lat, double lng){
		this.id = id;
		this.name = name;
		this.address = address;
		this.description = description.trim();
		this.website = website;
		this.lat = lat;
		this.lng = lng;
		this.distanceText = "";
		
		this.location = new Location("");
		location.setLatitude(this.lat);
        location.setLongitude(this.lng);
        latLng = new LatLng(lat, lng);
        
        setFieldsDependingOnDescription();
	}
	
	private void setFieldsDependingOnDescription(){
		if (description.indexOf("Rollstuhlgerechte") != -1 || description.indexOf("rollstuhlgerechte") != -1){
			this.icon = R.drawable.yellow_pin_w;
			this.activeIcon = R.drawable.yellow_pin_w_active;
			this.isWheelchairAccessible = true;
		} else {
			this.icon = R.drawable.yellow_pin;
			this.activeIcon = R.drawable.yellow_pin_active;
			isWheelchairAccessible = false;
		}
	}
	
	public boolean isWheelchairAccessible(){
		return this.isWheelchairAccessible;
	}
	
	public Location getLocation(){		
		return location;	
	}
	
	public void setDistanceToUser(Location userLocation){
		this.distanceToUser = this.location.distanceTo(userLocation);
		
		NumberFormat numberFormat = new DecimalFormat("0.0");
		numberFormat.setRoundingMode(RoundingMode.DOWN);
		//show distance rounded in kilometers if greater than 999 meters
		this.distanceText = this.distanceToUser > 999 ? 
					String.valueOf(numberFormat.format(this.distanceToUser*0.001)) + " km" : 
					String.valueOf((int) Math.round(this.distanceToUser)) + " m";
	}
	
	/** returns distance to user in meters (float) */
	public float getDistanceToUser(){
		return distanceToUser;
	}
	/** returns distance to user in meters (int) */
	public int getDistanceToUserInt(){
		return (int) distanceToUser;
	}
	/** returns distance to user as text */
	public String getDistanceToUserString(){
		return distanceText;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public POI(Parcel in){
		this.id = in.readInt();
		this.name = in.readString();
		this.address = in.readString();
		this.description = in.readString();
		this.website = in.readString();
		this.lat = in.readDouble();
		this.lng = in.readDouble();
		this.distanceToUser = in.readFloat();
		this.distanceText = in.readString();
		
		this.location = new Location("");
		location.setLatitude(this.lat);
        location.setLongitude(this.lng);
        latLng = new LatLng(lat, lng);
        
        setFieldsDependingOnDescription();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(name);
	    dest.writeString(address);
	    dest.writeString(description);
	    dest.writeString(website);
	    dest.writeDouble(lat);
	    dest.writeDouble(lng);
	    dest.writeFloat(distanceToUser);
	    dest.writeString(distanceText);
	}
	
	public static final Parcelable.Creator<POI> CREATOR = new Parcelable.Creator<POI>() {
         public POI createFromParcel(Parcel in) {
             return new POI(in);
         }

         public POI[] newArray (int size) {
             return new POI[size];
         }
    };
	
    
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
	
	public String getWebsite() {
		return website;
	}
	
	public int getIcon(){
		return icon;
	}
	
	public int getActiveIcon(){
		return activeIcon;
	}
}
