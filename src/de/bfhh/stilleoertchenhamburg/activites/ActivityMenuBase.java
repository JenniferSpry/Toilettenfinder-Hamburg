package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.R;
import android.content.Intent;
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
		    case R.id.menu_start:
	    		startActivity(new Intent(this, ActivityMain.class));
	            return true;
	        case R.id.menu_impressum:
	    		startActivity(new Intent(this, ActivityImpressum.class));
	            return true;
	        case R.id.menu_test:
	    		startActivity(new Intent(this, ToiletTestActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
