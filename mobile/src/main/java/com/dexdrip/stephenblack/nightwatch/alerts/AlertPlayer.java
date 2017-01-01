package com.dexdrip.stephenblack.nightwatch.alerts;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.dexdrip.stephenblack.nightwatch.model.ActiveBgAlert;
import com.dexdrip.stephenblack.nightwatch.model.AlertType;
import com.dexdrip.stephenblack.nightwatch.model.UserError.Log;
import com.dexdrip.stephenblack.nightwatch.R;

import java.util.Date;

public class AlertPlayer {

    static AlertPlayer singletone;

    private final static String TAG = AlertPlayer.class.getSimpleName();
    private MediaPlayer mediaPlayer;
    int volumeBeforeAlert;
    int volumeForThisAlert;

    final static int ALERT_PROFILE_HIGH = 1;
    final static int ALERT_PROFILE_ASCENDING = 2;
    final static int ALERT_PROFILE_MEDIUM = 3;
    final static int ALERT_PROFILE_VIBRATE_ONLY = 4;
    final static int ALERT_PROFILE_SILENT = 5;

    final static int  MAX_VIBRATING = 2;
    final static int  MAX_ASCENDING = 5;


    public static AlertPlayer getPlayer() {
        if(singletone == null) {
            Log.i(TAG,"getPlayer: Creating a new AlertPlayer");
            singletone = new AlertPlayer();
        } else {
            Log.i(TAG,"getPlayer: Using existing AlertPlayer");
        }
        return singletone;
    }

    public synchronized  void startAlert(Context ctx, boolean trendingToAlertEnd, AlertType newAlert, String bgValue )  {
        Log.d(TAG, "startAlert called, Threadid " + Thread.currentThread().getId());
        if (trendingToAlertEnd) {
            Log.d(TAG,"startAlert: This alert is trending to it's end will not do anything");
            return;
        }

        stopAlert(ctx, true, false);

        long nextAlertTime = newAlert.getNextAlertTime(ctx);
        ActiveBgAlert.Create(newAlert.uuid, false, nextAlertTime );
        Vibrate(ctx, newAlert, bgValue, newAlert.override_silent_mode, 0);
    }

    public synchronized void stopAlert(Context ctx, boolean ClearData, boolean clearIfSnoozeFinished) {

        Log.d(TAG, "stopAlert: stop called ClearData " + ClearData + "  ThreadID " + Thread.currentThread().getId());
        if (ClearData) {
            ActiveBgAlert.ClearData();
        }
        if(clearIfSnoozeFinished) {
            ActiveBgAlert.ClearIfSnoozeFinished();
        }
        notificationDismiss(ctx);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public synchronized  void Snooze(Context ctx, int repeatTime) {
        Log.i(TAG, "Snooze called repeatTime = " + repeatTime);
        stopAlert(ctx, false, false);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            Log.w(TAG, "Error, snooze was called but no alert is active. alert was probably removed in ui ");
            return;
        }
        activeBgAlert.snooze(repeatTime);
    }

