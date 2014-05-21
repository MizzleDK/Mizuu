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
import android.graphics.Typeface;

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

public class MizuuApplication extends Application {

	private static DbAdapterTvShow dbTvShow;
	private static DbAdapterTvShowEpisode dbTvShowEpisode;
	private static DbAdapterSources dbSources;
	private static DbAdapter db;
	private static HashMap<String, String[]> map = new HashMap<String, String[]>();
	private static Picasso mPicasso;
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
		mPicasso = new Picasso.Builder(context).downloader(getDownloader(context)).executor(getThreadPoolExecutor()).memoryCache(getLruCache(context)).build();
		
		return mPicasso;
	}

	private static ThreadPoolExecutor getThreadPoolExecutor() {
		if (mThreadPoolExecutor == null)
			mThreadPoolExecutor = new ThreadPoolExecutor(1, 2, 2, TimeUnit.SECONDS,
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
}