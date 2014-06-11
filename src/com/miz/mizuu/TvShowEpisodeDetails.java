package com.miz.mizuu;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.TvShowEpisodeDetailsFragment;

public class TvShowEpisodeDetails extends MizActivity {

	private static final String SHOW_ID = "showId";

	private ArrayList<TvShowEpisode> mEpisodes = new ArrayList<TvShowEpisode>();
	private int mSeason, mEpisode;
	private String mShowId, mShowTitle;
	private ViewPager mViewPager;
	private DbAdapterTvShowEpisode mDatabaseHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!MizLib.isPortrait(this))
			if (isFullscreen())
				setTheme(R.style.Theme_Example_Transparent_NoBackGround_FullScreen);
			else
				setTheme(R.style.Theme_Example_Transparent_NoBackGround);
		else
			if (isFullscreen())
				setTheme(R.style.Theme_Example_Transparent_FullScreen);
			else
				setTheme(R.style.Theme_Example_Transparent);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		mShowId = getIntent().getExtras().getString(SHOW_ID);
		mSeason = getIntent().getExtras().getInt("season");
		mEpisode = getIntent().getExtras().getInt("episode");

		mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();
		
		Cursor cursor = mDatabaseHelper.getAllEpisodes(mShowId, DbAdapterTvShowEpisode.OLDEST_FIRST);	
		try {
			while (cursor.moveToNext()) {
				mEpisodes.add(new TvShowEpisode(this,
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
		} catch (Exception e) {
		} finally {
			cursor.close();
		}

		mShowTitle = MizuuApplication.getTvDbAdapter().getShowTitle(mShowId);
		getActionBar().setTitle(mShowTitle);
		
		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setAdapter(new TvShowEpisodeDetailsAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateActionBar(position);
			}
		});

		if (savedInstanceState != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			for (int i = 0; i < mEpisodes.size(); i++) {
				if (mEpisodes.get(i).getSeason().equals(MizLib.addIndexZero(mSeason)) && mEpisodes.get(i).getEpisode().equals(MizLib.addIndexZero(mEpisode))) {
					mViewPager.setCurrentItem(i);
					updateActionBar(i);
					continue;
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
	}
	
	private void updateActionBar(int position) {
		getActionBar().setSubtitle("S" + mEpisodes.get(position).getSeason() + "E" + mEpisodes.get(position).getEpisode());
	}

	private class TvShowEpisodeDetailsAdapter extends FragmentPagerAdapter {

		public TvShowEpisodeDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			return TvShowEpisodeDetailsFragment.newInstance(mShowId, Integer.parseInt(mEpisodes.get(index).getSeason()), Integer.parseInt(mEpisodes.get(index).getEpisode()));
		}  

		@Override  
		public int getCount() {  
			return mEpisodes.size();
		}
	}
}
