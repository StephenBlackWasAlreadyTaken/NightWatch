package com.dexdrip.stephenblack.nightwatch;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by stephenblack on 12/26/14.
 */
public interface PebbleEndpointInterface {
    @GET("/pebble")
    PebbleEndpoint getPebbleInfo(@Query("units") String units, @Query("count") int count);

    @GET("/pebble")
    PebbleEndpoint getPebbleInfo(@Query("count") int count);

    @GET("/pebble")
    PebbleEndpoint getPebbleInfo(@Query("units") String units);

    @GET("/pebble")
    PebbleEndpoint getPebbleInfo();

}
