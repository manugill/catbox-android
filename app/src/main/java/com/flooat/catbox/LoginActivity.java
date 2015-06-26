package com.flooat.catbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.AppUtil;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity {

    SharedPreferences prefs;
    EditText usernameText;
    Button continueButton;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set up the login form
        usernameText = (EditText) findViewById(R.id.usernameText);
        usernameText.setText(AppUtil.getEmail(getApplicationContext()));

        continueButton = (Button) findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTokenRequest();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Attempts to sign in the account specified by the login form.
     */
    private void sendTokenRequest() {
        username = usernameText.getText().toString().trim();

        // Check for a valid username, show error otherwise
        usernameText.setError(null);
        if (TextUtils.isEmpty(username)) {
            usernameText.setError(getString(R.string.error_field_required));
            usernameText.requestFocus();
            return;
        }

        continueButton.setEnabled(false);
        continueButton.setText(R.string.message_loading);

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // Create POST Request
                    HttpPost request = new HttpPost(App.URL + "/request_token");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("user", username));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = App.httpClient.execute(request);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject responseObject = new JSONObject(responseBody);
                    Log.d("Catbox", responseObject.toString());

                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LoginActivity.this.infoDialog();
                        }
                    });

                } catch (Exception e) {
                    AppUtil.showToast("Couldn't connect, please try again.", LoginActivity.this);
                    e.printStackTrace();
                }

                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        continueButton.setEnabled(true);
                        continueButton.setText("Continue");
                    }
                });

                return;
            }
        });

        thread.start();
    }

    private void infoDialog() {
        final Dialog dialog = new Dialog(LoginActivity.this);
        dialog.setContentView(R.layout.dialog_login_info);
        dialog.setTitle("Check you email");
        dialog.setCancelable(true);

        dialog.show();
    }

}
