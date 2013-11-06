package com.miz.mizuu;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;

import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ShowEpisodeDetailsFragment;

public class EpisodeDetails extends FragmentActivity {

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
		if (!MizLib.runsInPortraitMode(this) && MizLib.runsOnTablet(this)) {
			finish();
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