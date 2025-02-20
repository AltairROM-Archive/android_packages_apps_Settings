/*
 * Copyright (C) 2022 Altair ROM Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

public class MonetUtils {

    public static final String KEY_MONET_COLOR_ACCENT = "monet_engine_color_accent";
    public static final String KEY_MONET_TINT_SURFACE = "monet_engine_tint_surface";
    public static final String KEY_MONET_ACCURATE_SHADES = "monet_engine_accurate_shades";
    public static final String KEY_MONET_RICHER_COLORS = "monet_engine_richer_colors";
    public static final String KEY_MONET_CHROMA_FACTOR = "monet_engine_chroma_factor";
    public static final String KEY_MONET_WHITE_LUMINANCE = "monet_engine_white_luminance";
    public static final String KEY_MONET_LINEAR_LIGHTNESS = "monet_engine_linear_lightness";

    public static final int ACCENT_COLOR_DISABLED = 0;
    public static final boolean SURFACE_TINT_DEFAULT = true;
    public static final boolean ACCURATE_SHADES_DEFAULT = true;
    public static final boolean RICHER_COLORS_DEFAULT = false;
    public static final boolean LINEAR_LIGHTNESS_DEFAULT = false;

    public static final int CHROMA_FACTOR_DEFAULT = 100;
    public static final int CHROMA_FACTOR_MIN = 0;
    public static final int CHROMA_FACTOR_MAX = 1000;

    public static final int WHITE_LUMINANCE_DEFAULT = 425;
    public static final int WHITE_LUMINANCE_MIN = 0;
    public static final int WHITE_LUMINANCE_MAX = 400;

    private Context mContext;

    public MonetUtils(Context context) {
        mContext = context;
    }

    /*
     * Private helper functions.
     */

    // Obtain integer value from Settings.Secure key.
    private int getInt(String key, int defaultValue) {
        return Settings.Secure.getInt(mContext.getContentResolver(), key, defaultValue);
    }

    // Set Settings.Secure key to integer value.
    private void putInt(String key, int value) {
        Settings.Secure.putInt(mContext.getContentResolver(), key, value);
    }

    // Obtain boolean value (0 or 1) from Settings.Secure key.
    private boolean getBoolean(String key, boolean defaultValue) {
        return Settings.Secure.getInt(mContext.getContentResolver(), key,
                defaultValue ? 1 : 0) != 0;
    }

    // Set Settings.Secure key to boolean value (0 or 1).
    private void putBoolean(String key, boolean value) {
        Settings.Secure.putInt(mContext.getContentResolver(), key, value ? 1 : 0);
    }

    /*
     * Public class functions.
     */

    // Returns true if accent color is set, false if not.
    public boolean isAccentColorSet() {
        return getAccentColor() != ACCENT_COLOR_DISABLED;
    }

    // Returns the current accent color.
    public int getAccentColor() {
        return getInt(KEY_MONET_COLOR_ACCENT, ACCENT_COLOR_DISABLED);
    }

    // Sets the accent color. Setting to ACCENT_COLOR_DISABLED removes the custom color and
    // returns the system to using the color obtained from the current wallpaper.
    public void setAccentColor(int color) {
        putInt(KEY_MONET_COLOR_ACCENT, color);
    }

    // Returns true if surface color tinting is enabled, false if not.
    public boolean isSurfaceTintEnabled() {
        return getBoolean(KEY_MONET_TINT_SURFACE, SURFACE_TINT_DEFAULT);
    }

    // Enables or disables surface color tinting.
    public void setSurfaceTintEnabled(boolean enable) {
        putBoolean(KEY_MONET_TINT_SURFACE, enable);
    }

    // Returns true if accurate color shading is enabled, false if not.
    public boolean isAccurateShadesEnabled() {
        return getBoolean(KEY_MONET_ACCURATE_SHADES, ACCURATE_SHADES_DEFAULT);
    }

    // Enables or disables accurate color shading.
    public void setAccurateShadesEnabled(boolean enable) {
        putBoolean(KEY_MONET_ACCURATE_SHADES, enable);
    }

    // Returns true if richer colors are enabled, false if not.
    public boolean isRicherColorsEnabled() {
        return getBoolean(KEY_MONET_RICHER_COLORS, RICHER_COLORS_DEFAULT);
    }

    // Enables or disables richer colors.
    public void setRicherColorsEnabled(boolean enable) {
        putBoolean(KEY_MONET_RICHER_COLORS, enable);
    }

    // Returns the current chroma factor value.
    public int getChromaFactor() {
        return getInt(KEY_MONET_CHROMA_FACTOR, CHROMA_FACTOR_DEFAULT);
    }

    // Sets the chroma factor value. Value cannot be set higher than CHROMA_FACTOR_MAX or lower
    // than CHROMA_FACTOR_MIN.
    public void setChromaFactor(int value) {
        if (value < CHROMA_FACTOR_MIN) {
            putInt(KEY_MONET_CHROMA_FACTOR, CHROMA_FACTOR_MIN);
        } else if (value > CHROMA_FACTOR_MAX) {
            putInt(KEY_MONET_CHROMA_FACTOR, CHROMA_FACTOR_MAX);
        } else {
            putInt(KEY_MONET_CHROMA_FACTOR, value);
        }
    }

    // Returns the current white luminance value.
    public int getWhiteLuminance() {
        return getInt(KEY_MONET_WHITE_LUMINANCE, WHITE_LUMINANCE_DEFAULT);
    }

    // Sets the white luminance value. Value cannot be set higher than WHITE_LUMINANCE_MAX or lower
    // than WHITE_LUMINANCE_MIN.
    public void setWhiteLuminance(int value) {
        if (value < WHITE_LUMINANCE_MIN) {
            putInt(KEY_MONET_WHITE_LUMINANCE, WHITE_LUMINANCE_MIN);
        } else if (value > WHITE_LUMINANCE_MAX) {
            putInt(KEY_MONET_WHITE_LUMINANCE, WHITE_LUMINANCE_MAX);
        } else {
            putInt(KEY_MONET_WHITE_LUMINANCE, value);
        }
    }

    // Returns true if linear lightness is enabled, false if not.
    public boolean isLinearLightnessEnabled() {
        return getBoolean(KEY_MONET_RICHER_COLORS, LINEAR_LIGHTNESS_DEFAULT);
    }

    // Enables or disables linear lightness.
    public void setLinearLightnessEnabled(boolean enable) {
        putBoolean(KEY_MONET_LINEAR_LIGHTNESS, enable);
    }
}
