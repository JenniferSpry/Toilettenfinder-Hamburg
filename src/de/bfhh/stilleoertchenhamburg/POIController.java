package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Location;

import de.bfhh.stilleoertchenhamburg.models.POI;

/*
 * @author: Steffi
 * This class receives Broadcasts from POIUpdateService or Activities, which
 * pass to it an ArrayList<HashMap<String,String>> poiList.
 * From this data it will create a List<POI>, to have easier access to the data.
 * It then has all the information it needs for calculating the distance from 
 * a given user Location to all the POI.
 * It will have a method to pass the ten closest POI to the user's location
 * back to the calling activity.
 */

public class POIController {
	
	protected static final int RESULT_OK = -1;
	private boolean poiReceived;//has the broadcast been received?
	ArrayList<HashMap<String,String>> poiArrayList;
	List<POI> poiList;

	public POIController(ArrayList<HashMap<String,String>> poi ){
		poiList = new ArrayList<POI>();
		poiArrayList = poi;
		if(poiArrayList != null){
			toPOIList(poiArrayList); //fill poiList
			poiReceived = true;
		}else {
			poiReceived = false;
		}
	}
	
	//turns ArrayList<HashMap<String,String>> into List<POI>, which is an instance variable
	private void toPOIList(ArrayList<HashMap<String,String>> p){
		if(p != null){
  		  for(int i = 0; i < p.size(); i++){
  			  HashMap<String,String> poiMap = p.get(i);
  			  double lat = 0.0, lng = 0.0;
  			  int id = 0;
  			  String name = "", address = "", descr = "";
  			    			  
  			  for (Entry<String, String> entry : poiMap.entrySet()) {
  				  String keyName = entry.getKey();  	        		
  				  switch (keyName){
	                  case "id":
	                	  id = Integer.valueOf(entry.getValue());
	                	  break;
	                  case "name":
	                	  name = entry.getValue();
	                	  break;
	                  case "address":
	                	  address = entry.getValue();
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
  			  
  			  //add newly created POI to List
  			  poiList.add(new POI(
  					  id, 
  					  name, 
  					  lat, 
  					  lng, 
  					  address, 
  					  descr)
  			  );
  		  }//end for
		}//end if
	}
	
	//return boolean that shows whether broadcast has been received
	public boolean poiReceived(){
		return poiReceived;
	}
	
	//set distance to user for each individual POI in poiList.
	//TODO:this has to be called when user location is changed
	public void setDistancePOIToUser(double userLat, double userLng){
		Location loc = new Location("");
		loc.setLatitude(userLat);
		loc.setLongitude(userLng);
		//set distance to current user position for every POI in the List
		for(int i=0; i < poiList.size(); i++){
			poiList.get(i).setDistanceToUser(loc);	
		}
	}
	
	//returns the closest ten POI to the user's current location
	public List<POI> getClosestPOI(int amount){	
		List<POI> closestPOI = new ArrayList<POI>();
		
		Collections.sort(poiList, new Comparator<POI>() {
	        @Override 
	        public int compare(POI p1, POI p2) {
	            return  p1.getDistanceToUserInt() - p2.getDistanceToUserInt(); // Ascending
	        }
	    });
		if(amount > 0){
			for(int i = 0; i < amount; i++){ //add ten closes points to other List
				closestPOI.add(poiList.get(i));
			}	
		}
		return closestPOI; //could be empty of amount == 0
	}
	
	public List<POI> getAllPOI(){
		return poiList;
	}
	
	//returns the MarkerOptions for a passed POI
	public MarkerOptions getMarkerOptionsForPOI(POI poi){
		MarkerOptions marker = new MarkerOptions();
		BitmapDescriptor icon;
		if(poi.isWheelchairAccessible()){// wheelchair accessible POI
			icon = BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin_w);
		}else{
			icon = BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin);
		}	
		return marker.position(new LatLng(poi.getLat(), poi.getLng()))
					 .title(poi.getName())
					 .icon(icon);
	}
	
}