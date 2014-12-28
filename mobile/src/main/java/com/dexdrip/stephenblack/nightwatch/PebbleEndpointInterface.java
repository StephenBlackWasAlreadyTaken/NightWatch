package com.dexdrip.stephenblack.nightwatch;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by stephenblack on 12/26/14.
 */
public interface PebbleEndpointInterface {
    @GET("/pebble")
    PebbleEndpoint getPebbleInfo();
}
