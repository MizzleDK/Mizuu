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
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.functions.CoverItem;
import com.miz.functions.MenuItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.MovieDiscoveryViewPagerFragment;
import com.miz.mizuu.fragments.MovieLibraryFragment;
import com.miz.mizuu.fragments.TvShowLibraryFragment;
import com.miz.mizuu.fragments.WebVideosViewPagerFragment;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

@SuppressLint("NewApi")
public class Main extends MizActivity {

	public static final int MOVIES = 0, SHOWS = 1, WATCHLIST = 2, WEB_MOVIES = 3, WEB_VIDEOS = 4;
	private int mNumMovies, mNumShows, mNumWatchlist, selectedIndex;
	private Typeface tf, tfCondensed, tfLight;
	private DrawerLayout mDrawerLayout;
	protected ListView mDrawerList;
	private TextView tab1, tab2;
	private ActionBarDrawerToggle mDrawerToggle;
	private DbAdapter dbHelper;
	private DbAdapterTvShow dbHelperTv;
	private boolean confirmExit, hasTriedOnce = false;
	private String startup;
	private ArrayList<MenuItem> menu = new ArrayList<MenuItem>(), thirdPartyApps = new ArrayList<MenuItem>();
	private View mDrawerUserInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MizuuApplication.setupTheme(this);

		setContentView(R.layout.menu_drawer);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		confirmExit = settings.getBoolean("prefsConfirmBackPress", false);
		startup = settings.getString("prefsStartup", "1");
		if (startup.equals("0"))
			startup = "1";

		dbHelper = MizuuApplication.getMovieAdapter();
		dbHelperTv = MizuuApplication.getTvDbAdapter();


