/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.bluetooth.DockEventReceiver;

public class InterfaceSettings extends PreferenceActivity
        implements OnPreferenceChangeListener {

    private static final String TAG = "DisplaySettings";

    private static final int DIALOG_NOT_DOCKED = 1;

    private static final String KEY_PARENT = "interface_settings";

    private static final String DOCK_SETTINGS_CATEGORY = "dock_settings";
    private static final String KEY_AUDIO_SETTINGS = "dock_audio";
    private static final String KEY_DOCK_SOUNDS = "dock_sounds";

    private static final String POWER_PROMPT_PREF = "power_dialog_prompt";
    private static final String PINCH_REFLOW_PREF = "pref_pinch_reflow";
    private static final String SHARE_SCREENSHOT_PREF = "pref_share_screenshot";

    private static final String OVERSCROLL_PREF = "pref_overscroll_effect";
    private static final String OVERSCROLL_WEIGHT_PREF = "pref_overscroll_weight";

    private static final String TABLET_SETTINGS = "tablet_settings";

    private Preference mDockSettings;
    private Preference mAudioSettings;
    private CheckBoxPreference mDockSounds;

    private CheckBoxPreference mPowerPromptPref;
    private CheckBoxPreference mPinchReflowPref;
    private CheckBoxPreference mShareScreenshotPref;
    private ListPreference mOverscrollPref;
    private ListPreference mOverscrollWeightPref;

    private PreferenceScreen mTabletSettingsScreen;

    private Intent mDockIntent;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
                handleDockChange(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        setTitle(R.string.settings_title_interface);
        addPreferencesFromResource(R.xml.interface_settings);

        initDockSettings();

        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_PARENT);

        if (getResources().getBoolean(R.bool.has_dock_settings) == false) {
            PreferenceCategory dockSettings = (PreferenceCategory) prefSet.findPreference(DOCK_SETTINGS_CATEGORY);
            if (dockSettings != null) {
                prefSet.removePreference(dockSettings);
            }
        }

        mTabletSettingsScreen = (PreferenceScreen) prefSet.findPreference(TABLET_SETTINGS);

        /* Power prompt */
        mPowerPromptPref = (CheckBoxPreference) prefSet.findPreference(POWER_PROMPT_PREF);

        /* Pinch reflow */
        mPinchReflowPref = (CheckBoxPreference) prefSet.findPreference(PINCH_REFLOW_PREF);
        mPinchReflowPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.WEB_VIEW_PINCH_REFLOW, 0) == 1);

        /* Share Screenshot */
        mShareScreenshotPref = (CheckBoxPreference) prefSet.findPreference(SHARE_SCREENSHOT_PREF);
        mShareScreenshotPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHARE_SCREENSHOT, 0) == 1);

        /* Overscroll Effect */
        mOverscrollPref = (ListPreference) prefSet.findPreference(OVERSCROLL_PREF);
        int overscrollEffect = Settings.System.getInt(getContentResolver(),
                Settings.System.OVERSCROLL_EFFECT, 1);
        mOverscrollPref.setValue(String.valueOf(overscrollEffect));
        mOverscrollPref.setOnPreferenceChangeListener(this);

        mOverscrollWeightPref = (ListPreference) prefSet.findPreference(OVERSCROLL_WEIGHT_PREF);
        int overscrollWeight = Settings.System.getInt(getContentResolver(),
                Settings.System.OVERSCROLL_WEIGHT, 5);
        mOverscrollWeightPref.setValue(String.valueOf(overscrollWeight));
        mOverscrollWeightPref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    private void initDockSettings() {
        ContentResolver resolver = getContentResolver();

        mAudioSettings = findPreference(KEY_AUDIO_SETTINGS);
        if (mAudioSettings != null) {
            mAudioSettings.setSummary(R.string.dock_audio_summary_none);
        }

        mDockSounds = (CheckBoxPreference) findPreference(KEY_DOCK_SOUNDS);
        mDockSounds.setPersistent(false);
        mDockSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.DOCK_SOUNDS_ENABLED, 0) != 0);
    }

    private void handleDockChange(Intent intent) {
        if (mAudioSettings != null) {
            int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);

            boolean isBluetooth = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

            if (!isBluetooth) {
                // No dock audio if not on Bluetooth.
                mAudioSettings.setEnabled(false);
                mAudioSettings.setSummary(R.string.dock_audio_summary_unknown);
            } else {
                mAudioSettings.setEnabled(true);

                mDockIntent = intent;
                int resId = R.string.dock_audio_summary_unknown;
                switch (dockState) {
                case Intent.EXTRA_DOCK_STATE_CAR:
                    resId = R.string.dock_audio_summary_car;
                    break;
                case Intent.EXTRA_DOCK_STATE_DESK:
                    resId = R.string.dock_audio_summary_desk;
                    break;
                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                    resId = R.string.dock_audio_summary_none;
                }
                mAudioSettings.setSummary(resId);
            }

            if (dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                // remove undocked dialog if currently showing.
                try {
                    dismissDialog(DIALOG_NOT_DOCKED);
                } catch (IllegalArgumentException iae) {
                    // Maybe it was already dismissed
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mAudioSettings) {
            int dockState = mDockIntent != null
                    ? mDockIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0)
                    : Intent.EXTRA_DOCK_STATE_UNDOCKED;
            if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                showDialog(DIALOG_NOT_DOCKED);
            } else {
                Intent i = new Intent(mDockIntent);
                i.setAction(DockEventReceiver.ACTION_DOCK_SHOW_UI);
                i.setClass(this, DockEventReceiver.class);
                sendBroadcast(i);
            }
            return true;
        } else if (preference == mDockSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.DOCK_SOUNDS_ENABLED,
                    mDockSounds.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mPinchReflowPref) {
            value = mPinchReflowPref.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.WEB_VIEW_PINCH_REFLOW,
                    value ? 1 : 0);
            return true;
        } else if (preference == mShareScreenshotPref) {
            value = mShareScreenshotPref.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.SHARE_SCREENSHOT,
                    value ? 1 : 0);
            return true;
        } else if (preference == mPowerPromptPref) {
            value = mPowerPromptPref.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_DIALOG_PROMPT,
                    value ? 1 : 0);
            return true;
        /*} else if (preference == mTabletSettingsScreen) {
            startActivity(mTabletSettingsScreen.getIntent());
            return true;*/
        }

        return false;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOT_DOCKED) {
            return createUndockedMessage();
        }
        return null;
    }

    private Dialog createUndockedMessage() {
        final AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(R.string.dock_not_found_title);
        ab.setMessage(R.string.dock_not_found_text);
        ab.setPositiveButton(android.R.string.ok, null);
        return ab.create();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mOverscrollPref) {
            int overscrollEffect = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.OVERSCROLL_EFFECT,
                    overscrollEffect);
            return true;
        } else if (preference == mOverscrollWeightPref) {
            int overscrollWeight = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.OVERSCROLL_WEIGHT,
                    overscrollWeight);
            return true;
        }
        return false;
    }

    ColorPickerDialog.OnColorChangedListener mWidgetColorListener = new ColorPickerDialog.OnColorChangedListener() {
        public void colorChanged(int color) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.EXPANDED_VIEW_WIDGET_COLOR, color);
        }

        public void colorUpdate(int color) {
        }
    };
}
