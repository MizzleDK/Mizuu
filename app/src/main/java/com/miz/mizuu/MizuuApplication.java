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

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;

import com.google.common.collect.ArrayListMultimap;
import com.miz.abstractclasses.MovieApiService;
import com.miz.abstractclasses.TvShowApiService;
import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.apis.tmdb.TMDbTvShowService;
import com.miz.db.DbAdapterCollections;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class MizuuApplication extends Application {

	private static DbAdapterTvShows sDbTvShow;
	private static DbAdapterTvShowEpisodes sDbTvShowEpisode;
	private static DbAdapterTvShowEpisodeMappings sDbTvShowEpisodeMappings;
	private static DbAdapterSources sDbSources;
	private static DbAdapterMovies sDbMovies;
	private static DbAdapterMovieMappings sDbMovieMapping;
	private static DbAdapterCollections sDbCollections;
	private static HashMap<String, String[]> sMap = new HashMap<String, String[]>();
	private static Picasso sPicasso;
	private static HashMap<String, Typeface> sTypefaces = new HashMap<String, Typeface>();
	private static HashMap<String, Palette> sPalettes = new HashMap<String, Palette>();
	private static Bus sBus;
	private static File sBaseAppFolder, sMovieThumbFolder, sMovieBackdropFolder, sTvShowThumbFolder, sTvShowBackdropFolder, sTvShowEpisodeFolder, sTvShowSeasonFolder, sAvailableOfflineFolder, sCacheFolder;
	private static Context mInstance;
	private static ArrayListMultimap<String, String> mMovieFilepaths;
	private static OkHttpClient mOkHttpClient;

	@Override
	public void onCreate() {
		super.onCreate();

		mInstance = this;

		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");

		// Initialize the preferences
		initializePreferences();

		// Database setup
		sDbMovies = new DbAdapterMovies(this);
		sDbMovieMapping = new DbAdapterMovieMappings(this);
		sDbTvShowEpisode = new DbAdapterTvShowEpisodes(this);
		sDbTvShow = new DbAdapterTvShows(this);
		sDbTvShowEpisodeMappings = new DbAdapterTvShowEpisodeMappings(this);
		sDbSources = new DbAdapterSources(this);
		sDbCollections = new DbAdapterCollections(this);

		getMovieThumbFolder(this);
		getMovieBackdropFolder(this);
		getTvShowThumbFolder(this);
		getTvShowBackdropFolder(this);
		getTvShowEpisodeFolder(this);
		getTvShowSeasonFolder(this);
		getAvailableOfflineFolder(this);

		transitionLocalizationPreference();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		sDbTvShow.close();
		sDbTvShowEpisode.close();
		sDbSources.close();
		sDbMovies.close();
		sDbMovieMapping.close();
		sDbCollections.close();
	}

	public static Context getContext() {
		return mInstance;
	}

	private void initializePreferences() {
		PreferenceManager.setDefaultValues(this, R.xml.advanced_prefs, false);
		PreferenceManager.setDefaultValues(this, R.xml.general_prefs, false);
		PreferenceManager.setDefaultValues(this, R.xml.identification_search_prefs, false);
		PreferenceManager.setDefaultValues(this, R.xml.other_prefs, false);
		PreferenceManager.setDefaultValues(this, R.xml.user_interface_prefs, false);
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

	public static DbAdapterTvShows getTvDbAdapter() {
		return sDbTvShow;
	}

	public static DbAdapterTvShowEpisodes getTvEpisodeDbAdapter() {
		return sDbTvShowEpisode;
	}

	public static DbAdapterTvShowEpisodeMappings getTvShowEpisodeMappingsDbAdapter() {
		return sDbTvShowEpisodeMappings;
	}

	public static DbAdapterSources getSourcesAdapter() {
		return sDbSources;
	}

	public static DbAdapterMovies getMovieAdapter() {
		return sDbMovies;
	}

	public static DbAdapterMovieMappings getMovieMappingAdapter() {
		return sDbMovieMapping;
	}

	public static DbAdapterCollections getCollectionsAdapter() {
		return sDbCollections;
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
			sPicasso = Picasso.with(context);
		return sPicasso;
	}

	public static Bitmap.Config getBitmapConfig() {
		return Bitmap.Config.RGB_565;
	}

	public static Typeface getOrCreateTypeface(Context context, String key) {
		if (!sTypefaces.containsKey(key))
			sTypefaces.put(key, Typeface.createFromAsset(context.getAssets(), key));
		return sTypefaces.get(key);
	}

	public static Palette getPalette(String key) {
		return sPalettes.get(key);
	}

	public static void addToPaletteCache(String key, Palette palette) {
		sPalettes.put(key, palette);
	}

	public static void setupTheme(Context context) {
		context.setTheme(R.style.Mizuu_Theme);
	}

	public static Bus getBus() {
		if (sBus == null)
			sBus = new Bus();
		return sBus;
	}

	public static File getAppFolder(Context c) {
		if (sBaseAppFolder == null) {
			sBaseAppFolder = c.getExternalFilesDir(null);
		}
		return sBaseAppFolder;
	}

	private static File getSubAppFolder(Context c, String foldername) {
		return new File(getAppFolder(c), foldername);
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getMovieThumbFolder(Context c) {
		if (sMovieThumbFolder == null) {
			sMovieThumbFolder = getSubAppFolder(c, "movie-thumbs");
			sMovieThumbFolder.mkdirs();
		}
		return sMovieThumbFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getMovieBackdropFolder(Context c) {
		if (sMovieBackdropFolder == null) {
			sMovieBackdropFolder = getSubAppFolder(c, "movie-backdrops");
			sMovieBackdropFolder.mkdirs();
		}
		return sMovieBackdropFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowThumbFolder(Context c) {
		if (sTvShowThumbFolder == null) {
			sTvShowThumbFolder = getSubAppFolder(c, "tvshows-thumbs");
			sTvShowThumbFolder.mkdirs();
		}
		return sTvShowThumbFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowBackdropFolder(Context c) {
		if (sTvShowBackdropFolder == null) {
			sTvShowBackdropFolder = getSubAppFolder(c, "tvshows-backdrops");
			sTvShowBackdropFolder.mkdirs();
		}
		return sTvShowBackdropFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowEpisodeFolder(Context c) {
		if (sTvShowEpisodeFolder == null) {
			sTvShowEpisodeFolder = getSubAppFolder(c, "tvshows-episodes");
			sTvShowEpisodeFolder.mkdirs();
		}
		return sTvShowEpisodeFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific image.
	 */
	public static File getTvShowSeasonFolder(Context c) {
		if (sTvShowSeasonFolder == null) {
			sTvShowSeasonFolder = getSubAppFolder(c, "tvshows-seasons");
			sTvShowSeasonFolder.mkdirs();
		}
		return sTvShowSeasonFolder;
	}

	/*
	 * Please refrain from using this when you need a File object for a specific video.
	 */
	public static File getAvailableOfflineFolder(Context c) {
		if (sAvailableOfflineFolder == null) {
			sAvailableOfflineFolder = getSubAppFolder(c, "offline_storage");
			sAvailableOfflineFolder.mkdirs();
		}
		return sAvailableOfflineFolder;
	}

	/*
	 * Cache folder is used to store videos that are available offline as well
	 * as user profile photo from Trakt.
	 */
	public static File getCacheFolder(Context c) {
		if (sCacheFolder == null) {
			sCacheFolder = getSubAppFolder(c, "app_cache");
			sCacheFolder.mkdirs();
		}
		return sCacheFolder;
	}

	public static TvShowApiService getTvShowService(Context context) {
		return TMDbTvShowService.getInstance(context);
	}

	public static MovieApiService getMovieService(Context context) {
		return TMDbMovieService.getInstance(context);
	}

	/**
	 * This is used as an optimization to loading the movie library view.
	 * @param filepaths
	 */
	public static void setMovieFilepaths(ArrayListMultimap<String, String> filepaths) {
		mMovieFilepaths = filepaths;
	}

	public static List<String> getMovieFilepaths(String id) {
		if (mMovieFilepaths != null && mMovieFilepaths.containsKey(id))
			return mMovieFilepaths.get(id);
		return null;
	}

	/**
	 * OkHttpClient singleton with 2 MB cache.
	 * @return
	 */
	public static OkHttpClient getOkHttpClient() {
		if (mOkHttpClient == null) {
			mOkHttpClient = new OkHttpClient();

			File cacheDir = getContext().getCacheDir();
			Cache cache = new Cache(cacheDir, 2 * 1024 * 1024);
			mOkHttpClient.setCache(cache);
		}

		return mOkHttpClient;
	}

	public static void clearPicassoCache(Context context) {
		PicassoTools.clearCache(getPicasso(context));
	}
}