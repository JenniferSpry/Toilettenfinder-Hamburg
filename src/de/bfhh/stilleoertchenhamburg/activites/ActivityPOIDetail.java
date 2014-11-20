package de.bfhh.stilleoertchenhamburg.activites;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;

public class ActivityPOIDetail extends ActivityMenuBase {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_detail);
        
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        POI poi = (POI) bundle.get(TagNames.EXTRA_POI);
        
        TextView txtName = (TextView) findViewById(R.id.detail_toilet_name);
        txtName.setText(poi.getName());
        
        TextView txtDescr = (TextView) findViewById(R.id.detail_toilet_description);
        txtDescr.setText(poi.getDescription());
        
        TextView txtAdress = (TextView) findViewById(R.id.detail_toilet_adress);
        txtAdress.setText(poi.getAddress());
        
        TextView txtWebsite = (TextView) findViewById(R.id.detail_toilet_website);
        txtWebsite.setText(poi.getWebsite());
        if (!poi.getWebsite().equals("")){
        	txtWebsite.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
        	txtWebsite.setVisibility(View.GONE);
        }
    }
    

}
