package com.flooat.catbox;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.Box;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;

public class BoxAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Box> boxes;

    public BoxAdapter(Context context, ArrayList<Box> boxes) {
        this.context = context;
        this.boxes = boxes;
    }

    @Override
    public int getCount() {
        return boxes.size();
    }
    @Override
    public Box getItem(int i) {
        return boxes.get(i);
    }
    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // Check if the view has been created for the row. If not, lets inflate it
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_box_item, null); // List layout here
        }

        // Grab the Inputs in our custom layout
        TextView nameText = (TextView)view.findViewById(R.id.nameText);
        TextView locationText = (TextView)view.findViewById(R.id.locationText);

        try {
            Box box = boxes.get(i);
            LatLng latlng = new LatLng(box.getCentroid().getDouble(1), box.getCentroid().getDouble(0));
            Location centroid = new Location("Box Location");
            centroid.setLongitude(latlng.longitude);
            centroid.setLatitude(latlng.latitude);
            int distance = (int) App.currentLocation.distanceTo(centroid);

            nameText.setText(box.getName());
            locationText.setText(String.format("%d meters away", distance));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }


}