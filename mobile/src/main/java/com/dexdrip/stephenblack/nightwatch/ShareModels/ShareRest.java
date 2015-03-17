package com.dexdrip.stephenblack.nightwatch.ShareModels;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.Notifications;
import com.dexdrip.stephenblack.nightwatch.Rest;
import com.dexdrip.stephenblack.nightwatch.WatchUpdaterService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
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
    private SharedPreferences prefs;
    OkClient client;

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public ShareRest(Context context) {
        client = getOkClient();
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
            dexcomShareAuthorizeInterface().getSessionId(new ShareAuthenticationBody(password, login), new Callback() {
                @Override
                public void success(Object o, Response response) {
                    Log.d("ShareRest", "Success!! got a response on auth.");
                    String returnedSessionId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");

                    getBgData(returnedSessionId);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e("RETROFIT ERROR: ", ""+retrofitError.toString());
                }
            });
            return true;
        } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "BOOOO");
                    return false;
        }
    }

    private void getBgData(String sessionId) {
        DataFetcher dataFetcher = new DataFetcher(mContext, sessionId);
        dataFetcher.execute((Void) null);
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
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("SHAREREST"))
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
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("SHAREREST"))
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

    public OkHttpClient getUnsafeOkHttpClient() {

        try {
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
//                    if (hostname.compareTo("https://share1.dexcom.com") == 0)
                        return true;
//                    else
//                        return false;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public OkClient  getOkClient (){
        OkHttpClient client1 = getUnsafeOkHttpClient();
        OkClient _client = new OkClient(client1);
        return _client;
    }

    public Map<String, String> queryParamMap(String sessionId) {
        Map map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        map.put("minutes", "1440");
        map.put("maxCount", "1");
        return map;

    }

    public class DataFetcher extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mSessionId;
        DataFetcher(Context context, String sessionId) {
            mContext = context;
            mSessionId = sessionId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                try {
                    final ShareGlucose[] shareGlucoses = dexcomShareGetBgInterface().getShareBg(queryParamMap(mSessionId));
                    Log.d("REST Success: ", "YAY!");
                    if(shareGlucoses != null && shareGlucoses.length > 0) {
                        for (ShareGlucose shareGlucose : shareGlucoses) {
                            shareGlucose.processShareData(mContext);
                        }
                    return true;
                    }
                    return false;
                } catch (Exception e) {
                    Log.d("REST CALL ERROR: ", "BOOOO");
                    return false;
                }
            }
            catch (RetrofitError e) { Log.d("Retrofit Error: ", "BOOOO"); }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
            return false;
        }
    }
}
