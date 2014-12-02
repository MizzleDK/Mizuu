/*
 * Copyright (C) 2014 Michell Bak
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

package com.miz.mizuu.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.miz.mizuu.R;

public class IgnoredFilesFragment extends Fragment {

    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

    public IgnoredFilesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setAdapter(new IgnoredFilesAdapter(getFragmentManager()));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        mTabs.setBackgroundResource(R.drawable.bg);
        mTabs.setViewPager(mViewPager);

        return v;
    }

    private class IgnoredFilesAdapter extends FragmentPagerAdapter {

        public IgnoredFilesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new IgnoredMovieFilesFragment();
                default:
                    return new IgnoredTvShowFilesFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.chooserMovies);
                default: return getString(R.string.chooserTVShows);
            }
        }
    }
}