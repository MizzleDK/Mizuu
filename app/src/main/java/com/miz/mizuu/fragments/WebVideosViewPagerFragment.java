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
import com.miz.mizuu.R;

public class WebVideosViewPagerFragment extends Fragment {

    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

	public WebVideosViewPagerFragment() {}

	public static WebVideosViewPagerFragment newInstance() {		
		return new WebVideosViewPagerFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		return inflater.inflate(R.layout.viewpager_with_tabs, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(new WebVideosAdapter(getChildFragmentManager()));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        mTabs.setShouldExpand(true);
        mTabs.setViewPager(mViewPager);
        if (MizLib.hasLollipop())
            mTabs.setElevation(1f);

        if (MizLib.hasLollipop())
            ((ActionBarActivity) getActivity()).getSupportActionBar().setElevation(0);

		if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt("selectedIndex", 0));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("selectedIndex", mViewPager.getCurrentItem());
	}

	private class WebVideosAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.choiceYouTube), getString(R.string.choiceReddit), getString(R.string.choiceTedTalks)};

		public WebVideosAdapter(FragmentManager fm) {
			super(fm);
		}

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

		@Override
		public Fragment getItem(int index) {
			return WebVideoFragment.newInstance(getPageTitle(index).toString());
		}  

		@Override  
		public int getCount() {
			return 3;
		}
	}
}