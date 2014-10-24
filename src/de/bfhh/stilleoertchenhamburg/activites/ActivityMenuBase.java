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
	    	//if user clicks on the app icon (home button), current activity is closed
		    case android.R.id.home: 
	            this.finish();
	            return true;
		    case R.id.menu_start:
		    	Intent map = new Intent(this, ActivityMap.class);
		    	//set flag for intent to get activity from stack if it's already running
		    	map.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(map);
	            return true;
	        case R.id.menu_impressum:
	        	Intent impressum = new Intent(getApplicationContext(), ActivityImpressum.class);
	        	impressum.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(impressum);
	            return true;
	        case R.id.menu_test:
	        	Intent testToiletList = new Intent(this, ToiletTestActivity.class);
	        	testToiletList.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(testToiletList);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
