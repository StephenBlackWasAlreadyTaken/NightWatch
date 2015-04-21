package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
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
    private SharedPreferences prefs;

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

    @Expose
    @Column(name = "filtered")
    public double filtered;

    @Expose
    @Column(name = "unfiltered")
    public double unfiltered;

    @Expose
    @Column(name = "noise")
    public double noise;

    public String unitized_string() {
        double value = sgv_double();
        DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            return "HIGH";
        } else if (value >= 40) {
            if(doMgdl()) {
                df.setMaximumFractionDigits(0);
                return df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                df.setMinimumFractionDigits(1);
                return df.format(unitized(value));
            }
        } else if (value >= 11) {
            return "LOW";
        } else {
            return "???";
        }
    }
    public String unitized_string(SharedPreferences aPrefs) {
        prefs = aPrefs;
        return unitized_string();
    }

    public String unitizedDeltaString() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
        String delta_sign = "";
        if (bgdelta > 0.1) { delta_sign = "+"; }
        if(doMgdl()) {
            return delta_sign + df.format(unitized(bgdelta)) + " mg/dl";
        } else {
            return delta_sign + df.format(unitized(bgdelta)) + " mmol";
        }
    }
    public String unitizedDeltaStringNoUnit() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
        String delta_sign = "";
        if (bgdelta > 0.1) { delta_sign = "+"; }
        if(doMgdl()) {
            return delta_sign + df.format(unitized(bgdelta));
        } else {
            return delta_sign + df.format(unitized(bgdelta));
        }
    }

    public boolean sgvContainsWhiteSpace(){
        if(sgv != null){
            for(int i = 0; i < sgv.length(); i++){
                if(Character.isWhitespace(sgv.charAt(i))){ return true; }
            }
        }
        return false;
    }

    public double sgv_double() { //This is dumb but for some reason parseint wasnt working properly...
        if (sgvContainsWhiteSpace()){
            return 0;
        } else if (sgv.startsWith("1") && sgv.length() <= 2) {
            return 5;
        } else {
            return Integer.parseInt(sgv);
        }
    }

    public double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }


    public boolean doMgdl() {
        String unit = prefs.getString("units", "mgdl");
        if (unit.compareTo("mgdl") == 0) {
            return true;
        } else {
            return false;
        }
    }

    public int battery_int() {
        return Integer.parseInt(battery);
    }

    public String slopeArrow() {
        String arrow = "--";
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
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " Minute ago";
        }
        return minutesAgo + " Minutes ago";
    }

    public double timeSince() {
        return new Date().getTime() - datetime;
    }

    public DataMap dataMap(SharedPreferences sPrefs) {
        prefs = sPrefs;

        Double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        DataMap dataMap = new DataMap();
        dataMap.putString("sgvString", unitized_string());
        dataMap.putString("slopeArrow", slopeArrow());
        dataMap.putDouble("timestamp", datetime);
        dataMap.putString("delta", unitizedDeltaString());
        dataMap.putString("battery", battery);
        dataMap.putLong("sgvLevel", sgvLevel(prefs));
        dataMap.putInt("batteryLevel", batteryLevel());

        dataMap.putDouble("sgvDouble", sgv_double());
        dataMap.putDouble("high", inMgdl(highMark));
        dataMap.putDouble("low", inMgdl(lowMark));
        return dataMap;
    }

    public double inMgdl(double value) {
        if (!doMgdl()) {
            return value * Constants.MMOLL_TO_MGDL;
        } else {
            return value;
        }

    }

    public double fromRaw(Cal cal) {
        double factor = doMgdl() ? 1 : Constants.MMOLL_TO_MGDL;
        double raw;
        double mgdl = sgv_double();
        if (filtered == Double.NaN || mgdl <= 30) {
            raw = cal.scale * (unfiltered - cal.intercept) / cal.slope;
        } else {
            double ratio = cal.scale * (filtered - cal.intercept) / cal.slope / mgdl;
            raw = cal.scale * (unfiltered - cal.intercept) / cal.slope / ratio;
        }
        return raw * factor;
    }

    public long sgvLevel(SharedPreferences prefs) {
        Double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        if(unitized(sgv_double()) >= highMark) {
            return 1;
        } else if (unitized(sgv_double()) >= lowMark) {
            return 0;
        } else {
            return -1;
        }
    }

    public double unitized(double value) {
        if(doMgdl()) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public int batteryLevel() {
        int bat = battery != null ? Integer.valueOf(battery.replaceAll("[^\\d.]", "")) : 0;
        if(bat >= 30) {
            return 1;
        } else {
            return 0;
        }
    }

    public int ageLevel() {
        if(timeSince() <= (1000 * 60 * 12)) {
            return 1;
        } else {
            return 0;
        }
    }

    public static Bg last() {
        return new Select()
                .from(Bg.class)
                .orderBy("datetime desc")
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

    public static Bg mostRecentBefore(double timestamp) {
        return new Select()
                .from(Bg.class)
                .where("datetime < ?", (timestamp))
                .orderBy("datetime desc")
                .executeSingle();
    }

    public static boolean alreadyExists(double timestamp) {
        Bg bg = new Select()
                .from(Bg.class)
                .where("datetime <= ?", (timestamp + (2 * 1000)))
                .orderBy("datetime desc")
                .executeSingle();
        if(bg != null && bg.datetime >= (timestamp - (2 * 1000))) {
            return true;
        } else {
            return false;
        }
    }

}
