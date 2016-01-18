/*
 * Copyright (C) 2011 The CyanogenMod Project
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

import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.CmSystem;
import android.provider.Settings;

public class LockscreenSettings extends PreferenceActivity
        implements OnPreferenceChangeListener {

    private static final String PREF_DISABLE_LOCKSCREEN = "pref_disable_lockscreen";
    private static final String PREF_LOCKSCREEN_STYLECFG = "pref_lockscreen_stylecfg";
    private static final String PREF_LOCKSCREEN_WIDGETS = "pref_lockscreen_widgets";
    private static final String PREF_LOCKSCREEN_UNLOCK = "pref_lockscreen_unlock";
    private static final String PREF_LOCKSCREEN_GESTURES = "pref_lockscreen_gestures";
    private static final String PREF_LOCKSCREEN_TIMEOUT = "pref_lockscreen_timeout";

    private CheckBoxPreference mDisableLockscreen;
    private PreferenceScreen mLockscreenStylecfg;
    private PreferenceScreen mLockscreenWidgets;
    private PreferenceScreen mLockscreenUnlock;
    private PreferenceScreen mLockscreenGestures;
    private PreferenceScreen mLockscreenTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        mDisableLockscreen = (CheckBoxPreference) prefSet.findPreference(PREF_DISABLE_LOCKSCREEN);
        mLockscreenStylecfg = (PreferenceScreen) prefSet.findPreference(PREF_LOCKSCREEN_STYLECFG);
        mLockscreenWidgets = (PreferenceScreen) prefSet.findPreference(PREF_LOCKSCREEN_WIDGETS);
        mLockscreenUnlock = (PreferenceScreen) prefSet.findPreference(PREF_LOCKSCREEN_UNLOCK);
        mLockscreenGestures = (PreferenceScreen) prefSet.findPreference(PREF_LOCKSCREEN_GESTURES);
        mLockscreenTimeout = (PreferenceScreen) prefSet.findPreference(PREF_LOCKSCREEN_TIMEOUT);

        int defValue=CmSystem.getDefaultBool(getBaseContext(), CmSystem.CM_DEFAULT_DISABLE_LOCKSCREEN)==true ? 1 : 0;
        mDisableLockscreen.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_DISABLED, defValue) == 1));

        adjustPreferences();
    }

    private void adjustPreferences() {
        boolean value = mDisableLockscreen.isChecked();
        
        mLockscreenStylecfg.setEnabled(!value);
        mLockscreenWidgets.setEnabled(!value);
        mLockscreenUnlock.setEnabled(!value);
        mLockscreenGestures.setEnabled(!value);
        mLockscreenTimeout.setEnabled(!value);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mDisableLockscreen) {
            value = mDisableLockscreen.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_DISABLED,
                    value ? 1 : 0);
            adjustPreferences();
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
