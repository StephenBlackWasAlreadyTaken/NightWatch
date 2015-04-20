package com.dexdrip.stephenblack.nightwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.ShareModels.ShareRest;

import java.util.Calendar;
import java.util.Date;

import retrofit.RetrofitError;

public class DataCollectionService extends Service {
    DataFetcher dataFetcher;
    SharedPreferences mPrefs;
    boolean wear_integration  = false;
    boolean pebble_integration  = false;
    boolean endpoint_set = false;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setSettings();
        listenForChangeInSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setFailoverTimer();
        setSettings();
        if(endpoint_set) { doService(); }
        setAlarm();
        return START_STICKY;
    }

    public void setSettings() {
        wear_integration = mPrefs.getBoolean("watch_sync", false);
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        if (mPrefs.getBoolean("nightscout_poll", false) || mPrefs.getBoolean("share_poll", false)) {
            endpoint_set = true;
            doService();
        } else {
            endpoint_set = false;
        }
    }
    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        long retry_in = (1000 * 60 * 7);
        Log.d("DataCollectionService", "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));
    }

    public void listenForChangeInSettings() {
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                setSettings();
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void doService() {
        Log.d("Performing data fetch: ", "Wish me luck");
        dataFetcher = new DataFetcher(getApplicationContext());
        dataFetcher.execute((Void) null);
    }

    public void setAlarm() {
        long retry_in = (long) sleepTime();
        Log.d("DataCollectionService", "Next packet should be available in " + (retry_in / (60 * 1000)) + " minutes");
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0));
    }

    public double sleepTime() {
        Bg last_bg = Bg.last();
        if (last_bg != null) {
            return Math.max((1000 * 60), Math.min(((long) (((1000 * 60 * 5) + 10000) - ((new Date().getTime()) - last_bg.datetime))), (1000 * 60 * 5)));
        } else {
            return (1000 * 60 * 5);
        }
    }

    public class DataFetcher extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        DataFetcher(Context context) { mContext = context; }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if(mPrefs.getBoolean("nightscout_poll", false)) {
                    boolean success = new Rest(mContext).getBgData();
                    Thread.sleep(10000);
                    if (success) {
                        mContext.startService(new Intent(mContext, WatchUpdaterService.class));
                    }
                    Notifications.notificationSetter(mContext);
                    return true;
                }
                if(mPrefs.getBoolean("share_poll", false)) {
                    boolean success = new ShareRest(mContext).getBgData();
                    Thread.sleep(10000);
                    if (success) {
                        mContext.startService(new Intent(mContext, WatchUpdaterService.class));
                    }
                    Notifications.notificationSetter(mContext);
                    return true;
                }
                return true;
            }
            catch (RetrofitError e) { Log.d("Retrofit Error: ", "BOOOO"); }
            catch (InterruptedException exx) { Log.d("Interruption Error: ", "BOOOO"); }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
            return false;
        }
    }

    public static void newDataArrived(Context context, boolean success) {
        if (success) { context.startService(new Intent(context, WatchUpdaterService.class)); }
        Notifications.notificationSetter(context);
    }
}
