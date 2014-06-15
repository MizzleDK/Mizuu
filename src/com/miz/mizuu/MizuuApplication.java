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

import static com.miz.functions.PreferenceKeys.DARK_THEME;
import static com.miz.functions.PreferenceKeys.FULLSCREEN_TAG;

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
import com.miz.functions.Utils;
import com.squareup.otto.Bus;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

public class MizuuApplication extends Application {

	private static DbAdapterTvShow sDbTvShow;
	private static DbAdapterTvShowEpisode sDbTvShowEpisode;
	private static DbAdapterSources sDbSources;
	private static DbAdapter sDb;
	private static HashMap<String, String[]> sMap = new HashMap<String, String[]>();
	private static Picasso sPicasso, sPicassoDetailsView;
	private static LruCache sLruCache;
	private static Downloader sDownloader;
	private static ThreadPoolExecutor sThreadPoolExecutor;
	private static HashMap<String, Typeface> sTypefaces = new HashMap<String, Typeface>();
	private static Bus sBus;

	@Override
	public void onCreate() {
		super.onCreate();

		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");

		if (!(0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)))
			Crashlytics.start(this);

		// Database setup
		sDbTvShow = new DbAdapterTvShow(this);
		sDbTvShowEpisode = new DbAdapterTvShowEpisode(this);
		sDbSources = new DbAdapterSources(this);
		sDb = new DbAdapter(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		sDbTvShow.close();
		sDbTvShowEpisode.close();
		sDbSources.close();
		sDb.close();
	}

	public static DbAdapterTvShow getTvDbAdapter() {
		return sDbTvShow;
	}

	public static DbAdapterTvShowEpisode getTvEpisodeDbAdapter() {
		return sDbTvShowEpisode;
	}

	public static DbAdapterSources getSourcesAdapter() {
		return sDbSources;
	}

	public static DbAdapter getMovieAdapter() {
		return sDb;
	}

	public static String[] getCifsFilesList(String parentPath) {
		return sMap.get(parentPath);
	}

	public static void putCifsFilesList(String parentPath, String[] list) {
		if (!sMap.containsKey(parentPath))
			sMap.put(parentPath, list);
	}

	public static Picasso getPicasso(Context context) {
		if (sPicasso == null)
			sPicasso = new Picasso.Builder(context).downloader(getDownloader(context)).executor(getThreadPoolExecutor()).memoryCache(getLruCache(context)).build();
		return sPicasso;
	}

	/**
	 * A Picasso instance used in the details view for movies and TV shows
	 * to load cover art and backdrop images instantly.
	 * @param context
	 * @return
	 */
	public static Picasso getPicassoDetailsView(Context context) {
		if (sPicassoDetailsView == null)
			sPicassoDetailsView = new Picasso.Builder(context).downloader(getDownloader(context)).build();
		return sPicassoDetailsView;
	}

	private static ThreadPoolExecutor getThreadPoolExecutor() {
		if (sThreadPoolExecutor == null)
			sThreadPoolExecutor = new ThreadPoolExecutor(2, 2, 2, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(), new PicassoThreadFactory());
		return sThreadPoolExecutor;
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
		if (sLruCache == null)
			sLruCache = new LruCache(calculateMemoryCacheSize(context));
		return sLruCache;
	}

	public static Downloader getDownloader(Context context) {
		if (sDownloader == null)
			sDownloader = Utils.createDefaultDownloader(context);
		return sDownloader;
	}

	public static int calculateMemoryCacheSize(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
		int memoryClass = am.getLargeMemoryClass();

		// Target 20% of the available heap.
		return 1024 * 1024 * memoryClass / 5;
	}

	public static Bitmap.Config getBitmapConfig() {
		return Bitmap.Config.RGB_565;
	}

	public static Typeface getOrCreateTypeface(Context context, String key) {
		if (!sTypefaces.containsKey(key))
			sTypefaces.put(key, Typeface.createFromAsset(context.getAssets(), key));
		return sTypefaces.get(key);
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

	private static boolean usesDarkTheme(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DARK_THEME, true);
	}

	private static boolean isFullscreen(Context context) {
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
	
	public static Bus getBus() {
		if (sBus == null)
			sBus = new Bus();	
		return sBus;
	}
}