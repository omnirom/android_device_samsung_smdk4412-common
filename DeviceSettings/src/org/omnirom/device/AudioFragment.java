/*
* Copyright (C) 2012 The CyanogenMod Project
*               2015 The OmniROM Project
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

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class AudioFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.audio_preferences);
        Context context = getActivity();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        PreferenceCategory preferenceCategory = null;

        /* Dock Audio */
        preferenceCategory = (PreferenceCategory) findPreference("category_dock_audio");

        if (!DockAudio.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(DockAudio.KEY_USE_DOCK_AUDIO));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }
    }

    public static void restore(Context context) {
        DockAudio.restore(context);
    }

    public static boolean hasSupportedPreferences(Context context) {
        boolean isSupported = false;
        isSupported |= DockAudio.isSupported(context);
        return isSupported;
    }
}
