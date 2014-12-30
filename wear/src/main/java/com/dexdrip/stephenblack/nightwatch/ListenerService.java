package com.dexdrip.stephenblack.nightwatch;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ListenerService extends WearableListenerService {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEARABLE_DATA_PATH)) {
                }
                dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                Intent messageIntent = new Intent();
                messageIntent.setAction(Intent.ACTION_SEND);
                messageIntent.putExtra("data", dataMap.toBundle());
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            }
        }
    }
}
