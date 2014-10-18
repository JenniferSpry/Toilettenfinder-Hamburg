package de.bfhh.stilleoertchenhamburg.activites;

import de.bfhh.stilleoertchenhamburg.R;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class ActivityImpressum extends ActivityMenuBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.impressum);
		
		TextView t2 = (TextView) findViewById(R.id.impressum_link);
	    t2.setMovementMethod(LinkMovementMethod.getInstance());
	}

}