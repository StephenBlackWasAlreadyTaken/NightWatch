package com.dexdrip.stephenblack.nightwatch;

import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by stephenblack on 12/26/14.
 */
public class PebbleEndpoint {
    @Expose
    public List<Status> status;

    @Expose
    public List<Bg> bgs;

    @Expose
    public List<Cal> cals;

}
