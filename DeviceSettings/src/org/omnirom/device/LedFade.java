/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

public class LedFade extends SwitchPreference implements OnPreferenceChangeListener {

    public static final String KEY_LED_FADE = "led_fade";
    private static String FILE = null;

    public LedFade(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
    }

    public static boolean isSupported(Context context) {
        if (FILE == null) {
            FILE = context.getResources().getString(R.string.led_fade_sysfs_file);
        }
        return Utils.fileExists(FILE);
    }

    /**
     * Restore led fading mode setting from SharedPreferences. (Write to kernel.)
     * @param context       The context to read the SharedPreferences from
     */
    public static void restore(Context context) {
        if (!isSupported(context)) {  // also sets FILE
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Utils.writeValue(FILE, sharedPrefs.getBoolean(KEY_LED_FADE, true));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Utils.writeValue(FILE, (Boolean) newValue);
        return true;
    }

}
