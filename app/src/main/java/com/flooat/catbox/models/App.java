package com.flooat.catbox.models;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.flooat.catbox.BoxListActivity;
import com.flooat.catbox.LoginActivity;
import com.flooat.catbox.MainActivity;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by Manu on 05-Jun-15.
 */
public class App {
    public static String PACKAGE = "com.flooat.catbox";
    public static String URL = "http://flooat.com:3000";
    public static HttpClient httpClient = new DefaultHttpClient();
    public static String userId;
    public static String userName;
    public static String userEmail;
    public static Location currentLocation;
    public static Socket mSocket;
}
