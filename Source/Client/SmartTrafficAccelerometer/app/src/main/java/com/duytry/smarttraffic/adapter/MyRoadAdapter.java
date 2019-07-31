package com.duytry.smarttraffic.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.duytry.smarttraffic.R;
import com.duytry.smarttraffic.entity.RoadEntity;

public class MyRoadAdapter extends ArrayAdapter<RoadEntity> {

    private RoadEntity[] roads;
    Context mContext;
    int layoutResourceId;

    public MyRoadAdapter(@NonNull Context context, int resource, @NonNull RoadEntity[] objects) {
        super(context, resource, objects);
        this.roads = objects;
        this.layoutResourceId = resource;
        this.mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        RoadEntity road = roads[position];

        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView textViewItem = (TextView) convertView.findViewById(R.id.textView_road_entitty);
        StringBuilder roadName = new StringBuilder();
        roadName.append(road.getShortName());
//        roadName.append(Common.SPACE_CHARACTER);
//        roadName.append(Common.DASH_CHARACTER);
//        roadName.append(Common.SPACE_CHARACTER);
//        roadName.append(road.getLongName());
        textViewItem.setText(roadName.toString());
        textViewItem.setTag(road.getId());

        return convertView;
    }
}
