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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.BlurTransformation;
import com.miz.functions.MenuItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.AccountsFragment;
import com.miz.mizuu.fragments.ContactDeveloperFragment;
import com.miz.mizuu.fragments.MovieDiscoveryViewPagerFragment;
import com.miz.mizuu.fragments.MovieLibraryOverviewFragment;
import com.miz.mizuu.fragments.TvShowLibraryOverviewFragment;
import com.miz.mizuu.fragments.WebVideosViewPagerFragment;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.miz.functions.PreferenceKeys.CONFIRM_BACK_PRESS;
import static com.miz.functions.PreferenceKeys.STARTUP_SELECTION;
import static com.miz.functions.PreferenceKeys.TRAKT_FULL_NAME;
import static com.miz.functions.PreferenceKeys.TRAKT_USERNAME;

@SuppressLint("NewApi")
public class Main extends MizActivity {

    public static final int MOVIES = 1, SHOWS = 2, WEB_MOVIES = 3, WEB_VIDEOS = 4;
    private int mNumMovies, mNumShows, selectedIndex, mStartup;
    private Typeface mTfMedium, mTfRegular;
    private DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DbAdapterMovies mDbHelper;
    private DbAdapterTvShows mDbHelperTv;
    private boolean mConfirmExit, mTriedOnce = false;
    private ArrayList<MenuItem> mMenuItems = new ArrayList<MenuItem>();
    private List<ApplicationInfo> mApplicationList;
    private Picasso mPicasso;

    @Override
    protected int getLayoutResource() {
        return R.layout.menu_drawer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Mizuu_Theme_Overview);

        super.onCreate(savedInstanceState);

        mPicasso = MizuuApplication.getPicasso(getApplicationContext());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mConfirmExit = settings.getBoolean(CONFIRM_BACK_PRESS, false);
        mStartup = Integer.valueOf(settings.getString(STARTUP_SELECTION, "1"));

        mDbHelper = MizuuApplication.getMovieAdapter();
        mDbHelperTv = MizuuApplication.getTvDbAdapter();

        mTfMedium = TypefaceUtils.getRobotoMedium(getApplicationContext());
        mTfRegular = TypefaceUtils.getRoboto(getApplicationContext());

        setupMenuItems(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_list_shadow, GravityCompat.START);

