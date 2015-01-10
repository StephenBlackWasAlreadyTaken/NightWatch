package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.Date;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 12/26/14.
 */
public class Rest {
    private Context mContext;
    private String mUrl;
    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    Rest(Context context) {
        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mUrl = prefs.getString("dex_collection_method", "https://{yoursite}.azurewebsites.net");

    }

    public boolean getBgData() {
        try {
            PebbleEndpoint response = pebbleEndpointInterface().getPebbleInfo();
            Bg returnedBg = response.bgs.get(0);
            if (Bg.is_new(returnedBg)) {
                returnedBg.save();
                return true;
            }
            Log.d("REST CALL SUCCESS: ", "HORRAY");
        } catch (Exception e) {
            Log.d("REST CALL ERROR: ", "BOOOO");
            return false;
        }
        return false;
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
