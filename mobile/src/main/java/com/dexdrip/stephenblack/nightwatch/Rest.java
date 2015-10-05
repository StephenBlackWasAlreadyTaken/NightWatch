package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 12/26/14.
 */
public class Rest {
    private Context mContext;
    private String mUrl;
    private static final String UNITS = "mgdl";
    SharedPreferences prefs;
    PowerManager.WakeLock wakeLock;
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    Rest(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUrl = prefs.getString("dex_collection_method", "https://{yoursite}.azurewebsites.net");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rest wakelock");
    }

    public boolean getBg(int count) {
        if (!prefs.getBoolean("nightscout_poll", false) && mUrl.compareTo("") != 0 && mUrl.compareTo("https://{yoursite}.azurewebsites.net") != 0) {
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return false;
        }
        try {
            PebbleEndpoint response;
            boolean newData = false;

            if (count > 1) {
                response =  pebbleEndpointInterface().getPebbleInfo(UNITS, count);
            } else
                response = pebbleEndpointInterface().getPebbleInfo(UNITS);
            double slope = 0, intercept = 0, scale = 0;
            if (response.cals != null && response.cals.size() != 0){
                Cal cal = response.cals.get(0);
                slope = cal.slope;
                intercept = cal.intercept;
                scale = cal.scale;
            }

            for (Bg returnedBg: response.bgs) {
                if (Bg.is_new(returnedBg)) {

                    //raw logic from https://github.com/nightscout/cgm-remote-monitor/blob/master/lib/plugins/rawbg.js#L59
                    if (slope != 0 && intercept != 0 && scale != 0) {
                        if (returnedBg.filtered == 0 || returnedBg.sgv_double() < 40) {
                            returnedBg.raw = scale * (returnedBg.unfiltered - intercept) / slope;
                        } else {
                            double ratio = scale * (returnedBg.filtered - intercept) / slope / returnedBg.sgv_double();
                            returnedBg.raw = scale * (returnedBg.unfiltered - intercept) / slope / ratio;
                        }
                    }
                    returnedBg.save();
                    DataCollectionService.newDataArrived(mContext, true, returnedBg);
                    newData = true;
                }
            }
            Log.d("REST CALL SUCCESS: ", "HORRAY");
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return newData;
        } catch (Exception e) {
            Log.d("REST CALL ERROR: ", e.getMessage());
            e.printStackTrace();
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return false;
        }
    }

    public boolean getBg() {
        return getBg(1);
    }

    private PebbleEndpointInterface pebbleEndpointInterface() {
        RestAdapter adapter = adapterBuilder().build();
        PebbleEndpointInterface pebbleEndpointInterface =
                adapter.create(PebbleEndpointInterface.class);

        return pebbleEndpointInterface;
    }

    private RestAdapter.Builder adapterBuilder() {
        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(1, TimeUnit.MINUTES);
        okHttpClient.setConnectTimeout(1, TimeUnit.MINUTES);

        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(new OkClient(okHttpClient))
                .setEndpoint(mUrl)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));

        return adapterBuilder;
    }
}
