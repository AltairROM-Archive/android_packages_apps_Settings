/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2021-2022 Altair ROM Project
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

package com.android.settings.utils;

import static android.os.UserHandle.USER_SYSTEM;

import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.PathParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ThemeUtils {

    public static final String TAG = "ThemeUtils";

    public static final String FONT_KEY = "android.theme.customization.font";
    public static final String ICON_SHAPE_KEY= "android.theme.customization.adaptive_icon_shape";
    public static final String SIGNAL_ICON_KEY = "android.theme.customization.signal_icon";
    public static final String WIFI_ICON_KEY = "android.theme.customization.wifi_icon";
    public static final String NAVBAR_KEY = "android.theme.customization.navbar";
    public static final String DARK_THEME_KEY = "android.theme.customization.system_palette";

    public static final Comparator<OverlayInfo> OVERLAY_INFO_COMPARATOR =
            Comparator.comparingInt(a -> a.priority);

    private Context mContext;
    private UiModeManager mUiModeManager;
    private IOverlayManager mOverlayManager;
    private PackageManager pm;
    private Resources overlayRes;

    public ThemeUtils(Context context) {
        mContext = context;
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mOverlayManager = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        pm = context.getPackageManager();
    }

    public void setOverlayEnabled(String category, String packageName) {
		setOverlayEnabled(category, packageName, "android");
	}

    public void setOverlayEnabled(String category, String packageName, String target) {
        final String currentPackageName = getOverlayInfos(category, target).stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse(null);

        try {
            if (target.equals(packageName)) {
                mOverlayManager.setEnabled(currentPackageName, false, USER_SYSTEM);
            } else {
                mOverlayManager.setEnabledExclusiveInCategory(packageName,
                        USER_SYSTEM);
            }

            writeSettings(category, packageName, target.equals(packageName));

        } catch (RemoteException e) {
            // Do nothing
        }
    }

    public void writeSettings(String category, String packageName, boolean disable) {
        final String overlayPackageJson = Settings.Secure.getStringForUser(
                mContext.getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, USER_SYSTEM);
        JSONObject object;
        try {
            if (overlayPackageJson == null) {
                object = new JSONObject();
            } else {
                object = new JSONObject(overlayPackageJson);
            }
            if (disable) {
                if (object.has(category)) object.remove(category);
            } else {
                object.put(category, packageName);
            }
            Settings.Secure.putStringForUser(mContext.getContentResolver(),
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    object.toString(), USER_SYSTEM);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e);
        }
    }

    public List<String> getOverlayPackagesForCategory(String category) {
        return getOverlayPackagesForCategory(category, "android");
    }

    public List<String> getOverlayPackagesForCategory(String category, String target) {
        List<String> overlays = new ArrayList<>();
        List<String> mPkgs = new ArrayList<>();
        overlays.add(target);
        for (OverlayInfo info : getOverlayInfos(category, target)) {
            if (category.equals(info.getCategory())) {
                mPkgs.add(info.getPackageName());
            }
        }
        Collections.sort(mPkgs);
        overlays.addAll(mPkgs);
        return overlays;
    }

    public List<OverlayInfo> getOverlayInfos(String category) {
        return getOverlayInfos(category, "android");
    }

    public List<OverlayInfo> getOverlayInfos(String category, String target) {
        final List<OverlayInfo> filteredInfos = new ArrayList<>();
        try {
            List<OverlayInfo> overlayInfos = mOverlayManager
                    .getOverlayInfosForTarget(target, USER_SYSTEM);
            for (OverlayInfo overlayInfo : overlayInfos) {
                if (category.equals(overlayInfo.category)) {
                    filteredInfos.add(overlayInfo);
                }
            }
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
        filteredInfos.sort(new OverlayInfoComparator());
        return filteredInfos;
    }

    class OverlayInfoComparator implements Comparator {
        public int compare(Object obj1, Object obj2) {
            OverlayInfo o1 = (OverlayInfo) obj1;
            OverlayInfo o2 = (OverlayInfo) obj2;
            return o1.packageName.compareTo(o2.packageName);
        }
    }

    public List<String> getLabels(String category) {
        return getLabels(category, "android");
    }

    public List<String> getLabels(String category, String target) {
        List<String> labels = new ArrayList<>();
        labels.add("Default");
        for (OverlayInfo info : getOverlayInfos(category, target)) {
            if (category.equals(info.getCategory())) {
                try {
                    labels.add(pm.getApplicationInfo(info.packageName, 0)
                            .loadLabel(pm).toString());
                } catch (PackageManager.NameNotFoundException e) {
                    labels.add(info.packageName);
                }
            }
        }
        return labels;
    }

    public List<Typeface> getFonts() {
        final List<Typeface> fontlist = new ArrayList<>();
            for (String overlayPackage : getOverlayPackagesForCategory(FONT_KEY)) {
                try {
                    overlayRes = overlayPackage.equals("android") ? Resources.getSystem()
                            : pm.getResourcesForApplication(overlayPackage);
                    final String font = overlayRes.getString(
                            overlayRes.getIdentifier("config_bodyFontFamily",
                            "string", overlayPackage));
                    fontlist.add(Typeface.create(font, Typeface.NORMAL));
                } catch (NameNotFoundException | NotFoundException e) {
                    // Do nothing
                }
            }
        return fontlist;
    }

    public List<ShapeDrawable> getShapeDrawables() {
        final List<ShapeDrawable> shapelist = new ArrayList<>();
            for (String overlayPackage : getOverlayPackagesForCategory(ICON_SHAPE_KEY)) {
                    shapelist.add(createShapeDrawable(overlayPackage));
            }
        return shapelist;
    }

    public ShapeDrawable createShapeDrawable(String overlayPackage) {
        try {
            if (overlayPackage.equals("android")) {
                overlayRes = Resources.getSystem();
            } else {
                if (overlayPackage.equals("default")) {
                    overlayPackage = "android";
                }
                overlayRes = pm.getResourcesForApplication(overlayPackage);
            }
        } catch (NameNotFoundException | NotFoundException e) {
            // Do nothing
        }
        final String shape = overlayRes.getString(
                overlayRes.getIdentifier("config_icon_mask",
                "string", overlayPackage));
        Path path = TextUtils.isEmpty(shape) ? null : PathParser.createPathFromPathData(shape);
        PathShape pathShape = new PathShape(path, 100f, 100f);
        ShapeDrawable shapeDrawable = new ShapeDrawable(pathShape);
        int mThumbSize = (int) (mContext.getResources().getDisplayMetrics().density * 72);
        shapeDrawable.setIntrinsicHeight(mThumbSize);
        shapeDrawable.setIntrinsicWidth(mThumbSize);
        return shapeDrawable;
    }
}
