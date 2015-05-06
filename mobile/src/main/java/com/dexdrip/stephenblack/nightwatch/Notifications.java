package com.dexdrip.stephenblack.nightwatch;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/28/14.
 */
public class Notifications {
    public static final long[] vibratePattern = {0,1000,300,1000,300,1000};
    public static boolean bg_notifications;
    public static boolean bg_ongoing;
    public static boolean bg_vibrate;
    public static boolean bg_lights;
    public static boolean bg_sound;
    public static boolean bg_sound_in_silent;
    public static int bg_snooze;
    public static String bg_notification_sound;

    public static boolean calibration_notifications;
    public static boolean calibration_vibrate;
    public static boolean calibration_lights;
    public static boolean calibration_sound;
    public static int calibration_snooze;
    public static String calibration_notification_sound;

    public static Context mContext;

    public static int BgNotificationId = 1;
    public static int calibrationNotificationId = 2;
    public static int doubleCalibrationNotificationId = 3;
    public static int extraCalibrationNotificationId = 4;
    public static final int OngoingNotificationId = 5;
    public static SharedPreferences prefs;

    public static int currentVolume;
    public static AudioManager manager;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    public static void setNotificationSettings(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        bg_notifications = prefs.getBoolean("bg_notifications", true);
        bg_ongoing = prefs.getBoolean("bg_ongoing", true);
        bg_vibrate = prefs.getBoolean("bg_vibrate", true);
        bg_lights = prefs.getBoolean("bg_lights", true);
        bg_sound = prefs.getBoolean("bg_play_sound", true);
        bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);
        bg_snooze = Integer.parseInt(prefs.getString("bg_snooze", "20"));
        bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");
    }

    public static void notificationSetter(Context context) {
        setNotificationSettings(context);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        double high = bgGraphBuilder.highMark;
        double low = bgGraphBuilder.lowMark;

        Bg bgReading = Bg.last();

        if (bg_ongoing && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            bgOngoingNotification(bgGraphBuilder);
        }
        if (bg_notifications) {
            if (bgGraphBuilder.unitized(bgReading.sgv_double()) >= high || bgGraphBuilder.unitized(bgReading.sgv_double()) <= low) {
                if(bgReading.sgv_double() > 13 && bgReading.datetime > (new Date().getTime() - (60 * 1000 * 60))) {
                    bgAlert(bgReading.unitized_string(prefs), bgReading.slopeArrow());
                } else {
                    clearBgAlert();
                }
            } else {
                clearBgAlert();
            }
        } else {
            clearAllBgNotifications();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void bgOngoingNotification(BgGraphBuilder bgGraphBuilder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mContext, Home.class);
                List<Bg> lastReadings = Bg.latest(2);
                Bg lastReading = null;
                if (lastReadings != null && lastReadings.size() >= 2) {
                    lastReading = lastReadings.get(0);
                }

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                stackBuilder.addParentStack(Home.class);
                stackBuilder.addNextIntent(intent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                NotificationCompat.Builder b = new NotificationCompat.Builder(mContext);
                //b.setOngoing(true);
                b.setCategory(NotificationCompat.CATEGORY_STATUS);
                b.setContentTitle(lastReading == null ? "BG Reading Unavailable" : (lastReading.unitized_string(prefs) + " " + lastReading.slopeArrow()))
                        .setContentText("xDrip Data collection service is running.")
                        .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                        .setUsesChronometer(false);
                if (lastReading != null) {
                    Bitmap wearBitmap = new BgSparklineBuilder(mContext)
                            .setBgGraphBuilder(new BgGraphBuilder(mContext))
                            .showHighLine()
                            .showLowLine()
                            .showAxes()
                            .setWidth(400)
                            .setHeight(400)
                            .build();

                    b.setWhen((long) lastReading.datetime);
                    String deltaText = "Delta: " + lastReading.unitizedDeltaString();
                    b.setContentText(deltaText);
                    b.setLargeIcon(new BgSparklineBuilder(mContext)
                            .setHeight(64)
                            .setWidth(64)
                            .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                            .setBgGraphBuilder(new BgGraphBuilder(mContext))
                            .build());

                    NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                    bigPictureStyle.bigPicture(new BgSparklineBuilder(mContext)
                            .setBgGraphBuilder(new BgGraphBuilder(mContext))
                            .showHighLine()
                            .showLowLine()
                            .build())
                            .setSummaryText(deltaText).setBigContentTitle(deltaText);
                    b.setStyle(bigPictureStyle)
                            .extend(new NotificationCompat.WearableExtender()
                                    .setBackground(wearBitmap)
                                    .addPage(new NotificationCompat.Builder(mContext)
                                            //.setContentTitle(deltaText)
                                            .extend(new NotificationCompat.WearableExtender()
                                                    .setBackground(wearBitmap)
                                                    .setHintShowBackgroundOnly(true)
                                                    .setHintAvoidBackgroundClipping(true))
                                            .build())
                                    );
                }
                b.setContentIntent(resultPendingIntent);
                NotificationManagerCompat
                        .from(mContext)
                        .notify(OngoingNotificationId, b.build());
            }
        });
    }

    public static void clearAllBgNotifications() {
        notificationDismiss(BgNotificationId);
    }

    public static void bgNotificationCreate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        if (bg_vibrate) { mBuilder.setVibrate(vibratePattern);}
        if (bg_lights) { mBuilder.setLights(0xff00ff00, 300, 1000);}
        if (bg_sound && !bg_sound_in_silent) { mBuilder.setSound(Uri.parse(bg_notification_sound), AudioAttributes.FLAG_AUDIBILITY_ENFORCED);}
        if (bg_sound && bg_sound_in_silent) { soundAlert(bg_notification_sound);}
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    public static void soundAlert(String soundUri) {
        manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        Uri notification = Uri.parse(bg_notification_sound);
        MediaPlayer player = MediaPlayer.create(mContext, notification);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        });
        player.start();

    }

    public static void notificationUpdate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    public static NotificationCompat.Builder notificationBuilder(String title, String content, Intent intent) {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(notificationIntent(intent));
    }
    public static PendingIntent notificationIntent(Intent intent){
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public static void notificationDismiss(int notificationId) {
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
    }

    public static void bgAlert(String value, String slopeArrow) {
        UserNotification userNotification = UserNotification.lastBgAlert();

        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * bg_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification newUserNotification = UserNotification.create(value + " " + slopeArrow, "bg_alert");
            String title = value + " " + slopeArrow;
            String content = "BG LEVEL ALERT: " + value + " " + slopeArrow;
            Intent intent = new Intent(mContext, Home.class);
            bgNotificationCreate(title, content, intent, BgNotificationId);

        } else if ((userNotification != null) && (userNotification.timestamp >= ((new Date().getTime()) - (60000 * bg_snooze))))  {
            Bg bg = Bg.last();
            String title = value + " " + slopeArrow;
            String content = "BG LEVEL ALERT: " + value + " " + slopeArrow;
            Intent intent = new Intent(mContext, Home.class);
            notificationUpdate(title, content, intent, BgNotificationId);
        }
    }

    public static void clearBgAlert() {
        UserNotification userNotification = UserNotification.lastBgAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(BgNotificationId);
        }
    }
}
