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

package com.miz.functions;

import java.io.File;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterMovieMapping;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieBackdropWidgetProvider;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;
import static com.miz.functions.PreferenceKeys.IGNORED_FILENAME_TAGS;

public class TheMovieDb {

	private MovieLibraryUpdateCallback callback;
	private Context context;
	private TMDbMovie movie;
	private String LOCALE = "", filepath = "", language = "", mOldTmdbId;
	private boolean isFromManualIdentify = false;
	private SharedPreferences settings;

	public TheMovieDb(Context context, String filepath, boolean isFromManualIdentify, String language, String oldTmdbId, String tmdbId) {
		this.context = context;
		this.filepath = filepath;
		this.isFromManualIdentify = isFromManualIdentify;
		this.language = language;
		mOldTmdbId = oldTmdbId;

		setup();

		TMDb tmdb = new TMDb(context);
		if (isFromManualIdentify)
			movie = tmdb.getMovie(tmdbId, LOCALE);
		else {
			DecryptedMovie mMovie = MizLib.decryptMovie(filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[0] : filepath, settings.getString(IGNORED_FILENAME_TAGS, ""));
			movie = tmdb.searchForMovie(mMovie.getDecryptedFileName(), mMovie.getFileNameYear(), filepath.contains("<MiZ>") ? filepath.split("<Miz>")[0] : filepath, LOCALE);
		}

		download();
	}

	public TheMovieDb(Context context, String filepath, MovieLibraryUpdateCallback callback) {
		this.context = context;
		this.filepath = filepath;
		this.language = "";
		this.callback = callback;

		setup();

		TMDb tmdb = new TMDb(context);
		DecryptedMovie mMovie = MizLib.decryptMovie(filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[0] : filepath, settings.getString(IGNORED_FILENAME_TAGS, ""));		
		movie = tmdb.searchForMovie(mMovie.getDecryptedFileName(), mMovie.getFileNameYear(), filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[0] : filepath, LOCALE);

		download();
	}

	private void setup() {
		settings = PreferenceManager.getDefaultSharedPreferences(context);

		if (language.isEmpty()) {
			LOCALE = PreferenceManager.getDefaultSharedPreferences(context).getString(LANGUAGE_PREFERENCE, "en");
		} else {
			LOCALE = language;
		}
	}

	private void download() {
		String thumb_filepath = MizLib.getMovieThumb(context, movie.getId()).getAbsolutePath();

		// Download the movie.getCover() file and try again if it fails
		if (!MizLib.downloadFile(movie.getCover(), thumb_filepath))
			MizLib.downloadFile(movie.getCover(), thumb_filepath);

		// Download the backdrop file and try again if it fails
		if (!movie.getBackdrop().equals("NOIMG")) {
			String backdropFile = MizLib.getMovieBackdrop(context, movie.getId()).getAbsolutePath();

			if (!MizLib.downloadFile(movie.getBackdrop(), backdropFile))
				MizLib.downloadFile(movie.getBackdrop(), backdropFile);
		}

		// Download the collection image
		if (!movie.getCollectionImage().isEmpty()) {
			String collectionImage = MizLib.getMovieThumb(context, movie.getCollectionId()).getAbsolutePath();

			if (!MizLib.downloadFile(movie.getCollectionImage(), collectionImage))
				MizLib.downloadFile(movie.getCollectionImage(), collectionImage);
		}

		addToDatabase();
	}

	private void addToDatabase() {

		DbAdapterMovieMapping dbHelperMovieMapping = MizuuApplication.getMovieMappingAdapter();
		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();

		if (isFromManualIdentify) {
			// Update the mapping TMDb ID to the new one
			dbHelperMovieMapping.updateTmdbId(filepath, movie.getId());
			
			// Let's check if the old TMDb ID is mapped to
			// any other file paths - if not, remove the movie
			// entry in the movie database
			if (!dbHelperMovieMapping.exists(mOldTmdbId))
				dbHelper.deleteMovie(mOldTmdbId);
		} else {
			// Create the mapping
			dbHelperMovieMapping.createFilepathMapping(filepath, movie.getId());
		}
		
		// Finally, create or update the movie
		dbHelper.createOrUpdateMovie(movie.getId(), movie.getTitle(), movie.getPlot(), movie.getImdbId(), movie.getRating(), movie.getTagline(),
				movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0",
				movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

		updateNotification();
	}

	private void updateNotification() {
		if (!isFromManualIdentify) {
			File backdropFile = MizLib.getMovieBackdrop(context, movie.getId());
			if (!backdropFile.exists())
				backdropFile = MizLib.getMovieThumb(context, movie.getId());

			if (callback != null)
				callback.onMovieAdded(movie.getTitle(), MizLib.getMovieThumb(context, movie.getId()).getAbsolutePath(), backdropFile.getAbsolutePath());
		} else {
			sendUpdateBroadcast(new Intent("mizuu-movies-identification"));
			sendUpdateBroadcast(new Intent("mizuu-library-change"));
			updateWidgets();
		}
	}

	private void sendUpdateBroadcast(Intent i) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
	}

	private void updateWidgets() {
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}
}