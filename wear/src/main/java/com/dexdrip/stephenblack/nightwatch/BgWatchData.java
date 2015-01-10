package com.dexdrip.stephenblack.nightwatch;

/**
 * Created by stephenblack on 1/7/15.
 */
public class BgWatchData {
    public double sgv;
    public double high;
    public double low;
    public double timestamp;

    public BgWatchData(double aSgv, double aHigh, double aLow, double aTimestamp) {
        this.sgv = aSgv;
        this.high = aHigh;
        this.low = aLow;
        this.timestamp = aTimestamp;
    }
}
