package com.dexdrip.stephenblack.nightwatch;

import android.content.SharedPreferences;
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

    public boolean sgvContainsWhiteSpace(){
        if(sgv != null){
            for(int i = 0; i < sgv.length(); i++){
                if(Character.isWhitespace(sgv.charAt(i))){ return true; }
            }
        }
        return false;
    }

    public int sgv_int() { //This is dumb but for some reason parseint wasnt working properly...
        if (sgvContainsWhiteSpace()){
            return 0;
        } else if (sgv.startsWith("1") && sgv.length() <= 2) {
            return 5;
        } else {
            return Integer.parseInt(sgv);
        }
    }

    public String sgv_string() {
        int asInt = sgv_int();
        if (asInt >= 400) {
            return "High";
        } else if (asInt >= 40) {
            return sgv;
        } else if (asInt >= 11) {
            return "Low";
        } else {
            return "???";
        }
    }

    public String deltaString(SharedPreferences prefs) {
        String unit = prefs.getString("units", "mgdl");
        String unitPretty = "mg/dL";
        if (unit.compareTo("mmol") == 0) {
            unitPretty = "mmol";
        }
        if(bgdelta < 0) {
            return bgdelta + " " + unitPretty;
        } else {
            return "+" + bgdelta + " " + unitPretty;
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

    public DataMap dataMap(SharedPreferences prefs) {
        DataMap dataMap = new DataMap();
        dataMap.putString("sgvString", sgv_string());
        dataMap.putString("slopeArrow", slopeArrow());
        dataMap.putString("readingAge", readingAge());
        dataMap.putString("delta", deltaString(prefs));
        dataMap.putString("battery", battery);
        dataMap.putLong("sgvLevel", sgvLevel(prefs));
        dataMap.putInt("batteryLevel", batteryLevel());
        dataMap.putInt("ageLevel", ageLevel());
        return dataMap;
    }

    public long sgvLevel(SharedPreferences prefs) {
        int highMark = Integer.parseInt(prefs.getString("highValue", "170"));
        int lowMark = Integer.parseInt(prefs.getString("lowValue", "70"));
        if(sgv_int() >= highMark) {
            return 1;
        } else if (sgv_int() >= lowMark) {
            return 0;
        } else {
            return -1;
        }
    }

    public int batteryLevel() {
        int bat = Integer.valueOf(battery.replaceAll("[^\\d.]", ""));
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
