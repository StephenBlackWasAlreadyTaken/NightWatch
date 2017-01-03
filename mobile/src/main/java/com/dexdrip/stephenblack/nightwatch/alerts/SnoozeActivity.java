package com.dexdrip.stephenblack.nightwatch.alerts;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.dexdrip.stephenblack.nightwatch.activities.BaseActivity;
import com.dexdrip.stephenblack.nightwatch.activities.Home;
import com.dexdrip.stephenblack.nightwatch.BgGraphBuilder;
import com.dexdrip.stephenblack.nightwatch.R;
import com.dexdrip.stephenblack.nightwatch.model.ActiveBgAlert;
import com.dexdrip.stephenblack.nightwatch.model.AlertType;
import com.dexdrip.stephenblack.nightwatch.model.UserError;

import java.text.DateFormat;
import java.util.Date;

public class SnoozeActivity extends BaseActivity {
    public static final String MENU_NAME = "Snooze Alert";

    TextView alertStatus;
    Button buttonSnooze;
    Button disableAlerts;
    Button clearDisabled;
    Button disableLowAlerts;
    Button clearLowDisabled;
    Button disableHighAlerts;
    Button clearHighDisabled;
    SharedPreferences prefs;
    boolean doMgdl;

    NumberPicker snoozeValue;

    @Override
    public String getMenuName() {
        return MENU_NAME;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_snooze;
    }

    static final int snoozeValues[] = new int []{5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360, 420, 480, 540, 600};

    static int getSnoozeLocation(int time) {
        for (int i=0; i < snoozeValues.length; i++) {
            if(time == snoozeValues[i]) {
                return i;
            } else if (time < snoozeValues[i]) {
                // we are in the middle of two, return the smaller
                if (i == 0) {
                    return 0;
                }
                return i-1;
            }
        }
        return snoozeValues.length-1;
    }

    static String getNameFromTime(int time) {
        if (time < 120) {
            return time + " minutes";
        }
        return (time / 60.0) + " hours";
    }

    static int getTimeFromSnoozeValue(int pickedNumber) {
        return snoozeValues[pickedNumber];
    }

    static public int getDefaultSnooze(boolean above) {
        if (above) {
            return 120;
        }
        return 30;
    }

    static void SetSnoozePickerValues(NumberPicker picker, boolean above, int default_snooze) {
        String[] values=new String[snoozeValues.length];
        for(int i=0;i<values.length;i++){
            values[i]=getNameFromTime(snoozeValues[i]);
        }

        picker.setMaxValue(values.length -1);
        picker.setMinValue(0);
        picker.setDisplayedValues(values);
        picker.setWrapSelectorWheel(false);
        if(default_snooze != 0) {
            picker.setValue(getSnoozeLocation(default_snooze));
        } else {
            picker.setValue(getSnoozeLocation(getDefaultSnooze(above)));
        }
    }


    private final static String TAG = AlertPlayer.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alertStatus = (TextView) findViewById(R.id.alert_status);
        snoozeValue = (NumberPicker) findViewById(R.id.snooze);

