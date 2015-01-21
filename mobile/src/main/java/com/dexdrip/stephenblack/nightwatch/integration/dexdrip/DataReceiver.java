package com.dexdrip.stephenblack.nightwatch.integration.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Tells us when G4 data has been updated.
 */
public class DataReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, IntentService.class)
                .setAction(IntentService.ACTION_NEW_DATA)
                .putExtras(intent));
    }
}
