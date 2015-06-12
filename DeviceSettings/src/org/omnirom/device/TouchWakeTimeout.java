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
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class TouchWakeTimeout extends SeekBarPreference implements Preference.OnPreferenceChangeListener {

    public static final String KEY_TOUCHWAKE_TIMEOUT = "touchwake_timeout";
    private static String FILE_TOUCHWAKE_TIMEOUT = null;

    public TouchWakeTimeout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
        FILE_TOUCHWAKE_TIMEOUT = context.getResources().getString(R.string.touchwaketimeout_sysfs_file);
    }

    public static boolean isSupported(Context context) {
        if (FILE_TOUCHWAKE_TIMEOUT == null) {
            FILE_TOUCHWAKE_TIMEOUT = context.getResources().getString(R.string.touchwaketimeout_sysfs_file);
        }
        return Utils.fileExists(FILE_TOUCHWAKE_TIMEOUT);
    }

    public static void restore(Context context) {
        if (!isSupported(context)) { // also sets FILE_*
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int i = sharedPrefs.getInt(KEY_TOUCHWAKE_TIMEOUT, 5) * 1000;
        Utils.writeValue(FILE_TOUCHWAKE_TIMEOUT, Integer.toString(i));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int i = (Integer) newValue;
        String s = Integer.toString (i * 1000);
        Utils.writeValue(FILE_TOUCHWAKE_TIMEOUT, s);
        return true;
    }
}
