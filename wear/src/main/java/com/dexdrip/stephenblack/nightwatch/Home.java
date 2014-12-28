package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Home extends Activity {
    private final static IntentFilter INTENT_FILTER;
    private TextView mTime, mBattery, mSgv, mBgDelta, mTrend, mDirection, mTimestamp, mUploaderBattery;
    private final String TIME_FORMAT_DISPLAYED = "hh:mm a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
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
                mTimeInfoReceiver.onReceive(Home.this, registerReceiver(null, INTENT_FILTER));
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

            mSgv.setText(dataMap.getString("sgvString"));
//            mBgDelta.setText(String.valueOf(dataMap.getDouble("bgdelta")));
//            mTrend.setText(dataMap.getString("trend"));
            mDirection.setText(dataMap.getString("slopeArrow"));
//            mTimestamp.setText(dataMap.getString("readingAge"));
//            mUploaderBattery.setText(dataMap.getInt("battery_int"));
        }
    }
}
