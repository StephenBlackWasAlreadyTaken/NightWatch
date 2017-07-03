package com.dexdrip.stephenblack.nightwatch.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dexdrip.stephenblack.nightwatch.activities.About;
import com.dexdrip.stephenblack.nightwatch.activities.Home;
import com.dexdrip.stephenblack.nightwatch.activities.SettingsActivity;
import com.dexdrip.stephenblack.nightwatch.alerts.AlertList;
import com.dexdrip.stephenblack.nightwatch.alerts.SnoozeActivity;
import com.dexdrip.stephenblack.nightwatch.stats.StatsActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/5/14.
 */
public class NavDrawerBuilder {
    public double time_now = new Date().getTime();
    public List<Intent> nav_drawer_intents = new ArrayList<>();
    public List<String> nav_drawer_options = new ArrayList<>();
    public Context context;

    public NavDrawerBuilder(Context aContext) {
        context = aContext;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);

        if (IUnderstand == false) {
            this.nav_drawer_options.add(SettingsActivity.MENU_NAME);
            this.nav_drawer_intents.add(new Intent(context, SettingsActivity.class));
            return;
        }

        this.nav_drawer_options.add(Home.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, Home.class));

        this.nav_drawer_options.add(StatsActivity.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, StatsActivity.class));

        this.nav_drawer_options.add(AlertList.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, AlertList.class));

        this.nav_drawer_options.add(SnoozeActivity.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, SnoozeActivity.class));


        this.nav_drawer_options.add(SettingsActivity.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, SettingsActivity.class));

        this.nav_drawer_options.add(About.MENU_NAME);
        this.nav_drawer_intents.add(new Intent(context, About.class));
    }
}
