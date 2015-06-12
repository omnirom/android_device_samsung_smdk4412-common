/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package org.omnirom.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

public class Touchkey extends SwitchPreference implements OnPreferenceChangeListener {

    public static final String KEY_TOUCHKEY_LIGHT = "touchkey_light";
    private static String FILE_TOUCHKEY_BRIGHTNESS = null;
    private static String FILE_TOUCHKEY_DISABLE = null;

    public Touchkey(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
        FILE_TOUCHKEY_BRIGHTNESS = context.getResources().getString(R.string.touchkey_brightness_sysfs_file);
        FILE_TOUCHKEY_DISABLE = context.getResources().getString(R.string.touchkey_disable_sysfs_file);
    }

    public static boolean isSupported(Context context) {
        if (FILE_TOUCHKEY_BRIGHTNESS == null) {
            FILE_TOUCHKEY_BRIGHTNESS = context.getResources().getString(R.string.touchkey_brightness_sysfs_file);
        }
        if (FILE_TOUCHKEY_DISABLE == null) {
            FILE_TOUCHKEY_DISABLE = context.getResources().getString(R.string.touchkey_disable_sysfs_file);
        }

        boolean hasTouchKey = context.getResources().getBoolean(R.bool.has_touchkey);

        return (Utils.fileExists(FILE_TOUCHKEY_BRIGHTNESS) && Utils.fileExists(FILE_TOUCHKEY_DISABLE) && hasTouchKey);
    }

    public static void restore(Context context) {
        if (!isSupported(context)) { // also sets FILE_*
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean light = sharedPrefs.getBoolean(KEY_TOUCHKEY_LIGHT, true);

        Utils.writeValue(FILE_TOUCHKEY_DISABLE, light ? "0" : "1");
        Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, light ? "1" : "2");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean b = (Boolean) newValue;
        Utils.writeValue(FILE_TOUCHKEY_DISABLE, b ? "0" : "1");
        Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, b ? "1" : "2");
        return true;
    }

}
