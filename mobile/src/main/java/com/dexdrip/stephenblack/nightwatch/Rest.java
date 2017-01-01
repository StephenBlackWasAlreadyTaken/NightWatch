package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.converter.gson.GsonConverterFactory;

import retrofit2.Retrofit;



/**
 * Created by stephenblack on 12/26/14.
 */
public class Rest {
    private Context mContext;
    private String mUrl;
    private static final String UNITS = "mgdl";
    SharedPreferences prefs;
    PowerManager.WakeLock wakeLock;


    Rest(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUrl = prefs.getString("dex_collection_method", "https://{yoursite}.azurewebsites.net");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rest wakelock");
        wakeLock.acquire();
    }

    public boolean getBg(int count) {
        if (!prefs.getBoolean("nightscout_poll", false) && mUrl.compareTo("") != 0 && mUrl.compareTo("https://{yoursite}.azurewebsites.net") != 0) {
            if(wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            return false;
        }
        try {
            boolean newData = false;
            PebbleEndpoint Bgs;

            Bgs = pebbleEndpointInterface().getPebbleInfo(UNITS,count).execute().body();


            double slope = 0, intercept = 0, scale = 0;

            for (Bg returnedBg: Bgs.bgs) {
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


        Retrofit restInf = new Retrofit.Builder()
                .baseUrl(mUrl)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder() .excludeFieldsWithoutExposeAnnotation().create()))
                .build();
        return restInf.create(PebbleEndpointInterface.class);

    }


}