		tfCondensed = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "RobotoCondensed-Regular.ttf");
		tf = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "Roboto-Medium.ttf");
		tfLight = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "Roboto-Light.ttf");

		setupMenuItems();

		mDrawerUserInfo = findViewById(R.id.drawer_user_info);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_list_shadow, GravityCompat.START);

		if ((!MizLib.isTablet(this) && !MizLib.isPortrait(this)) || MizLib.isGoogleTV(this)) {
			findViewById(R.id.personalizedArea).setVisibility(View.GONE);
		} else
			setupUserDetails();

		((TextView) findViewById(R.id.username)).setTextSize(26f);
		((TextView) findViewById(R.id.username)).setTypeface(tfCondensed);

		tab1 = (TextView) findViewById(R.id.tab1);
		tab1.setTextSize(12f);
		tab2 = (TextView) findViewById(R.id.tab2);
		tab2.setTextSize(12f);

		mDrawerList = (ListView) findViewById(R.id.listView1);
		mDrawerList.setAdapter(new MenuAdapter());
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (tab1.isSelected()) {
					loadFragment(arg2 + 1);
				} else {
					final PackageManager pm = getPackageManager();
					Intent i = pm.getLaunchIntentForPackage(thirdPartyApps.get(arg2).getPackageName());
					if (i != null) {
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					}
					mDrawerList.setItemChecked(arg2, false);
				}
			}
		});
		mDrawerList.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		getActionBar().setDisplayHomeAsUpEnabled(true);
		if (MizLib.hasICS())
			getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description for accessibility */
				R.string.drawer_close  /* "close drawer" description for accessibility */
				) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(null);
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				invalidateOptionsMenu();
				if (MizLib.isGoogleTV(getApplicationContext()))
					mDrawerList.requestFocus();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState != null && savedInstanceState.containsKey("tabIndex")) {
			selectedIndex = savedInstanceState.getInt("selectedIndex");
			changeTabSelection(savedInstanceState.getInt("tabIndex"));
			loadFragment(selectedIndex + 1);
		} else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("startup")) {
			tab1.setSelected(true);
			loadFragment(Integer.parseInt(getIntent().getExtras().getString("startup")));
		} else {
			tab1.setSelected(true);
			loadFragment(Integer.parseInt(startup));
		}

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-update"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-update"));
	}

	private void loadFragment(int type) {
		type--; // Since the prefsStartup preference starts at 0 for Welcome, and I'm using 0 for Movies here, we subtract one from type.

		setTitle(null);

		Fragment frag = getSupportFragmentManager().findFragmentByTag("frag" + type);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
			switch (type) {
			case MOVIES:
				ft.replace(R.id.content_frame, MovieLibraryFragment.newInstance(MovieLibraryFragment.MAIN), "frag" + type);
				break;
			case SHOWS:
				ft.replace(R.id.content_frame, TvShowLibraryFragment.newInstance(), "frag" + type);
				break;
			case WATCHLIST:
				ft.replace(R.id.content_frame, MovieLibraryFragment.newInstance(MovieLibraryFragment.OTHER), "frag" + type);
				break;
			case WEB_MOVIES:
				ft.replace(R.id.content_frame, MovieDiscoveryViewPagerFragment.newInstance(), "frag" + type);
				break;
			case WEB_VIDEOS:
				ft.replace(R.id.content_frame, WebVideosViewPagerFragment.newInstance(), "frag" + type);
				break;
			}
			ft.commit();
		}

		selectListIndex(type);

		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawers();
	}

	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);

		if (!newIntent.hasExtra("fromUpdate")) {
			Intent i = null;
			if (selectedIndex == MOVIES)
				i = new Intent("mizuu-movie-actor-search");
			else // TV shows
				i = new Intent("mizuu-shows-actor-search");
			i.putExtras(newIntent.getExtras());
			LocalBroadcastManager.getInstance(this).sendBroadcast(i);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("tabIndex", tab1.isSelected() ? 0 : 1);
		outState.putInt("selectedIndex", selectedIndex);
	}

	private void setupUserDetails() {
		String filepath = MizLib.getLatestBackdropPath(getApplicationContext());

		if (!filepath.isEmpty())
			Picasso.with(getApplicationContext()).load("file://" + filepath).resize(MizLib.convertDpToPixels(getApplicationContext(), 320), MizLib.convertDpToPixels(getApplicationContext(), 170)).into(((ImageView) findViewById(R.id.userCover)), new Callback() {
				@Override
				public void onError() {
					((ImageView) findViewById(R.id.userCover)).setImageResource(R.drawable.gray);
				}

				@Override
				public void onSuccess() {}
			});
		else
			((ImageView) findViewById(R.id.userCover)).setImageResource(R.drawable.gray);

		String full_name = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("traktFullName", "");
		if (!MizLib.isEmpty(full_name))
			((TextView) findViewById(R.id.username)).setText(full_name);
		else
			mDrawerUserInfo.setVisibility(View.GONE);

		if (!MizLib.isEmpty(full_name)) {
			CoverItem c = new CoverItem();
			c.cover = ((ImageView) findViewById(R.id.userPhoto));
			Picasso.with(getApplicationContext()).load("file://" + new File(MizLib.getCacheFolder(getApplicationContext()), "avatar.jpg").getAbsolutePath()).resize(MizLib.convertDpToPixels(getApplicationContext(), 80), MizLib.convertDpToPixels(getApplicationContext(), 80)).error(R.drawable.unknown_user).into(c);
		}
	}

	private void changeTabSelection(int index) {
		if (index == 0) {
			tab1.setSelected(true);
			tab2.setSelected(false);
			((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
			mDrawerList.setItemChecked(selectedIndex, true);
		} else {
			tab1.setSelected(false);
			tab2.setSelected(true);
			((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
			selectedIndex = mDrawerList.getCheckedItemPosition();
			mDrawerList.setItemChecked(mDrawerList.getCheckedItemPosition(), false);
		}
	}

	public void myLibraries(View v) {
		changeTabSelection(0);
	}

	public void mediaApps(View v) {
		changeTabSelection(1);
	}

	private void setupThirdPartyApps() {
		thirdPartyApps.clear();

		final PackageManager pm = getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		for (int i = 0; i < packages.size(); i++) {
			if (MizLib.isMediaApp(packages.get(i))) {
				thirdPartyApps.add(new MenuItem(pm.getApplicationLabel(packages.get(i)).toString(), 0, false, packages.get(i).packageName));
			}
		}

		Collections.sort(thirdPartyApps, new Comparator<MenuItem>() {
			@Override
			public int compare(MenuItem o1, MenuItem o2) {
				return o1.getTitle().compareToIgnoreCase(o2.getTitle());
			}
		});
	}

	private void setupMenuItems() {
		menu.clear();

		menu.add(new MenuItem(getString(R.string.drawerMyMovies), mNumMovies, false));
		menu.add(new MenuItem(getString(R.string.drawerMyTvShows), mNumShows, false));
		menu.add(new MenuItem(getString(R.string.chooserWatchList), mNumWatchlist, false));
		menu.add(new MenuItem(getString(R.string.drawerOnlineMovies), 0, false));
		menu.add(new MenuItem(getString(R.string.drawerWebVideos), 0, false));

		setupThirdPartyApps();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateLibraryCounts();
		}
	};

	protected void selectListIndex(int index) {
		if (index == -1)
			index = 0;
		if (!menu.get(index).isThirdPartyApp()) {
			selectedIndex = index;
			mDrawerList.setItemChecked(index, true);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		updateLibraryCounts();
	}

	private void updateLibraryCounts() {
		new Thread() {
			@Override
			public void run() {
				try {					
					mNumMovies = dbHelper.count();
					mNumWatchlist = dbHelper.countWatchlist();
					mNumShows = dbHelperTv.count();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setupMenuItems();
							((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
						}
					});
				} catch (Exception e) {} // Problemer med at kontakte databasen
			}
		}.start();
	}

	@Override
	public void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch(item.getItemId()) {
		case android.R.id.home:
			if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.openDrawer(mDrawerList);
			} else {
				mDrawerLayout.closeDrawer(mDrawerList);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	public class MenuAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return tab1.isSelected() ? menu.size() : thirdPartyApps.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.menu_drawer_item, null);
			TextView title = (TextView) convertView.findViewById(R.id.title);
			if (MizLib.isTablet(getApplicationContext()))
				title.setTextSize(22f);
			TextView description = (TextView) convertView.findViewById(R.id.count);

			description.setTypeface(tfLight);
			description.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			if (position == mDrawerList.getCheckedItemPosition()) {
				title.setTypeface(tf, Typeface.BOLD);
			} else {
				title.setTypeface(tfLight);
			}
			title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			title.setText(getMenuItem(position).getTitle());

			if (getMenuItem(position).getCount() > 0)
				description.setText(String.valueOf(getMenuItem(position).getCount()));
			else
				description.setVisibility(View.GONE);

			return convertView;
		}

		private MenuItem getMenuItem(int position) {
			if (tab1.isSelected())
				return menu.get(position);
			return thirdPartyApps.get(position);
		}
	}

	@Override
	public void onBackPressed() {
		if (startup.equals("0") && !mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer)) && MizLib.isTablet(this)) { // Welcome screen
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.setClass(getApplicationContext(), Welcome.class);
			startActivity(i);
			finish();
			return;
		}

		if (!mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer)) && confirmExit) {
			if (hasTriedOnce) {
				super.onBackPressed();
			} else {
				Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
				hasTriedOnce = true;
			}
		} else {
			if (MizLib.isGoogleTV(this))
				mDrawerLayout.closeDrawers();
			else
				super.onBackPressed();
		}
	}

	public void showDrawerOptionsMenu(Menu menu, MenuInflater inflater) {
		getActionBar().setTitle(R.string.app_name);
		inflater.inflate(R.menu.drawer, menu);
	}

	public boolean isDrawerOpen() {
		View v = findViewById(R.id.left_drawer);
		if (v == null)
			return false;
		return mDrawerLayout.isDrawerOpen(v);
	}
}