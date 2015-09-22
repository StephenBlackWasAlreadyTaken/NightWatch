package com.dexdrip.stephenblack.nightwatch.AlertsCode;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;

/**
 * Created by stephenblack on 11/29/14.
 */

@Table(name = "NotificationsV2", id = BaseColumns._ID)
public class UserNotificationV2 extends Model {

    @Column(name = "timestamp", index = true)
    public double timestamp;

    @Column(name = "message")
    public String message;

    @Column(name = "bg_alert")
    public boolean bg_alert;

    @Column(name = "calibration_alert")
    public boolean calibration_alert;

    @Column(name = "double_calibration_alert")
    public boolean double_calibration_alert;

    @Column(name = "extra_calibration_alert")
    public boolean extra_calibration_alert;

    @Column(name = "bg_unclear_readings_alert")
    public boolean bg_unclear_readings_alert;

    @Column(name = "bg_missed_alerts")
    public boolean bg_missed_alerts;

    @Column(name = "bg_rise_alert")
    public boolean bg_rise_alert;

    @Column(name = "bg_fall_alert")
    public boolean bg_fall_alert;

    public static UserNotificationV2 lastBgAlert() {
        return new Select()
                .from(UserNotificationV2.class)
                .where("bg_alert = ?", true)
                .orderBy("_ID desc")
                .executeSingle();
    }
    public static UserNotificationV2 lastCalibrationAlert() {
        return new Select()
                .from(UserNotificationV2.class)
                .where("calibration_alert = ?", true)
                .orderBy("_ID desc")
                .executeSingle();
    }
    public static UserNotificationV2 lastDoubleCalibrationAlert() {
        return new Select()
                .from(UserNotificationV2.class)
                .where("double_calibration_alert = ?", true)
                .orderBy("_ID desc")
                .executeSingle();
    }
    public static UserNotificationV2 lastExtraCalibrationAlert() {
        return new Select()
                .from(UserNotificationV2.class)
                .where("extra_calibration_alert = ?", true)
                .orderBy("_ID desc")
                .executeSingle();
    }


    public static UserNotificationV2 GetNotificationByType(String type) {
        type = type + " = ?";
        return new Select()
        .from(UserNotificationV2.class)
        .where(type, true)
        .orderBy("_ID desc")
        .executeSingle();
    }

    public static void DeleteNotificationByType(String type) {
        UserNotificationV2 userNotification = UserNotificationV2.GetNotificationByType(type);
        if (userNotification != null) {
            userNotification.delete();
        }
    }

    public static UserNotificationV2 create(String message, String type) {
        UserNotificationV2 userNotification = new UserNotificationV2();
        userNotification.timestamp = new Date().getTime();
        userNotification.message = message;
        if (type == "bg_alert") {
            userNotification.bg_alert = true;
        } else if (type == "calibration_alert") {
            userNotification.calibration_alert = true;
        } else if (type == "double_calibration_alert") {
            userNotification.double_calibration_alert = true;
        } else if (type == "extra_calibration_alert") {
            userNotification.extra_calibration_alert = true;
        } else if (type == "bg_unclear_readings_alert") {
            userNotification.bg_unclear_readings_alert = true;
        } else if (type == "bg_missed_alerts") {
            userNotification.bg_missed_alerts = true;
        } else if (type == "bg_rise_alert") {
            userNotification.bg_rise_alert = true;
        } else if (type == "bg_fall_alert") {
            userNotification.bg_fall_alert = true;
        }
        userNotification.save();
        return userNotification;

    }
}
