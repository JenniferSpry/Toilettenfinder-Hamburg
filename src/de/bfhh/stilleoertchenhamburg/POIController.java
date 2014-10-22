package de.bfhh.stilleoertchenhamburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.location.Location;

import de.bfhh.stilleoertchenhamburg.models.POI;

/*
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

/*	
private BroadcastReceiver receiver = new BroadcastReceiver() {
		
	    @Override
	    public void onReceive(Context context, Intent intent) {
	      //Bundle bundle = intent.getExtras();
	      String action = intent.getAction();
	      if(action.equals("toiletLocation")){ //WATCH OUT!

	    	  poiArrayList = (ArrayList<HashMap<String,String>>) intent.getSerializableExtra("poiList");
	    	  if(poiArrayList != null){
	    		  for(int i = 0; i < poiArrayList.size(); i++){
	    			  HashMap<String,String> poiMap = poiArrayList.get(i);
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
	    		  poiReceived = true;
	    		  
	    	  }
	      }
	    }
	};
*/
	
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
	public List<POI> getClosestTenPOI(){	
		List<POI> closestTenPOI = new ArrayList<POI>();
		
		Collections.sort(poiList, new Comparator<POI>() {
	        @Override 
	        public int compare(POI p1, POI p2) {
	            return  p1.getDistanceToUserInt() - p2.getDistanceToUserInt(); // Ascending
	        }
	    });
		
		for(int i = 0; i < 10; i++){ //add ten closes points to other List
			closestTenPOI.add(poiList.get(i));
		}	
		return closestTenPOI;
	}
	
	public List<POI> getAllPOI(){
		return poiList;
	}
	
}