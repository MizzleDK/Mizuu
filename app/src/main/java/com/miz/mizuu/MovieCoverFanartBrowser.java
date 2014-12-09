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

package com.miz.mizuu;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.miz.base.MizActivity;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.CollectionCoverSearchFragment;
import com.miz.mizuu.fragments.CoverSearchFragment;
import com.miz.mizuu.fragments.FanartSearchFragment;
import com.miz.utils.ViewUtils;

public class MovieCoverFanartBrowser extends MizActivity  {

	private String mTmdbId, mCollectionId, mBaseUrl = "", mJson = "", mCollection = "", mTmdbApiKey;
	private ViewPager mViewPager;
	private ProgressBar mProgressBar;
    private PagerSlidingTabStrip mTabs;
    private int mToolbarColor;

	@Override
	protected int getLayoutResource() {
		return R.layout.viewpager_with_toolbar_and_tabs;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MizuuApplication.setupTheme(this);

        if (MizLib.hasLollipop())
            getSupportActionBar().setElevation(0);

        mTmdbId = getIntent().getExtras().getString("tmdbId");
        mCollectionId = getIntent().getExtras().getString("collectionId");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);
		mTmdbApiKey = MizLib.getTmdbApiKey(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        mViewPager = (ViewPager) findViewById(R.id.awesomepager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPageMargin(MizLib.convertDpToPixels(this, 16));

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mTabs.setVisibility(View.GONE);

		if (savedInstanceState != null) {
            mJson = savedInstanceState.getString("json", "");
            mBaseUrl = savedInstanceState.getString("baseUrl");
            mCollection = savedInstanceState.getString("collection");
			setupActionBarStuff();

            mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new MovieLoader(getApplicationContext()).execute(mTmdbId, mCollectionId);
		}
	}

    @Override
    public void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
        mTabs.setBackgroundColor(mToolbarColor);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
		outState.putString("json", mJson);
		outState.putString("baseUrl", mBaseUrl);
		outState.putString("collection", mCollection);
	}

	private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.coverart), getString(R.string.backdrop), getString(R.string.collectionart)};

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
				return CoverSearchFragment.newInstance(mTmdbId, mJson, mBaseUrl);
			case 1:
				return FanartSearchFragment.newInstance(mTmdbId, mJson, mBaseUrl);
			default:
				return CollectionCoverSearchFragment.newInstance(mCollectionId, mCollection, mBaseUrl);
			}
		}  

		@Override  
		public int getCount() {
			if (!MizLib.isValidTmdbId(mCollectionId))
				return 2;
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

                mJson = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + params[0] + "/images?api_key=" + mTmdbApiKey).toString();

				if (MizLib.isValidTmdbId(mCollectionId)) {
                    mCollection = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/collection/" + params[1] + "/images?api_key=" + mTmdbApiKey).toString();
				}

				return mJson;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				setupActionBarStuff();
			} else {
				Toast.makeText(getApplicationContext(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void setupActionBarStuff() {
		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(0);

        mProgressBar.setVisibility(View.GONE);
        mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        mTabs.setViewPager(mViewPager);
        mTabs.setVisibility(View.VISIBLE);
	}
}