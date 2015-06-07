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
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

public class ScreenFragmentActivity extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "DeviceSettings_Screen";

    private static final String KEY_TOUCHKEY_LIGHT = "touchkey_light";
    private static final String KEY_TOUCHWAKE_ENABLE = "touchwake_enable";
    private static final String KEY_TOUCHWAKE_TIMEOUT = "touchwake_timeout";
    private static final String FILE_TOUCHWAKE_ENABLE = "/sys/devices/virtual/misc/touchwake/enabled";
    private static final String FILE_TOUCHWAKE_TIMEOUT = "/sys/devices/virtual/misc/touchwake/delay";

    private static final String FILE_TOUCHKEY_BRIGHTNESS = "/sys/class/sec/sec_touchkey/brightness";
    private static final String FILE_TOUCHKEY_DISABLE = "/sys/class/sec/sec_touchkey/force_disable";

    private TouchkeyTimeout mTouchKeyTimeout;
    private SwitchPreference mTouchwakeEnable;
    private SeekBarPreference mTouchwakeTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Resources res = getResources();

        /* CABC */
        CABC cabc = (CABC) findPreference(CABC.KEY_CABC);
        cabc.setEnabled(CABC.isSupported(res.getString(R.string.mdnie_cabc_sysfs_file)));

        /* mDNIe */
        mDNIeScenario scenario = (mDNIeScenario) findPreference(mDNIeScenario.KEY_MDNIE_SCENARIO);
        scenario.setEnabled(mDNIeScenario.isSupported(res.getString(R.string.mdnie_scenario_sysfs_file)));

        mDNIeMode mode = (mDNIeMode) findPreference(mDNIeMode.KEY_MDNIE_MODE);
        mode.setEnabled(mDNIeMode.isSupported(res.getString(R.string.mdnie_mode_sysfs_file)));

        // mDNIeNegative negative = (mDNIeNegative) findPreference(mDNIeNegative.KEY_MDNIE_NEGATIVE);
        // negative.setEnabled(mDNIeNegative.isSupported(res.getString(R.string.mdnie_negative_sysfs_file)));

        /* LED */
        LedFade ledFade = (LedFade) findPreference(LedFade.KEY_LED_FADE);
        ledFade.setEnabled(LedFade.isSupported());

        /* Touchkey */
        boolean touchkeySupport = res.getBoolean(R.bool.has_touchkey);
        CheckBoxPreference touchKey = (CheckBoxPreference) preferenceScreen.findPreference(KEY_TOUCHKEY_LIGHT);
        touchKey.setEnabled(touchkeySupport);

        mTouchKeyTimeout = (TouchkeyTimeout)preferenceScreen.findPreference(TouchkeyTimeout.KEY_TOUCHKEY_TIMEOUT);

        if (touchKey.isChecked() && touchKey.isEnabled()) {
            mTouchKeyTimeout.setEnabled(TouchkeyTimeout.isSupported());
        } else {
            mTouchKeyTimeout.setEnabled(false);
        }

        /* Touchwake */
        mTouchwakeEnable = (SwitchPreference) findPreference(KEY_TOUCHWAKE_ENABLE);
        mTouchwakeTimeout = (SeekBarPreference) findPreference(KEY_TOUCHWAKE_TIMEOUT);

        if (!isSupported(FILE_TOUCHWAKE_ENABLE)) {
            mTouchwakeEnable.setEnabled(false);
            mTouchwakeEnable.setSummary(R.string.kernel_does_not_support);

            mTouchwakeTimeout.setEnabled(false);
            mTouchwakeTimeout.setSummary(R.string.kernel_does_not_support);
        } else {
            int b = Integer.parseInt(Utils.readOneLine(FILE_TOUCHWAKE_ENABLE));
            mTouchwakeEnable.setChecked(b != 0);
            mTouchwakeEnable.setOnPreferenceChangeListener(this);

            int i = Integer.parseInt(Utils.readOneLine(FILE_TOUCHWAKE_TIMEOUT));
            mTouchwakeTimeout.setValue(i / 1000);
            mTouchwakeTimeout.setOnPreferenceChangeListener(this);
        }

        /* S-Pen */
        String sPenFilePath = res.getString(R.string.spen_sysfs_file);
        boolean sPenSupported = SPenPowerSavingMode.isSupported(sPenFilePath);

        PreferenceCategory sPenCategory = (PreferenceCategory) findPreference(SPenPowerSavingMode.KEY_CATEGORY_SPEN);
        if (!sPenSupported) {
            preferenceScreen.removePreference(sPenCategory);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        Log.w(TAG, "key: " + key);

        if (key.compareTo(KEY_TOUCHKEY_LIGHT) == 0) {
            if (((CheckBoxPreference)preference).isChecked()) {
                Utils.writeValue(FILE_TOUCHKEY_DISABLE, "0");
                Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, "1");
                mTouchKeyTimeout.setEnabled(TouchkeyTimeout.isSupported());
            } else {
                Utils.writeValue(FILE_TOUCHKEY_DISABLE, "1");
                Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, "2");
                mTouchKeyTimeout.setEnabled(false);
            }
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTouchwakeEnable) {
            boolean b = (Boolean) newValue;
            mTouchwakeEnable.setChecked(b);

            Utils.writeValue(FILE_TOUCHWAKE_ENABLE, b);

            return true;
        } else if (preference == mTouchwakeTimeout) {
            int i = (Integer) newValue;
            mTouchwakeTimeout.setValue(i);

            String s = Integer.toString (i * 1000);
            Utils.writeValue(FILE_TOUCHWAKE_TIMEOUT, s);

            return true;
        }
        return false;
    }

    public static boolean isSupported(String FILE) {
        return Utils.fileExists(FILE);
    }

    public static void restore(Context context) {
        CABC.restore(context);
        LedFade.restore(context);
        mDNIeScenario.restore(context);
        mDNIeMode.restore(context);
        // mDNIeNegative.restore(context);
        SPenPowerSavingMode.restore(context);
        TouchkeyTimeout.restore(context);

        /* Touchwake */
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean light = sharedPrefs.getBoolean(KEY_TOUCHKEY_LIGHT, true);

        Utils.writeValue(FILE_TOUCHKEY_DISABLE, light ? "0" : "1");
        Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, light ? "1" : "2");

        if (isSupported(FILE_TOUCHWAKE_ENABLE)) {
            boolean b = sharedPrefs.getBoolean(KEY_TOUCHWAKE_ENABLE, false);
            Utils.writeValue(FILE_TOUCHWAKE_ENABLE, b);

            int i = sharedPrefs.getInt(KEY_TOUCHWAKE_TIMEOUT, 10) * 1000;
            Utils.writeValue(FILE_TOUCHWAKE_TIMEOUT, Integer.toString(i));
        }
    }
}
