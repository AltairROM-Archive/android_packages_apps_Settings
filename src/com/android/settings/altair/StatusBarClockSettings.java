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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.CMSettings;

public class StatusBarClockSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {

    private static final String TAG = "StatusBarClock";

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_CLOCK_SHOW_SECONDS = "status_bar_clock_show_seconds";
    private static final String STATUS_BAR_CLOCK_FONT_STYLE = "status_bar_clock_font_style";
    private static final String STATUS_BAR_CLOCK_DATE_DISPLAY = "status_bar_clock_date_display";
    private static final String STATUS_BAR_CLOCK_DATE_STYLE = "status_bar_clock_date_style";
    private static final String STATUS_BAR_CLOCK_DATE_FORMAT = "status_bar_clock_date_format";
    private static final String STATUS_BAR_CLOCK_USE_CUSTOM_COLOR = "status_bar_clock_use_custom_color";
    private static final String STATUS_BAR_CLOCK_CUSTOM_COLOR = "status_bar_clock_custom_color";

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    public static final int STYLE_HIDE_CLOCK    = 0;
    public static final int STYLE_CLOCK_RIGHT   = 1;
    public static final int STYLE_CLOCK_CENTER  = 2;
    public static final int STYLE_CLOCK_LEFT    = 3;

    public static final int CLOCK_DATE_DISPLAY_GONE = 0;
    public static final int CLOCK_DATE_DISPLAY_SMALL = 1;
    public static final int CLOCK_DATE_DISPLAY_NORMAL = 2;

    public static final int CLOCK_DATE_STYLE_REGULAR = 0;
    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;

    public static final int FONT_BOLD = 0;
    public static final int FONT_CONDENSED = 1;
    public static final int FONT_LIGHT = 2;
    public static final int FONT_LIGHT_ITALIC = 3;
    public static final int FONT_NORMAL = 4;

    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 23;

    private static final int DLG_RESET = 0;

