package com.dexdrip.stephenblack.nightwatch.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import com.dexdrip.stephenblack.nightwatch.NavigationDrawerFragment;
import com.dexdrip.stephenblack.nightwatch.R;
import com.dexdrip.stephenblack.nightwatch.utils.NavDrawerBuilder;
import java.util.List;


/**
 * Created by stephenblack on 9/8/15.
 */
public abstract class BaseActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private String menu_name = "Home Screen";



    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

    }

    @Override
    protected void onResume(){
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

    }

    public void setUp(DrawerLayout drawerLayout, Activity activity) {
        mNavigationDrawerFragment.setUp( R.id.drawer_layout, drawerLayout, "Home Screen", this);
    }

    protected abstract String getMenuName();
    protected abstract int getLayoutId();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if(mNavigationDrawerFragment != null) {
                if (mNavigationDrawerFragment.isDrawerOpen()) {
                    mNavigationDrawerFragment.closeDrawer();
                } else {
                    mNavigationDrawerFragment.openDrawer();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        NavDrawerBuilder navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        List<Intent> intent_list = navDrawerBuilder.nav_drawer_intents;
        if (!getMenuName().equals(menu_option_list.get(position))) {
            startActivity(intent_list.get(position));
            //do not close activity if it is the Launcher or "Home".
            if (!getMenuName().equalsIgnoreCase(Home.MENU_NAME)) {
                finish();
            }
        }
    }

}
