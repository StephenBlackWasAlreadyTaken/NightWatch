package com.dexdrip.stephenblack.nightwatch;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

/**
 * Created by stephenblack on 12/29/14.
 */
public  abstract class BaseWatchFaceActivity extends WatchFaceActivity{
    public final static IntentFilter INTENT_FILTER;
    public static final long[] vibratePattern = {0,400,300,400,300,400};
    public TextView mTime, mSgv, mDirection, mTimestamp, mUploaderBattery, mDelta;
    public RelativeLayout mRelativeLayout;
    public LinearLayout mLinearLayout;
    public final String TIME_FORMAT_DISPLAYED = "h:mm";
    public long sgvLevel = 0;
    public int batteryLevel = 1;
    public int ageLevel = 1;
    public boolean screenAwake = true;
    public int highColor = Color.YELLOW;
    public int lowColor = Color.RED;
    public int midColor = Color.WHITE;
    public int pointSize = 2;
    public boolean singleLine = false;
    public boolean layoutSet = false;
    public int missed_readings_alert_id = 818;
    public BgGraphBuilder bgGraphBuilder;
    public LineChartView chart;
    public double datetime;
    public List<BgWatchData> bgDataList = new ArrayList<BgWatchData>();

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
                mSgv = (TextView) stub.findViewById(R.id.sgv);
                mDirection = (TextView) stub.findViewById(R.id.direction);
                mTimestamp = (TextView) stub.findViewById(R.id.timestamp);
                mUploaderBattery = (TextView) stub.findViewById(R.id.uploader_battery);
                mDelta = (TextView) stub.findViewById(R.id.delta);
                mRelativeLayout = (RelativeLayout) stub.findViewById(R.id.main_layout);
                mLinearLayout = (LinearLayout) stub.findViewById(R.id.secondary_layout);
                chart = (LineChartView) stub.findViewById(R.id.chart);
                layoutSet = true;
                mTimeInfoReceiver.onReceive(getApplicationContext(), registerReceiver(null, INTENT_FILTER));
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
            }
        });
    }

    public int ageLevel() {
        if(timeSince() <= (1000 * 60 * 12)) {
            return 1;
        } else {
            return 0;
        }
    }

    public double timeSince() {
        return new Date().getTime() - datetime;
    }

    public String readingAge() {
        if (datetime == 0) { return "-- Minute ago"; }
        int minutesAgo = (int) Math.floor(timeSince()/(1000*60));
        if (minutesAgo == 1) {
            return minutesAgo + " Minute ago";
        }
        return minutesAgo + " Minutes ago";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTimeInfoReceiver);
    }

    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mTime.setText(
                    new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                            .format(Calendar.getInstance().getTime()));
            mTimestamp.setText(readingAge());
            missedReadingAlert();
        }
    };

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            if (layoutSet) {
                sgvLevel = dataMap.getLong("sgvLevel");
                batteryLevel = dataMap.getInt("batteryLevel");
                datetime = dataMap.getDouble("timestamp");

                mSgv.setText(dataMap.getString("sgvString"));
                mDirection.setText(dataMap.getString("slopeArrow"));
                mUploaderBattery.setText("Uploader: " + dataMap.getString("battery") + "%");
                mDelta.setText(dataMap.getString("delta"));

                if (chart != null) {
                    addToWatchSet(dataMap);
                    setupCharts();
                }
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

    public void missedReadingAlert() {
        int minutes_since = (int) Math.floor(timeSince()/(1000*60));
        if(minutes_since >= 16 && (minutes_since - 16) % 5 == 0) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Missed BG Readings")
                        .setVibrate(vibratePattern);
            NotificationManager mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            mNotifyMgr.notify(missed_readings_alert_id, notification.build());
        }
    }

    public void addToWatchSet(DataMap dataMap){
        double sgv = dataMap.getDouble("sgvDouble");
        double high = dataMap.getDouble("high");
        double low = dataMap.getDouble("low");
        double timestamp = dataMap.getDouble("timestamp");
        bgDataList.add(new BgWatchData(sgv, high, low, timestamp));
        for(int i = 0; i < bgDataList.size(); i++) {
            if(bgDataList.get(i).timestamp < (new Date().getTime() - (1000 * 60 * 60 * 5))) {
                bgDataList.remove(i); //Get rid of anything more than 5 hours old
            }
        }
    }

    public void setupCharts() {
        if(bgDataList.size() > 0) { //Dont crash things just because we dont have values, people dont like crashy things
            if (singleLine) {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, pointSize, midColor);
            } else {
                bgGraphBuilder = new BgGraphBuilder(getApplicationContext(), bgDataList, pointSize, highColor, lowColor, midColor);
            }

            chart.setLineChartData(bgGraphBuilder.lineData());
            chart.setViewportCalculationEnabled(true);
            chart.setMaximumViewport(chart.getMaximumViewport());
        }
    }
}
