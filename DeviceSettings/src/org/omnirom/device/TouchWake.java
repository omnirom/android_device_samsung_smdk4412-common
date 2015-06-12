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

public class TouchWake extends SwitchPreference implements OnPreferenceChangeListener {

    public static final String KEY_TOUCHWAKE_ENABLE = "touchwake_enable";
    private static String FILE_TOUCHWAKE_ENABLE = null;

    public TouchWake(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
        FILE_TOUCHWAKE_ENABLE = context.getResources().getString(R.string.touchwake_sysfs_file);
    }

    public static boolean isSupported(Context context) {
        if (FILE_TOUCHWAKE_ENABLE == null) {
            FILE_TOUCHWAKE_ENABLE = context.getResources().getString(R.string.touchwake_sysfs_file);
        }
        return Utils.fileExists(FILE_TOUCHWAKE_ENABLE);
    }

    public static void restore(Context context) {
        if (!isSupported(context)) { // also sets FILE_TOUCHWAKE_ENABLE
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean b = sharedPrefs.getBoolean(KEY_TOUCHWAKE_ENABLE, false);
        Utils.writeValue(FILE_TOUCHWAKE_ENABLE, b);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean b = (Boolean) newValue;
        Utils.writeValue(FILE_TOUCHWAKE_ENABLE, b);
        return true;
    }

}
