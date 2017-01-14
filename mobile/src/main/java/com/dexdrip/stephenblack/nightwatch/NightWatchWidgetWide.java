package com.dexdrip.stephenblack.nightwatch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.dexdrip.stephenblack.nightwatch.activities.Home;

import java.util.Date;


/**
 * Implementation of App Widget functionality.
 */
public class NightWatchWidgetWide extends AppWidgetProvider {

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
        Intent intent = new Intent(context, Home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views = new RemoteViews(context.getPackageName(), R.layout.nightwatch_widget_wide);
        views.setOnClickPendingIntent(R.id.widget_wide, pendingIntent);;
        Log.d(TAG, "Update widget signal received");
        Log.d(TAG, "Update widget signal received");
        displayCurrentInfo(appWidgetManager, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    public static void displayCurrentInfo(AppWidgetManager appWidgetManager, int appWidgetId) {
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(mContext);
        Bg lastBgreading = Bg.last();
        if (lastBgreading != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                int height = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                int width = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                views.setImageViewBitmap(R.id.widgetGraphWide, new BgSparklineBuilder(mContext)
                        .setBgGraphBuilder(bgGraphBuilder)
                        .setHeight(height).setWidth(width).build());
            }
            double estimate = 0;
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.datetime > 0) {
                estimate = lastBgreading.sgv_double();
                Log.d(TAG, "old value, estimate " + estimate);
                views.setTextViewText(R.id.wwidgetBg, bgGraphBuilder.unitized_string(estimate));
                views.setTextViewText(R.id.wwidgetArrow, "--");
                views.setInt(R.id.wwidgetBg, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                estimate = lastBgreading.sgv_double();
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                String slope_arrow = lastBgreading.slopeArrow();
                Log.d(TAG, "newish value, estimate " + stringEstimate + slope_arrow);
                views.setTextViewText(R.id.wwidgetBg, stringEstimate);
                views.setTextViewText(R.id.wwidgetArrow, slope_arrow);
                views.setInt(R.id.wwidgetBg, "setPaintFlags", 0);
            }
            views.setTextViewText(R.id.wwidgetDelta, bgGraphBuilder.unitizedDeltaString(lastBgreading.bgdelta));

            int timeAgo = (int) Math.floor((new Date().getTime() - lastBgreading.datetime) / (1000 * 60));
            String raw_string = "";
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if(prefs.getBoolean("widget_show_raw", false)){
                raw_string = "\n" + Bg.threeRaw((prefs.getString("units", "mgdl").equals("mgdl")));
            }
            if (timeAgo == 1) {
                views.setTextViewText(R.id.wreadingAge, timeAgo + " Minute ago" + raw_string);
            } else {
                views.setTextViewText(R.id.wreadingAge, timeAgo + " Minutes ago" + raw_string);
            }
            if (timeAgo > 15) {
                views.setTextColor(R.id.wreadingAge, Color.parseColor("#FFBB33"));
            } else {
                views.setTextColor(R.id.wreadingAge, Color.WHITE);
            }

            if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
                views.setTextColor(R.id.wwidgetBg, Color.parseColor("#C30909"));
                views.setTextColor(R.id.wwidgetDelta, Color.parseColor("#C30909"));
                views.setTextColor(R.id.wwidgetArrow, Color.parseColor("#C30909"));
            } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
                views.setTextColor(R.id.wwidgetBg, Color.parseColor("#FFBB33"));
                views.setTextColor(R.id.wwidgetDelta, Color.parseColor("#FFBB33"));
                views.setTextColor(R.id.wwidgetArrow, Color.parseColor("#FFBB33"));
            } else {
                views.setTextColor(R.id.wwidgetBg, Color.WHITE);
                views.setTextColor(R.id.wwidgetDelta, Color.WHITE);
                views.setTextColor(R.id.wwidgetArrow, Color.WHITE);
            }
        }
    }
}


