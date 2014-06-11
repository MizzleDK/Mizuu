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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.miz.base.MizActivity;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.TvShowEpisodesFragment;

public class TvShowEpisodes extends MizActivity {

	private static final String SHOW_ID = "showId";
	private static final String SEASON = "season";
	private static final String EPISODE_COUNT = "episodeCount";
	
	private String mTitle, mShowId;
	private int mSeason, mEpisodeCount;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (MizLib.isTablet(this) && !MizLib.isPortrait(this))
			finish(); // Return to the two-pane layout
		
		MizuuApplication.setupTheme(this);
		
		mShowId = getIntent().getExtras().getString(SHOW_ID);
		mTitle = MizuuApplication.getTvDbAdapter().getShowTitle(mShowId);
		mSeason = getIntent().getExtras().getInt(SEASON);
		mEpisodeCount = getIntent().getExtras().getInt(EPISODE_COUNT);
		
		setTitle(mTitle);
		getActionBar().setSubtitle(getString(R.string.showSeason) + " " + mSeason + " (" + mEpisodeCount + " " + getResources().getQuantityString(R.plurals.episodes, mEpisodeCount, mEpisodeCount) + ")");
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TvShowEpisodesFragment.class.toString());
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(android.R.id.content, TvShowEpisodesFragment.newInstance(mShowId, mSeason), TvShowEpisodesFragment.class.toString());
			ft.commit();
		}
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
}