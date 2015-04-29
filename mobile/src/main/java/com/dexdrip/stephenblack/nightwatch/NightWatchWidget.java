package com.dexdrip.stephenblack.nightwatch;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Date;


/**
 * Implementation of App Widget functionality.
 */
public class NightWatchWidget extends AppWidgetProvider {
    public static RemoteViews views;
    public static Context mContext;
    public static String TAG = "xDripWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "Widget enabled");
        context.startService(new Intent(context, WidgetUpdateService.class));
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "Widget disabled");
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mContext = context;
        views = new RemoteViews(context.getPackageName(), R.layout.nightwatch_widget_small);
        Log.d(TAG, "Update widget signal received");
        displayCurrentInfo();
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    public static void displayCurrentInfo() {
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(mContext);
        Bg lastBgreading = Bg.last();
        if (lastBgreading != null) {
            double estimate = 0;
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.datetime > 0) {
                estimate = lastBgreading.sgv_double();
                Log.d(TAG, "old value, estimate " + estimate);
                views.setTextViewText(R.id.widgetBg, bgGraphBuilder.unitized_string(estimate));
                views.setTextViewText(R.id.widgetArrow, "--");
                views.setInt(R.id.widgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                estimate = lastBgreading.sgv_double();
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                String slope_arrow = lastBgreading.slopeArrow();
                Log.d(TAG, "newish value, estimate " + stringEstimate + slope_arrow);
                views.setTextViewText(R.id.widgetBg, stringEstimate);
                views.setTextViewText(R.id.widgetArrow, slope_arrow);
                views.setInt(R.id.widgetBg, "setPaintFlags", 0);
            }
            views.setTextViewText(R.id.widgetDelta, bgGraphBuilder.unitizedDeltaString(lastBgreading.bgdelta));
            int timeAgo =(int) Math.floor((new Date().getTime() - lastBgreading.datetime)/(1000*60));
            if (timeAgo == 1) {
                views.setTextViewText(R.id.readingAge, timeAgo + " Minute ago");
            } else {
                views.setTextViewText(R.id.readingAge, timeAgo + " Minutes ago");
            }
            if (timeAgo > 15) {
                views.setTextColor(R.id.readingAge, Color.parseColor("#FFBB33"));
            } else {
                views.setTextColor(R.id.readingAge, Color.WHITE);
            }

            if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                views.setTextColor(R.id.widgetBg, Color.parseColor("#C30909"));
                views.setTextColor(R.id.widgetDelta, Color.parseColor("#C30909"));
                views.setTextColor(R.id.widgetArrow, Color.parseColor("#C30909"));
            } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                views.setTextColor(R.id.widgetBg, Color.parseColor("#FFBB33"));
                views.setTextColor(R.id.widgetDelta, Color.parseColor("#FFBB33"));
                views.setTextColor(R.id.widgetArrow, Color.parseColor("#FFBB33"));
            } else {
                views.setTextColor(R.id.widgetBg, Color.WHITE);
                views.setTextColor(R.id.widgetDelta, Color.WHITE);
                views.setTextColor(R.id.widgetArrow, Color.WHITE);
            }
        }
    }
}

