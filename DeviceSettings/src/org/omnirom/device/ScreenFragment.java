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
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class ScreenFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_preferences);
        Context context = getActivity();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        PreferenceCategory preferenceCategory = null;

        /* Colors */
        preferenceCategory = (PreferenceCategory) findPreference("category_colors");

        if (!CABC.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(CABC.KEY_CABC));
        }
        if (!mDNIeScenario.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(mDNIeScenario.KEY_MDNIE_SCENARIO));
        }
        if (!mDNIeMode.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(mDNIeMode.KEY_MDNIE_MODE));
        }
        if (!mDNIeNegative.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(mDNIeNegative.KEY_MDNIE_NEGATIVE));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }


        /* LED */
        preferenceCategory = (PreferenceCategory) findPreference("category_led");

        if (!LedFade.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(LedFade.KEY_LED_FADE));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }


        /* S-Pen */
        preferenceCategory = (PreferenceCategory) findPreference("category_spen");

        if (!SPenPowerSavingMode.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(SPenPowerSavingMode.KEY_SPEN_POWER_SAVE));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }


        /* Touchkey */
        preferenceCategory = (PreferenceCategory) findPreference("category_touchkeys");

        if (!Touchkey.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(Touchkey.KEY_TOUCHKEY_LIGHT));
            preferenceCategory.removePreference(findPreference(TouchkeyTimeout.KEY_TOUCHKEY_TIMEOUT));
        } else if (!TouchkeyTimeout.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(TouchkeyTimeout.KEY_TOUCHKEY_TIMEOUT));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }


        /* Touchwake */
        preferenceCategory = (PreferenceCategory) findPreference("category_touchwake");

        if (!TouchWake.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(TouchWake.KEY_TOUCHWAKE_ENABLE));
            preferenceCategory.removePreference(findPreference(TouchWakeTimeout.KEY_TOUCHWAKE_TIMEOUT));
        } else if (!TouchWakeTimeout.isSupported(context)) {
            preferenceCategory.removePreference(findPreference(TouchWakeTimeout.KEY_TOUCHWAKE_TIMEOUT));
        }

        if (preferenceCategory.getPreferenceCount() == 0) {
            preferenceScreen.removePreference(preferenceCategory);
        }
    }

    public static void restore(Context context) {
        CABC.restore(context);
        LedFade.restore(context);
        mDNIeScenario.restore(context);
        mDNIeMode.restore(context);
        mDNIeNegative.restore(context);
        SPenPowerSavingMode.restore(context);
        Touchkey.restore(context);
        TouchkeyTimeout.restore(context);
        TouchWake.restore(context);
        TouchWakeTimeout.restore(context);
    }

    public static boolean hasSupportedPreferences(Context context) {
        boolean isSupported = false;
        isSupported |= CABC.isSupported(context);
        isSupported |= mDNIeScenario.isSupported(context);
        isSupported |= mDNIeMode.isSupported(context);
        isSupported |= mDNIeNegative.isSupported(context);
        isSupported |= LedFade.isSupported(context);
        isSupported |= SPenPowerSavingMode.isSupported(context);
        isSupported |= Touchkey.isSupported(context);
        isSupported |= TouchWake.isSupported(context);
        return isSupported;
    }
}
