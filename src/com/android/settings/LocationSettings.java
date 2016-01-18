/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;


import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.content.ComponentName;

import com.authentec.AuthentecHelper;

public class LocationSettings extends PreferenceActivity implements OnPreferenceChangeListener {

    public static final String GPS_STATUS_CHANGED="com.android.settings.GPS_STATUS_CHANGED";

    // Location Settings
    private static final String LOCATION_NETWORK = "location_network";
    private static final String LOCATION_GPS = "location_gps";
    private static final String ASSISTED_GPS = "assisted_gps";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;
    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createPreferenceHierarchy();

        updateToggles();

        //add BT gps devices
        ListPreference btpref = (ListPreference) findPreference("location_gps_source");
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        for (String e : getResources().getStringArray(R.array.location_entries_gps_source) ) {
            entries.add(e);
        }
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();
        for (String v: getResources().getStringArray(R.array.location_values_gps_source)) {
            values.add(v);
        }
        // add known bonded BT devices
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ((mBluetoothAdapter != null) && (mBluetoothAdapter.isEnabled())) {
            for (BluetoothDevice d : mBluetoothAdapter.getBondedDevices()) {
                String dname = d.getName() + " - " + d.getAddress();
                entries.add(dname);
                values.add(d.getAddress());
            }
        }
        btpref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        btpref.setEntryValues(values.toArray(new CharSequence[values.size()]));
        btpref.setDefaultValue("0");
        btpref.setOnPreferenceChangeListener(this);

        
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mContentQueryMap.addObserver(new SettingsObserver());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String oldPref = Settings.System.getString(getContentResolver(),
                Settings.Secure.EXTERNAL_GPS_BT_DEVICE);
        String newPref = newValue == null ? "0" : (String) newValue;
        // "0" represents the internal GPS.
        Settings.System.putString(getContentResolver(), Settings.Secure.EXTERNAL_GPS_BT_DEVICE,
                newPref);
        if (!oldPref.equals(newPref) && ("0".equals(oldPref) || "0".equals(newPref)) ) {
            LocationManager locationManager = 
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.setGPSSource(newPref);

            // Show dialog to inform user that source has been switched
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(R.string.location_gps_source_notification_title);
            alertDialog.setMessage(getResources().getString(R.string.location_gps_source_notification));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getResources().getString(com.android.internal.R.string.ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            alertDialog.show();
        }
        return true;
    }

    
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = this.getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = this.getPreferenceScreen();

        mNetwork = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_NETWORK);
        mGps = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) getPreferenceScreen().findPreference(ASSISTED_GPS);

        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (preference == mNetwork) {
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.GPS_PROVIDER, enabled);

            //{PIAF - Send update of GPS status
            Intent gpsStatus = new Intent(GPS_STATUS_CHANGED);
            this.sendBroadcast(gpsStatus);
            //PIAF}
            if (mAssistedGps != null) {
                mAssistedGps.setEnabled(enabled);
            }
        } else if (preference == mAssistedGps) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSISTED_GPS_ENABLED,
                    mAssistedGps.isChecked() ? 1 : 0);
        }

        return false;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mNetwork.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(gpsEnabled);
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Secure.getInt(res,
                    Settings.Secure.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }
}
