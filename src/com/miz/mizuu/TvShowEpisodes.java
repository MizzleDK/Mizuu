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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.miz.base.MizActivity;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.GridSeason;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.mizuu.fragments.TvShowEpisodesFragment;
import com.miz.utils.FileUtils;

public class TvShowEpisodes extends MizActivity {

	private static final String SHOW_ID = "showId";
	private static final String SEASON = "season";
	private static final String EPISODE_COUNT = "episodeCount";
	
	private Context mContext;
	private String mTitle, mShowId;
	private int mSeason, mEpisodeCount;
	private ViewPager mViewPager;
	private SeasonAdapter mSeasonAdapter;
	private List<GridSeason> mItems = new ArrayList<GridSeason>();
	private ProgressBar mProgressBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		MizuuApplication.setupTheme(this);
		
		mContext = this;
		
		setContentView(R.layout.viewpager);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		
		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setAdapter(mSeasonAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				int episodeCount = mItems.get(position).getEpisodeCount();
				getActionBar().setSubtitle(getString(R.string.showSeason) + " " + mItems.get(position).getSeason() + " (" + episodeCount + " " + getResources().getQuantityString(R.plurals.episodes, episodeCount, episodeCount) + ")");
			}
		});
		
		mShowId = getIntent().getExtras().getString(SHOW_ID);
		mTitle = MizuuApplication.getTvDbAdapter().getShowTitle(mShowId);
		mSeason = getIntent().getExtras().getInt(SEASON);
		mEpisodeCount = getIntent().getExtras().getInt(EPISODE_COUNT);
		
		setTitle(mTitle);
		getActionBar().setSubtitle(getString(R.string.showSeason) + " " + mSeason + " (" + mEpisodeCount + " " + getResources().getQuantityString(R.plurals.episodes, mEpisodeCount, mEpisodeCount) + ")");
		
		loadSeasons();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	public class SeasonAdapter extends FragmentPagerAdapter {

		public SeasonAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Fragment getItem(int arg0) {
			return TvShowEpisodesFragment.newInstance(mShowId, mItems.get(arg0).getSeason());
		}
	}
	
	private void loadSeasons() {
		new SeasonLoader().execute();
	}
	
	private class SeasonLoader extends LibrarySectionAsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			mItems.clear();
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			HashMap<String, EpisodeCounter> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(mShowId);

			File temp = null;
			for (String key : seasons.keySet()) {
				temp = FileUtils.getTvShowSeason(mContext, mShowId, key);				
				mItems.add(new GridSeason(mContext, Integer.valueOf(key), seasons.get(key).getEpisodeCount(), seasons.get(key).getWatchedCount(),
						temp.exists() ? temp :
							FileUtils.getTvShowThumb(mContext, mShowId)));
			}

			seasons.clear();
			seasons = null;

			Collections.sort(mItems);

			return null;
		}

		@Override
		public void onPostExecute(Void result) {
			mProgressBar.setVisibility(View.GONE);
			
			mSeasonAdapter = new SeasonAdapter(getSupportFragmentManager());
			mViewPager.setAdapter(mSeasonAdapter);
			
			for (int i = 0; i < mItems.size(); i++) {
				if (mItems.get(i).getSeason() == mSeason) {
					mViewPager.setCurrentItem(i);
					break;
				}
			}
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				setResult(Activity.RESULT_OK);
				finish();
			}
		}
	}
}