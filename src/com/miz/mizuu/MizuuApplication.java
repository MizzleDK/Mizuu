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

import static com.miz.functions.PreferenceKeys.FULLSCREEN_TAG;
import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterMovieMapping;
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
	private static DbAdapterMovieMapping sDbMovieMapping;
	private static HashMap<String, String[]> sMap = new HashMap<String, String[]>();
	private static Picasso sPicasso, sPicassoDetailsView;
	private static LruCache sLruCache;
	private static Downloader sDownloader;
	private static ThreadPoolExecutor sThreadPoolExecutor;
	private static HashMap<String, Typeface> sTypefaces = new HashMap<String, Typeface>();
	private static Bus sBus;
	private static File sMovieThumbFolder, sMovieBackdropFolder, sTvShowThumbFolder, sTvShowBackdropFolder, sTvShowEpisodeFolder, sTvShowSeasonFolder, sAvailableOfflineFolder;

	@Override
	public void onCreate() {
		super.onCreate();

		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");

		// Database setup
		sDbTvShow = new DbAdapterTvShow(this);
		sDbTvShowEpisode = new DbAdapterTvShowEpisode(this);
		sDbSources = new DbAdapterSources(this);
		sDbMovieMapping = new DbAdapterMovieMapping(this); // IMPORTANT that this is initialized before the DbAdapter below
		sDb = new DbAdapter(this);

		getMovieThumbFolder(this);
		getMovieBackdropFolder(this);
		getTvShowThumbFolder(this);
		getTvShowBackdropFolder(this);
		getTvShowEpisodeFolder(this);
		getTvShowSeasonFolder(this);
		getAvailableOfflineFolder(this);

		transitionLocalizationPreference();

		
		/**
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * MERGE ALL DATABASES INTO ONE!!!!
		 * http://stackoverflow.com/a/3851344/762442
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 */

		File dbFile = getDatabasePath("mizuu_tv_show_data");
		System.out.println("DATABASE EXISTS: " + dbFile.exists());
		if (dbFile.exists()) {
			String KEY_SHOW_ID = "show_id";
			String KEY_SHOW_TITLE = "show_title";
			String KEY_SHOW_PLOT = "show_description";
			String KEY_SHOW_ACTORS = "show_actors";
			String KEY_SHOW_GENRES = "show_genres";
			String KEY_SHOW_RATING = "show_rating";
			String KEY_SHOW_CERTIFICATION = "show_certification";
			String KEY_SHOW_RUNTIME = "show_runtime";
			String KEY_SHOW_FIRST_AIRDATE = "show_first_airdate";
			String KEY_SHOW_EXTRA1 = "extra1"; // Favorite
			
			String[] SELECT_ALL = new String[]{KEY_SHOW_ID, KEY_SHOW_TITLE, KEY_SHOW_PLOT, KEY_SHOW_ACTORS,
					KEY_SHOW_GENRES, KEY_SHOW_RATING, KEY_SHOW_RATING, KEY_SHOW_CERTIFICATION, KEY_SHOW_RUNTIME,
					KEY_SHOW_FIRST_AIRDATE, KEY_SHOW_EXTRA1};
			
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			Cursor c = db.query("tvshows", SELECT_ALL, null, null, null, null, KEY_SHOW_TITLE + " ASC");
			
			
			
			while (c.moveToNext()) {
				System.out.println(c.getString(c.getColumnIndex(KEY_SHOW_TITLE)));
			}
			
			c.close();
		}

	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		sDbTvShow.close();
		sDbTvShowEpisode.close();
		sDbSources.close();
		sDb.close();
	}

	private void transitionLocalizationPreference() {
		// Transition from the old localization preference if such exists
		String languagePref;
		boolean oldLocalizedPref = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefsUseLocalData", false);
		if (oldLocalizedPref) {
			languagePref = Locale.getDefault().getLanguage();
			PreferenceManager.getDefaultSharedPreferences(this).edit().remove("prefsUseLocalData").commit();
		} else {
			languagePref = PreferenceManager.getDefaultSharedPreferences(this).getString(LANGUAGE_PREFERENCE, "en");
		}

		PreferenceManager.getDefaultSharedPreferences(this).edit().putString(LANGUAGE_PREFERENCE, languagePref).commit();
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

	public static DbAdapterMovieMapping getMovieMappingAdapter() {
		return sDbMovieMapping;
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

	public static int getBackgroundColorResource(Context context) {
		return R.color.dark_background;
	}

	public static boolean isFullscreen(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FULLSCREEN_TAG, false);
	}

	public static void setupTheme(Context context) {
		if (isFullscreen(context))
			context.setTheme(R.style.Mizuu_Theme_FullScreen);
		else
			context.setTheme(R.style.Mizuu_Theme);
	}

	public static Bus getBus() {
		if (sBus == null)
			sBus = new Bus();	
		return sBus;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getMovieThumbFolder(Context c) {
		if (sMovieThumbFolder == null) {
			sMovieThumbFolder = new File(c.getExternalFilesDir(null), "movie-thumbs");
			sMovieThumbFolder.mkdirs();
		}
		return sMovieThumbFolder;
	}


	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getMovieBackdropFolder(Context c) {
		if (sMovieBackdropFolder == null) {
			sMovieBackdropFolder = new File(c.getExternalFilesDir(null), "movie-backdrops");
			sMovieBackdropFolder.mkdirs();
		}
		return sMovieBackdropFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowThumbFolder(Context c) {
		if (sTvShowThumbFolder == null) {
			sTvShowThumbFolder = new File(c.getExternalFilesDir(null), "tvshows-thumbs");
			sTvShowThumbFolder.mkdirs();
		}
		return sTvShowThumbFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowBackdropFolder(Context c) {
		if (sTvShowBackdropFolder == null) {
			sTvShowBackdropFolder = new File(c.getExternalFilesDir(null), "tvshows-backdrops");
			sTvShowBackdropFolder.mkdirs();
		}
		return sTvShowBackdropFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowEpisodeFolder(Context c) {		
		if (sTvShowEpisodeFolder == null) {
			sTvShowEpisodeFolder = new File(c.getExternalFilesDir(null), "tvshows-episodes");
			sTvShowEpisodeFolder.mkdirs();
		}
		return sTvShowEpisodeFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowSeasonFolder(Context c) {
		if (sTvShowSeasonFolder == null) {
			sTvShowSeasonFolder = new File(c.getExternalFilesDir(null), "tvshows-seasons");
			sTvShowSeasonFolder.mkdirs();
		}
		return sTvShowSeasonFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific video.
	 */
	public static File getAvailableOfflineFolder(Context c) {
		if (sAvailableOfflineFolder == null) {
			sAvailableOfflineFolder = new File(c.getExternalFilesDir(null), "offline_storage");
			sAvailableOfflineFolder.mkdirs();
		}
		return sAvailableOfflineFolder;
	}
}