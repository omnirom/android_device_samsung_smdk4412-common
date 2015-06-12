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
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class ScreenFragmentActivity extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_preferences);
        Context context = getActivity();

        /* CABC */
        if (!CABC.isSupported(context)) {
            findPreference(CABC.KEY_CABC).setEnabled(false);
        }

        /* mDNIe */
        if (!mDNIeScenario.isSupported(context)) {
            findPreference(mDNIeScenario.KEY_MDNIE_SCENARIO).setEnabled(false);
        }

        if (!mDNIeMode.isSupported(context)) {
            findPreference(mDNIeMode.KEY_MDNIE_MODE).setEnabled(false);
        }

        // if (!mDNIeNegative.isSupported(context)) {
        //     findPreference(mDNIeNegative.KEY_MDNIE_NEGATIVE).setEnabled(false);
        // }

        /* LED */
        if (!LedFade.isSupported(context)) {
            findPreference(LedFade.KEY_LED_FADE).setEnabled(false);
        }

        /* Touchkey */
        TouchkeyTimeout touchKeyTimeout = (TouchkeyTimeout) findPreference(TouchkeyTimeout.KEY_TOUCHKEY_TIMEOUT);
        if (!Touchkey.isSupported(context)) {
            findPreference(Touchkey.KEY_TOUCHKEY_LIGHT).setEnabled(false);
            touchKeyTimeout.setEnabled(false);  // TODO needed?
        } else if (!TouchkeyTimeout.isSupported(context)) {
            touchKeyTimeout.setEnabled(false);
        }

        /* Touchwake */
        TouchWakeTimeout touchwakeTimeout = (TouchWakeTimeout) findPreference(TouchWakeTimeout.KEY_TOUCHWAKE_TIMEOUT);

        if (!TouchWake.isSupported(context)) {
            findPreference(TouchWake.KEY_TOUCHWAKE_ENABLE).setEnabled(false);
            touchwakeTimeout.setEnabled(false); // TODO needed?
        } else if (!TouchWakeTimeout.isSupported(context)) {
            touchwakeTimeout.setEnabled(false);
        }

        /* S-Pen */
        if (!SPenPowerSavingMode.isSupported(context)) {
            findPreference(SPenPowerSavingMode.KEY_SPEN_POWER_SAVE).setEnabled(false);
        }
    }

    public static void restore(Context context) {
        CABC.restore(context);
        LedFade.restore(context);
        mDNIeScenario.restore(context);
        mDNIeMode.restore(context);
        // mDNIeNegative.restore(context);
        SPenPowerSavingMode.restore(context);
        Touchkey.restore(context);
        TouchkeyTimeout.restore(context);
        TouchWake.restore(context);
        TouchWakeTimeout.restore(context);
    }
}
