package com.dexdrip.stephenblack.nightwatch.integration.dexdrip;

import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.dexdrip.stephenblack.nightwatch.Bg;
import com.dexdrip.stephenblack.nightwatch.DataCollectionService;

import java.util.Date;

/**
 * Used by DataReceiver to store new estimates from DexDrip.
 *
 * @see com.dexdrip.stephenblack.nightwatch.integration.dexdrip.DataReceiver
 */
public class IntentService extends android.app.IntentService {
    public static final String ACTION_NEW_DATA = "com.dexdrip.stephenblack.nightwatch.action.NEW_DATA";

    public IntentService() {
        super("DexDripIntentService");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        final String action = intent.getAction();

        try {
            if (ACTION_NEW_DATA.equals(action)) {
                final double bgEstimate = intent.getDoubleExtra(Intents.EXTRA_BG_ESTIMATE, 0);
                if (bgEstimate == 0)
                    return;

                int battery = intent.getIntExtra(Intents.EXTRA_SENSOR_BATTERY, 0);

                final Bg bg = new Bg();
                bg.direction = intent.getStringExtra(Intents.EXTRA_BG_SLOPE_NAME);
                bg.battery = Integer.toString(battery);
                bg.bgdelta = (intent.getDoubleExtra(Intents.EXTRA_BG_SLOPE, 0) * 1000 * 60 * 5);
                bg.datetime = intent.getLongExtra(Intents.EXTRA_TIMESTAMP, new Date().getTime());
                bg.sgv = Integer.toString((int) bgEstimate, 10);

                bg.save();
            }

            DataCollectionService.newDataArrived(this, true);
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
