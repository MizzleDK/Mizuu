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

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShows;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBrowserFragmentTv;
import com.miz.mizuu.fragments.TvShowDetailsFragment;
import com.miz.mizuu.fragments.TvShowSeasonsEpisodesFragment;
import com.miz.utils.LocalBroadcastUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.TVSHOWS_START_PAGE;

public class TvShowDetails extends MizActivity implements OnNavigationListener {

	private ViewPager mViewPager;
	private TvShow thisShow;
	private DbAdapterTvShows dbHelper;
	private boolean ignorePrefixes;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private ActionBar mActionBar;
	private Bus mBus;
	private Drawable mActionBarBackgroundDrawable;
	private ImageView mActionBarOverlay;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBus = MizuuApplication.getBus();

		if (isFullscreen())
			setTheme(R.style.Mizuu_Theme_Transparent_NoBackGround_FullScreen);
		else
			setTheme(R.style.Mizuu_Theme_NoBackGround_Transparent);

		if (MizLib.isPortrait(this)) {
			getWindow().setBackgroundDrawableResource(R.drawable.bg);
		}

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		mActionBarOverlay = (ImageView) findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));

		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(this, spinnerItems);

		setTitle(null);

		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setOffscreenPageLimit(3); // Required in order to retain all fragments when swiping between them
		mViewPager.setAdapter(new ShowDetailsAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mActionBar.setSelectedNavigationItem(position);
				invalidateOptionsMenu();

				updateActionBarDrawable(1, false, false);
			}
		});

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IGNORED_TITLE_PREFIXES, false);

		// Create and open database
		dbHelper = MizuuApplication.getTvDbAdapter();

		String showId = "";
		// Fetch the database ID of the TV show to view
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			showId = getIntent().getStringExtra(SearchManager.EXTRA_DATA_KEY);
		} else {
			showId = getIntent().getStringExtra("showId");
		}

		Cursor cursor = dbHelper.getShow(showId);
		try {
			while (cursor.moveToNext()) {
				thisShow = new TvShow(this,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ACTORS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RUNTIME)),
						ignorePrefixes,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
						MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)))
						);
			}
		} catch (Exception e) {
			finish();
			return;
		} finally {
			cursor.close();
		}

		if (thisShow == null) {
			finish(); // Finish the activity if the movie doesn't load
			return;
		}

		setupSpinnerItems();

		if (savedInstanceState != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		}

		// Set the current page item to 1 (episode page) if the TV show start page setting has been changed from TV show details
		if (!PreferenceManager.getDefaultSharedPreferences(this).getString(TVSHOWS_START_PAGE, getString(R.string.showDetails)).equals(getString(R.string.showDetails)))
			mViewPager.setCurrentItem(1);
	}

	@Subscribe
	public void onScrollChanged(Integer newAlpha) {
		updateActionBarDrawable(newAlpha, true, false);
	}

	private void updateActionBarDrawable(int newAlpha, boolean setBackground, boolean showActionBar) {
		if (mViewPager.getCurrentItem() == 0) { // Details page
			mActionBarOverlay.setVisibility(View.VISIBLE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				if (newAlpha == 0) {
					mActionBar.hide();
					mActionBarOverlay.setVisibility(View.GONE);
				} else
					mActionBar.show();

			if (setBackground) {
				mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
				mActionBarOverlay.setImageDrawable(mActionBarBackgroundDrawable);
			}
		} else { // Actors page
			mActionBarOverlay.setVisibility(View.GONE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				mActionBar.show();
		}

		if (showActionBar) {
			mActionBar.show();
		}
	}

	public void onResume() {
		super.onResume();

		mBus.register(this);
		updateActionBarDrawable(1, true, true);
	}

	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}

	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.overview)));
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.seasons)));
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.detailsActors)));

		mActionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		switch (mViewPager.getCurrentItem()) {
		case 0:
			getMenuInflater().inflate(R.menu.tv_show_details, menu);

			try {
				if (thisShow.isFavorite()) {
					menu.findItem(R.id.show_fav).setIcon(R.drawable.fav);
					menu.findItem(R.id.show_fav).setTitle(R.string.menuFavouriteTitleRemove);
				} else {
					menu.findItem(R.id.show_fav).setIcon(R.drawable.ic_action_star_0);
					menu.findItem(R.id.show_fav).setTitle(R.string.menuFavouriteTitle);
				}

			} catch (Exception e) {} // This can happen if thisShow is null for whatever reason
			break;

		case 1:
			getMenuInflater().inflate(R.menu.seasons_tab, menu);

			break;
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (getIntent().getExtras().getBoolean("isFromWidget")) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				i.putExtra("startup", String.valueOf(Main.SHOWS));
				i.setClass(getApplicationContext(), Main.class);
				startActivity(i);
			}
			finish();

			return true;
		case R.id.menuDeleteShow:
			deleteShow();
			break;
		case R.id.change_cover:
			searchCover();
			break;
		case R.id.identify_show:
			identifyShow();
			break;
		case R.id.openInBrowser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW);
			if (thisShow.getIdType() == TvShow.TMDB) {
				browserIntent.setData(Uri.parse("https://www.themoviedb.org/tv/" + thisShow.getIdWithoutHack()));
			} else {
				browserIntent.setData(Uri.parse("http://thetvdb.com/?tab=series&id=" + thisShow.getId()));
			}
			startActivity(browserIntent);
			break;
		}
		return false;
	}

	private void identifyShow() {
		ArrayList<String> files = new ArrayList<String>();

		Cursor cursor = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllFilepaths(thisShow.getId());
		while (cursor.moveToNext())
			files.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));

		cursor.close();

		Intent i = new Intent();
		i.setClass(this, IdentifyTvShow.class);
		i.putExtra("showTitle", thisShow.getTitle());
		i.putExtra("showId", thisShow.getId());
		startActivityForResult(i, 0);
	}

	public void favAction(MenuItem item) {
		// Create and open database
		thisShow.setFavorite(!thisShow.isFavorite()); // Reverse the favourite boolean

		if (dbHelper.updateShowSingleItem(thisShow.getId(), DbAdapterTvShows.KEY_SHOW_FAVOURITE, thisShow.getFavorite())) {
			invalidateOptionsMenu();

			if (thisShow.isFavorite()) {
				Toast.makeText(this, getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
				setResult(2); // Favorite removed
			}

			LocalBroadcastUtils.updateTvShowLibrary(this);

		} else Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

		new Thread() {
			@Override
			public void run() {
				ArrayList<TvShow> show = new ArrayList<TvShow>();
				show.add(thisShow);
				Trakt.tvShowFavorite(show, getApplicationContext());
			}
		}.start();
	}

	private void searchCover() {
		Intent i = new Intent();
		i.putExtra("id", thisShow.getId());
		i.setClass(this, ShowCoverFanartBrowser.class);
		startActivity(i);
	}

	private void deleteShow() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.areYouSure))
		.setTitle(getString(R.string.removeShow))
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				MizLib.deleteShow(getApplicationContext(), thisShow, true);
				LocalBroadcastUtils.updateTvShowLibrary(getApplicationContext());
				finish();
				return;
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.create().show();
	}

	private class ShowDetailsAdapter extends FragmentPagerAdapter {

		public ShowDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return TvShowDetailsFragment.newInstance(thisShow.getId());
			case 1:
				return TvShowSeasonsEpisodesFragment.newInstance(thisShow.getId());
			default:
				return ActorBrowserFragmentTv.newInstance(thisShow.getId());
			}
		}  

		@Override  
		public int getCount() {  
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0: return getString(R.string.overview);
			case 1: return getString(R.string.episodes);
			default: return getString(R.string.detailsActors);
			}
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition);

		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				finish();
			}
		}
	}
}