        mDrawerList = (ListView) findViewById(R.id.listView1);
        mDrawerList.setLayoutParams(new FrameLayout.LayoutParams(ViewUtils.getNavigationDrawerWidth(this), FrameLayout.LayoutParams.MATCH_PARENT));
        mDrawerList.setAdapter(new MenuAdapter());
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (mMenuItems.get(arg2).getType()) {
                    case MenuItem.HEADER:

                        Intent intent = new Intent(getApplicationContext(), Preferences.class);
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AccountsFragment.class.getName());
                        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, getString(R.string.social));
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, getString(R.string.social));

                        startActivity(intent);
                        break;

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
                    case MenuItem.SETTINGS_AREA:

                        Intent smallIntent = new Intent(getApplicationContext(), Preferences.class);
                        if (mMenuItems.get(arg2).getIcon() == R.drawable.ic_help_grey600_24dp) {
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ContactDeveloperFragment.class.getName());
                            smallIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, getString(R.string.menuAboutContact));
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, getString(R.string.menuAboutContact));
                        }

                        startActivity(smallIntent);

                        mDrawerLayout.closeDrawers();

                        break;
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState != null && savedInstanceState.containsKey("selectedIndex")) {
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            loadFragment(selectedIndex);
        } else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("startup")) {
            loadFragment(Integer.parseInt(getIntent().getExtras().getString("startup")));
        } else {
            loadFragment(mStartup);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_LIBRARY));
    }

    private void loadFragment(int type) {
        if (type == 0)
            type = 1;

        Fragment frag = getSupportFragmentManager().findFragmentByTag("frag" + type);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            switch (type) {
                case MOVIES:
                    ft.replace(R.id.content_frame, MovieLibraryOverviewFragment.newInstance(), "frag" + type);
                    break;
                case SHOWS:
                    ft.replace(R.id.content_frame, TvShowLibraryOverviewFragment.newInstance(), "frag" + type);
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

        switch (type) {
            case MOVIES:
                setTitle(R.string.chooserMovies);
                break;
            case SHOWS:
                setTitle(R.string.chooserTVShows);
                break;
            case WEB_MOVIES:
                setTitle(R.string.drawerOnlineMovies);
                break;
            case WEB_VIDEOS:
                setTitle(R.string.drawerWebVideos);
                break;
        }

        selectListIndex(type);

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        if (!newIntent.hasExtra("fromUpdate")) {
            Intent i;
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
        mMenuItems.add(new MenuItem(null, -1, MenuItem.HEADER, null));

        // Regular menu items
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyMovies), mNumMovies, MenuItem.SECTION, null, MOVIES, R.drawable.ic_movie_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyTvShows), mNumShows, MenuItem.SECTION, null, SHOWS, R.drawable.ic_tv_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerOnlineMovies), -1, MenuItem.SECTION, null, WEB_MOVIES, R.drawable.ic_local_movies_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerWebVideos), -1, MenuItem.SECTION, null, WEB_VIDEOS, R.drawable.ic_cloud_grey600_24dp));

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

        if (temp.size() > 0) {
            // Menu section header
            mMenuItems.add(new MenuItem(MenuItem.SEPARATOR));
            mMenuItems.add(new MenuItem(getString(R.string.installed_media_apps), -1, MenuItem.SUB_HEADER, null));
        }

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

        mMenuItems.add(new MenuItem(MenuItem.SEPARATOR_EXTRA_PADDING));

        mMenuItems.add(new MenuItem(getString(R.string.settings_name), MenuItem.SETTINGS_AREA, R.drawable.ic_settings_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.menuAboutContact), MenuItem.SETTINGS_AREA, R.drawable.ic_help_grey600_24dp));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateLibraryCounts();
        }
    };

    protected void selectListIndex(int index) {
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
                    mNumShows = mDbHelperTv.count();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupMenuItems(false);
                            ((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {}
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

        private String mBackdropPath;
        private LayoutInflater mInflater;

        public MenuAdapter() {
            mInflater = LayoutInflater.from(getApplicationContext());
            mBackdropPath = MizLib.getRandomBackdropPath(getApplicationContext());
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
            return 6;
        }

        @Override
        public int getItemViewType(int position) {
            switch (mMenuItems.get(position).getType()) {
                case MenuItem.HEADER:
                    return 0;
                case MenuItem.SEPARATOR:
                    return 1;
                case MenuItem.SEPARATOR_EXTRA_PADDING:
                    return 2;
                case MenuItem.SUB_HEADER:
                    return 3;
                case MenuItem.SECTION:
                case MenuItem.THIRD_PARTY_APP:
                    return 4;
                default:
                    return 5;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            int type = mMenuItems.get(position).getType();
            return !(type == MenuItem.SEPARATOR ||
                    type == MenuItem.SEPARATOR_EXTRA_PADDING ||
                    type == MenuItem.SUB_HEADER);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mMenuItems.get(position).getType() == MenuItem.HEADER) {
                convertView = mInflater.inflate(R.layout.menu_drawer_header, parent, false);

                final String fullName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(TRAKT_FULL_NAME, "");
                final String userName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(TRAKT_USERNAME, "");
                final ImageView backgroundImage = ((ImageView) convertView.findViewById(R.id.userCover));
                final ImageView userImage = ((ImageView) convertView.findViewById(R.id.userPhoto));
                final ImageView plusIcon = ((ImageView) convertView.findViewById(R.id.plus_icon));
                final TextView realName = ((TextView) convertView.findViewById(R.id.real_name));
                final TextView userNameTextField = ((TextView) convertView.findViewById(R.id.username));

                realName.setTypeface(mTfMedium);
                userNameTextField.setTypeface(mTfRegular);

                // Full name
                realName.setText(!TextUtils.isEmpty(fullName) ? fullName : "");

                // User name
                if (!TextUtils.isEmpty(userName)) {
                    userNameTextField.setText(String.format(getString(R.string.logged_in_as), userName));
                    plusIcon.setVisibility(View.GONE);
                } else {
                    userNameTextField.setText(R.string.sign_in_with_trakt);
                    plusIcon.setVisibility(View.VISIBLE);
                }

                // This should be loaded in the background, but doesn't matter much at the moment
                Bitmap src = BitmapFactory.decodeFile(new File(MizuuApplication.getCacheFolder(getApplicationContext()), "avatar.jpg").getAbsolutePath());
                if (src != null) {
                    RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getResources(), src);
                    dr.setCornerRadius(Math.min(dr.getMinimumWidth(), dr.getMinimumHeight()));
                    dr.setAntiAlias(true);
                    userImage.setImageDrawable(dr);
                } else {
                    userImage.setVisibility(View.GONE);
                }

                // Background image
                if (!TextUtils.isEmpty(mBackdropPath))
                    mPicasso.load(mBackdropPath)
                            .resize(MizLib.convertDpToPixels(getApplicationContext(), 320),
                                    MizLib.convertDpToPixels(getApplicationContext(), 180))
                            .into(backgroundImage);
                else
                    mPicasso.load(R.drawable.default_menu_backdrop)
                            .resize(MizLib.convertDpToPixels(getApplicationContext(), 320),
                                    MizLib.convertDpToPixels(getApplicationContext(), 180))
                            .into(backgroundImage);

                // Dark color filter on the background image
                backgroundImage.setColorFilter(Color.parseColor("#50181818"), android.graphics.PorterDuff.Mode.SRC_OVER);

            } else if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR) {
                convertView = mInflater.inflate(R.layout.menu_drawer_separator, parent, false);
            } else if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR_EXTRA_PADDING) {
                convertView = mInflater.inflate(R.layout.menu_drawer_separator_extra_padding, parent, false);
            } else if (mMenuItems.get(position).getType() == MenuItem.SUB_HEADER) {
                convertView = mInflater.inflate(R.layout.menu_drawer_header_item, parent, false);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);
            } else if (mMenuItems.get(position).getType() == MenuItem.THIRD_PARTY_APP || mMenuItems.get(position).getType() == MenuItem.SECTION) {
                convertView = mInflater.inflate(R.layout.menu_drawer_item, parent, false);

                // Icon
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                icon.setImageResource(mMenuItems.get(position).getIcon());

                // Title
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);

                // Description
                TextView description = (TextView) convertView.findViewById(R.id.count);
                description.setTypeface(mTfRegular);

                if (mMenuItems.get(position).getType() == MenuItem.SECTION &&
                        mMenuItems.get(position).getFragment() == selectedIndex) {
                    convertView.setBackgroundColor(Color.parseColor("#e8e8e8"));

                    int color = Color.parseColor("#3f51b5");

                    title.setTextColor(color);
                    description.setTextColor(color);
                    icon.setColorFilter(color);
                } else {
                    int color = Color.parseColor("#DD000000");

                    title.setTextColor(color);
                    description.setTextColor(color);
                    icon.setColorFilter(Color.parseColor("#999999"));
                }

                if (mMenuItems.get(position).getCount() >= 0)
                    description.setText(String.valueOf(mMenuItems.get(position).getCount()));
                else
                    description.setVisibility(View.GONE);

            } else {
                convertView = mInflater.inflate(R.layout.menu_drawer_small_item, parent, false);

                // Icon
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                icon.setImageResource(mMenuItems.get(position).getIcon());
                icon.setColorFilter(Color.parseColor("#737373"));

                // Title
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);

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
}