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

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisode;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;

import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ShowEpisodeDetailsFragment;

public class EpisodeDetails extends MizActivity {

	private ViewPager awesomePager;
	private ArrayList<TvShowEpisode> episodes = new ArrayList<TvShowEpisode>();
	private DbAdapterTvShowEpisode db;
	private boolean showOldestEpisodeFirst;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.viewpager);

		showOldestEpisodeFirst = PreferenceManager.getDefaultSharedPreferences(this).getString("prefsEpisodesOrder", getString(R.string.oldestFirst)).equals(getString(R.string.oldestFirst)) ? true : false;
		
		db = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor cursor = db.getAllEpisodes(getIntent().getExtras().getString("showId"), showOldestEpisodeFirst ? DbAdapterTvShowEpisode.OLDEST_FIRST : DbAdapterTvShowEpisode.NEWEST_FIRST);	
		while (cursor.moveToNext()) {
			episodes.add(new TvShowEpisode(this,
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_DIRECTOR)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_WRITER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_GUESTSTARS)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EXTRA_1))
					));
		}

		cursor.close();

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
		awesomePager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				getActionBar().setTitle(episodes.get(arg0).getTitle());
				getActionBar().setSubtitle("S" + episodes.get(arg0).getSeason() + "E" + episodes.get(arg0).getEpisode());
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
		});

		// This is safe
		getActionBar().setTitle(episodes.get(0).getTitle());
		getActionBar().setSubtitle("S" + episodes.get(0).getSeason() + "E" + episodes.get(0).getEpisode());

		for (int i = 0; i < episodes.size(); i++) {
			if (episodes.get(i).getRowId().equals(getIntent().getExtras().getString("rowId"))) {
				awesomePager.setCurrentItem(i);
				continue;
			}
		}

		// This gets called on screen rotation as well
		if (!MizLib.isPortrait(this) && MizLib.isTablet(this)) {
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
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

	private class PagerAdapter extends FragmentPagerAdapter {

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			return ShowEpisodeDetailsFragment.newInstance(episodes.get(index).getRowId());
		}  

		@Override  
		public int getCount() {  
			return episodes.size();  
		}
	}
}