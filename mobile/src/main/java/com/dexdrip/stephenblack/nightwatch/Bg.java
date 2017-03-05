package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.dexdrip.stephenblack.nightwatch.alerts.Notifications;
import com.dexdrip.stephenblack.nightwatch.alerts.UserError.Log;
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
    public SharedPreferences prefs;
    public static String TAG_ALERT = "ALERTS";

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

    @Expose
    @Column(name = "raw")
    public double raw; //Calibrated raw value

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
            return minutesAgo + " " + R.string.s_minute_ago;
        }
        return minutesAgo + " " + R.string.s_minutes_ago;
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
        dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
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

    public static List<Bg> latest(int number) {
        return new Select()
                .from(Bg.class)
                .orderBy("datetime desc")
                .limit(number)
                .execute();
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

    public static Bg findByTimestamp(double timestamp) {
        return new Select()
                .from(Bg.class)
                .where("datetime = ?", (timestamp))
                .executeSingle();
    }

    public static String threeRaw(boolean doMgdl){
        StringBuilder sb = new StringBuilder();
        List<Bg> bgs = latest(3);
        long now = System.currentTimeMillis();
        sb.append(addRaw(bgs, 2, now, doMgdl));
        sb.append(" | ");
        sb.append(addRaw(bgs, 1, now, doMgdl));
        sb.append(" | ");
        sb.append(addRaw(bgs, 0, now, doMgdl));
        return sb.toString();
    }

    private static String addRaw(List<Bg> bgs, int number, long now, boolean doMgdl){
        Bg bg;

        long to = now - number*(1000*60*5);
        long from = to - (1000*60*5);

        for (int i= 0; i<bgs.size(); i++){
            bg = bgs.get(i);
            if(bg !=null && bg.datetime >=from && bg.datetime< to && bg.raw >0 && bg.raw < 600){

                DecimalFormat df = new DecimalFormat("#");
                if(doMgdl) {
                    df.setMaximumFractionDigits(0);
                    return df.format(bg.raw);
                } else {
                    df.setMaximumFractionDigits(1);
                    return df.format(bg.raw*Constants.MGDL_TO_MMOLL);
                }
            }
        }


        return "x";
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

    public static Long getTimeSinceLastReading() {
        Bg bgReading = Bg.last();
        if (bgReading != null) {
            return (long)(new Date().getTime() - bgReading.datetime);
        }
        return (long) 0;
    }

    public String displayValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "mgdl");
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);
        double calculated_value = sgv_double();
        if (sgv_double() >= 400) {
            return "HIGH";
        } else if (calculated_value >= 40) {
            if(unit.compareTo("mgdl") == 0) {
                df.setMaximumFractionDigits(0);
                return df.format(calculated_value);
            } else {
                df.setMaximumFractionDigits(1);
                return df.format(calculated_value_mmol());
            }
        } else if (calculated_value > 12) {
            return "LOW";
        } else {
            switch((int)calculated_value) {
                case 0:
                    return "??0";
                case 1:
                    return "?SN";
                case 2:
                    return "??2";
                case 3:
                    return "?NA";
                case 5:
                    return "?NC";
                case 6:
                    return "?CD";
                case 9:
                    return "?AD";
                case 12:
                    return "?RF";
                default:
                    return "???";
            }
        }
    }

    public double calculated_value_mmol() {
        return mmolConvert(sgv_double());
    }

    public static void checkForRisingAllert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean rising_alert = prefs.getBoolean("rising_alert", false);
        if(!rising_alert) {
            return;
        }
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.i("NOTIFICATIONS", "checkForRisingAllert: Notifications are currently disabled!!");
            return;
        }

        String riseRate = prefs.getString("rising_bg_val", "2");
        float friseRate = 2;

        try
        {
            friseRate = Float.parseFloat(riseRate);
        }
        catch (NumberFormatException nfe)
        {
            Log.e(TAG_ALERT, "checkForRisingAllert reading falling_bg_val failed, continuing with 2", nfe);
        }
        Log.d(TAG_ALERT, "checkForRisingAllert will check for rate of " + friseRate);

        boolean riseAlert = checkForDropRiseAllert(friseRate, false);
        Notifications.RisingAlert(context, riseAlert);
    }


    public static void checkForDropAllert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean falling_alert = prefs.getBoolean("falling_alert", false);
        if(!falling_alert) {
            return;
        }
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.w("NOTIFICATIONS", "checkForDropAllert: Notifications are currently disabled!!");
            return;
        }

        String dropRate = prefs.getString("falling_bg_val", "2");
        float fdropRate = 2;

        try
        {
            fdropRate = Float.parseFloat(dropRate);
        }
        catch (NumberFormatException nfe)
        {
            Log.w(TAG_ALERT, "reading falling_bg_val failed, continuing with 2", nfe);
        }
        Log.i(TAG_ALERT, "checkForDropAllert will check for rate of " + fdropRate);

        boolean dropAlert = checkForDropRiseAllert(fdropRate, true);
        Notifications.DropAlert(context, dropAlert);
    }

    // true say, alert is on.
    private static boolean checkForDropRiseAllert(float MaxSpeed, boolean drop) {
        Log.d(TAG_ALERT, "checkForDropRiseAllert called drop=" + drop);
        List<Bg> latest = getXRecentPoints(4);
        if(latest == null) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert we don't have enough points from the last 15 minutes, returning false");
            return false;
        }
        float time3 = (float)(latest.get(0).datetime - latest.get(3).datetime) / 60000;
        double bg_diff3 = latest.get(3).sgv_double() - latest.get(0).sgv_double();
        if (!drop) {
            bg_diff3 *= (-1);
        }
        Log.i(TAG_ALERT, "bg_diff3=" + bg_diff3 + " time3 = " + time3);
        if(bg_diff3 < time3 * MaxSpeed) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert for latest 4 points not fast enough, returning false");
            return false;
        }
        // we should alert here, but if the last measurement was less than MaxSpeed / 2, I won't.


        float time1 = (float)(latest.get(0).datetime - latest.get(1).datetime) / 60000;
        double bg_diff1 = latest.get(1).sgv_double() - latest.get(0).sgv_double();
        if (!drop) {
            bg_diff1 *= (-1);
        }

        if(time1 > 7.0) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert the two points are not close enough, returning true");
            return true;
        }
        if(bg_diff1 < time1 * MaxSpeed /2) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert for latest 2 points not fast enough, returning false");
            return false;
        }
        Log.d(TAG_ALERT, "checkForDropRiseAllert returning true speed is " + (bg_diff3 / time3));
        return true;
    }

    public static List<Bg> getXRecentPoints(int NumReadings) {
        List<Bg> latest = Bg.latest(NumReadings);
        if (latest == null || latest.size() != NumReadings) {
            // for less than NumReadings readings, we can't tell what the situation
            //
            Log.d(TAG_ALERT, "getXRecentPoints we don't have enough readings, returning null");
            return null;
        }
        // So, we have at least three values...
        for(Bg bgReading : latest) {
            Log.d(TAG_ALERT, "getXRecentPoints - reading: time = " + bgReading.datetime + " calculated_value " + bgReading.sgv_double());
        }

        // now let's check that they are relevant. the last reading should be from the last 5 minutes,
        // x-1 more readings should be from the last (x-1)*5 minutes. we will allow 5 minutes for the last
        // x to allow one packet to be missed.
        if (new Date().getTime() - latest.get(NumReadings - 1).datetime > (NumReadings * 5 + 6) * 60 * 1000) {
            Log.d(TAG_ALERT, "getXRecentPoints we don't have enough points from the last " + (NumReadings * 5 + 6) + " minutes, returning null");
            return null;
        }
        return latest;

    }
    public static boolean trendingToAlertEnd(Context context, boolean above) {
        // TODO: check if we are not in an UnclerTime.
        Log.d(TAG_ALERT, "trendingToAlertEnd called");

        List<Bg> latest = getXRecentPoints(3);
        if(latest == null) {
            Log.d(TAG_ALERT, "trendingToAlertEnd we don't have enough points from the last 15 minutes, returning false");
            return false;
        }

        if(above == false) {
            // This is a low alert, we should be going up
            if((latest.get(0).sgv_double() - latest.get(1).sgv_double() > 4) ||
                    (latest.get(0).sgv_double() - latest.get(2).sgv_double() > 10)) {
                Log.d(TAG_ALERT, "trendingToAlertEnd returning true for low alert");
                return true;
            }
        } else {
            // This is a high alert we should be heading down
            if((latest.get(1).sgv_double() - latest.get(0).sgv_double() > 4) ||
                    (latest.get(2).sgv_double() - latest.get(0).sgv_double() > 10)) {
                Log.d(TAG_ALERT, "trendingToAlertEnd returning true for high alert");
                return true;
            }
        }
        Log.d(TAG_ALERT, "trendingToAlertEnd returning false, not in the right direction (or not fast enough)");
        return false;

    }

    public static double currentSlope(){
        List<Bg> last_2 = Bg.latest(2);
        if (last_2.size() == 2) {
            return calculateSlope(last_2.get(0), last_2.get(1));
        } else{
            return 0d;
        }
    }

    public static double calculateSlope(Bg current, Bg last) {
        if (current.datetime == last.datetime || current.sgv_double() == last.sgv_double()) {
            return 0;
        } else {
            return (last.sgv_double() - current.sgv_double()) / (last.datetime - current.datetime);
        }
    }


}