    public synchronized  void PreSnooze(Context ctx, String uuid, int repeatTime) {
        Log.i(TAG, "PreSnooze called repeatTime = "+ repeatTime);
        stopAlert(ctx, true, false);
        ActiveBgAlert.Create(uuid, true, new Date().getTime() + repeatTime * 60000);
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            Log.wtf(TAG, "Just created the alert, where did it go...");
            return;
        }
        activeBgAlert.snooze(repeatTime);
    }

 // Check the state and alrarm if needed
    public void ClockTick(Context ctx, boolean trendingToAlertEnd, String bgValue)
    {
        if (trendingToAlertEnd) {
            Log.d(TAG,"ClockTick: This alert is trending to it's end will not do anything");
            return;
        }
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if (activeBgAlert  == null) {
            // Nothing to do ...
            return;
        }
        if(activeBgAlert.ready_to_alarm()) {
            stopAlert(ctx, false, false);

            int timeFromStartPlaying = activeBgAlert.getUpdatePlayTime();
            AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if (alert == null) {
                Log.w(TAG, "ClockTick: The alert was already deleted... will not play");
                ActiveBgAlert.ClearData();
                return;
            }
            Log.d(TAG, "ClockTick: Playing the alert again");
            long nextAlertTime = alert.getNextAlertTime(ctx);
            activeBgAlert.updateNextAlertAt(nextAlertTime);

            Vibrate(ctx, alert, bgValue, alert.override_silent_mode, timeFromStartPlaying);
        }

    }

    private void PlayFile(final Context ctx, String FileName, float VolumeFrac) {
        Log.i(TAG, "PlayFile: called FileName = " + FileName);
        if(mediaPlayer != null) {
            Log.i(TAG, "ERROR, PlayFile:going to leak a mediaplayer !!!");
        }
        if(FileName != null && FileName.length() > 0) {
            mediaPlayer = MediaPlayer.create(ctx, Uri.parse(FileName), null);
        }
        if(mediaPlayer == null) {
            Log.i(TAG, "PlayFile: Creating mediaplayer with file " + FileName + " failed. using default alarm");
            mediaPlayer = MediaPlayer.create(ctx, R.raw.default_alert);
        }
        if(mediaPlayer != null) {
            AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volumeBeforeAlert = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeForThisAlert = (int)(maxVolume * VolumeFrac);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeForThisAlert, 0);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.i(TAG, "PlayFile: onCompletion called (finished playing) ");
                    AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                    int currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if(volumeForThisAlert == currentVolume) {
                        // If the user has changed the volume, don't change it again.
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeAlert, 0);
                    }
                }
            });
            Log.i(TAG, "PlayFile: calling mediaPlayer.start() ");
            mediaPlayer.start();
        } else {
            // TODO, what should we do here???
            Log.wtf(TAG, "PlayFile: Starting an alert failed, what should we do !!!");
        }
    }

    private PendingIntent notificationIntent(Context ctx, Intent intent){
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
    private PendingIntent snoozeIntent(Context ctx){
        Intent intent = new Intent(ctx, SnoozeOnNotificationDismissService.class);
        return PendingIntent.getService(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static private int getAlertProfile(Context ctx){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String profile = prefs.getString("bg_alert_profile", "ascending");
        if(profile.equals("High")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_HIGH");
            return ALERT_PROFILE_HIGH;
        }
        if(profile.equals("ascending")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_ASCENDING");
            return ALERT_PROFILE_ASCENDING;
        }
        if(profile.equals("medium")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_MEDIUM");
            return ALERT_PROFILE_MEDIUM;
        }
        if(profile.equals("vibrate only")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_VIBRATE_ONLY");
            return ALERT_PROFILE_VIBRATE_ONLY;
        }
        if(profile.equals("Silent")) {
            Log.i(TAG, "getAlertProfile returning ALERT_PROFILE_SILENT");
            return ALERT_PROFILE_SILENT;
        }
        Log.wtf(TAG, "getAlertProfile unknown value " + profile + " ALERT_PROFILE_ASCENDING");
        return ALERT_PROFILE_ASCENDING;

    }

    public static boolean isAscendingMode(Context ctx){
        Log.d("Adrian", "(getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING): " + (getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING));
        return getAlertProfile(ctx) == ALERT_PROFILE_ASCENDING;
    }

    private void Vibrate(Context ctx, AlertType alert, String bgValue, Boolean overrideSilent, int timeFromStartPlaying) {
        Log.d(TAG, "Vibrate called timeFromStartPlaying = " + timeFromStartPlaying);
        Log.d("ALARM", "setting vibrate alarm");
        int profile = getAlertProfile(ctx);

        // We use timeFromStartPlaying as a way to force vibrating/ non vibrating...
        if (profile != ALERT_PROFILE_ASCENDING) {
            // We start from the non ascending part...
            timeFromStartPlaying = MAX_ASCENDING;
        }

        String title = bgValue + " " + alert.name;
        String content = "BG LEVEL ALERT: " + bgValue;
        Intent intent = new Intent(ctx, SnoozeActivity.class);

        NotificationCompat.Builder  builder = new NotificationCompat.Builder(ctx)
            .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(notificationIntent(ctx, intent))
            .setDeleteIntent(snoozeIntent(ctx));
        if (profile != ALERT_PROFILE_VIBRATE_ONLY && profile != ALERT_PROFILE_SILENT) {
            if (timeFromStartPlaying >= MAX_VIBRATING) {
                // Before this, we only vibrate...
                float volumeFrac = (float)(timeFromStartPlaying - MAX_VIBRATING) / (MAX_ASCENDING - MAX_VIBRATING);
                volumeFrac = Math.min(volumeFrac, 1);
                if(profile == ALERT_PROFILE_MEDIUM) {
                    volumeFrac = (float)0.7;
                }
                Log.d(TAG, "Vibrate volumeFrac = " + volumeFrac);
                boolean isRingTone = EditAlertActivity.isPathRingtone(ctx, alert.mp3_file);
                if(isRingTone && !overrideSilent) {
                        builder.setSound(Uri.parse(alert.mp3_file));
                } else {
                    if(overrideSilent || isLoudPhone(ctx)) {
                        PlayFile(ctx, alert.mp3_file, volumeFrac);
                    }
                }
            }
        }
        if (profile != ALERT_PROFILE_SILENT && alert.vibrate) {
            builder.setVibrate(Notifications.vibratePattern);
        }
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Notifications.exportAlertNotificationId);
        mNotifyMgr.notify(Notifications.exportAlertNotificationId, builder.build());
    }

    private void notificationDismiss(Context ctx) {
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Notifications.exportAlertNotificationId);
    }

    // True means play the file false means only vibrate.
    private boolean isLoudPhone(Context ctx) {
        AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                return false;
            case AudioManager.RINGER_MODE_VIBRATE:
                return false;
            case AudioManager.RINGER_MODE_NORMAL:
                return true;
        }
        // unknown mode, not sure let's play just in any case.
        return true;
    }
}
