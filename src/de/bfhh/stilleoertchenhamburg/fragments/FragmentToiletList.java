package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;
import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
    private ArrayList<POI> poiList = new ArrayList<POI>();
    
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
    

}
