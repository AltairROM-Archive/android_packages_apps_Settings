/*
 * Copyright (C) 2015-2016 The Altair ROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.altair;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import cyanogenmod.providers.CMSettings;

public class StatusBarBatterySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {

    private static final String TAG = "StatusBarBattery";

    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_USE_CUSTOM_COLORS = "status_bar_battery_use_custom_colors";
    private static final String STATUS_BAR_BATTERY_CUSTOM_COLOR_CHARGE = "status_bar_battery_custom_color_charge";
    private static final String STATUS_BAR_BATTERY_CUSTOM_COLOR_BOLT = "status_bar_battery_custom_color_bolt";
    private static final String STATUS_BAR_BATTERY_CUSTOM_COLOR_TEXT = "status_bar_battery_custom_color_text";

    private static final int STATUS_BAR_BATTERY_STYLE_HIDDEN = 4;
    private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 6;

    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;
    private SwitchPreference mStatusBarBatteryUseCustomColors;
    private ColorPickerPreference mStatusBarBatteryCustomColorCharge;
    private ColorPickerPreference mStatusBarBatteryCustomColorBolt;
    private ColorPickerPreference mStatusBarBatteryCustomColorText;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.status_bar_battery_settings);

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return;
        }

        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryShowPercent =
                (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);

        int batteryStyle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int batteryShowPercent = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(batteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        enableStatusBarBatteryDependents(batteryStyle);
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);

        mStatusBarBatteryUseCustomColors = (SwitchPreference) findPreference(STATUS_BAR_BATTERY_USE_CUSTOM_COLORS);
        mStatusBarBatteryUseCustomColors.setChecked((CMSettings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                CMSettings.System.STATUS_BAR_BATTERY_USE_CUSTOM_COLORS, 0) == 1));
        mStatusBarBatteryUseCustomColors.setOnPreferenceChangeListener(this);

        mStatusBarBatteryCustomColorCharge = (ColorPickerPreference) findPreference(STATUS_BAR_BATTERY_CUSTOM_COLOR_CHARGE);
        mStatusBarBatteryCustomColorCharge.setOnPreferenceChangeListener(this);
        int intColor = CMSettings.System.getInt(getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_CHARGE, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_charge_color", null, null));
            mStatusBarBatteryCustomColorCharge.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarBatteryCustomColorCharge.setSummary(hexColor);
        }
        mStatusBarBatteryCustomColorCharge.setNewPreviewColor(intColor);

        mStatusBarBatteryCustomColorBolt = (ColorPickerPreference) findPreference(STATUS_BAR_BATTERY_CUSTOM_COLOR_BOLT);
        mStatusBarBatteryCustomColorBolt.setOnPreferenceChangeListener(this);
        intColor = CMSettings.System.getInt(getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_BOLT, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_bolt_color", null, null));
            mStatusBarBatteryCustomColorBolt.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarBatteryCustomColorBolt.setSummary(hexColor);
        }
        mStatusBarBatteryCustomColorBolt.setNewPreviewColor(intColor);

        mStatusBarBatteryCustomColorText = (ColorPickerPreference) findPreference(STATUS_BAR_BATTERY_CUSTOM_COLOR_TEXT);
        mStatusBarBatteryCustomColorText.setOnPreferenceChangeListener(this);
        intColor = CMSettings.System.getInt(getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_TEXT, -2);
        if (intColor == -2) {
            intColor = 0xffffffff;
            mStatusBarBatteryCustomColorText.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarBatteryCustomColorText.setSummary(hexColor);
        }
        mStatusBarBatteryCustomColorText.setNewPreviewColor(intColor);
    }

    @Override
    protected int getMetricsCategory() {
        // todo add a constant in MetricsLogger.java
        return MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_BATTERY_STYLE, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents(batteryStyle);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            int batteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, batteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarBatteryUseCustomColors) {
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_BATTERY_USE_CUSTOM_COLORS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBatteryCustomColorCharge) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_CHARGE, intHex);
            return true;
        } else if (preference == mStatusBarBatteryCustomColorBolt) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_BOLT, intHex);
            return true;
        } else if (preference == mStatusBarBatteryCustomColorText) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_BATTERY_CUSTOM_COLOR_TEXT, intHex);
            return true;
        }
        return false;
    }

    private void enableStatusBarBatteryDependents(int batteryIconStyle) {
        if (batteryIconStyle == STATUS_BAR_BATTERY_STYLE_HIDDEN ||
                batteryIconStyle == STATUS_BAR_BATTERY_STYLE_TEXT) {
            mStatusBarBatteryShowPercent.setEnabled(false);
        } else {
            mStatusBarBatteryShowPercent.setEnabled(true);
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.status_bar_battery_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}

