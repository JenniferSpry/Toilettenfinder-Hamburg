package de.bfhh.stilleoertchenhamburg.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Location;

import de.bfhh.stilleoertchenhamburg.models.POI;

/*
 * @author: Steffi
 * Helper class for all POI and POI-list needs
 */

public class POIHelper {
	
	/**
	 * set distance to user for each individual POI in poiList.
	 */
	public static ArrayList<POI> setDistancePOIToUser(ArrayList<POI> poiList, double userLat, double userLng){
		Location loc = new Location("");
		loc.setLatitude(userLat);
		loc.setLongitude(userLng);
		//set distance to current user position for every POI in the List
		for(int i=0; i < poiList.size(); i++){
			poiList.get(i).setDistanceToUser(loc);	
		}
		return poiList;
	}
	
	public static POI setDistanceSinglePOIToUser(POI poi, double userLat, double userLng){
		Location loc = new Location("");
		loc.setLatitude(userLat);
		loc.setLongitude(userLng);
		poi.setDistanceToUser(loc);	
		return poi;
	}
	
	/**
	 * returns the closest ten POI to the user's current location
	 */
	public static ArrayList<POI> getClosestPOI(ArrayList<POI> poiList, int amount){
		ArrayList<POI> closestPOI = new ArrayList<POI>();
		
		if (poiList == null) return closestPOI;
		
		if (!poiList.isEmpty()){
			Collections.sort(poiList, new Comparator<POI>() {
		        @Override 
		        public int compare(POI p1, POI p2) {
		            return  p1.getDistanceToUserInt() - p2.getDistanceToUserInt(); // Ascending
		        }
		    });
		}
		
		if(amount > 0 && !poiList.isEmpty()){
			for(int i = 0; i < amount; i++){ //add ten closes points to other List
				closestPOI.add(poiList.get(i));
			}	
		}
		return closestPOI; //could be empty if amount == 0
	}
	
	/**
	 * returns the MarkerOptions for a passed POI
	 */
	public static MarkerOptions getMarkerOptionsForPOI(POI poi, boolean active){
		MarkerOptions marker = new MarkerOptions();
		BitmapDescriptor icon = null;
		if (active){
			icon = BitmapDescriptorFactory.fromResource(poi.getActiveIcon());
		} else {
			icon = BitmapDescriptorFactory.fromResource(poi.getIcon());
		}
		return marker.position(new LatLng(poi.getLat(), poi.getLng()))
					 .title(poi.getName())
					 .icon(icon);
	}

	/**
	 * find a POI by it's associated markers id
	 */
	public static POI getPoiByIdReference(HashMap<String, Integer> markerPOIIdMap, ArrayList<POI> allPOIList, String markerId) {
		return getPoiById(allPOIList, markerPOIIdMap.get(markerId));
	}
	
	/**
	 * select a POI by it's id out of an arrayList
	 */
	public static POI getPoiById(ArrayList<POI> allPOIList, Integer id) {
		for(POI poi: allPOIList){
			if (poi.getId() == id){
				return poi;
			}
		}
		return null;
	}
	
}