package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class HomeDark extends BaseWatchFaceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        performViewSetup();
    }

    @Override
    public void setColor() {
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
        if (ageLevel == 1) {
            mTimestamp.setTextColor(Color.BLACK);
        } else {
            mTimestamp.setTextColor(Color.RED);
        }

        if (batteryLevel == 1) {
            mUploaderBattery.setTextColor(Color.BLACK);
        } else {
            mUploaderBattery.setTextColor(Color.RED);
        }
    }
}
