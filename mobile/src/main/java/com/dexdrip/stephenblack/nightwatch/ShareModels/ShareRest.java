package com.dexdrip.stephenblack.nightwatch.ShareModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.Bg;
import com.dexdrip.stephenblack.nightwatch.PebbleEndpoint;
import com.dexdrip.stephenblack.nightwatch.PebbleEndpointInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ShareRest {
    private Context mContext;
    private String login;
    private String password;
    private String sessionId;
    private SharedPreferences prefs;

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public ShareRest(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        login = prefs.getString("dexcom_account_name", "");
        password = prefs.getString("dexcom_account_password", "");
    }

    public boolean getBgData() {
        if (prefs.getBoolean("share_poll", false) && login.compareTo("") != 0 && password.compareTo("") != 0) {
            return loginAndGetData();
        } else {
            return false;
        }
    }

    private boolean loginAndGetData() {
        try {
            dexcomShareAuthorizeInterface().getSessionId(new Callback() {
                @Override
                public void success(Object o, Response response) {
                    getBgData(new String(((TypedByteArray) response.getBody()).getBytes()));
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e("RETROFIT ERROR: ", ""+retrofitError.getResponse().getStatus());
                }
            });
            return true;
        } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "BOOOO");
                    return false;
        }
    }

    private void getBgData(String sessionId) {
        try {
            final ShareGlucose[] shareGlucoses = dexcomShareAuthorizeInterface().getShareBg(sessionId);
            Log.d("REST Success: ", "YAY!");
            if(shareGlucoses != null && shareGlucoses.length > 0)
            for(ShareGlucose shareGlucose : shareGlucoses) {
                shareGlucose.processShareData(mContext);
            }
        } catch (Exception e) {
            Log.d("REST CALL ERROR: ", "BOOOO");
        }
    }

    private DexcomShareInterface dexcomShareAuthorizeInterface() {
        RestAdapter adapter = authoirizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }
    
    private DexcomShareInterface dexcomShareGetBgInterface() {
        RestAdapter adapter = getBgAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    private RestAdapter.Builder authoirizeAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(authorizationRequestInterceptor)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    private RestAdapter.Builder getBgAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(getBgRequestInterceptor)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    RequestInterceptor authorizationRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestInterceptor.RequestFacade request) {
            request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
        }
    };
    RequestInterceptor getBgRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestInterceptor.RequestFacade request) {
            request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Content-Length", "0");
            request.addHeader("Accept", "application/json");
        }
    };
}
