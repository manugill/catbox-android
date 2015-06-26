package com.flooat.catbox.models;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;


public class Box {
    private String id;
    private String name;
    private String userId;
    private JSONArray shape;
    private JSONArray centroid;

    // Default constructor with required params
    public Box(String id, String name, String userId, JSONArray shape, JSONArray centroid) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.shape = shape;
        this.centroid = centroid;
    }

    public String getId() {
        return id;
    };
    public String getName() {
        return name;
    };
    public String getUserId() {
        return userId;
    };
    public JSONArray getShape() {
        return shape;
    };
    public JSONArray getCentroid() {
        return centroid;
    };

}