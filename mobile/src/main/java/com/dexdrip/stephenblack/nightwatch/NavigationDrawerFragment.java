package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dexdrip.stephenblack.nightwatch.AlertsCode.UserError;
import com.dexdrip.stephenblack.nightwatch.Utils.NavDrawerBuilder;

import java.util.List;

public class NavigationDrawerFragment extends android.app.Fragment {
    private NavigationDrawerCallbacks mCallbacks;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mCurrentSelectedPosition = 0;
    private ListView mDrawerListView;
    private DrawerLayout mDrawerLayout;
    public NavDrawerBuilder navDrawerBuilder;
    private int menu_position;
    private List<String> menu_option_list;
    private List<Intent> intent_list;

    public NavigationDrawerFragment() { }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

//        navDrawerBuilder = new NavDrawerBuilder(getActivity());
//        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
//        String[] menu_options = menu_option_list.toArray(new String[menu_option_list.size()]);
//        intent_list = navDrawerBuilder.nav_drawer_intents;
////
//        mDrawerListView.setAdapter(new ArrayAdapter<String>(
//                getActionBar().getThemedContext(),
//                android.R.layout.simple_list_item_activated_1,
//                android.R.id.text1,
//                menu_options
//        ));
        return mDrawerListView;
    }


    public void setUp(DrawerLayout drawerLayout, Context context) {
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        android.support.v7.app.ActionBar actionBar = getActionBar();
        if (actionBar != null)
            UserError.Log.e("OPEN", "GOT BAR");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        navDrawerBuilder = new NavDrawerBuilder(getActivity());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        String[] menu_options = menu_option_list.toArray(new String[menu_option_list.size()]);
        intent_list = navDrawerBuilder.nav_drawer_intents;
        if (actionBar != null)
            UserError.Log.e("OPEN", "Setting adapter");

        mDrawerListView.setAdapter(new ArrayAdapter<String>(
                getActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                menu_options
        ));

        if( mDrawerListView.getAdapter() != null)
            UserError.Log.e("OPEN", "addapter isnt null");
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                R.drawable.ic_launcher,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                UserError.Log.e("OPEN", "CLOSE");
                super.onDrawerClosed(drawerView);
                if (!isAdded()) { return; }
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                UserError.Log.e("OPEN", "OPEN");
                super.onDrawerOpened(drawerView);
                if (!isAdded()) { return; }
                getActivity().invalidateOptionsMenu();
            }
        };

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            closeDrawer();
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private android.support.v7.app.ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListView);
    }
    public void openDrawer() {
        if (mDrawerLayout != null) mDrawerLayout.openDrawer(mDrawerListView);
    }
    public void closeDrawer() {
        if (mDrawerLayout != null) mDrawerLayout.closeDrawer(mDrawerListView);
    }
    public void swapContext(int position) {
        if (position != menu_position) {
            Intent[] intent_array = intent_list.toArray(new Intent[intent_list.size()]);
            startActivity(intent_array[position]);
            if(menu_position != 0) {
                getActivity().finish();
            }
        }
    }
    public static interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }
}
