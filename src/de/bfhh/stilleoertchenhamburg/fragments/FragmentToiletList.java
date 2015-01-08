package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.activites.ActivityMap;
import de.bfhh.stilleoertchenhamburg.activites.ActivityToiletList;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.helpers.POIHelper;
import de.bfhh.stilleoertchenhamburg.helpers.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;

public class FragmentToiletList extends ListFragment {

    AdapterToiletList adapter;
	private ListView _listView;
	private Button _loadMoreButton;
	private ActivityToiletList activityToiletList;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.fragment_toilet_list, null, false);
    }
    
    @Override
    public void onViewCreated (View view, Bundle savedInstanceState){
		Log.d("FragmentToiletList onViewCreated", "***********+");
    	activityToiletList = (ActivityToiletList) getActivity();
    	
    	//hide the button
    	_loadMoreButton = (Button) view.findViewById(R.id.load_more);
    	_loadMoreButton.setVisibility(View.GONE);
    	_loadMoreButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO: refactor
				activityToiletList.extendFragmentList();
			}
		});
    	_listView = this.getListView();
    	_listView.setOnScrollListener(new OnScrollListener() {	

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				switch(view.getId()) {
					case android.R.id.list:     

					// Make your calculation stuff here. You have all your
					// needed info from the parameters of this function.

					// Sample calculation to determine if the last 
					// item is fully visible.
					final int lastItem = firstVisibleItem + visibleItemCount;
					if(lastItem == totalItemCount) {
						//to avoid multiple calls for last item
							Log.d("Last", "Last");
							_loadMoreButton.setVisibility(View.VISIBLE);
						
					}else{
						_loadMoreButton.setVisibility(View.GONE);
					}
				}
			}
		});
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
    
    //start ActivityMap onclick (with extra to open slider)
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
    	Intent i = new Intent(getActivity(), ActivityMap.class);
    	i.setAction(TagNames.ACTION_SHOW_SLIDER); //set show slider action
        i.putExtra(TagNames.EXTRA_POI, (POI) getListView().getItemAtPosition(position));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	  	i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
  	  	startActivity(i); //start ActivityMap
    }
    
    public void addArguments(Bundle bundle){
        if(bundle != null){
        	ArrayList<POI> poiList = bundle.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
        	if(adapter != null){
        		adapter.updatePOIList(poiList);
        		setListAdapter(adapter);
        		
        	}
    	   
        }
    }
    
    public void setPOISelected(int selectedID){
    	getListView().setSelection(selectedID);
    }
    

}
