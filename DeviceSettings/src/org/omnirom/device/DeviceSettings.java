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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentActivity;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.MenuItem;

import java.util.ArrayList;

public class DeviceSettings extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.top);

        ((PagerTabStrip) findViewById(R.id.pagerStrip)).setTabIndicatorColor(0x009688);

        final ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setTitle(R.string.app_name);
        bar.setDisplayHomeAsUpEnabled(true);

        Resources res = getResources();
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);

        TabsAdapter tabsAdapter = new TabsAdapter(this, viewPager);
        tabsAdapter.addTab(res.getString(R.string.category_radio_title),
                RadioFragment.class, null);
        tabsAdapter.addTab(res.getString(R.string.category_screen_title),
                ScreenFragment.class, null);
        tabsAdapter.addTab(res.getString(R.string.category_haptic_title),
                HapticFragment.class, null);
        // tabsAdapter.addTab(res.getString(R.string.category_audio_title),
        //        AudioFragment.class, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                DeviceSettings.this.onBackPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class TabsAdapter extends FragmentPagerAdapter {

        private final Context mContext;
        private final ArrayList<TabInfo> mTabs = new ArrayList<>();

        static final class TabInfo {

            private final String title;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _title, Class<?> _class, Bundle _args) {
                title = _title;
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            pager.setAdapter(this);
            mContext = activity;
        }

        public void addTab(String title, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(title, clss, args);
            mTabs.add(info);
            // TODO - needed?
            notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return  mTabs.get(position).title;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }
    }
}