        prefs =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        addListenerOnButton();
        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            alertStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonSnooze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }

        displayStatus();
    }

    public void addListenerOnButton() {
        buttonSnooze = (Button)findViewById(R.id.button_snooze);

        //low alerts
        disableLowAlerts = (Button)findViewById(R.id.button_disable_low_alerts);
        clearLowDisabled = (Button)findViewById(R.id.enable_low_alerts);

        //high alerts
        disableHighAlerts = (Button)findViewById(R.id.button_disable_high_alerts);
        clearHighDisabled = (Button)findViewById(R.id.enable_high_alerts);

        //all alerts
        disableAlerts = (Button)findViewById(R.id.button_disable_alerts);
        clearDisabled = (Button)findViewById(R.id.enable_alerts);
        buttonSnooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int intValue = getTimeFromSnoozeValue(snoozeValue.getValue());
                AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);
                Intent intent = new Intent(getApplicationContext(), Home.class);
                if (ActiveBgAlert.getOnly() != null) {
                    startActivity(intent);
                }
                finish();
            }

        });
        showDisableEnableButtons();

        setOnClickListenerOnDisableButton(disableAlerts, "alerts_disabled_until");
        setOnClickListenerOnDisableButton(disableLowAlerts, "low_alerts_disabled_until");
        setOnClickListenerOnDisableButton(disableHighAlerts, "high_alerts_disabled_until");

        setOnClickListenerOnClearDisabledButton(clearDisabled, "alerts_disabled_until");
        setOnClickListenerOnClearDisabledButton(clearLowDisabled, "low_alerts_disabled_until");
        setOnClickListenerOnClearDisabledButton(clearHighDisabled, "high_alerts_disabled_until");
    }

    /**
     * Functionality used at least three times moved to a function. Adds an onClickListener that will re-enable the identified alert
     * @param button to which onclicklistener should be added
     * @param alert identifies the alert, the text string used in the preferences for example alerts_disabled_until
     */
    private void setOnClickListenerOnClearDisabledButton(Button button, String alert) {
        final String theAlert = alert;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit().putLong(theAlert, 0).apply();
                showDisableEnableButtons();
            }
        });
    }

    /**
     * Functionality used at least three times moved to a function. Adds an onClickListener that will disable the identified alert<br>
     * Depending on type of disable, also active alarms will be set to inactive<br>
     * - if alert = "alerts_disabled_until" then the active bg alert will be deleted if any<br>
     * - if alert = "low_alerts_disabled_until" and if active low bg alert exists then it will be deleted<br>
     * - if alert = "high_alerts_disabled_until" and if active high bg alert exists then it will be deleted<br>
     * @param button to which onclicklistener should be added
     * @param alert identifies the alert, the text string used in the preferences ie alerts_disabled_until, low_alerts_disabled_until or high_alerts_disabled_until
     */
    private void setOnClickListenerOnDisableButton(Button button, String alert) {
        final String disableType = alert;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Dialog d = new Dialog(SnoozeActivity.this);
                d.setTitle("Default Snooze");
                d.setContentView(R.layout.snooze_picker);
                Button b1 = (Button) d.findViewById(R.id.button1);
                Button b2 = (Button) d.findViewById(R.id.button2);
                final NumberPicker snoozeValue = (NumberPicker) d.findViewById(R.id.numberPicker1);

                SnoozeActivity.SetSnoozePickerValues(snoozeValue, false, 60);
                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Long disableUntil = new Date().getTime() + (SnoozeActivity.getTimeFromSnoozeValue(snoozeValue.getValue()) * 1000 * 60);
                        prefs.edit().putLong(disableType, disableUntil).apply();
                        //check if active bg alert exists and delete it depending on type of alert
                        ActiveBgAlert aba = ActiveBgAlert.getOnly();
                        if (aba != null) {
                            AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
                            if (disableType.equalsIgnoreCase("alerts_disabled_until")
                                    || (activeBgAlert.above && disableType.equalsIgnoreCase("high_alerts_disabled_until"))
                                    || (!activeBgAlert.above && disableType.equalsIgnoreCase("low_alerts_disabled_until"))
                                    ) {
                                //active bg alert exists which is a type that is being disabled so let's remove it completely from the database
                                ActiveBgAlert.ClearData();
                                //also make sure the text in the Activity is changed
                                displayStatus();
                            }
                        }

                        if (disableType.equalsIgnoreCase("alerts_disabled_until")) {
                            //disabling all , after the Snooze time set, all alarms will be re-enabled, inclusive low and high bg alarms
                            prefs.edit().putLong("high_alerts_disabled_until", 0).apply();
                            prefs.edit().putLong("low_alerts_disabled_until", 0).apply();
                        }

                        d.dismiss();
                        showDisableEnableButtons();
                    }
                });
                b2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                        showDisableEnableButtons();
                    }
                });
                d.show();

            }
        });

    }


    public void showDisableEnableButtons() {
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            disableAlerts.setVisibility(View.GONE);
            clearDisabled.setVisibility(View.VISIBLE);
            //all alerts are disabled so no need to show the buttons related to disabling/enabling the low and high alerts
            disableLowAlerts.setVisibility(View.GONE);
            clearLowDisabled.setVisibility(View.GONE);
            disableHighAlerts.setVisibility(View.GONE);
            clearHighDisabled.setVisibility(View.GONE);
        } else {
            clearDisabled.setVisibility(View.GONE);
            disableAlerts.setVisibility(View.VISIBLE);
            if (prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()) {
                disableLowAlerts.setVisibility(View.GONE);
                clearLowDisabled.setVisibility(View.VISIBLE);
            } else {
                disableLowAlerts.setVisibility(View.VISIBLE);
                clearLowDisabled.setVisibility(View.GONE);
            }
            if (prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()) {
                disableHighAlerts.setVisibility(View.GONE);
                clearHighDisabled.setVisibility(View.VISIBLE);
            } else {
                disableHighAlerts.setVisibility(View.VISIBLE);
                clearHighDisabled.setVisibility(View.GONE);
            }
        }
    }


    void displayStatus() {
        ActiveBgAlert aba = ActiveBgAlert.getOnly();
        AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();

        // aba and activeBgAlert should both either exist ot not exist. all other cases are a bug in another place
        if(aba == null && activeBgAlert!= null) {
            UserError.Log.wtf(TAG, "ERRRO displayStatus: aba == null, but activeBgAlert != null exiting...");
            return;
        }
        if(aba != null && activeBgAlert== null) {
            UserError.Log.wtf(TAG, "ERRRO displayStatus: aba != null, but activeBgAlert == null exiting...");
            return;
        }
        String status;
        if(activeBgAlert == null ) {
            status = "No active alert exists";
            alertStatus.setText(status);
            buttonSnooze.setVisibility(View.GONE);
            snoozeValue.setVisibility(View.GONE);
        } else {
            if(!aba.ready_to_alarm()) {
                status = "Active alert exists named \"" + activeBgAlert.name + "\" Alert snoozed until " +
                    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(aba.next_alert_at)) +
                    " (" + (aba.next_alert_at - new Date().getTime()) / 60000 + " minutes left)";
            } else {
                status = "Active alert exists named \"" + activeBgAlert.name + "\" (not snoozed)";
            }
            SetSnoozePickerValues(snoozeValue, activeBgAlert.above, activeBgAlert.default_snooze);
            alertStatus.setText(status);
        }

    }

}
