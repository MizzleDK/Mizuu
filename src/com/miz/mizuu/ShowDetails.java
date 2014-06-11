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
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBrowserFragmentTv;
import com.miz.mizuu.fragments.ShowDetailsFragment;
import com.miz.mizuu.fragments.TvShowSeasonsEpisodesFragment;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.TVSHOWS_START_PAGE;

public class ShowDetails extends MizActivity implements OnNavigationListener {

	private ViewPager awesomePager;
	private TvShow thisShow;
	private DbAdapterTvShow dbHelper;
	private boolean ignorePrefixes;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private ActionBar actionBar;

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

		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(this, spinnerItems);
		
		setTitle(null);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(3); // Required in order to retain all fragments when swiping between them
		awesomePager.setAdapter(new ShowDetailsAdapter(getSupportFragmentManager()));
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
				invalidateOptionsMenu();
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
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
						ignorePrefixes,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1))
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
			awesomePager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		}

		// Set the current page item to 1 (episode page) if the TV show start page setting has been changed from TV show details
		if (!PreferenceManager.getDefaultSharedPreferences(this).getString(TVSHOWS_START_PAGE, getString(R.string.showDetails)).equals(getString(R.string.showDetails)))
			awesomePager.setCurrentItem(1);
	}

	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.overview)));
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.seasons)));
		spinnerItems.add(new SpinnerItem(thisShow.getTitle(), getString(R.string.detailsActors)));

		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", awesomePager.getCurrentItem());
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		switch (awesomePager.getCurrentItem()) {
		case 0:
			getMenuInflater().inflate(R.menu.detailstv, menu);

			try {
				if (thisShow.isFavorite()) {
					menu.findItem(R.id.show_fav).setIcon(R.drawable.fav);
					menu.findItem(R.id.show_fav).setTitle(R.string.menuFavouriteTitleRemove);
				} else {
					menu.findItem(R.id.show_fav).setIcon(R.drawable.reviews);
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
		}
		return false;
	}

	private void identifyShow() {
		ArrayList<String> files = new ArrayList<String>();

		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor cursor = db.getAllEpisodes(thisShow.getId(), DbAdapterTvShowEpisode.OLDEST_FIRST);
		while (cursor.moveToNext())
			files.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)));

		cursor.close();

		Intent i = new Intent();
		i.setClass(this, IdentifyTvShow.class);
		i.putExtra("rowId", "0");
		i.putExtra("files", files.toArray(new String[files.size()]));
		i.putExtra("showName", thisShow.getTitle());
		i.putExtra("oldShowId", thisShow.getId());
		i.putExtra("isShow", true);
		startActivity(i);
	}

	public void favAction(MenuItem item) {
		// Create and open database
		thisShow.setFavorite(!thisShow.isFavorite()); // Reverse the favourite boolean

		if (dbHelper.updateShowSingleItem(thisShow.getId(), DbAdapterTvShow.KEY_SHOW_EXTRA1, thisShow.getFavorite())) {
			invalidateOptionsMenu();

			if (thisShow.isFavorite()) {
				Toast.makeText(this, getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
				setResult(2); // Favorite removed
			}

			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("mizuu-shows-update"));

		} else Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

		new Thread() {
			@Override
			public void run() {
				ArrayList<TvShow> show = new ArrayList<TvShow>();
				show.add(thisShow);
				MizLib.tvShowFavorite(show, getApplicationContext());
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
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("mizuu-shows-update"));
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
				return ShowDetailsFragment.newInstance(thisShow.getId());
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
		if (itemPosition == 0)
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_actionbar));
		else
			actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#aa000000")));
		
		awesomePager.setCurrentItem(itemPosition);
		return true;
	}
}