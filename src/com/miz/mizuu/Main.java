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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MenuItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.MovieDiscoveryViewPagerFragment;
import com.miz.mizuu.fragments.MovieLibraryFragment;
import com.miz.mizuu.fragments.TvShowLibraryFragment;
import com.miz.mizuu.fragments.WebVideosViewPagerFragment;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.TRAKT_FULL_NAME;
import static com.miz.functions.PreferenceKeys.STARTUP_SELECTION;
import static com.miz.functions.PreferenceKeys.CONFIRM_BACK_PRESS;

@SuppressLint("NewApi")
public class Main extends MizActivity {

	public static final int MOVIES = 1, SHOWS = 2, WATCHLIST = 3, WEB_MOVIES = 4, WEB_VIDEOS = 5;
	private int mNumMovies, mNumShows, mNumWatchlist, selectedIndex, mStartup;
	private Typeface mTfMedium, mTfCondensed, mTfLight;
	private DrawerLayout mDrawerLayout;
	protected ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private DbAdapter mDbHelper;
	private DbAdapterTvShow mDbHelperTv;
	private boolean mConfirmExit, mTriedOnce = false;
	private ArrayList<MenuItem> mMenuItems = new ArrayList<MenuItem>();
	private List<ApplicationInfo> mApplicationList;
	private Picasso mPicasso;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MizuuApplication.setupTheme(this);

		setContentView(R.layout.menu_drawer);

		mPicasso = MizuuApplication.getPicasso(getApplicationContext());

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		mConfirmExit = settings.getBoolean(CONFIRM_BACK_PRESS, false);
		mStartup = Integer.valueOf(settings.getString(STARTUP_SELECTION, "1"));

		mDbHelper = MizuuApplication.getMovieAdapter();
		mDbHelperTv = MizuuApplication.getTvDbAdapter();

