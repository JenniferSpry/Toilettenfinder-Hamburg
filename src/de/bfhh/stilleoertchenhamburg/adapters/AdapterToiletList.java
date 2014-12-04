package de.bfhh.stilleoertchenhamburg.adapters;

import java.util.List;

import de.bfhh.stilleoertchenhamburg.R;
import de.bfhh.stilleoertchenhamburg.models.POI;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AdapterToiletList extends BaseAdapter {

    Context context;
    List<POI> pois;

    public AdapterToiletList(Context context, List<POI> pois) {
        this.context = context;
        this.pois = pois;
    }

    @Override
    public int getCount() {
        return pois.size();
    }

    @Override
    public Object getItem(int position) {
        return pois.get(position);
    }

    @Override
    public long getItemId(int position) {
        return pois.indexOf(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.toilet, parent, false);
        }
        
        POI poi = pois.get(position);
        
        TextView txtName = (TextView) convertView.findViewById(R.id.toilet_name);
        txtName.setText(poi.getName());
        
        TextView txtDescription = (TextView) convertView.findViewById(R.id.toilet_description);
        //trim end of line characters off description text
        String descr = poi.getDescription().trim();
        txtDescription.setText(descr);
                
        TextView txtAddress = (TextView) convertView.findViewById(R.id.toilet_address);
        txtAddress.setText(poi.getAddress());      
        
        //distance in int
        int d = (int) Math.round(poi.getDistanceToUser());
        String distance = String.valueOf(d) + " m";  
        TextView txtDistance = (TextView) convertView.findViewById(R.id.toilet_distance);
        txtDistance.setText(distance);
        txtDistance.setGravity(Gravity.CENTER);
         
        if (poi.isWheelchairAccessible()){
        	ImageView imagePin = (ImageView) convertView.findViewById(R.id.icon);
            imagePin.setImageResource(R.drawable.yellow_icon_w);
        }        

        return convertView;
    }
    

}