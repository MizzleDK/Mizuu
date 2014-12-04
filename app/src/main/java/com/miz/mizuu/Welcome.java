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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.MizLib;
import com.miz.utils.FileUtils;
import com.miz.utils.TypefaceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static com.miz.functions.PreferenceKeys.CONFIRM_BACK_PRESS;

public class Welcome extends MizActivity implements ViewFactory {

	private long interval = 10000;
	private int mNumMovies, mNumShows, mNumWatchlist, index = -1;
	private ListView list;
	private Typeface tf;
	private DbAdapterMovies dbHelper;
	private DbAdapterTvShows dbHelperTv;
	private ArrayList<Backdrop> backdrops = new ArrayList<Backdrop>();
	private boolean isRunning = true, confirmExit, hasTriedOnce = false;
	private ImageSwitcher imageSwitcher;
	private TextView title;
	private Handler handler;

	@Override
	protected int getLayoutResource() {
		return R.layout.welcome;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		confirmExit = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(CONFIRM_BACK_PRESS, false);

		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE); // This works on API level 13 as well

		super.onCreate(savedInstanceState);
		
		tf = TypefaceUtils.getRobotoThin(this);

		dbHelper = MizuuApplication.getMovieAdapter();
		dbHelperTv = MizuuApplication.getTvDbAdapter();

		Cursor cursor = dbHelper.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
		while (cursor.moveToNext()) {
			try {
				backdrops.add(new Backdrop(
						FileUtils.getMovieBackdrop(this, cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID))).getAbsolutePath(),
						cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID)),
						true,
						cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TITLE))
						));
			} catch (NullPointerException e) {}
		}
		cursor = dbHelperTv.getAllShows();
		while (cursor.moveToNext()) {
			try {
				backdrops.add(new Backdrop(FileUtils.getTvShowBackdrop(this, cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID))).getAbsolutePath(),
								cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)),
								false,
								cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_TITLE))
						));
			} catch (NullPointerException e) {}
		}
		cursor.close();

		Collections.shuffle(backdrops, new Random(System.nanoTime()));

		list = (ListView) findViewById(R.id.list);
		list.setDivider(null);
		list.setAdapter(new MenuAdapter());
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (!(arg2 == 0 || arg2 == 4)) {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
					switch (arg2) {
					case 1:
						i.putExtra("startup", "1");
						break;
					case 2:
						i.putExtra("startup", "2");
						break;
					case 3:
						i.putExtra("startup", "3");
						break;
					case 5:
						i.putExtra("startup", "4");
						break;
					case 6:
						i.putExtra("startup", "5");
						break;
					}
					i.setClass(getApplicationContext(), Main.class);
					startActivity(i);
					overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
				}
			}
		});
		list.setSelection(1); // Google TV goodies

		updateLibraryCounts();

		title = (TextView) findViewById(R.id.title);
		if (MizLib.isTablet(this))
			title.setTextSize(26f);
		else
			title.setVisibility(View.GONE);

		Animation aniIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		aniIn.setDuration(800);
		Animation aniOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		aniOut.setDuration(800);

		imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher1);
		if (!MizLib.usesNavigationControl(getApplicationContext()))
			imageSwitcher.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					if (index == -1)
						index = 0;
					if (backdrops.get(index).isMovie()) {
						intent.putExtra("tmdbId", backdrops.get(index).getId());
						intent.setClass(getApplicationContext(), MovieDetails.class);
					} else {
						intent.putExtra("showId", backdrops.get(index).getId());
						intent.setClass(getApplicationContext(), TvShowDetails.class);
					}
					startActivity(intent);
				}
			});
		imageSwitcher.setInAnimation(aniIn);
		imageSwitcher.setOutAnimation(aniOut);
		imageSwitcher.setFactory(this);

		handler = new Handler();

		if (backdrops.size() > 0) {
			handler.post(runnable);
		}
	}

	private void startAnimatedBackground() {
		handler.postDelayed(runnable, interval);
	}

	private int tries = 0;
	private File tFile;

	private void increaseIndex() {
		index++;
		index = index % backdrops.size();
		tFile = new File(backdrops.get(index).getPath());
		if (!tFile.exists() && tries < 10) {
			tries++;
			increaseIndex();
		}
	}

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (isRunning) {
				//index++;
				//index = index % backdrops.size();
				increaseIndex();
				imageSwitcher.setImageURI(Uri.parse(backdrops.get(index).getPath()));

				final Animation aniIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
				Animation aniOut = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
				aniOut.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						title.setText(backdrops.get(index).getTitle());
						title.startAnimation(aniIn);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationStart(Animation animation) {}
				});

				title.startAnimation(aniOut);

				handler.postDelayed(this, interval);
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
							((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
						}
					});
				} catch (Exception e) {} // Problemer med at kontakte databasen
			}
		}.start();
	}

	public class MenuAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return 7;
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
			if (position == 0 || position == 4)
				return 0;
			else
				return 1;
		}

		@Override
		public boolean isEnabled(int position) {
            return !(position == 0 || position == 4);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == 0 || position == 4) {
				convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row_header, parent, false);
				if (position == 0) {
					((TextView) convertView.findViewById(R.id.options)).setText(getString(R.string.stringLocal));
				} else {
					((TextView) convertView.findViewById(R.id.options)).setText(getString(R.string.stringDiscover));
				}
				return convertView;
			}

			convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row, parent, false);
			ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
			TextView title = (TextView) convertView.findViewById(R.id.row_title);
			if (MizLib.isTablet(getApplicationContext()))
				title.setTextSize(26f);
			TextView description = (TextView) convertView.findViewById(R.id.local_movie_count);

			title.setTypeface(tf);
			title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			switch (position) {
			case 1:
				icon.setImageResource(R.drawable.ic_movie_white_24dp);
				title.setText(getString(R.string.chooserMovies));
				description.setText(String.valueOf(mNumMovies));
				break;
			case 2:
				icon.setImageResource(R.drawable.ic_tv_white_24dp);
				title.setText(getString(R.string.chooserTVShows));
				description.setText(String.valueOf(mNumShows));
				break;
			case 3:
				icon.setImageResource(R.drawable.ic_list_white_24dp);
				title.setText(getString(R.string.chooserWatchList));
				description.setText(String.valueOf(mNumWatchlist));
				break;
			case 5:
				icon.setImageResource(R.drawable.ic_movie_white_24dp);
				title.setText(getString(R.string.chooserMovies));
				description.setVisibility(View.GONE);
				break;
			case 6:
				icon.setImageResource(R.drawable.ic_language_white_24dp);
				title.setText(getString(R.string.chooserWebVideos));
				description.setVisibility(View.GONE);
				break;
			}

			return convertView;
		}	
	}

	@Override
	public void onBackPressed() {
		if (confirmExit) {
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

	private class Backdrop {
		private String mPath, mId, mTitle;
		private boolean mMovie;

		public Backdrop(String path, String id, boolean isMovie, String title) {
			mPath = path;
			mId = id;
			mMovie = isMovie;
			mTitle = title;
		}

		public String getTitle() {
			return mTitle;
		}
		
		public String getId() {
			return mId;
		}

		public String getPath() {
			return mPath;
		}

		public boolean isMovie() {
			return mMovie;
		}
	}

	@Override
	public View makeView() {
		ImageView imageView = new ImageView(this);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return imageView;
	}
}