		mTfCondensed = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "RobotoCondensed-Regular.ttf");
		mTfMedium = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "Roboto-Medium.ttf");
		mTfLight = MizuuApplication.getOrCreateTypeface(getApplicationContext(), "Roboto-Light.ttf");

		setupMenuItems(true);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_list_shadow, GravityCompat.START);

		mDrawerList = (ListView) findViewById(R.id.listView1);
		mDrawerList.setAdapter(new MenuAdapter());
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				switch (mMenuItems.get(arg2).getType()) {
				case MenuItem.SECTION:
					loadFragment(mMenuItems.get(arg2).getFragment());
					break;
				case MenuItem.THIRD_PARTY_APP:
					final PackageManager pm = getPackageManager();
					Intent i = pm.getLaunchIntentForPackage(mMenuItems.get(arg2).getPackageName());
					if (i != null) {
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					}
					break;
				}
			}
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
		if (savedInstanceState != null && savedInstanceState.containsKey("selectedIndex")) {
			selectedIndex = savedInstanceState.getInt("selectedIndex");
			loadFragment(selectedIndex);
		} else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("startup")) {
			loadFragment(Integer.parseInt(getIntent().getExtras().getString("startup")));
		} else {
			loadFragment(mStartup);
		}

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
	}

	private void loadFragment(int type) {
		if (type == 0)
			type = 1;
		
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

		outState.putInt("selectedIndex", selectedIndex);
	}

	private void setupMenuItems(boolean refreshThirdPartyApps) {
		mMenuItems.clear();

		// Menu header
		// We only want to add a header if there's a backdrop image and we're not running on Google TV or a non-tablet in landscape mode
		if (!MizLib.getLatestBackdropPath(getApplicationContext()).isEmpty() && !((!MizLib.isTablet(this) && !MizLib.isPortrait(this)) || MizLib.isGoogleTV(this)))
			mMenuItems.add(new MenuItem(null, -1, MenuItem.HEADER, null));
		
		// Regular menu items
		mMenuItems.add(new MenuItem(getString(R.string.drawerMyMovies), mNumMovies, MenuItem.SECTION, null, MOVIES));
		mMenuItems.add(new MenuItem(getString(R.string.drawerMyTvShows), mNumShows, MenuItem.SECTION, null, SHOWS));
		mMenuItems.add(new MenuItem(getString(R.string.chooserWatchList), mNumWatchlist, MenuItem.SECTION, null, WATCHLIST));
		mMenuItems.add(new MenuItem(getString(R.string.drawerOnlineMovies), -1, MenuItem.SECTION, null, WEB_MOVIES));
		mMenuItems.add(new MenuItem(getString(R.string.drawerWebVideos), -1, MenuItem.SECTION, null, WEB_VIDEOS));

		// Third party applications
		final PackageManager pm = getPackageManager();

		if (refreshThirdPartyApps) {
			mApplicationList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		}

		List<MenuItem> temp = new ArrayList<MenuItem>();
		for (int i = 0; i < mApplicationList.size(); i++) {
			if (MizLib.isMediaApp(mApplicationList.get(i))) {
				temp.add(new MenuItem(pm.getApplicationLabel(mApplicationList.get(i)).toString(), -1, MenuItem.THIRD_PARTY_APP, mApplicationList.get(i).packageName));
			}
		}

		if (temp.size() > 0)
			// Menu section header
			mMenuItems.add(new MenuItem(getString(R.string.installed_media_apps), -1, MenuItem.SEPARATOR, null));

		Collections.sort(temp, new Comparator<MenuItem>() {
			@Override
			public int compare(MenuItem lhs, MenuItem rhs) {
				return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
			}
		});

		for (int i = 0; i < temp.size(); i++) {
			mMenuItems.add(temp.get(i));
		}

		temp.clear();
		temp = null;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateLibraryCounts();
		}
	};

	protected void selectListIndex(int index) {
		if (!(!MizLib.getLatestBackdropPath(getApplicationContext()).isEmpty() && !((!MizLib.isTablet(this) && !MizLib.isPortrait(this)) || MizLib.isGoogleTV(this))))
			index--;
		
		if (mMenuItems.get(index).getType() == MenuItem.SECTION) {
			selectedIndex = mMenuItems.get(index).getFragment();
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
					mNumMovies = mDbHelper.count();
					mNumWatchlist = mDbHelper.countWatchlist();
					mNumShows = mDbHelperTv.count();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setupMenuItems(false);
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

		private boolean mTablet;
		private LayoutInflater mInflater;

		public MenuAdapter() {
			mInflater = LayoutInflater.from(getApplicationContext());
			mTablet = MizLib.isTablet(getApplicationContext());
		}

		@Override
		public int getCount() {
			return mMenuItems.size();
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
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			switch (mMenuItems.get(position).getType()) {
			case MenuItem.HEADER:
				return 0;
			case MenuItem.SEPARATOR:
				return 1;
			default:
				return 2;
			}
		}

		@Override
		public boolean isEnabled(int position) {
			if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR || mMenuItems.get(position).getType() == MenuItem.HEADER)
				return false;
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (mMenuItems.get(position).getType() == MenuItem.HEADER) {
				convertView = mInflater.inflate(R.layout.menu_drawer_header, parent, false);
				
				final String fullName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(TRAKT_FULL_NAME, "");
				final ImageView backgroundImage = ((ImageView) convertView.findViewById(R.id.userCover));
				final ImageView userImage = ((ImageView) convertView.findViewById(R.id.userPhoto));
				final TextView userName = ((TextView) convertView.findViewById(R.id.username));
				
				userName.setTextSize(26f);
				userName.setTypeface(mTfCondensed);
				
				if (!MizLib.isEmpty(fullName)) {
					userName.setText(fullName);
					mPicasso.load(new File(MizLib.getCacheFolder(getApplicationContext()), "avatar.jpg")).resize(MizLib.convertDpToPixels(getApplicationContext(), 72), MizLib.convertDpToPixels(getApplicationContext(), 72)).error(R.drawable.unknown_user).into(userImage);
				} else
					convertView.findViewById(R.id.drawer_user_info).setVisibility(View.GONE);

				mPicasso.load(MizLib.getLatestBackdropPath(getApplicationContext())).resize(MizLib.convertDpToPixels(getApplicationContext(), 320), MizLib.convertDpToPixels(getApplicationContext(), 180)).into(backgroundImage);
				
			} else if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR) {	
				convertView = mInflater.inflate(R.layout.menu_drawer_header_item, parent, false);
				TextView title = (TextView) convertView.findViewById(R.id.title);
				title.setText(mMenuItems.get(position).getTitle());
			} else {
				convertView = mInflater.inflate(R.layout.menu_drawer_item, parent, false);

				// Title
				TextView title = (TextView) convertView.findViewById(R.id.title);
				title.setText(mMenuItems.get(position).getTitle());				
				if (mMenuItems.get(position).getFragment() == selectedIndex)
					title.setTypeface(mTfMedium, Typeface.BOLD);
				else
					title.setTypeface(mTfLight);
				title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

				// Tablets need slightly larger text size :-)
				if (mTablet)
					title.setTextSize(22f);

				// Description
				TextView description = (TextView) convertView.findViewById(R.id.count);
				description.setTypeface(mTfLight);
				description.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

				if (mMenuItems.get(position).getCount() >= 0)
					description.setText(String.valueOf(mMenuItems.get(position).getCount()));
				else
					description.setVisibility(View.GONE);
			}

			return convertView;
		}
	}

	@Override
	public void onBackPressed() {
		if (mStartup == 0 && !mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer)) && MizLib.isTablet(this)) { // Welcome screen
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.setClass(getApplicationContext(), Welcome.class);
			startActivity(i);
			finish();
			return;
		}

		if (!mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer))) {
			if (mConfirmExit) {
				if (mTriedOnce) {
					finish();
				} else {
					Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
					mTriedOnce = true;
				}
			} else {
				finish();
			}
		} else {
			mDrawerLayout.closeDrawers();
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