    private ListPreference mStatusBarClock;
    private ListPreference mStatusBarAmPm;
    private SwitchPreference mStatusBarClockShowSeconds;
    private ListPreference mFontStyle;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private SwitchPreference mStatusBarClockUseCustomColor;
    private ColorPickerPreference mStatusBarClockCustomColor;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        createCustomView();
    }

    private void createCustomView() {
        mCheckPreferences = false;
        addPreferencesFromResource(R.xml.status_bar_clock_settings);

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return;
        }

        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarClock = (ListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
        int clockStyle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_CLOCK, STYLE_CLOCK_RIGHT);
        mStatusBarClock.setValue(String.valueOf(clockStyle));
        mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);

        mStatusBarClockShowSeconds = (SwitchPreference) findPreference(STATUS_BAR_CLOCK_SHOW_SECONDS);
        mStatusBarClockShowSeconds.setChecked((CMSettings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_SHOW_SECONDS, 0) == 1));
        mStatusBarClockShowSeconds.setOnPreferenceChangeListener(this);

	    mFontStyle = (ListPreference) findPreference(STATUS_BAR_CLOCK_FONT_STYLE);
        mFontStyle.setOnPreferenceChangeListener(this);
        mFontStyle.setValue(Integer.toString(CMSettings.System.getInt(
                getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_FONT_STYLE, FONT_NORMAL)));
        mFontStyle.setSummary(mFontStyle.getEntry());

        mClockDateDisplay = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_DISPLAY);
        mClockDateDisplay.setOnPreferenceChangeListener(this);
        mClockDateDisplay.setValue(Integer.toString(CMSettings.System.getInt(
                getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, CLOCK_DATE_DISPLAY_GONE)));
        mClockDateDisplay.setSummary(mClockDateDisplay.getEntry());

        mClockDateStyle = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_STYLE);
        mClockDateStyle.setOnPreferenceChangeListener(this);
        mClockDateStyle.setValue(Integer.toString(CMSettings.System.getInt(
                getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_DATE_STYLE, CLOCK_DATE_STYLE_REGULAR)));
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());

        mClockDateFormat = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

        mStatusBarClockUseCustomColor = (SwitchPreference) findPreference(STATUS_BAR_CLOCK_USE_CUSTOM_COLOR);
        mStatusBarClockUseCustomColor.setChecked((CMSettings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_USE_CUSTOM_COLOR, 0) == 1));
        mStatusBarClockUseCustomColor.setOnPreferenceChangeListener(this);

        mStatusBarClockCustomColor = (ColorPickerPreference) findPreference(STATUS_BAR_CLOCK_CUSTOM_COLOR);
        mStatusBarClockCustomColor.setOnPreferenceChangeListener(this);
        int intColor = CMSettings.System.getInt(getActivity().getContentResolver(),
                    CMSettings.System.STATUS_BAR_CLOCK_CUSTOM_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/status_bar_clock_color", null, null));
            mStatusBarClockCustomColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarClockCustomColor.setSummary(hexColor);
        }
        mStatusBarClockCustomColor.setNewPreviewColor(intColor);

        setPreferenceStatus();
        mCheckPreferences = true;
    }

    @Override
    protected int getMetricsCategory() {
        // todo add a constant in MetricsLogger.java
        return MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Adjust clock position for RTL if necessary
        Configuration config = getResources().getConfiguration();
        if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mStatusBarClock.setEntries(getActivity().getResources().getStringArray(
                        R.array.status_bar_clock_style_entries_rtl));
                mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        ContentResolver resolver = getActivity().getContentResolver();
        AlertDialog dialog;

        if (preference == mStatusBarClock) {
            int clockStyle = Integer.parseInt((String) newValue);
            int index = mStatusBarClock.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(resolver,
                    STATUS_BAR_CLOCK_STYLE, clockStyle);
            mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
            setPreferenceStatus();
            return true;
        } else if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) newValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(resolver,
                    STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarClockShowSeconds) {
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_SHOW_SECONDS,
                    (Boolean) newValue ? 1 : 0);
            return true;
	    } else if (preference == mFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mFontStyle.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_FONT_STYLE, val);
            mFontStyle.setSummary(mFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mClockDateDisplay) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateDisplay.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, val);
            mClockDateDisplay.setSummary(mClockDateDisplay.getEntries()[index]);
            setPreferenceStatus();
            return true;
        } else if (preference == mClockDateStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_DATE_STYLE, val);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.status_bar_clock_date_string_edittext_title);
                alert.setMessage(R.string.status_bar_clock_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = CMSettings.System.getString(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        CMSettings.System.putString(getActivity().getContentResolver(),
                            CMSettings.System.STATUS_BAR_CLOCK_DATE_FORMAT, value);
                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    CMSettings.System.putString(resolver,
                        CMSettings.System.STATUS_BAR_CLOCK_DATE_FORMAT,
                        (String) newValue);
                }
            }
            return true;
        } else if (preference == mStatusBarClockUseCustomColor) {
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_USE_CUSTOM_COLOR,
                    (Boolean) newValue ? 1 : 0);
            setPreferenceStatus();
            return true;
        } else if (preference == mStatusBarClockCustomColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            CMSettings.System.putInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK_CUSTOM_COLOR, intHex);
            return true;
        }
        return false;
    }

    private void setPreferenceStatus() {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean mEnablePreference = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_CLOCK, STYLE_CLOCK_RIGHT) != 0;

        mStatusBarClockShowSeconds.setEnabled(mEnablePreference);
        mFontStyle.setEnabled(mEnablePreference);
        mClockDateDisplay.setEnabled(mEnablePreference);
        mStatusBarClockUseCustomColor.setEnabled(mEnablePreference);

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        } else if (mEnablePreference) {
            int statusBarAmPm = CMSettings.System.getInt(resolver,
                    CMSettings.System.STATUS_BAR_AM_PM, AM_PM_STYLE_GONE);
            mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
            mStatusBarAmPm.setOnPreferenceChangeListener(this);
            mStatusBarAmPm.setEnabled(true);
        } else {
            mStatusBarAmPm.setEnabled(false);
        }

        boolean mClockDateToggle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_CLOCK_DATE_DISPLAY,
                CLOCK_DATE_DISPLAY_GONE) != CLOCK_DATE_DISPLAY_GONE;
        mClockDateStyle.setEnabled(mClockDateToggle && mEnablePreference);
        mClockDateFormat.setEnabled(mClockDateToggle && mEnablePreference);
        
        boolean mClockDateCustomColor = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_CLOCK_USE_CUSTOM_COLOR, 0) != 0;
        mStatusBarClockCustomColor.setEnabled(mClockDateCustomColor && mEnablePreference);
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.status_bar_clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = CMSettings.System.getInt(
                getActivity().getContentResolver(),
                CMSettings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }
                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        StatusBarClockSettings getOwner() {
            return (StatusBarClockSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.status_bar_clock_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            CMSettings.System.putInt(getActivity().getContentResolver(),
                                CMSettings.System.STATUS_BAR_CLOCK_CUSTOM_COLOR, -2);
                            getOwner().createCustomView();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

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
                    sir.xmlResId = R.xml.status_bar_clock_settings;
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

