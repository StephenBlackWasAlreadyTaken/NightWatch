package com.dexdrip.stephenblack.nightwatch;


import com.dexdrip.stephenblack.nightwatch.model.Bg;

import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.Call;
import java.util.List;

/**
 * Created by stephenblack on 12/26/14.
 */
public interface PebbleEndpointInterface {
    @GET("/pebble/?")
    Call<PebbleEndpoint> getPebbleInfo(@Query("units") String units, @Query("count") int count);

    @GET("/pebble/?")
    Call< PebbleEndpoint> getPebbleInfo(@Query("count") int count);

    @GET("/pebble/?")
    Call<PebbleEndpoint> getPebbleInfo(@Query("units") String units);

    @GET("/pebble/?")
    Call<PebbleEndpoint> getPebbleInfo();

}
