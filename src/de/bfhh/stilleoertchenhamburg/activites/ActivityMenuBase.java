package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.R;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Magical Menu providing class because I don't want to copy paste this into every Activity
 * @author Jenne
 */

public class ActivityMenuBase extends ActionBarActivity {
	
	private POIController poiController; //class that handles Broadcast, methods return List<POI>
	private Location userLocation;
	private double userLat, userLng;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home: //if user clicks on the icon (home button), this activity is closed
	            this.finish();
	            return true;
		    case R.id.menu_start:
		    	Intent i = new Intent(this, ActivityMain.class);
		    	//set flag for intent to get activity from stack if it's already running
		    	i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(i);
	            return true;
	        case R.id.menu_impressum:
	    		startActivity(new Intent(getApplicationContext(), ActivityImpressum.class));
	            return true;
	        case R.id.menu_test:
	    		startActivity(new Intent(this, ToiletTestActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
