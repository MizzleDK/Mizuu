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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.mizuu.SearchWebMovies;

public class MovieDiscoveryViewPagerFragment extends Fragment {

	private ProgressBar mProgressBar;
	private String mBaseUrl = "", mJson = "";
	private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

	public MovieDiscoveryViewPagerFragment() {}

	public static MovieDiscoveryViewPagerFragment newInstance() {		
		return new MovieDiscoveryViewPagerFragment();
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

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setPageMargin(MizLib.convertDpToPixels(getActivity(), 16));

        mTabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        mTabs.setVisibility(View.GONE);
        mTabs.setShouldExpand(true);

		if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt("selectedIndex", 0));
		}
		
		if (mJson.isEmpty()) {
			new MovieLoader(getActivity()).execute();
		} else {
            setupAdapterAndTabs();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_web_movies, menu);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search_textbox:
			Intent i = new Intent(getActivity().getApplicationContext(), SearchWebMovies.class);
			startActivity(i);
			break;
		}

		return super.onOptionsItemSelected(item);	
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("selectedIndex", mViewPager.getCurrentItem());
	}

	private class WebVideosAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.stringUpcoming), getString(R.string.stringNowPlaying), getString(R.string.stringPopular)};

		public WebVideosAdapter(FragmentManager fm) {
			super(fm);
		}

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

		@Override  
		public Fragment getItem(int index) {		
			switch (index) {
			case 0: return MovieDiscoveryFragment.newInstance("upcoming", mJson, mBaseUrl);
			case 1: return MovieDiscoveryFragment.newInstance("now_playing", mJson, mBaseUrl);
			default: return MovieDiscoveryFragment.newInstance("popular", mJson, mBaseUrl);
			}
		}  

		@Override  
		public int getCount() {
			return 3;
		}
	}
	
	private class MovieLoader extends AsyncTask<Object, Object, String> {
		
		private Context mContext;
		
		public MovieLoader(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(Object... params) {
			try {
                mBaseUrl = MizLib.getTmdbImageBaseUrl(mContext);
                mJson = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie?api_key=" + MizLib.getTmdbApiKey(mContext) + "&append_to_response=upcoming,now_playing,popular,top_rated").toString();
				
				return mJson;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
                setupAdapterAndTabs();
			} else {
				Toast.makeText(getActivity(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
			}
		}
	}

    private void setupAdapterAndTabs() {
        if (MizLib.hasLollipop())
            ((ActionBarActivity) getActivity()).getSupportActionBar().setElevation(0);

        mViewPager.setAdapter(new WebVideosAdapter(getChildFragmentManager()));
        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);
        if (MizLib.hasLollipop())
            mTabs.setElevation(1f);

        mProgressBar.setVisibility(View.GONE);
    }
}