package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;
import java.util.List;
import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.POIController;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.TagNames;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
    private List<POI> pois;
    private ArrayList<POI> poiList;
    private POIController poiController;
	private double userLat;
	private double userLng;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_toilet_list, null, false);
    }

    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent i = getActivity().getIntent();
        Bundle bundle = i.getExtras();
        if(bundle != null){
        	if(bundle.getDouble(TagNames.EXTRA_LAT) != 0.0 && bundle.getDouble(TagNames.EXTRA_LONG) != 0.0){
        		//set user Position
        		userLat = bundle.getDouble(TagNames.EXTRA_LAT);
        		userLng = bundle.getDouble(TagNames.EXTRA_LONG);
        		poiList = i.getParcelableArrayListExtra(TagNames.EXTRA_POI_LIST);
        		poiController = new POIController(poiList);
        		poiController.setDistancePOIToUser(userLat, userLng);
        		pois = poiController.getClosestPOI(20);
        		adapter = new AdapterToiletList(getActivity(), pois);
        	    setListAdapter(adapter);
        	}
        }
    }

}
