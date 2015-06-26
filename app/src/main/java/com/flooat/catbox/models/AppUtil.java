package com.flooat.catbox.models;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.flooat.catbox.MainActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.List;

/**
 * Created by Manu on 05-Jun-15.
 */
public class AppUtil {

    private static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        } return account;
    }

    public static String getEmail(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = getAccount(accountManager);
        if (account == null) {
            return null;
        } else {
            return account.name;
        }
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static void showToast(final String message, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }



}
