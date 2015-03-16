package com.dexdrip.stephenblack.nightwatch.ShareModels;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.dexdrip.stephenblack.nightwatch.Bg;
import com.dexdrip.stephenblack.nightwatch.DataCollectionService;
import com.dexdrip.stephenblack.nightwatch.integration.dexdrip.Intents;
import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * Created by stephenblack on 3/16/15.
 */
@Table(name = "ShareGlucose", id = BaseColumns._ID)
public class ShareGlucose extends Model {
    public Context mContext;
    @Expose
    @Column(name = "DT")
    public String DT;

    @Expose
    @Column(name = "ST")
    public String ST;

    @Expose
    @Column(name = "Trend")
    public double Trend;

    @Expose
    @Column(name = "Value")
    public double Value;

    @Expose
    @Column(name = "WT")
    public String WT;

    public void processShareData(Context context) {
        mContext = context;
        double timestamp = Integer.parseInt(WT.split("/\\((.*)\\)/", 3)[1]) * 1000;
        if (!Bg.alreadyExists(timestamp)) {
            Bg bg = new Bg();
            bg.direction = slopeDirection();
            bg.battery = Integer.toString(getBatteryLevel());
            bg.bgdelta = 0;
            bg.datetime = timestamp;
            bg.sgv = Integer.toString((int) Value);
            bg.save();
            DataCollectionService.newDataArrived(mContext, true);
            Log.d("SHARE", "Share Data Processed Successfully!");
        } else {
            Log.d("SHARE", "A Bg Value similar to this timestamp already exists.");
        }
    }

    public String slopeDirection() {
        switch((int) Trend) {
            case 1:
                return "DoubleUp";
            case 2:
                return "SingleUp";
            case 3:
                return "FortyFiveUp";
            case 4:
                return "Flat";
            case 5:
                return "FortyFiveDown";
            case 6:
                return "SingleDown";
            case 7:
                return "DoubleDown";
            default:
                return "";
        }
    }


    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) {
            return 50;
        }
        return (int)(((float)level / (float)scale) * 100.0f);
    }
}
