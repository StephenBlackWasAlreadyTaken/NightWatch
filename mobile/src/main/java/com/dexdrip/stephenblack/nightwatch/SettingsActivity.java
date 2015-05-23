package com.dexdrip.stephenblack.nightwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    public static SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new AllPrefsFragment()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static class AllPrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            addPreferencesFromResource(R.xml.pref_bg_notification);
            addPreferencesFromResource(R.xml.pref_data_source);
            addPreferencesFromResource(R.xml.pref_watch_integration);

            bindPreferenceSummaryToValue(findPreference("highValue"));
            bindPreferenceSummaryToValue(findPreference("lowValue"));
            bindPreferenceSummaryToValue(findPreference("dex_collection_method"));
            bindPreferenceSummaryToValue(findPreference("units"));
            bindPreferenceSummaryToValue(findPreference("dexcom_account_name"));

            final PreferenceCategory dataSource = (PreferenceCategory) findPreference("dataSource");
            final Preference share_poll = findPreference("share_poll");
            final Preference dexcom_account_name = findPreference("dexcom_account_name");
            final Preference dexcom_account_password = findPreference("dexcom_account_password");
            final Preference nightscout_poll = findPreference("nightscout_poll");
            final Preference dex_collection_method = findPreference("dex_collection_method");

            prefs = getPreferenceManager().getDefaultSharedPreferences(getActivity());
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
                    if (value == true) {
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
                    if (value == true) {
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
        }
    }

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

}
