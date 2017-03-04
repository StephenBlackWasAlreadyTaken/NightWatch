package com.dexdrip.stephenblack.nightwatch.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Settings;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.dexdrip.stephenblack.nightwatch.alerts.AlertPlayer;
import com.dexdrip.stephenblack.nightwatch.alerts.MissedReadingService;
import com.dexdrip.stephenblack.nightwatch.alerts.Notifications;
import com.dexdrip.stephenblack.nightwatch.model.UserError.Log;
import com.dexdrip.stephenblack.nightwatch.model.Bg;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.activeandroid.Cache.getContext;

/**
 * Created by stephenblack on 1/14/15.
 */
@Table(name = "AlertType", id = BaseColumns._ID)
public class AlertType extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "active")
    public boolean active;

    @Column(name = "volume")
    public int volume;

    @Column(name = "vibrate")
    public boolean vibrate;

    @Column(name = "light")
    public boolean light;

    @Column(name = "override_silent_mode")
    public boolean override_silent_mode;

    @Column(name = "predictive")
    public boolean predictive;

    @Column(name = "time_until_threshold_crossed")
    public double time_until_threshold_crossed;


    @Column(name = "type" )
    public alertType type;

    public enum alertType{high,low,missed};

    @Column(name = "threshold")
    public double threshold;

    @Column( name = "missed_minutes_threshold")
    public int missed_minutes_threshold;

    @Column(name = "all_day")
    public boolean all_day;

    @Column(name = "start_time_minutes")
    public int start_time_minutes;  // This have probable be in minutes from start of day. this is not time...

    @Column(name = "end_time_minutes")
    public int end_time_minutes;

    @Column(name = "minutes_between") //??? what is the difference between minutes_between and default_snooze ???
    public int minutes_between; // The idea here was if ignored it will go off again each x minutes, snooze would be if it was aknowledged and dismissed it will go off again in y minutes
    // that said, Im okay with doing away with the minutes between and just doing it at a set 5 mins like dex

    @Column(name = "default_snooze")
    public int default_snooze;

    @Column(name = "text") // ??? what's that? is it different from name?
    public String text; // I figured if we wanted some special text, Its

    @Column(name = "mp3_file")
    public String mp3_file;

    @Column(name = "uuid", index = true)
    public String uuid;

    private final static String TAG = Notifications.class.getSimpleName();
    private final static String TAG_ALERT = "AlertBg";

    public static AlertType get_alert(String uuid) {

        return new Select()
        .from(AlertType.class)
        .where("uuid = ? ", uuid)
        .executeSingle();
    }

    /*
     * This function has 3 needs. In the case of "unclear state" return null
     * In the case of "unclear state" for more than predefined time, return the "55" alert
     * In case that alerts are turned off, only return the 55.
     */
    public static AlertType get_highest_active_alert(Context context, double bg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            UserError.Log.w("NOTIFICATIONS", "Notifications are currently disabled!!");
            return null;
        }

        AlertType missedDataAlert = checkIfMissedReadingAlert(context);

        if ( missedDataAlert != null ) {
            Log.d( TAG_ALERT, "get_highest_active_alert_helper returned alert uuid = " +  missedDataAlert.uuid + " alert name = " + missedDataAlert.name );
            return missedDataAlert;
        }
        if (bg <= 14) { // Special dexcom codes should not set off low alarms
            return null;
        }

        AlertType at = get_highest_active_alert_helper(bg);
        if (at != null) {
            Log.d(TAG_ALERT, "get_highest_active_alert_helper returned alert uuid = " + at.uuid + " alert name = " + at.name);
        } else {
            Log.d(TAG_ALERT, "get_highest_active_alert_helper returned NULL");
        }
        return at;
    }

    public static AlertType checkIfMissedReadingAlert(Context context){
        return get_missed_active_alert_helper();
    }

    private static AlertType get_missed_active_alert_helper() {
        List<AlertType> missedAlerts  = new Select()
                .from(AlertType.class)
                .where("type = ?", alertType.missed)
                .orderBy("threshold asc")
                .execute();

        for (AlertType missedAlert : missedAlerts) {
            if( missedAlert.should_alarm_missed_data( getContext() )  ) {

                return missedAlert;
            }
        }
        return null;
    }
    // bg_minute is the estimate of the bg change rate
    private static AlertType get_highest_active_alert_helper(double bg) {
        // Check the missed data alerts
        List<AlertType> missedAlerts  = new Select()
                .from(AlertType.class)
                .where("type = ?", alertType.missed)
                .orderBy("threshold asc")
                .execute();

        for (AlertType missedAlert : missedAlerts) {
            if( missedAlert.should_alarm_missed_data( getContext() )  ) {
                return missedAlert;
            }
        }

        // Check the low alerts
        List<AlertType> lowAlerts  = new Select()
            .from(AlertType.class)
            .where("threshold >= ?", bg)
            .where("type = ?", alertType.low)
            .orderBy("threshold asc")
            .execute();

        for (AlertType lowAlert : lowAlerts) {
            if(lowAlert.should_alarm(bg)) {
                return lowAlert;
            }
        }

        // If no low alert found, check higher alert.
        List<AlertType> HighAlerts  = new Select()
            .from(AlertType.class)
            .where("threshold <= ?", bg)
            .where("type = ?", alertType.high)
            .orderBy("threshold desc")
            .execute();

        for (AlertType HighAlert : HighAlerts) {
            //Log.e(TAG, "Testing high alert " + HighAlert.toString());
            if(HighAlert.should_alarm(bg)) {
                return HighAlert;
            }
        }
        // no alert found
        return null;
    }

    // returns true, if one alert is up and the second is down
    public static boolean OpositeDirection(AlertType a1, AlertType a2) {
        if ( a1.type == AlertType.alertType.high &&
                a2.type == AlertType.alertType.low ) {
            return true;
        }
        return false;
    }

    // Checks if a1 is more important than a2. returns the higher one
    // This function compares the 2 alerts and returns the one that has
    // the highest priority. The priority is in this order
    //
    // missed_data <- highest since correctitve action needed you don't know if the reading is high
    //                or low
    // low <- if low then the lowest
    //
    // high <- if high then the highest
    //
    public static AlertType HigherAlert(AlertType a1, AlertType a2) {


        if (a1.type == alertType.missed && a2.type != alertType.missed) {
            return a1;
        }
        if ( a2.type == alertType.missed && a1.type != alertType.missed ){
            return a2;
        }
        if (a1.type == alertType.missed && a2.type == alertType.missed ) {
            // if both are missed - the one with the smallest threshold wins
            if (a1.missed_minutes_threshold < a2.missed_minutes_threshold) {
                return a1;
            } else {
                return a2;
            }
        }
        if (a1.type == alertType.high && a2.type == alertType.low) {
            return a2;
        }
        if (a1.type == alertType.low && a2.type == alertType.high) {
            return a1;
        }
        if (a1.type == alertType.high && a2.type == alertType.high) {
            // both are high, the higher the better
            if (a1.threshold > a2.threshold) {
                return a1;
            } else {
                return a2;
            }
        }
        if ( a1.type == alertType.low && a2.type == alertType.low ) {
            // both are low, the lower the better
            if (a1.threshold < a2.threshold) {
                return a1;
            } else {
                return a2;
            }
        }
        // FIXME what should the default return be?
        return a1;
    }

    public static void remove_all() {
        List<AlertType> Alerts  = new Select()
        .from(AlertType.class)
        .execute();

        for (AlertType alert : Alerts) {
            alert.delete();
        }
        ActiveBgAlert.ClearData();
    }

    public static void add_alert(
            String uuid,
            String name,
            alertType type,
            double threshold,
            boolean all_day,
            int minutes_between,
            String mp3_file,
            int start_time_minutes,
            int end_time_minutes,
            boolean override_silent_mode,
            int snooze,
            boolean vibrate) {
        AlertType at = new AlertType();
        at.name = name;
        at.type = type;
        at.threshold = threshold;
        if ( at.type == AlertType.alertType.missed ) {
            at.missed_minutes_threshold = (int) threshold;
        }

        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid != null? uuid : UUID.randomUUID().toString();
        at.active = true;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.default_snooze = snooze;
        at.vibrate = vibrate;
        at.save();
    }

    public static void update_alert(
            String uuid,
            String name,
            alertType type,
            double threshold,
            boolean all_day,
            int minutes_between,
            String mp3_file,
            int start_time_minutes,
            int end_time_minutes,
            boolean override_silent_mode,
            int snooze,
            boolean vibrate) {

        AlertType at = get_alert(uuid);
        at.name = name;
        at.type= type;
        if ( at.type == AlertType.alertType.missed ) {
            at.missed_minutes_threshold = (int)threshold;
        }
        at.threshold = threshold;

        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid;
        at.active = true;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.default_snooze = snooze;
        at.vibrate = vibrate;
        at.save();
    }
    public static void remove_alert(String uuid) {
        AlertType alert = get_alert(uuid);
		if(alert != null) {
	        alert.delete();
        }
    }

    public String toString() {

        String name = "name: " + this.name;
        String type = "type: " + this.type;
        String threshold = "threshold: " + this.threshold;
        String all_day = "all_day: " + this.all_day;
        String time = "Start time: " + this.start_time_minutes + " end time: "+ this.end_time_minutes;
        String minutes_between = "minutes_between: " + this.minutes_between;
        String uuid = "uuid: " + this.uuid;

        return name + " " + type + " " + threshold + " "+ all_day + " " +time +" " + minutes_between + " uuid" + uuid;
    }

    public static void print_all() {
        List<AlertType> Alerts  = new Select()
            .from(AlertType.class)
            .execute();

        Log.d(TAG,"List of all alerts");
        for (AlertType alert : Alerts) {
            Log.d(TAG, alert.toString());
        }
    }

    public static List<AlertType> getAll(AlertType.alertType type) {
        String order;
        if (type == alertType.high) {
            order = "threshold asc";
        } else {
            order = "threshold desc";
        }
        List<AlertType> alerts  = new Select()
            .from(AlertType.class)
            .where("type = ?", type)
            .orderBy(order)
            .execute();

        return alerts;
    }


    public static void testAll(Context context) {

        remove_all();
        add_alert(null, "high alert 1", alertType.high, 180, true, 10, null, 0, 0, true, 20, true);
        add_alert(null, "high alert 2", alertType.high, 200, true, 10, null, 0, 0, true, 20, true);
        add_alert(null, "high alert 3", alertType.high, 220, true, 10, null, 0, 0, true, 20, true);
        print_all();
        AlertType a1 = get_highest_active_alert(context, 190);
        Log.d(TAG, "a1 = " + a1.toString());
        AlertType a2 = get_highest_active_alert(context, 210);
        Log.d(TAG, "a2 = " + a2.toString());


        AlertType a3 = get_alert(a1.uuid);
        Log.d(TAG, "a1 == a3 ? need to see true " + (a1==a3) + a1 + " " + a3);

        add_alert(null, "low alert 1", alertType.low, 80, true, 10, null, 0, 0, true, 20, true);
        add_alert(null, "low alert 2", alertType.low, 60, true, 10, null, 0, 0, true, 20, true);
        print_all();
        AlertType al1 = get_highest_active_alert(context, 90);
        Log.d(TAG, "al1 should be null  " + al1);
        al1 = get_highest_active_alert(context, 80);
        Log.d(TAG, "al1 = " + al1.toString());
        AlertType al2 = get_highest_active_alert(context, 50);
        Log.d(TAG, "al2 = " + al2.toString());

        add_alert(null, "missed data 1", alertType.missed, 15, true, 10, null, 0, 0, true, 20, true);
        add_alert(null, "missed data 2", alertType.missed, 30, true, 10, null, 0, 0, true, 20, true);
        print_all();

        Log.d(TAG, "HigherAlert(a1, a2) = a1? " +  (HigherAlert(a1,a2) == a2));
        Log.d(TAG, "HigherAlert(al1, al2) = al1? " +  (HigherAlert(al1,al2) == al2));
        Log.d(TAG, "HigherAlert(a1, al1) = al1? " +  (HigherAlert(a1,al1) == al1));
        Log.d(TAG, "HigherAlert(al1, a2) = al1? " +  (HigherAlert(al1,a2) == al1));

        // Make sure we do not influence on real data...
        remove_all();

    }


    private boolean in_time_frame() {
        if (all_day) {
            //Log.e(TAG, "in_time_frame returning true " );
            return true;
        }
        // time_now is the number of minutes that have passed from the start of the day.
        Calendar rightNow = Calendar.getInstance();
        int time_now = toTime(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
        Log.d(TAG, "time_now is " + time_now + " minutes" + " start_time " + start_time_minutes + " end_time " + end_time_minutes);
        if(start_time_minutes < end_time_minutes) {
            if (time_now >= start_time_minutes && time_now <= end_time_minutes) {
                return true;
            }
        } else {
            if (time_now >= start_time_minutes || time_now <= end_time_minutes) {
                return true;
            }
        }
        return false;
    }

    private boolean beyond_threshold(double bg) {
        if (type == alertType.high && bg >= threshold) {
//            Log.e(TAG, "beyond_threshold returning true " );
            return true;
        } else if (type == alertType.low && bg <= threshold) {
            return true;
        }
        return false;
    }

    private boolean trending_to_threshold(double bg) {
        if (!predictive) { return false; }
        if (type == alertType.high && bg >= threshold) {
            return true;
        } else if (type == alertType.low && bg <= threshold) {
            return true;
        }
        return false;
    }
    /**
     * Gets the state of Airplane Mode.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean isAirplaneModeOn(Context context) {

        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

    }
    public long getNextAlertTime(Context ctx) {
        int time = minutes_between;
        if (time < 1 || AlertPlayer.isAscendingMode(ctx)) {
            time = 1;
        }
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis() + (time * 60000);
    }

    //
    // check to see if we should alarm on a missed data alert
    private boolean should_alarm_missed_data( Context context ) {
        boolean ret;
        if ( !isAirplaneModeOn(context)
                && ( Bg.readingAgeInMins() > (missed_minutes_threshold ) )
                && (in_time_frame() && active) ) {
            ret = true;
        } else {
            ret = false;
        }
        return ret;

    }
    private boolean should_alarm(double bg) {
//        Log.e(TAG, "should_alarm called active =  " + active );
        if(in_time_frame() && active && (beyond_threshold(bg) || trending_to_threshold(bg))) {
            return true;
        } else {
            return false;
        }
    }

    public static void testAlert(
            String name,
            alertType type,
            double threshold,
            boolean all_day,
            int minutes_between,
            String mp3_file,
            int start_time_minutes,
            int end_time_minutes,
            boolean override_silent_mode,
            int snooze,
            boolean vibrate,
            Context context) {
            AlertType at = new AlertType();
            at.name = name;
            at.type = type;
            at.threshold = threshold;
            at.all_day = all_day;
            at.minutes_between = minutes_between;
            at.uuid = UUID.randomUUID().toString();
            at.active = true;
            at.mp3_file = mp3_file;
            at.start_time_minutes = start_time_minutes;
            at.end_time_minutes = end_time_minutes;
            at.override_silent_mode = override_silent_mode;
            at.default_snooze = snooze;
            at.vibrate = vibrate;
            AlertPlayer.getPlayer().startAlert(context, false, at, "TEST");
    }

    // Time is calculated in minutes. that is 01:20 means 80 minutes.

    // This functions are a bit tricky. We can only set time from 00:00 to 23:59 which leaves one minute out. this is because we ignore the
    // seconds. so if the user has set 23:59 we will consider this as 24:00
    // This will be done at the code that reads the time from the ui.



    // return the minutes part of the time
    public static int time2Minutes(int minutes) {
        return (minutes - 60*time2Hours(minutes)) ;
    }

 // return the hours part of the time
    public static int time2Hours(int minutes) {
        return minutes / 60;
    }

    // create the time from hours and minutes.
    public static int toTime(int hours, int minutes) {
        return hours * 60 + minutes;
    }
}
