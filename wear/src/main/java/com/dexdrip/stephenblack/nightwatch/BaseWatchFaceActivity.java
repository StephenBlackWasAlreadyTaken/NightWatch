package com.dexdrip.stephenblack.nightwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by stephenblack on 12/29/14.
 */
public  abstract class BaseWatchFaceActivity extends WatchFaceActivity{
    public final static IntentFilter INTENT_FILTER;
    public TextView mTime, mBattery, mSgv, mDirection, mTimestamp, mUploaderBattery, mDelta;
    public RelativeLayout mRelativeLayout;
    public LinearLayout mLinearLayout;
    public final String TIME_FORMAT_DISPLAYED = "h:mm";
    public long sgvLevel = 0;
    public int batteryLevel = 1;
    public int ageLevel = 1;
    public boolean screenAwake = true;
    public boolean layoutSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        performViewSetup();
    }

    public void performViewSetup() {
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);

        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTime = (TextView) stub.findViewById(R.id.watch_time);
                mBattery = (TextView) stub.findViewById(R.id.watch_battery);
                mSgv = (TextView) stub.findViewById(R.id.sgv);
                mDirection = (TextView) stub.findViewById(R.id.direction);
                mTimestamp = (TextView) stub.findViewById(R.id.timestamp);
                mUploaderBattery = (TextView) stub.findViewById(R.id.uploader_battery);
                mDelta = (TextView) stub.findViewById(R.id.delta);
                mRelativeLayout = (RelativeLayout) stub.findViewById(R.id.main_layout);
                mLinearLayout = (LinearLayout) stub.findViewById(R.id.secondary_layout);
                layoutSet = true;
                mTimeInfoReceiver.onReceive(getApplicationContext(), registerReceiver(null, INTENT_FILTER));
                registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatInfoReceiver);
        unregisterReceiver(mTimeInfoReceiver);
    }

    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mBattery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
        }
    };
    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mTime.setText(
                    new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                            .format(Calendar.getInstance().getTime()));
        }
    };

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            if (layoutSet) {
                sgvLevel = dataMap.getLong("sgvLevel");
                batteryLevel = dataMap.getInt("batteryLevel");
                ageLevel = dataMap.getInt("ageLevel");

                mSgv.setText(dataMap.getString("sgvString"));
                mDirection.setText(dataMap.getString("slopeArrow"));
                mTimestamp.setText(dataMap.getString("readingAge"));
                mUploaderBattery.setText("Uploader: " + dataMap.getString("battery") + "%");
                mDelta.setText(dataMap.getString("delta"));
            } else {
                Log.d("ERROR: ", "DATA IS NOT YET SET");
            }
            setColor();
        }
    }

    public void setColor() { Log.e("ERROR: ", "MUST OVERRIDE IN CLASS"); }

    @Override
    public void onScreenDim() {
        screenAwake = false;
        if (layoutSet) { setColor(); }
    }

    @Override
    public void onScreenAwake() {
        screenAwake = true;
        if (layoutSet) { setColor(); }
    }


}
