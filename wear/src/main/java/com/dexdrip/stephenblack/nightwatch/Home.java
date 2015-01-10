package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import lecho.lib.hellocharts.util.Utils;

public class Home extends BaseWatchFaceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        performViewSetup();
    }

    @Override
    public void setColor() {
        if (screenAwake) {
            mRelativeLayout.setBackgroundColor(Color.WHITE);
            mLinearLayout.setBackgroundColor(Color.BLACK);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Utils.COLOR_ORANGE);
                mDirection.setTextColor(Utils.COLOR_ORANGE);
                mDelta.setTextColor(Utils.COLOR_ORANGE);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.BLACK);
                mDirection.setTextColor(Color.BLACK);
                mDelta.setTextColor(Color.BLACK);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(Color.RED);
                mDirection.setTextColor(Color.RED);
                mDelta.setTextColor(Color.RED);
            }

            if (ageLevel == 1) {
                mTimestamp.setTextColor(Color.WHITE);
            } else {
                mTimestamp.setTextColor(Color.RED);
            }

            if (batteryLevel == 1) {
                mUploaderBattery.setTextColor(Color.WHITE);
            } else {
                mUploaderBattery.setTextColor(Color.RED);
            }

            mTime.setTextColor(Color.BLACK);
            if (chart != null) {
                highColor = Utils.COLOR_ORANGE;
                midColor = Color.BLUE;
                lowColor = Color.RED;
                singleLine = false;
                pointSize = 2;
                setupCharts();
            }
        } else {
            mRelativeLayout.setBackgroundColor(Color.BLACK);
            mLinearLayout.setBackgroundColor(Color.WHITE);
            if (sgvLevel == 1) {
                mSgv.setTextColor(Color.YELLOW);
                mDirection.setTextColor(Color.YELLOW);
                mDelta.setTextColor(Color.YELLOW);
            } else if (sgvLevel == 0) {
                mSgv.setTextColor(Color.WHITE);
                mDirection.setTextColor(Color.WHITE);
                mDelta.setTextColor(Color.WHITE);
            } else if (sgvLevel == -1) {
                mSgv.setTextColor(Color.RED);
                mDirection.setTextColor(Color.RED);
                mDelta.setTextColor(Color.RED);
            }

            mUploaderBattery.setTextColor(Color.BLACK);
            mTimestamp.setTextColor(Color.BLACK);

            mTime.setTextColor(Color.WHITE);
            if (chart != null) {
                highColor = Color.YELLOW;
                midColor = Color.WHITE;
                lowColor = Color.RED;
                singleLine = true;
                pointSize = 2;
                setupCharts();
            }
        }
    }
}
