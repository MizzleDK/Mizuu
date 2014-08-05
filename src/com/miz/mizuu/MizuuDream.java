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
import java.util.Random;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

@SuppressLint("NewApi")
public class MizuuDream extends DreamService implements ViewFactory {

	private DbAdapter mDatabaseHelper;
	private DbAdapterTvShow mDatabaseHelperTv;
	private ArrayList<Backdrop> mBackdrops = new ArrayList<Backdrop>();
	private long mInterval = 7500;
	private int mIndex = 0;
	private boolean mRunning = false;
	private TextView mTitle;

	@Override
	public void onDreamingStopped() {
		mRunning = false;
		mBackdrops.clear();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		mRunning = true;

		// Exit dream upon user touch
		setInteractive(false);
		// Hide system UI
		setFullscreen(true);
		// Set the dream layout
		setContentView(R.layout.dream);

		mTitle = (TextView) findViewById(R.id.title);
		mTitle.setTypeface(MizuuApplication.getOrCreateTypeface(this, "RobotoCondensed-Regular.ttf"));

		mDatabaseHelper = MizuuApplication.getMovieAdapter();
		mDatabaseHelperTv = MizuuApplication.getTvDbAdapter();

		File temp = null;

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor cursor = mDatabaseHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false);
		while (cursor.moveToNext()) {
			try {
				temp = MizLib.getMovieBackdrop(this, cursor.getString(cache.getColumnIndex(cursor, DbAdapter.KEY_TMDB_ID)));
				if (temp.exists())
					mBackdrops.add(new Backdrop(temp.getAbsolutePath(),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapter.KEY_TITLE))
							));
			} catch (NullPointerException e) {}
		}
		cursor.close();

		cursor = mDatabaseHelperTv.getAllShows();
		while (cursor.moveToNext()) {
			try {
				temp = MizLib.getTvShowBackdrop(this, cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_ID)));
				if (temp.exists())
					mBackdrops.add(new Backdrop(temp.getAbsolutePath(),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_TITLE))
							));
			} catch (NullPointerException e) {}
		}
		cursor.close();
		cache.clear();

		Collections.shuffle(mBackdrops, new Random(System.nanoTime()));

		if (mBackdrops.size() > 0) {
			mTitle.setText(mBackdrops.get(mIndex).getTitle());
			startAnimatedBackground();
		}
	}

	private void startAnimatedBackground() {
		Animation aniIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		aniIn.setDuration(800);
		Animation aniOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		aniOut.setDuration(800);

		final ImageSwitcher imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher1);
		imageSwitcher.setInAnimation(aniIn);
		imageSwitcher.setOutAnimation(aniOut);
		imageSwitcher.setFactory(this);
		imageSwitcher.setImageURI(Uri.parse(mBackdrops.get(mIndex).getPath()));

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (mRunning) {
					mIndex++;
					mIndex = mIndex % mBackdrops.size();
					imageSwitcher.setImageURI(Uri.parse(mBackdrops.get(mIndex).getPath()));

					final Animation aniIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
					Animation aniOut = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
					aniOut.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationEnd(Animation animation) {
							mTitle.setText(mBackdrops.get(mIndex).getTitle());
							mTitle.startAnimation(aniIn);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {}

						@Override
						public void onAnimationStart(Animation animation) {}
					});

					mTitle.startAnimation(aniOut);

					handler.postDelayed(this, mInterval);
				}
			}
		};
		handler.postDelayed(runnable, mInterval);

	}

	@Override
	public View makeView() {
		ImageView imageView = new ImageView(this);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return imageView;
	}

	private class Backdrop {
		String path, title;

		public Backdrop(String path, String title) {
			this.path = path;
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public String getPath() {
			return path;
		}
	}
}