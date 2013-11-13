package com.miz.mizuu;

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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.functions.MenuItem;
import com.miz.functions.MizLib;

@SuppressLint("NewApi")
public class MainMenuActivity extends MizActivity implements OnSharedPreferenceChangeListener {

	public static final int MOVIES = 1, SHOWS = 2, WATCHLIST = 3, WEB_MOVIES = 5, WEB_VIDEOS = 6;
	private int mNumMovies, mNumShows, mNumWatchlist;
	private Typeface tf;
	private DrawerLayout mDrawerLayout;
	protected ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private DbAdapter dbHelper;
	private DbAdapterTvShow dbHelperTv;
	private boolean confirmExit, hasTriedOnce = false, showMediaApps;
	private String startup;
	private ArrayList<MenuItem> menu = new ArrayList<MenuItem>(), thirdPartyApps = new ArrayList<MenuItem>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.drawer_test);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		confirmExit = settings.getBoolean("prefsConfirmBackPress", false);
		startup = settings.getString("prefsStartup", "1");
		showMediaApps = settings.getBoolean("showMediaApps", true);
		settings.registerOnSharedPreferenceChangeListener(this);

		dbHelper = MizuuApplication.getMovieAdapter();
		dbHelperTv = MizuuApplication.getTvDbAdapter();

		tf = Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");

		setupMenuItems();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_list_shadow, GravityCompat.START);

		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(new MenuAdapter());
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (menu.get(arg2).isThirdPartyApp()) {
					final PackageManager pm = getPackageManager();
					Intent i = pm.getLaunchIntentForPackage(menu.get(arg2).getPackageName());
					if (i != null) {
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					}
				} else {
					Intent i = new Intent();
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
					i.setClass(getApplicationContext(), menu.get(arg2).getClassName());
					startActivity(i);
					overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
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
			public void onDrawerClosed(View view) {}

			public void onDrawerOpened(View drawerView) {}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-update"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-update"));
	}

	private void setupThirdPartyApps() {
		thirdPartyApps.clear();

		final PackageManager pm = getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		for (ApplicationInfo ai : packages) {
			if (MizLib.isMediaApp(ai)) {
				thirdPartyApps.add(new MenuItem(pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai), 0, false, ai.packageName));
			}
		}

		Collections.sort(thirdPartyApps, new Comparator<MenuItem>() {
			@Override
			public int compare(MenuItem o1, MenuItem o2) {
				return o1.getStringTitle().compareToIgnoreCase(o2.getStringTitle());
			}
		});
	}

	private void setupMenuItems() {
		menu.clear();
		menu.add(new MenuItem(R.string.stringLocal, 0, 0, true, null));
		menu.add(new MenuItem(R.string.chooserMovies, R.drawable.ic_action_movie, mNumMovies, false, MainMovies.class));
		menu.add(new MenuItem(R.string.chooserTVShows, R.drawable.ic_action_tv, mNumShows, false, MainTvShows.class));
		menu.add(new MenuItem(R.string.chooserWatchList, R.drawable.ic_action_list_2, mNumWatchlist, false, MainWatchlist.class));
		menu.add(new MenuItem(R.string.stringDiscover, 0, 0, true, null));
		menu.add(new MenuItem(R.string.chooserMovies, R.drawable.ic_action_movie, 0, false, MovieDiscovery.class));
		menu.add(new MenuItem(R.string.chooserWebVideos, R.drawable.ic_action_globe, 0, false, MainWeb.class));

		if (showMediaApps) {
			setupThirdPartyApps();
			if (thirdPartyApps.size() > 0) {
				menu.add(new MenuItem(R.string.installed_media_apps, 0, 0, true, null));
				menu.addAll(thirdPartyApps);
			}
		}
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateLibraryCounts();
		}
	};

	protected void selectListIndex(int index) {
		if (!menu.get(index).isThirdPartyApp())
			mDrawerList.setItemChecked(index, true);
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
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

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
			return menu.size();
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
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return menu.get(position).isHeader() ? 0 : 1;
		}

		@Override
		public boolean isEnabled(int position) {
			return menu.get(position).isHeader() ? false : true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (menu.get(position).isHeader()) {
				convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row_header, null);
				((TextView) convertView.findViewById(R.id.options)).setText(getString(menu.get(position).getTitle()));
				return convertView;
			} else {
				convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row, null);
				ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
				TextView title = (TextView) convertView.findViewById(R.id.row_title);
				if (MizLib.runsOnTablet(getApplicationContext()))
					title.setTextSize(26f);
				TextView description = (TextView) convertView.findViewById(R.id.local_movie_count);

				title.setTypeface(tf);
				title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

				if (menu.get(position).isThirdPartyApp()) {
					icon.setImageDrawable(menu.get(position).getDrawable());
					icon.setPadding(8, 8, 8, 8);
					title.setText(menu.get(position).getStringTitle());
				} else {
					icon.setImageResource(menu.get(position).getIcon());
					title.setText(getString(menu.get(position).getTitle()));
				}
				if (menu.get(position).getCount() > 0)
					description.setText(String.valueOf(menu.get(position).getCount()));
				else
					description.setVisibility(View.GONE);

				return convertView;
			}
		}	
	}

	@Override
	public void onBackPressed() {
		if (startup.equals("0") && !mDrawerLayout.isDrawerOpen(mDrawerList) && MizLib.runsOnTablet(this)) { // Welcome screen
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.setClass(getApplicationContext(), Welcome.class);
			startActivity(i);
			finish();
		}

		if (!mDrawerLayout.isDrawerOpen(mDrawerList) && confirmExit) {
			if (hasTriedOnce) {
				super.onBackPressed();
			} else {
				Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
				hasTriedOnce = true;
			}
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("showMediaApps")) {
			showMediaApps = sharedPreferences.getBoolean("showMediaApps", true);

			setupMenuItems();
			((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
		}
	}
}