/*
* Copyright (C) 2012 The CyanogenMod Project
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

package org.omnirom.device;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class AudioFragmentActivity extends PreferenceFragment {

    private static final String TAG = "DeviceSettings_Audio";

    private static final String KEY_USE_DOCK_AUDIO = "dock_audio";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.audio_preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        Log.w(TAG, "key: " + key);

        if (key.compareTo(KEY_USE_DOCK_AUDIO) == 0) {
            String boxValue = (((CheckBoxPreference)preference).isChecked() ? "1" : "0");
            Intent i = new Intent("org.omnirom.settings.SamsungDock");
            i.putExtra("data", boxValue);
            ActivityManagerNative.broadcastStickyIntent(i, null, UserHandle.USER_ALL);
        }
        return true;
    }

    public static void restore(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean dockAudio = sharedPrefs.getBoolean(KEY_USE_DOCK_AUDIO, false);
        Intent i = new Intent("org.omnirom.settings.SamsungDock");
        i.putExtra("data", (dockAudio? "1" : "0"));
        ActivityManagerNative.broadcastStickyIntent(i, null, UserHandle.USER_ALL);
    }
}
