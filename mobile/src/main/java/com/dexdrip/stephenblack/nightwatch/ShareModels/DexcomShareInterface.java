package com.dexdrip.stephenblack.nightwatch.ShareModels;

import com.dexdrip.stephenblack.nightwatch.PebbleEndpoint;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by stephenblack on 3/16/15.
 */
public interface DexcomShareInterface {
    @POST("https://share1.dexcom.com/ShareWebServices/Services/Publisher/ReadPublisherLatestGlucoseValues?sessionID={sessionId}&minutes=1440&maxCount=1")
    ShareGlucose[] getShareBg(@Path("sessionId") String sessionId);

    @POST("/General/LoginPublisherAccountByName")
    void getSessionId(Callback<Response> callback);
    //Since this seems to respond with a string we need a callback that will parse the response body
    //new String(((TypedByteArray) response.getBody()).getBytes());
}
