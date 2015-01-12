package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.R;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class ActivityImpressum extends ActivityMenuBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_impressum);

		TextView t2 = (TextView) findViewById(R.id.impressum_link);
		t2.setMovementMethod(LinkMovementMethod.getInstance());

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

}