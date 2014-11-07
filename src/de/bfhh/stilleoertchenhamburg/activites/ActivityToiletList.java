package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.R;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

/**
 * TODO: Vernünftige Fehlermeldung, wenn die Daten nicht kommen
 * @author Jenne
 *
 */

public class ActivityToiletList extends ActivityMenuBase {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_toilet_list);
		ActionBar actionBar = getSupportActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);
	}

}
