package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;
import java.util.ListIterator;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 12/26/14.
 */
public class Rest {
    private Context mContext;
    private String mUrl;
    private static final String UNITS = "mgdl";
    SharedPreferences prefs;
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    Rest(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUrl = prefs.getString("dex_collection_method", "https://{yoursite}.azurewebsites.net");
    }

    public boolean getBgData(int count) {
        if (!prefs.getBoolean("nightscout_poll", false) && mUrl.compareTo("") != 0 && mUrl.compareTo("https://{yoursite}.azurewebsites.net") != 0) {
            return false;
        }
        try {
            PebbleEndpoint response;
            boolean newData = false;

            if (count > 1) {
                response =  pebbleEndpointInterface().getPebbleInfo(UNITS, count);
            } else
                response = pebbleEndpointInterface().getPebbleInfo(UNITS);

            for (Bg returnedBg: response.bgs) {
                if (Bg.is_new(returnedBg)) {
                    returnedBg.save();
                    DataCollectionService.newDataArrived(mContext, true, returnedBg);
                    newData = true;
                }
            }
            Log.d("REST CALL SUCCESS: ", "HORRAY");
            return newData;
        } catch (Exception e) {

            Log.d("REST CALL ERROR: ", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean getBgData() {
        return getBgData(1);
    }

    private PebbleEndpointInterface pebbleEndpointInterface() {
        RestAdapter adapter = adapterBuilder().build();
        PebbleEndpointInterface pebbleEndpointInterface =
                adapter.create(PebbleEndpointInterface.class);

        return pebbleEndpointInterface;
    }

    private RestAdapter.Builder adapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint(mUrl)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));

        return adapterBuilder;
    }
}
