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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import org.omnirom.device.R;

public class ScreenFragmentActivity extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_ENABLED = "1";
    private static final String TAG = "DeviceSettings_Screen";
    private CABC mCABC;
    private mDNIeScenario mmDNIeScenario;
    private mDNIeMode mmDNIeMode;
    // private mDNIeNegative mmDNIeNegative;
    private LedFade mLedFade;
    private CheckBoxPreference mTouchKey;
    private TouchkeyTimeout mTouchKeyTimeout;

    private static boolean sSPenSupported;
    private static boolean sTouchkeySupport;
    private static final String FILE_TOUCHKEY_BRIGHTNESS = "/sys/class/sec/sec_touchkey/brightness";
    private static final String FILE_TOUCHKEY_DISABLE = "/sys/class/sec/sec_touchkey/force_disable";

    private SwitchPreference mTouchwakeEnable;
    private SeekBarPreference mTouchwakeTimeout;
    private static final String TOUCHWAKE_CATEGORY = "category_power_menu";
    private static final String KEY_TOUCHWAKE_ENABLE = "touchwake_enable";
    private static final String KEY_TOUCHWAKE_TIMEOUT = "touchwake_timeout";
    private static final String FILE_TOUCHWAKE_ENABLE = "/sys/devices/virtual/misc/touchwake/enabled";
    private static final String FILE_TOUCHWAKE_TIMEOUT = "/sys/devices/virtual/misc/touchwake/delay";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.screen_preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Resources res = getResources();

        /* CABC */
        mCABC = (CABC) findPreference(DeviceSettings.KEY_CABC);
        mCABC.setEnabled(CABC.isSupported(res.getString(R.string.mdnie_cabc_sysfs_file)));

        /* mDNIe */
        mmDNIeScenario = (mDNIeScenario) findPreference(DeviceSettings.KEY_MDNIE_SCENARIO);
        mmDNIeScenario.setEnabled(mDNIeScenario.isSupported(res.getString(R.string.mdnie_scenario_sysfs_file)));

        mmDNIeMode = (mDNIeMode) findPreference(DeviceSettings.KEY_MDNIE_MODE);
        mmDNIeMode.setEnabled(mDNIeMode.isSupported(res.getString(R.string.mdnie_mode_sysfs_file)));

        // mmDNIeNegative = (mDNIeNegative) findPreference(DeviceSettings.KEY_MDNIE_NEGATIVE);
        // mmDNIeNegative.setEnabled(mDNIeNegative.isSupported(res.getString(R.string.mdnie_negative_sysfs_file)));

        /* LED */
        mLedFade = (LedFade) findPreference(DeviceSettings.KEY_LED_FADE);
        mLedFade.setEnabled(LedFade.isSupported());

        /* Touchkey */
        sTouchkeySupport = res.getBoolean(R.bool.has_touchkey);
        mTouchKey = (CheckBoxPreference)preferenceScreen.findPreference(DeviceSettings.KEY_TOUCHKEY_LIGHT);
        mTouchKey.setEnabled(sTouchkeySupport);

        mTouchKeyTimeout = (TouchkeyTimeout)preferenceScreen.findPreference(DeviceSettings.KEY_TOUCHKEY_TIMEOUT);

        if (mTouchKey.isChecked() && mTouchKey.isEnabled()) {
            mTouchKeyTimeout.setEnabled(mTouchKeyTimeout.isSupported());
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
        String spenFilePath = res.getString(R.string.spen_sysfs_file);
        sSPenSupported = SPenPowerSavingMode.isSupported(spenFilePath);

        PreferenceCategory spenCategory = (PreferenceCategory) findPreference(DeviceSettings.KEY_CATEGORY_SPEN);
        if (!sSPenSupported) {
            preferenceScreen.removePreference(spenCategory);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        String key = preference.getKey();
        Log.w(TAG, "key: " + key);

        if (key.compareTo(DeviceSettings.KEY_TOUCHKEY_LIGHT) == 0) {
            if (((CheckBoxPreference)preference).isChecked()) {
                Utils.writeValue(FILE_TOUCHKEY_DISABLE, "0");
                Utils.writeValue(FILE_TOUCHKEY_BRIGHTNESS, "1");
                mTouchKeyTimeout.setEnabled(mTouchKeyTimeout.isSupported());
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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean light = sharedPrefs.getBoolean(DeviceSettings.KEY_TOUCHKEY_LIGHT, true);

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
