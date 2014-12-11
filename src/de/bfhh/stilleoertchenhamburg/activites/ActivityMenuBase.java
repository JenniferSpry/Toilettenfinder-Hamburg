package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.R;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Magical Menu providing class because I don't want to copy paste this into every Activity
 * @author Jenne
 */

public class ActivityMenuBase extends ActivityBase {
	
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
	            this.finish(); //causes the app to close if you choose I.L.I then home button / icon
		    	//NavUtils.navigateUpFromSameTask(this);//takes you back to map (map act. is created again with savedInstanceState)
	            return true;
		    case R.id.menu_map:
		    	Intent map = new Intent(this, ActivityMap.class);
		    	//set flag for intent to get activity from stack if it's already running
		    	map.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(map);
	            return true;
	        case R.id.menu_impressum:
	        	Intent activityImpressum = new Intent(getApplicationContext(), ActivityImpressum.class);
	        	activityImpressum.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(activityImpressum);
	            return true;
	        case R.id.menu_toi_list:
	        	Intent activityToiletList = new Intent(getApplicationContext(), ActivityToiletList.class);
	        	activityToiletList.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    		startActivity(activityToiletList);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
}
