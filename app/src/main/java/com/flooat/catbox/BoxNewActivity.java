package com.flooat.catbox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.AppUtil;


public class BoxNewActivity extends Activity implements OnMapReadyCallback, OnMarkerDragListener {

    EditText nameText;
    Button addButton;

    GoogleMap map;
    Polygon newPolygon;
    Marker[] vertexMarker = new Marker[4];
    String name;
    String coordinatesEncoded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_new);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nameText = (EditText) findViewById(R.id.nameText);

        addButton = (Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBoxRequest();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap receivedMap) {
        LatLng coords;
        Location location;
        double lon, lat;
        double defSize = 0.00025d;
        LatLng[] defPoints = new LatLng[4];

        map = receivedMap;
        map.setMyLocationEnabled(true);
        map.setBuildingsEnabled(true);
        map.setIndoorEnabled(true);
        map.setOnMarkerDragListener(this);

        // Set last location instantly, as we are coming from BoxListActivity
        LocationManager locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            coords = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, 18f));

            // Show current boxes
            showBoxes(location);

            // Show editable current box
            lon = location.getLongitude();
            lat = location.getLatitude();

            defPoints[0] = new LatLng(lat - defSize, lon - defSize);
            defPoints[1] = new LatLng(lat - defSize, lon + defSize);
            defPoints[2] = new LatLng(lat + defSize, lon + defSize);
            defPoints[3] = new LatLng(lat + defSize, lon - defSize);

            Log.d("Catbox", "Adding draggable markers");

            for (int i = 0; i < vertexMarker.length; i++) {
                vertexMarker[i] = map.addMarker(new MarkerOptions()
                        .position(defPoints[i])
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                Log.d("Catbox", "Added marker");
                Log.d("Catbox", new LatLng(lon, lat).toString());
            }

            drawNewPolygon();
        }
    }

    /*
     * Update polygon on marker drag
     */
    public void onMarkerDragStart(Marker marker) {
        newPolygon.remove();
        drawNewPolygon();
    }
    public void onMarkerDrag(Marker marker) {
        newPolygon.remove();
        drawNewPolygon();
    }
    public void onMarkerDragEnd(Marker marker) {
        newPolygon.remove();
        drawNewPolygon();
    }

    /* Draw new polygon */
    public void drawNewPolygon() {
        PolygonOptions polygonOptions = new PolygonOptions()
                .strokeWidth(3)
                .strokeColor(Color.argb(100, 30, 144, 255))
                .fillColor(Color.argb(70, 30, 144 , 255));

        for (int i = 0; i < vertexMarker.length; i++) {
            polygonOptions.add(vertexMarker[i].getPosition());
        }

        newPolygon = map.addPolygon(polygonOptions);
    }

    /*
     * Get boxes from server and draw them
     */
    private void showBoxes(final Location location) {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Log.d("Catbox", "Requesting boxes");
                    // Create GET Request
                    HttpGet request = new HttpGet(App.URL + "/boxes?coordinates=[" + location.getLongitude() + "," + location.getLatitude() + "]");
                    HttpResponse response = App.httpClient.execute(request);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (!responseBody.equals("Unauthorized")) {
                        JSONArray responseArray = new JSONArray(responseBody);
                        drawOtherPolygons(responseArray);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        });

        thread.start();
    }

    private void drawOtherPolygons(final JSONArray boxes) {
        BoxNewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSONObject box;
                JSONArray shape;
                String name;
                PolygonOptions polygonOptions;

                Log.d("Catbox", "Drawing polygons on map");
                try {
                    for (int i = 0; i < boxes.length(); i++) {
                        box = boxes.getJSONObject(i);
                        name = box.getString("name");
                        shape = box.getJSONObject("shape").getJSONArray("coordinates").getJSONArray(0);

                        // Draw polygon
                        polygonOptions = new PolygonOptions()
                                .strokeWidth(4)
                                .strokeColor(Color.argb(100, 30, 144, 255))
                                .fillColor(Color.argb(75, 30, 144 , 255));

                        for (int j = 0; j < shape.length(); j++) {
                            // LongLat pairs to LatLng
                            polygonOptions.add(new LatLng(shape.getJSONArray(j).getDouble(1), shape.getJSONArray(j).getDouble(0)));
                        }

                        map.addPolygon(polygonOptions);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        });
    }

    /**
     * Attempt to add the box
     */
    private void addBoxRequest() {
        name = nameText.getText().toString().trim();
        LatLng latlng;

        coordinatesEncoded = "["; // Add starting bracket
        for (int i = 0; i < vertexMarker.length; i++) {
            latlng = vertexMarker[i].getPosition();
            // Add longitude latitude pair
            coordinatesEncoded = coordinatesEncoded.concat(String.format("[%,.16f,%,.16f]", latlng.longitude, latlng.latitude));
            if (i != vertexMarker.length - 1) // Add commas to all but the last
                coordinatesEncoded = coordinatesEncoded.concat(",");
        }
        coordinatesEncoded = coordinatesEncoded.concat("]"); // Add ending bracket

        // Check for a valid username, show error otherwise
        nameText.setError(null);
        if (TextUtils.isEmpty(name)) {
            nameText.setError(getString(R.string.error_field_required));
            nameText.requestFocus();
            return;
        }

        addButton.setEnabled(false);
        addButton.setText(R.string.message_loading);

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // Create POST Request
                    HttpPost request = new HttpPost(App.URL + "/add_box");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                    nameValuePairs.add(new BasicNameValuePair("name", name));
                    nameValuePairs.add(new BasicNameValuePair("content", ""));
                    nameValuePairs.add(new BasicNameValuePair("coordinates", coordinatesEncoded));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = App.httpClient.execute(request);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    Log.d("Catbox", responseBody);

                    if (!responseBody.equals("Unauthorized")) {
                        JSONObject responseObject = new JSONObject(responseBody);
                        String result = responseObject.getString("result");

                        if (result.equals("success")) {
                            AppUtil.showToast("Box added successfully.", BoxNewActivity.this);
                            finish();
                        } else {
                            AppUtil.showToast(responseObject.getString("message"), BoxNewActivity.this);
                        }
                    }

                } catch (Exception e) {
                    AppUtil.showToast("Couldn't connect, please try again.", BoxNewActivity.this);
                    e.printStackTrace();
                }

                BoxNewActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addButton.setEnabled(true);
                        addButton.setText("Continue");
                    }
                });

                return;
            }
        });

        thread.start();
    }

}
