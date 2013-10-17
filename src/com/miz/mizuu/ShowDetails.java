package com.miz.mizuu;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ActorBrowserFragmentTv;
import com.miz.mizuu.fragments.ShowDetailsFragment;
import com.miz.mizuu.fragments.ShowEpisodesFragment;

public class ShowDetails extends FragmentActivity implements ActionBar.TabListener {

	private ViewPager awesomePager;
	private TvShow thisShow;
	private DbAdapterTvShow dbHelper;
	private boolean ignorePrefixes;
	private ActionBar actionBar;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MizLib.runsInPortraitMode(this))
			setTheme(R.style.Theme_Example_NoBackGround);

		if (!MizLib.runsInPortraitMode(this))
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.viewpager);

		actionBar = getActionBar();
		
		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(3); // Required in order to retain all fragments when swiping between them
		awesomePager.setAdapter(new ShowDetailsAdapter(getSupportFragmentManager()));

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		bar.addTab(bar.newTab()
				.setText(getString(R.string.overview))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.episodes))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.detailsActors))
				.setTabListener(this));

		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bar.getTabAt(position).select();
				ViewParent root = findViewById(android.R.id.content).getParent();
				MizLib.findAndUpdateSpinner(root, position);
				invalidateOptionsMenu();
			}
		});

		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefsIgnorePrefixesInTitles", false);

		// Create and open database
		dbHelper = MizuuApplication.getTvDbAdapter();

		Cursor cursor = dbHelper.getShow(getIntent().getExtras().getString("showId"));
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

		cursor.close();

		if (thisShow == null)
			finish(); // Finish the activity if the movie doesn't load

		try {
			setTitle(thisShow.getTitle());
			actionBar.setSubtitle(thisShow.getFirstAirdateYear());
		} catch (Exception e) {
			finish();
		}
		
		// Set the current page item to 1 (episode page) if the TV show start page setting has been changed from TV show details
		if (!PreferenceManager.getDefaultSharedPreferences(this).getString("prefsTvShowsStartPage", getString(R.string.showDetails)).equals(getString(R.string.showDetails)))
			awesomePager.setCurrentItem(1);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
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
			if (!MizLib.runsInPortraitMode(this) && MizLib.runsOnTablet(this)) {
				getMenuInflater().inflate(R.menu.seasons_tab_landscape, menu);
			} else {
				getMenuInflater().inflate(R.menu.seasons_tab, menu);
			}

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
				i.setClass(getApplicationContext(), MainTvShows.class);
				startActivity(i);

				finish();
			} else			
				onBackPressed();

			return true;
		case R.id.menuDeleteShow:
			deleteShow();
			break;
		case R.id.change_backdrop:
			searchBackdrops();
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

	private void searchBackdrops() {
		Intent i = new Intent();
		i.putExtra("id", thisShow.getId());
		i.putExtra("startPosition", 1);
		i.setClass(this, ShowCoverFanartBrowser.class);
		startActivity(i);
	}

	private void searchCover() {
		Intent i = new Intent();
		i.putExtra("id", thisShow.getId());
		i.putExtra("startPosition", 0);
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
				return ShowEpisodesFragment.newInstance(thisShow.getId());
			default:
				return ActorBrowserFragmentTv.newInstance(thisShow.getId(), true);
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
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		awesomePager.setCurrentItem(getActionBar().getSelectedTab().getPosition(), true);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
}