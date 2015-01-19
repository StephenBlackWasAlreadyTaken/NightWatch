package com.dexdrip.stephenblack.nightwatch.integration;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Tells us when G4 data has been updated.
 */
public class DexDripDataReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, DexDripIntentService.class)
                .setAction(DexDripIntentService.ACTION_NEW_DATA)
                .putExtras(intent));
    }
}
