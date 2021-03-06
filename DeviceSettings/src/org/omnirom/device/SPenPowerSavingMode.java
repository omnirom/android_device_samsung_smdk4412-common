/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2015 The OmniROM Project
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
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

public class SPenPowerSavingMode extends SwitchPreference implements OnPreferenceChangeListener {

    public static final String KEY_SPEN_POWER_SAVE = "spen_power_save";
    private static String FILE_PATH = null;

    public SPenPowerSavingMode(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
        FILE_PATH = context.getResources().getString(R.string.spen_powersaving_sysfs_file);
    }

    public static boolean isSupported(Context context) {
        if (context.getResources().getBoolean(R.bool.disable_spen_powersaving_preference)){
            return false;
        }

        if (FILE_PATH == null) {
            FILE_PATH = context.getResources().getString(R.string.spen_powersaving_sysfs_file);
        }
        return Utils.fileExists(FILE_PATH);
    }

    /**
     * Restore s-pen setting from SharedPreferences. (Write to kernel.)
     * @param context       The context to read the SharedPreferences from
     */
    public static void restore(Context context) {
        if (!isSupported(context)) { // also sets FILE_PATH
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Utils.writeValue(FILE_PATH, sharedPrefs.getBoolean(KEY_SPEN_POWER_SAVE, false) ? "1" : "0");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Utils.writeValue(FILE_PATH, ((Boolean) newValue) ? "1" : "0");
        return true;
    }
}
