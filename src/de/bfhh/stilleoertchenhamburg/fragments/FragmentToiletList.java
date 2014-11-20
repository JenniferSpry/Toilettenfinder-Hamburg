package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;
import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.activites.ActivityPOIDetail;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       return inflater.inflate(R.layout.fragment_toilet_list, null, false);
    }

    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        Bundle bundle = getArguments();
        if(bundle != null){
        	ArrayList<POI> poiList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
        	adapter = new AdapterToiletList(getActivity(), poiList);
    	    setListAdapter(adapter);
        }
        
    }
    
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
    	Intent i = new Intent(getActivity(), ActivityPOIDetail.class);
        i.putExtra(TagNames.EXTRA_POI, (POI) getListView().getItemAtPosition(position));
  	  	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	startActivity(i); //start POI Detail Activity
    }
    

}
