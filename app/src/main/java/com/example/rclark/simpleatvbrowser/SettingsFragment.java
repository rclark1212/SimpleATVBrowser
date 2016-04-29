package com.example.rclark.simpleatvbrowser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v17.preference.LeanbackPreferenceFragment;

/**
 * Created by rclark on 4/28/16.
 */
public class SettingsFragment extends LeanbackPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        SettingsActivity.mUpdateSomething = false;
        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.preferences);
        return v;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootkey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        //register listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregister
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    //TODO - complete... See http://developer.android.com/intl/es/guide/topics/ui/settings.html

    /**
     * Okay, this routine called when shared preferences change. Check to see what changed to see if we need to rebuild UI
     * (for example, a row gets hidden/unhidden)
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(getResources().getString(R.string.key_enable_file_access))
                || key.equals(getResources().getString(R.string.key_ua_string))
                || key.equals(getResources().getString(R.string.key_pan_speed))
                || key.equals(getResources().getString(R.string.key_zoom_speed))) {
            SettingsActivity.mUpdateSomething = true;
        }

    }
}
