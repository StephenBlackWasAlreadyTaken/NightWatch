package com.dexdrip.stephenblack.nightwatch.activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.dexdrip.stephenblack.nightwatch.DataCollectionService;
import com.dexdrip.stephenblack.nightwatch.PebbleSync;
import com.dexdrip.stephenblack.nightwatch.R;


public class SettingsActivity extends AppCompatActivity  {
    public static final String MENU_NAME = "Settings";
    public static SharedPreferences prefs;
    private static AllPrefsFragment prefsFragment;




    @Override
    protected void onResume(){
        super.onResume();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.navDrawer);
        this.prefsFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                this.prefsFragment).commit();
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }

    public static class AllPrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_license);

            //General
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("highValue"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("lowValue"));

            //Notifications
            addPreferencesFromResource(R.xml.pref_notifications);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("maxBgYAxis"));
            bindPreferenceSummaryToValue(findPreference("bg_alert_profile"));
            bindPreferenceSummaryToValue(findPreference("falling_bg_val"));
            bindPreferenceSummaryToValue(findPreference("rising_bg_val"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("other_alerts_snooze"));

            addPreferencesFromResource(R.xml.pref_data_source);
            addPreferencesFromResource(R.xml.pref_watch_integration);
            final Preference pebbleSync = findPreference("broadcast_to_pebble");

            addPreferencesFromResource(R.xml.pref_other);


            bindPreferenceSummaryToValue(findPreference("dex_collection_method"));
            bindPreferenceSummaryToValue(findPreference("units"));
            bindPreferenceSummaryToValue(findPreference("dexcom_account_name"));

            final PreferenceCategory dataSource = (PreferenceCategory) findPreference("dataSource");
            final Preference share_poll = findPreference("share_poll");
            final Preference dexcom_account_name = findPreference("dexcom_account_name");
            final Preference dexcom_account_password = findPreference("dexcom_account_password");
            final Preference nightscout_poll = findPreference("nightscout_poll");
            final Preference dex_collection_method = findPreference("dex_collection_method");

            prefs = getPreferenceManager().getSharedPreferences();

            if (!prefs.getBoolean("nightscout_poll", false)) {
                dataSource.removePreference(dex_collection_method);
            }
            if (!prefs.getBoolean("share_poll", false)) {
                dataSource.removePreference(dexcom_account_name);
                dataSource.removePreference(dexcom_account_password);
            }

            Preference.OnPreferenceChangeListener collectionPrefValueListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, DataCollectionService.class));
                    return true;
                }
            };

            share_poll.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    if ((boolean) value) {
                        dataSource.addPreference(dexcom_account_name);
                        dataSource.addPreference(dexcom_account_password);
                    } else {
                        dataSource.removePreference(dexcom_account_name);
                        dataSource.removePreference(dexcom_account_password);
                    }
                    Context context = preference.getContext();
                    context.startService(new Intent(context, DataCollectionService.class));
                    return true;
                }
            });

            nightscout_poll.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    if ((boolean) value) {
                        dataSource.addPreference(dex_collection_method);
                    } else {
                        dataSource.removePreference(dex_collection_method);
                    }
                    Context context = preference.getContext();
                    context.startService(new Intent(context, DataCollectionService.class));
                    return true;
                }
            });

            dexcom_account_name.setOnPreferenceChangeListener(collectionPrefValueListener);
            dexcom_account_password.setOnPreferenceChangeListener(collectionPrefValueListener);
            dex_collection_method.setOnPreferenceChangeListener(collectionPrefValueListener);

            pebbleSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    if ((Boolean) newValue) {
                        context.startService(new Intent(context, PebbleSync.class));
                    } else {
                        context.stopService(new Intent(context, PebbleSync.class));
                    }
                    return true;
                }
            });
        }
    }

    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (isNumeric(stringValue)) {
                preference.setSummary(stringValue);
                return true;
            }
            return false;
        }
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary("Silent");
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        preference.setSummary(null);
                    } else {
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
