/*
 * Copyright (C) 2012 ParanoidAndroid Project
 * Copyright (C) 2016 The Altair ROM Project
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
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
//import android.support.v4.app.Fragment;
//import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import cyanogenmod.providers.CMSettings;

public class HaloSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {

    private static final String TAG = "HaloSettings";
    private static final int RESID = R.xml.halo_settings;

    private static final String KEY_HALO_SIZE = "halo_size";
    private static final String KEY_HALO_MSGBOX_ANIMATION = "halo_msgbox_animation";
    private static final String KEY_HALO_NOTIFY_COUNT = "halo_notify_count";

    private ListPreference mHaloSize;
    private ListPreference mHaloNotifyCount;
    private ListPreference mHaloMsgAnimate;

    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(RESID);

        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = getActivity();

        mHaloSize = (ListPreference) prefSet.findPreference(KEY_HALO_SIZE);
        try {
            float haloSize = Settings.Secure.getFloat(mContext.getContentResolver(),
                    Settings.Secure.HALO_SIZE, 1.0f);
            mHaloSize.setValue(String.valueOf(haloSize));
        } catch(Exception ex) {
            // So what
        }
        mHaloSize.setOnPreferenceChangeListener(this);

        mHaloNotifyCount = (ListPreference) prefSet.findPreference(KEY_HALO_NOTIFY_COUNT);
        try {
            int haloCounter = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_NOTIFY_COUNT, 4);
            mHaloNotifyCount.setValue(String.valueOf(haloCounter));
        } catch(Exception ex) {
            // fail...
        }
        mHaloNotifyCount.setOnPreferenceChangeListener(this);

        mHaloMsgAnimate = (ListPreference) prefSet.findPreference(KEY_HALO_MSGBOX_ANIMATION);
        try {
            int haloMsgAnimation = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_MSGBOX_ANIMATION, 2);
            mHaloMsgAnimate.setValue(String.valueOf(haloMsgAnimation));
        } catch(Exception ex) {
            // fail...
        }
        mHaloMsgAnimate.setOnPreferenceChangeListener(this);
    }

    @Override
    protected int getMetricsCategory() {
        // todo add a constant in MetricsLogger.java
        return MetricsLogger.CUSTOM_ROM_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == mHaloSize) {
                float haloSize = Float.valueOf((String) newValue);
                Settings.Secure.putFloat(getActivity().getContentResolver(),
                        Settings.Secure.HALO_SIZE, haloSize);
                return true;
            } else if (preference == mHaloMsgAnimate) {
                int haloMsgAnimation = Integer.valueOf((String) newValue);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HALO_MSGBOX_ANIMATION, haloMsgAnimation);
                return true;
            } else if (preference == mHaloNotifyCount) {
                int haloNotifyCount = Integer.valueOf((String) newValue);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.HALO_NOTIFY_COUNT, haloNotifyCount);
                return true;
            }
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = RESID;
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

