package com.flooat.catbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.AppUtil;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean loginAttempt = false;
        try {
            Uri data = getIntent().getData();
            String host = data.getHost(); // domain name
            Log.d("Catbox", data.toString());
            if (host.equals("login")) {
                loginAttempt = true;
                String token = data.getQueryParameter("t");
                String uid = data.getQueryParameter("u");
                loginUser(token, uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!loginAttempt) {
            // Quick test jumps, to any screen
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.startActivity(intent);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("Catbox", "Destroying main activity.");
    }

    private void loginUser(final String token, final String uid) {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Log.d("Catbox", "Login - Attempt");
                    // Create GET Request
                    HttpGet request = new HttpGet(App.URL + "/login?token=" + token + "&uid=" + uid);
                    HttpResponse response = App.httpClient.execute(request);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject responseObject = new JSONObject(responseBody);
                    String result = responseObject.getString("result");
                    Log.d("Catbox", responseBody);
                    if (result.equals("success")) {
                        App.userId = uid;
                        App.userName = responseObject.getJSONObject("user").getString("name");
                        App.userEmail = responseObject.getJSONObject("user").getString("email");

                        AppUtil.showToast("Welcome!", MainActivity.this);
                    } else {
                        AppUtil.showToast("Invalid login token, please try again.", MainActivity.this);

                        intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        getApplicationContext().startActivity(intent);
                    }

                } catch (Exception e) {
                    AppUtil.showToast("Couldn't connect, please check your internet and try again.", MainActivity.this);
                    e.printStackTrace();
                }

                Intent intent = new Intent(getApplicationContext(), BoxListActivity.class);
                startActivity(intent);
                finish();

                return;
            }
        });

        thread.start();
    }
}
