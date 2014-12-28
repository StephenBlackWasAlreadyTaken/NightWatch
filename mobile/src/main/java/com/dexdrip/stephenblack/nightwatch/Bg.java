package com.dexdrip.stephenblack.nightwatch;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.android.gms.wearable.DataMap;
import com.google.gson.annotations.Expose;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 12/26/14.
 */

@Table(name = "Bg", id = BaseColumns._ID)
public class Bg extends Model {

    @Expose
    @Column(name = "sgv")
    public String sgv;

    @Expose
    @Column(name = "bgdelta")
    public double bgdelta;

    @Expose
    @Column(name = "trend")
    public double trend;

    @Expose
    @Column(name = "direction")
    public String direction;

    @Expose
    @Column(name = "datetime")
    public double datetime;

    @Expose
    @Column(name = "battery")
    public String battery;

    public int sgv_int() {
        return Integer.parseInt(sgv);
    }

    public String sgv_string() {
        int asInt = sgv_int();
        if (asInt >= 500) {
            return "High";
        } else if (asInt <= 40) {
            return "Low";
        } else {
            return sgv;
        }
    }

    public int battery_int() {
        return Integer.parseInt(battery);
    }

    public String slopeArrow() {
        String arrow = "";
        if (direction.compareTo("DoubleDown") == 0) {
            arrow = "\u21ca";
        } else if (direction.compareTo("SingleDown") == 0) {
            arrow = "\u2193";
        } else if (direction.compareTo("FortyFiveDown") == 0) {
            arrow = "\u2198";
        } else if (direction.compareTo("Flat") == 0) {
            arrow = "\u2192";
        } else if (direction.compareTo("FortyFiveUp") == 0) {
            arrow = "\u2197";
        } else if (direction.compareTo("SingleUp") == 0) {
            arrow = "\u2191";
        } else if (direction.compareTo("DoubleUp") == 0) {
            arrow = "\u21c8";
        }
        return arrow;
    }

    public String readingAge() {
        double timeSince = new Date().getTime() - datetime;
        int minutesAgo = (int) Math.floor(timeSince/(1000*60));
        if (minutesAgo == 0) {
            return "Now";
        }
        return minutesAgo + " Minutes ago";
    }

    public DataMap dataMap() {
        DataMap dataMap = new DataMap();
        dataMap.putString("sgv", sgv);
        dataMap.putString("sgvString", sgv_string());
        dataMap.putDouble("bgdelta", bgdelta);
        dataMap.putDouble("trend", trend);
        dataMap.putString("slopeArrow", slopeArrow());
        dataMap.putString("readingAge", readingAge());
        dataMap.putInt("battery_int", battery_int());
        return dataMap;
    }

    public static Bg last() {
        return new Select()
                .from(Bg.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static boolean is_new(Bg bg) {
        Bg foundBg = new Select()
                .from(Bg.class)
                .where("datetime = ?", bg.datetime)
                .orderBy("_ID desc")
                .executeSingle();
            return (foundBg == null);
    }

    public static List<Bg> latestForGraph(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(Bg.class)
                .where("datetime >= " + df.format(startTime))
                .orderBy("datetime desc")
                .limit(number)
                .execute();
    }
}
