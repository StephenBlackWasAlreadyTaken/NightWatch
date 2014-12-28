package com.dexdrip.stephenblack.nightwatch;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephenblack on 12/26/14.
 */
public class PebbleEndpoint {
    @Expose
    public List<Statu> status;

    @Expose
    public List<Bg> bgs;

}
