/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.content.Context;
import android.os.Bundle;
import android.app.settings.SettingsEnums;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.custom.ThemeUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/**
 * Settings screen for Dark UI Mode
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DarkModeSettingsFragment extends DashboardFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "DarkModeSettingsFragment";
    private DarkModeObserver mContentObserver;
    private Runnable mCallback = () -> {
        updatePreferenceStates();
    };

    private ThemeUtils mThemeUtils;
    private ListPreference mDarkModeOverlayPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        mContentObserver = new DarkModeObserver(context);
        mThemeUtils = new ThemeUtils(context);

        final PreferenceScreen screen = getPreferenceScreen();
        mDarkModeOverlayPreference = screen.findPreference(ThemeUtils.BACKGROUND_KEY);
        mDarkModeOverlayPreference.setOnPreferenceChangeListener(this);
        updateState(mDarkModeOverlayPreference);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Listen for changes only while visible.
        mContentObserver.subscribe(mCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop listening for state changes.
        mContentObserver.unsubscribe();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDarkModeOverlayPreference) {
            mThemeUtils.setOverlayEnabled(ThemeUtils.BACKGROUND_KEY, (String) newValue);
            return true;
        }
        return false;
    }

    public void updateState(ListPreference preference) {
        String currentPackageName = mThemeUtils.getOverlayInfos(preference.getKey()).stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse("Default");

        List<String> pkgs = mThemeUtils.getOverlayPackagesForCategory(preference.getKey());
        List<String> labels = mThemeUtils.getLabels(preference.getKey());

        preference.setEntries(labels.toArray(new String[labels.size()]));
        preference.setEntryValues(pkgs.toArray(new String[pkgs.size()]));
        preference.setValue("Default".equals(currentPackageName) ? pkgs.get(0) : currentPackageName);
        preference.setSummary("Default".equals(currentPackageName) ? "Default" : labels.get(pkgs.indexOf(currentPackageName)));
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dark_mode_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_dark_theme;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DARK_UI_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider();
}
