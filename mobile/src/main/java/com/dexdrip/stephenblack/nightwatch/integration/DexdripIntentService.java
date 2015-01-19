package com.dexdrip.stephenblack.nightwatch.integration;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.dexdrip.stephenblack.nightwatch.Bg;
import com.dexdrip.stephenblack.nightwatch.DataCollectionService;

import java.util.Date;

/**
 * Used by DexdripDataReceiver to store new estimates from DexDrip.
 */
public class DexDripIntentService extends IntentService {
    public static final String ACTION_NEW_DATA = "com.dexdrip.stephenblack.nightwatch.action.NEW_DATA";

    public DexDripIntentService() {
        super("DexDripIntentService");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        final String action = intent.getAction();

        if (ACTION_NEW_DATA.equals(action)) {
            final double bgEstimate = intent.getDoubleExtra(DexDripIntents.EXTRA_BG_ESTIMATE, 0);
            if (bgEstimate == 0)
                return;

            int battery = (int) (100 * intent.getIntExtra(DexDripIntents.EXTRA_SENSOR_BATTERY, 0) / 255f);

            final Bg bg = new Bg();
            bg.direction = intent.getStringExtra(DexDripIntents.EXTRA_BG_SLOPE_NAME);
            bg.battery = Integer.toString(battery);
            bg.bgdelta = intent.getDoubleExtra(DexDripIntents.EXTRA_BG_SLOPE, 0);
            bg.datetime = new Date().getTime();
            bg.sgv = Integer.toString((int) bgEstimate, 10);

            bg.save();
        }

        DataCollectionService.newDataArrived(this, true);

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}