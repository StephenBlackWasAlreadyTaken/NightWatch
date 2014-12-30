package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.Date;

import lecho.lib.hellocharts.ViewportChangeListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class Home extends Activity {
    private String menu_name = "DexDrip";
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    Viewport tempViewport = new Viewport();
    Viewport holdViewport = new Viewport();
    public float left;
    public float right;
    public float top;
    public float bottom;
    public boolean updateStuff;
    public boolean updatingPreviewViewport = false;
    public boolean updatingChartViewport = false;

    public BgGraphBuilder bgGraphBuilder;
    BroadcastReceiver _broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(getApplicationContext(), DataCollectionService.class));
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_bg_notification, false);


        setContentView(R.layout.activity_home);

    }

    @Override
    protected void onResume(){
        super.onResume();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo();
                }
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        holdViewport.set(0, 0, 0, 0);
        setupCharts();
        updateCurrentBgInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            default:
                return true;
        }
    }

    public void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);

        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        previewChart.setLineChartData(bgGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();

    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport, false);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport, false);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff == true) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart), false);
        } else {
            previewChart.setCurrentViewport(holdViewport, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null)
            unregisterReceiver(_broadcastReceiver);
    }

    public void updateCurrentBgInfo() {
        final TextView currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        notificationText.setText("");
        displayCurrentInfo();
    }

    public void displayCurrentInfo() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        Bg lastBgreading = Bg.last();

        if (lastBgreading != null) {
            double estimate =0;
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.datetime > 0) {
                notificationText.setTextColor(Color.parseColor("#C30909"));
                notificationText.setText("Signal Missed");
                estimate = lastBgreading.sgv_int();
                currentBgValueText.setText(df.format(estimate));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                if (lastBgreading != null) {
                    estimate = lastBgreading.sgv_int();
                    currentBgValueText.setText(df.format(estimate) + " " + lastBgreading.slopeArrow());
                    notificationText.setTextColor(Color.parseColor("#ffffff"));
                    notificationText.setText(lastBgreading.readingAge());
                }
            }
            if(estimate <= bgGraphBuilder.lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if(estimate >= bgGraphBuilder.highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
    setupCharts();
    }
}
