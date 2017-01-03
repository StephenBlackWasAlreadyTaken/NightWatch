package com.dexdrip.stephenblack.nightwatch.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.dexdrip.stephenblack.nightwatch.BuildConfig;
import com.dexdrip.stephenblack.nightwatch.R;


public class About extends AppCompatActivity  {
    public static final String MENU_NAME="About";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Update the Version Number and Name
        final TextView about = (TextView) findViewById(R.id.About);
        about.setText("About NightWatch");
        final TextView versionName = (TextView) findViewById(R.id.versionName);
        versionName.setText( "Version Number: " + BuildConfig.VERSION_NAME);
        final TextView versionNumber = (TextView) findViewById(R.id.versionNumber);
        versionNumber.setText( "Version Code: " + Integer.toString(BuildConfig.VERSION_CODE));
    }
}
