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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.AsyncTask;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MenuItem;
import com.miz.functions.MizLib;
import com.miz.utils.FileUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Welcome extends MizActivity {

    private final long INTERVAL = 10000;

    private int mNumMovies, mNumShows, index = -1;
    private ArrayList<WelcomeItem> mItems = new ArrayList<>();
    private boolean isRunning = true;
    private ImageSwitcher mImageSwitcher, mImageCover;
    private TextSwitcher title;
    private Handler mHandler = new Handler();
    private Animation mAnimationIn, mAnimationOut;
    private ArrayList<MenuItem> mMenuItems = new ArrayList<>();
    private Typeface mTfMedium, mTfRegular;
    private ListView mListView;

    @Override
    protected int getLayoutResource() {
        return R.layout.welcome;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE);

        super.onCreate(savedInstanceState);

        mTfMedium = TypefaceUtils.getRobotoMedium(getApplicationContext());
        mTfRegular = TypefaceUtils.getRoboto(getApplicationContext());

        mAnimationIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        mAnimationIn.setDuration(500);
        mAnimationOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        mAnimationOut.setDuration(500);

        mListView = (ListView) findViewById(R.id.list);
        mListView.setLayoutParams(new FrameLayout.LayoutParams(ViewUtils.getNavigationDrawerWidth(this), FrameLayout.LayoutParams.MATCH_PARENT));
        mListView.setAdapter(new MenuAdapter());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (mMenuItems.get(arg2).getType()) {
                    case MenuItem.SECTION:

                        Intent mainIntent = new Intent(getApplicationContext(), Main.class);
                        mainIntent.putExtra("startup", String.valueOf(mMenuItems.get(arg2).getFragment()));
                        startActivity(mainIntent);

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

        mImageCover = (ImageSwitcher) findViewById(R.id.imageCover);
        mImageCover.setInAnimation(mAnimationIn);
        mImageCover.setOutAnimation(mAnimationOut);
        mImageCover.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                return imageView;
            }
        });

        title = (TextSwitcher) findViewById(R.id.title);
        title.setInAnimation(mAnimationIn);
        title.setOutAnimation(mAnimationOut);
        title.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                TextView tv = new TextView(getApplicationContext());
                tv.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Large);
                tv.setShadowLayer(1f, 0, 0, Color.BLACK);
                tv.setTextIsSelectable(false);
                tv.setGravity(Gravity.RIGHT);
                tv.setTypeface(TypefaceUtils.getRobotoCondensedRegular(getApplicationContext()));
                tv.setSingleLine();
                tv.setEllipsize(TextUtils.TruncateAt.END);
                tv.setLayoutParams(new TextSwitcher.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                return tv;
            }
        });

        mImageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher1);
        if (!MizLib.usesNavigationControl(getApplicationContext()))
            mImageCover.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(mItems.get(index).getIntent(getApplicationContext()));
                }
            });
        mImageSwitcher.setInAnimation(mAnimationIn);
        mImageSwitcher.setOutAnimation(mAnimationOut);
        mImageSwitcher.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                return imageView;
            }
        });

        updateLibraryCounts();

        mListView.setSelection(0);

        new WelcomeLoader(getApplicationContext()).execute();
    }

    private void setupMenuItems() {
        mMenuItems.clear();

        // Regular menu items
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyMovies), mNumMovies, MenuItem.SECTION, null, 1, R.drawable.ic_movie_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyTvShows), mNumShows, MenuItem.SECTION, null, 2, R.drawable.ic_tv_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerOnlineMovies), -1, MenuItem.SECTION, null, 3, R.drawable.ic_local_movies_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerWebVideos), -1, MenuItem.SECTION, null, 4, R.drawable.ic_cloud_grey600_24dp));

        // Third party applications
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> mApplicationList = pm.getInstalledApplications(PackageManager.GET_META_DATA);

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
    }

    private void startAnimatedBackground() {
        mHandler.postDelayed(runnable, INTERVAL);
    }

    private void increaseIndex() {
        if (index >= (mItems.size() - 1))
            index = 0;
        else
            index++;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                increaseIndex();

                mImageSwitcher.setImageURI(Uri.parse(mItems.get(index).getBackdropPath()));
                mImageCover.setImageURI(Uri.parse(mItems.get(index).getCoverPath()));
                title.setCurrentText(mItems.get(index).getTitle());

                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, INTERVAL);
            }
        }
    };

    public void onResume() {
        super.onResume();

        if (!isRunning) {
            startAnimatedBackground();
            isRunning = true;
        }
    }

    public void onPause() {
        super.onPause();
        isRunning = false;
    }

    private void updateLibraryCounts() {
        mNumMovies = MizuuApplication.getMovieAdapter().count();
        mNumShows = MizuuApplication.getTvDbAdapter().count();

        setupMenuItems();
        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    private class WelcomeItem {

        public static final int MOVIE = 0, TV_SHOW = 1;

        private String mItemId, mTitle;
        private File mBackdrop, mCover;
        private int mType;

        public WelcomeItem(String itemId, String title, File backdrop, File cover, int type) {
            mItemId = itemId;
            mTitle = title;
            mBackdrop = backdrop;
            mCover = cover;
            mType = type;
        }

        public String getItemId() {
            return mItemId;
        }

        public String getTitle() {
            return mTitle;
        }

        public File getBackdrop() {
            return mBackdrop;
        }

        public String getBackdropPath() {
            return mBackdrop.getAbsolutePath();
        }

        public File getCover() {
            return mCover;
        }

        public String getCoverPath() {
            return mCover.getAbsolutePath();
        }

        public int getType() {
            return mType;
        }

        public Intent getIntent(Context context) {
            Intent i = new Intent(context, getType() == MOVIE ?
                    MovieDetails.class : TvShowDetails.class);
            i.putExtra(getType() == MOVIE ?
                    "tmdbId" : "showId", getItemId());
            return i;
        }

    }

    private class WelcomeLoader extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private ArrayList<WelcomeItem> mTempItems = new ArrayList<>();

        public WelcomeLoader(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {

            ColumnIndexCache cache = new ColumnIndexCache();

            Cursor movieCursor = MizuuApplication.getMovieAdapter().getAllMovies();
            try {
                while (movieCursor.moveToNext()) {
                    final String id = movieCursor.getString(cache.getColumnIndex(movieCursor, DbAdapterMovies.KEY_TMDB_ID));

                    mTempItems.add(new WelcomeItem(
                            id,
                            movieCursor.getString(cache.getColumnIndex(movieCursor, DbAdapterMovies.KEY_TITLE)),
                            FileUtils.getMovieBackdrop(mContext, id),
                            FileUtils.getMovieThumb(mContext, id),
                            WelcomeItem.MOVIE
                    ));
                }
            } catch (Exception e) {
            } finally {
                movieCursor.close();
            }

            // Clear the cache
            cache.clear();

            Cursor tvCursor = MizuuApplication.getTvDbAdapter().getAllShows();
            try {
                while (tvCursor.moveToNext()) {
                    final String id = tvCursor.getString(cache.getColumnIndex(tvCursor, DbAdapterTvShows.KEY_SHOW_ID));

                    mTempItems.add(new WelcomeItem(
                            id,
                            tvCursor.getString(cache.getColumnIndex(tvCursor, DbAdapterTvShows.KEY_SHOW_TITLE)),
                            FileUtils.getTvShowBackdrop(mContext, id),
                            FileUtils.getTvShowThumb(mContext, id),
                            WelcomeItem.TV_SHOW
                    ));
                }
            } catch (Exception e) {
            } finally {
                tvCursor.close();
            }

            // Clear the cache
            cache.clear();

            // Ensure that we only show content with an existing backdrop and cover image
            int totalSize = mTempItems.size();
            for (int i = 0; i < totalSize; i++) {
                if (!(mTempItems.get(i).getBackdrop().exists() && mTempItems.get(i).getCover().exists())) {
                    if (mTempItems.size() > i) {
                        mTempItems.remove(i);
                        i--;
                        totalSize--;
                    }
                }
            }

            // Shuffle the collection
            Collections.shuffle(mTempItems, new Random(System.nanoTime()));

            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            mItems.addAll(mTempItems);

            if (mItems.size() > 0) {
                mHandler.post(runnable);
            }
        }
    }

    public class MenuAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;

        public MenuAdapter() {
            mInflater = LayoutInflater.from(getApplicationContext());
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
                case MenuItem.SEPARATOR:
                    return 0;
                case MenuItem.SUB_HEADER:
                    return 1;
                default:
                    return 2;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            int type = mMenuItems.get(position).getType();
            return !(type == MenuItem.SEPARATOR || type == MenuItem.SUB_HEADER);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR) {
                convertView = mInflater.inflate(R.layout.menu_drawer_separator, parent, false);

                convertView.findViewById(R.id.divider).setBackgroundColor(Color.parseColor("#80666666"));

            } else if (mMenuItems.get(position).getType() == MenuItem.SUB_HEADER) {
                convertView = mInflater.inflate(R.layout.menu_drawer_header_item, parent, false);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);
                title.setTextColor(Color.parseColor("#DDDDDD"));
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

                int color = Color.parseColor("#F0F0F0");

                title.setTextColor(color);
                description.setTextColor(color);
                icon.setColorFilter(Color.parseColor("#DDDDDD"));

                if (mMenuItems.get(position).getCount() >= 0)
                    description.setText(String.valueOf(mMenuItems.get(position).getCount()));
                else
                    description.setVisibility(View.GONE);

            }

            return convertView;
        }
    }
}