package com.dexdrip.stephenblack.nightwatch;

import android.graphics.Color;
import android.os.Bundle;

public class LargeHomeDark extends BaseWatchFaceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_large);
        performViewSetup();
    }

    @Override
    public void setColor() {
        mRelativeLayout.setBackgroundColor(Color.BLACK);
        mLinearLayout.setBackgroundColor(Color.LTGRAY);
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
