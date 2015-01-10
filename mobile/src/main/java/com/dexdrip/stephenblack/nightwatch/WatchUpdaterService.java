package com.dexdrip.stephenblack.nightwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

public class WatchUpdaterService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient googleApiClient;
    public String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    boolean wear_integration  = false;
    SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        setSettings();
        if(wear_integration) { googleApiConnect(); }
    }

    public void listenForChangeInSettings() {
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                setSettings();
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void setSettings() {
        wear_integration = mPrefs.getBoolean("watch_sync", false);
        if(wear_integration) { googleApiConnect(); }
    }

    public void googleApiConnect() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        if (googleApiClient.isConnected()) {
            sendData();
        } else {
            googleApiClient.connect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pending = PendingIntent.getService(this, 0, new Intent(this, WatchUpdaterService.class), 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pending);

        if(wear_integration) {
            if (googleApiClient.isConnected()) {
                sendData();
            } else {
                googleApiClient.connect();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        sendData();
    }

    public void sendData() {

        Bg last_bg = Bg.last();
        if (last_bg != null) {
            new SendToDataLayerThread(WEARABLE_DATA_PATH, last_bg.dataMap(mPrefs), googleApiClient).start();
        }
    }

    @Override
    public void onDestroy() {
        if (null != googleApiClient && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }
}
