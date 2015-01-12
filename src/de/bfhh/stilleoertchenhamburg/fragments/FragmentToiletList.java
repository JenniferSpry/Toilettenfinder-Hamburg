package de.bfhh.stilleoertchenhamburg.fragments;

import java.util.ArrayList;

import android.support.v4.app.ListFragment;
import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.activites.ActivityMap;
import de.bfhh.stilleoertchenhamburg.activites.ActivityToiletList;
import de.bfhh.stilleoertchenhamburg.adapters.AdapterToiletList;
import de.bfhh.stilleoertchenhamburg.helpers.TagNames;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class FragmentToiletList extends ListFragment {

	AdapterToiletList adapter;
	private ListView _listView;
	private Button _loadMoreButton;
	private ActivityToiletList activityToiletList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_toilet_list, null, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		activityToiletList = (ActivityToiletList) getActivity();

		_listView = this.getListView();

		View footerView = ((LayoutInflater) getActivity().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.toilet_list_footer, null, false);
		_listView.addFooterView(footerView);

		_loadMoreButton = (Button) view.findViewById(R.id.load_more);
		_loadMoreButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activityToiletList.extendFragmentList();
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle bundle = getArguments();
		if (bundle != null) {
			ArrayList<POI> poiList = bundle
					.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
			adapter = new AdapterToiletList(getActivity(), poiList);
			setListAdapter(adapter);
		}

	}

	/** start ActivityMap onclick (with extra to open slider) */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(getActivity(), ActivityMap.class);
		i.setAction(TagNames.ACTION_SHOW_SLIDER); // set show slider action
		i.putExtra(TagNames.EXTRA_POI,
				(POI) getListView().getItemAtPosition(position));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(i); // start ActivityMap
	}

	/** updated poi list from activity toilet list */
	public void addArguments(Bundle bundle) {
		if (bundle != null) {
			ArrayList<POI> poiList = bundle
					.getParcelableArrayList(TagNames.EXTRA_POI_LIST);
			if (adapter != null) {
				adapter.updatePOIList(poiList);
				setListAdapter(adapter);
			}
		}
	}

	/** selected a certain entry to be shown */
	public void setPOISelected(int selectedID) {
		getListView().setSelection(selectedID);
	}
}
