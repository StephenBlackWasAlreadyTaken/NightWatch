package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;

import com.ustwo.clockwise.WatchMode;

import lecho.lib.hellocharts.util.Utils;

public class Home extends BaseWatchFace {

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.activity_home, null);
        performViewSetup();
    }

    protected void setColorDark() {
        mTime.setTextColor(Color.WHITE);
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
        if (chart != null) {
            highColor = Color.YELLOW;
           lowColor = Color.RED;
            midColor = Color.WHITE;
            singleLine = false;
            pointSize = 2;
            setupCharts();
        }

    }


    protected void setColorBright() {

        if (getCurrentWatchMode() == WatchMode.INTERACTIVE) {
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
