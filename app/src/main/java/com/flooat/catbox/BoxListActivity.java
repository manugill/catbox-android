package com.flooat.catbox;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationManager;

import com.flooat.catbox.models.AppUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.Box;


public class BoxListActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    ListView boxListView;
    ArrayList<Box> boxes;
    BoxAdapter adapter;

    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_list);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(60000) // 1 minute
                .setFastestInterval(1000)
                .setNumUpdates(10); // limit to 10 updates

        boxListView = (ListView)findViewById(R.id.boxListView);

        boxListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Grab the selected Reminder
                Box box = (Box) boxListView.getAdapter().getItem(i);

                // Return the object to the MainActivity and close this activity
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("id", box.getId());
                intent.putExtra("name", box.getName());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        googleApiClient.connect();

        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } else {
            getBoxes(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        getBoxes(location);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates( googleApiClient, locationRequest, this);
    }

    /*
     * Get boxes from server and add them
     */
    private void getBoxes(final Location location) {
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
                        App.currentLocation = location;

                        JSONArray responseArray = new JSONArray(responseBody);
                        addBoxesToAdapter(responseArray);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        });

        thread.start();
    }

    private void addBoxesToAdapter(final JSONArray boxesJSON) {
        Box box;
        JSONObject boxJSON;
        boxes = new ArrayList<>();
        Log.d("Catbox", boxesJSON.toString());
        Log.d("Catbox", "Adding to adapter");
        try {
            for (int i = 0; i < boxesJSON.length(); i++) {
                boxJSON = boxesJSON.getJSONObject(i);
                box = new Box(
                        boxJSON.getString("_id"), boxJSON.getString("name"), boxJSON.getString("user_id"),
                        boxJSON.getJSONObject("shape").getJSONArray("coordinates").getJSONArray(0),
                        boxJSON.getJSONObject("centroid").getJSONArray("coordinates"));
                Log.d("Catbox", boxJSON.toString());
                boxes.add(box);
            }

            BoxListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Set adapter to fill the list
                    BoxAdapter adapter = new BoxAdapter(BoxListActivity.this, boxes);
                    boxListView.setAdapter(adapter);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_box_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.createBox) {
            Intent intent = new Intent(this, BoxNewActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.changeAlias) {
            changeNameDialog();
        } else {
            finish();
            System.exit(0);
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeNameDialog() {
        final Dialog dialog = new Dialog(BoxListActivity.this);
        dialog.setContentView(R.layout.dialog_change_name);
        dialog.setTitle("Enter new alias");
        dialog.setCancelable(true);

        Button submitButton = (Button) dialog.findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendChangeNameRequest(dialog);
            }
        });

        dialog.show();
    }

    /**
     * Send a change name request
     */
    private void sendChangeNameRequest(final Dialog dialog) {
        final EditText nameText = (EditText) dialog.findViewById(R.id.nameText);
        final Button submitButton = (Button) dialog.findViewById(R.id.submitButton);
        final String name = nameText.getText().toString().trim();

        // Check for a valid username, show error otherwise
        nameText.setError(null);
        if (TextUtils.isEmpty(name)) {
            nameText.setError(getString(R.string.error_field_required));
            nameText.requestFocus();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText(R.string.message_loading);

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // Create POST Request
                    Log.d("Catbox", name);
                    HttpPost request = new HttpPost(App.URL + "/change_name");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("name", name));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = App.httpClient.execute(request);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject responseObject = new JSONObject(responseBody);
                    Log.d("Catbox", responseObject.toString());

                    App.userName = name;

                    BoxListActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                            AppUtil.showToast("Alias updated successfully.", BoxListActivity.this);
                        }
                    });

                } catch (Exception e) {
                    AppUtil.showToast("Couldn't update name, please try again.", BoxListActivity.this);
                    e.printStackTrace();
                }

                BoxListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        submitButton.setEnabled(true);
                        submitButton.setText("Done");
                    }
                });

                return;
            }
        });

        thread.start();
    }

}
