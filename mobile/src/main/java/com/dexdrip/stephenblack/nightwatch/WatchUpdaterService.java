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
import com.google.android.gms.wearable.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WatchUpdaterService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_RESEND = WatchUpdaterService.class.getName().concat(".Resend");

    private GoogleApiClient googleApiClient;
    public String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    public String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";

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
        Wearable.MessageApi.addListener(googleApiClient, this);
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

        String action = null;
        if (intent != null)
            action = intent.getAction();

        if (wear_integration) {
            if (googleApiClient.isConnected()) {
                if (ACTION_RESEND.equals(action)) {
                    resendData();
                } else {
                    sendData();
                }
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

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (wear_integration) {
            if (event.getPath().equals(WEARABLE_RESEND_PATH))
                resendData();
        }
    }

    public void sendData() {
        Bg last_bg = Bg.last();
        if (last_bg != null) {
            new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).execute(last_bg.dataMap(mPrefs));
        }
    }

    private void resendData() {
        double startTime = new Date().getTime() - (60000 * 60 * 24);
        Bg last_bg = Bg.last();
        List<Bg> graph_bgs = Bg.latestForGraph(60, startTime);
        if (!graph_bgs.isEmpty()) {
            DataMap entries = last_bg.dataMap(mPrefs);
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
            for (Bg bg : graph_bgs) {
                dataMaps.add(bg.dataMap(mPrefs));
            }
            entries.putDataMapArrayList("entries",dataMaps);

            new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).execute(entries);
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

}
