package com.miz.mizuu.fragments;/*
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.miz.functions.MizLib;
import com.miz.loader.TvShowLoader;
import com.miz.mizuu.R;

public class TvShowLibraryOverviewFragment extends Fragment {

    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

    public TvShowLibraryOverviewFragment() {} // Empty constructor

    public static TvShowLibraryOverviewFragment newInstance() {
        return new TvShowLibraryOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);

        if (MizLib.hasLollipop())
            ((ActionBarActivity) getActivity()).getSupportActionBar().setElevation(0);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setPageMargin(MizLib.convertDpToPixels(getActivity(), 16));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);

        mViewPager.setAdapter(new PagerAdapter(getChildFragmentManager()));
        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);

        // Work-around a bug that sometimes happens with the tabs
        mViewPager.setCurrentItem(0);

        if (MizLib.hasLollipop())
            mTabs.setElevation(1f);

        return v;
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.choiceAllShows), getString(R.string.choiceFavorites), getString(R.string.recently_aired),
                getString(R.string.watched_tv_shows), getString(R.string.unwatched_tv_shows)};

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case 0:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.ALL_SHOWS);
                case 1:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.FAVORITES);
                case 2:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.RECENTLY_AIRED);
                case 3:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.WATCHED);
                case 4:
                    return TvShowLibraryFragment.newInstance(TvShowLoader.UNWATCHED);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }
}
