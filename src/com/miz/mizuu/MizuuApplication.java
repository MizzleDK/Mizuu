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
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.MizLib;
import com.miz.functions.Utils;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.FULLSCREEN_TAG;
import static com.miz.functions.PreferenceKeys.DARK_THEME;

public class MizuuApplication extends Application {

	private static DbAdapterTvShow dbTvShow;
	private static DbAdapterTvShowEpisode dbTvShowEpisode;
	private static DbAdapterSources dbSources;
	private static DbAdapter db;
	private static HashMap<String, String[]> map = new HashMap<String, String[]>();
	private static Picasso mPicasso, mPicassoDetailsView;
	private static LruCache mLruCache;
	private static File mMovieThumbFolder, mTvShowThumbFolder;
	private static Downloader mDownloader;
	private static ThreadPoolExecutor mThreadPoolExecutor;
	private static HashMap<String, Typeface> mTypefaces = new HashMap<String, Typeface>();
	private static String mMovieThumbFolderPath, mTvShowThumbFolderPath;

	@Override
	public void onCreate() {
		super.onCreate();

		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");

		if (!(0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)))
			Crashlytics.start(this);

		// Database setup
		dbTvShow = new DbAdapterTvShow(this);
		dbTvShowEpisode = new DbAdapterTvShowEpisode(this);
		dbSources = new DbAdapterSources(this);
		db = new DbAdapter(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		dbTvShow.close();
		dbTvShowEpisode.close();
		dbSources.close();
		db.close();
	}

	public static DbAdapterTvShow getTvDbAdapter() {
		return dbTvShow;
	}

	public static DbAdapterTvShowEpisode getTvEpisodeDbAdapter() {
		return dbTvShowEpisode;
	}

	public static DbAdapterSources getSourcesAdapter() {
		return dbSources;
	}

	public static DbAdapter getMovieAdapter() {
		return db;
	}

	public static String[] getCifsFilesList(String parentPath) {
		return map.get(parentPath);
	}

	public static void putCifsFilesList(String parentPath, String[] list) {
		if (!map.containsKey(parentPath))
			map.put(parentPath, list);
	}

	public static Picasso getPicasso(Context context) {
		if (mPicasso == null)
			mPicasso = new Picasso.Builder(context).downloader(getDownloader(context)).executor(getThreadPoolExecutor()).memoryCache(getLruCache(context)).build();

		return mPicasso;
	}

	/**
	 * A Picasso instance used in the details view for movies and TV shows
	 * to load cover art and backdrop images instantly.
	 * @param context
	 * @return
	 */
	public static Picasso getPicassoDetailsView(Context context) {
		if (mPicassoDetailsView == null)
			mPicassoDetailsView = new Picasso.Builder(context).downloader(getDownloader(context)).build();
			
			return mPicassoDetailsView;
	}

	private static ThreadPoolExecutor getThreadPoolExecutor() {
		if (mThreadPoolExecutor == null)
			mThreadPoolExecutor = new ThreadPoolExecutor(2, 2, 2, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(), new PicassoThreadFactory());
		return mThreadPoolExecutor;
	}

	static class PicassoThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			return new PicassoThread(r);
		}
	}

	private static class PicassoThread extends Thread {
		public PicassoThread(Runnable r) {
			super(r);
		}

		@Override public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			super.run();
		}
	}

	public static LruCache getLruCache(Context context) {
		if (mLruCache == null)
			mLruCache = new LruCache(calculateMemoryCacheSize(context));
		return mLruCache;
	}

	public static Downloader getDownloader(Context context) {
		if (mDownloader == null)
			mDownloader = Utils.createDefaultDownloader(context);
		return mDownloader;
	}

	public static File getMovieThumbFolder(Context context) {
		if (mMovieThumbFolder == null)
			mMovieThumbFolder = MizLib.getMovieThumbFolder(context);
		return mMovieThumbFolder;
	}

	public static String getMovieThumbFolderPath(Context context) {
		if (mMovieThumbFolderPath == null)
			mMovieThumbFolderPath = getMovieThumbFolder(context).getAbsolutePath();
		return mMovieThumbFolderPath;
	}

	public static File getTvShowThumbFolder(Context context) {
		if (mTvShowThumbFolder == null)
			mTvShowThumbFolder = MizLib.getTvShowThumbFolder(context);
		return mTvShowThumbFolder;
	}

	public static String getTvShowThumbFolderPath(Context context) {
		if (mTvShowThumbFolderPath == null)
			mTvShowThumbFolderPath = getTvShowThumbFolder(context).getAbsolutePath();
		return mTvShowThumbFolderPath;
	}

	public static int calculateMemoryCacheSize(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
		int memoryClass = am.getLargeMemoryClass();

		// Target 20% of the available heap.
		return 1024 * 1024 * memoryClass / 5;
	}

	private static Bitmap.Config mBitmapConfig = Bitmap.Config.RGB_565;

	public static Bitmap.Config getBitmapConfig() {
		return mBitmapConfig;
	}

	public static Typeface getOrCreateTypeface(Context context, String key) {
		if (!mTypefaces.containsKey(key))
			mTypefaces.put(key, Typeface.createFromAsset(context.getAssets(), key));
		return mTypefaces.get(key);
	}

	public static int getCardDrawable(Context context) {
		if (usesDarkTheme(context))
			return R.drawable.card;
		return R.drawable.card_dark;
	}

	public static int getCardColor(Context context) {
		if (usesDarkTheme(context))
			return R.color.card_background_dark;
		return R.color.card_background_light;
	}

	public static int getCardTitleColor(Context context) {
		if (usesDarkTheme(context))
			return Color.parseColor("#d0d0d0");
		return Color.parseColor("#101010");
	}

	public static int getBackgroundColor(Context context) {
		if (usesDarkTheme(context))
			return Color.WHITE;
		return Color.BLACK;
	}

	public static int getBackgroundColorResource(Context context) {
		if (usesDarkTheme(context))
			return R.color.dark_background;
		return R.color.light_background;
	}

	public static boolean usesDarkTheme(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DARK_THEME, true);
	}

	public static boolean isFullscreen(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FULLSCREEN_TAG, false);
	}

	public static void setupTheme(Context context) {
		if (usesDarkTheme(context)) {
			if (isFullscreen(context))
				context.setTheme(R.style.Theme_Example_FullScreen);
			else
				context.setTheme(R.style.Theme_Example);
		} else {
			if (isFullscreen(context))
				context.setTheme(R.style.Theme_Example_Light_FullScreen);
			else
				context.setTheme(R.style.Theme_Example_Light);
		}
	}
}