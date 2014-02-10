package com.miz.functions;

import java.io.File;
import java.util.Locale;

import com.miz.db.DbAdapter;
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

public class TheMovieDb {

	private MovieLibraryUpdateCallback callback;
	private Context context;
	private TMDbMovie movie;
	private String LOCALE = "", filepath = "", language = "";
	private boolean localizedInfo = false, isFromManualIdentify = false;
	private File thumbsFolder;
	private long rowId;
	private SharedPreferences settings;

	public TheMovieDb(Context context, String filepath, boolean isFromManualIdentify, long rowId, String language, String tmdbId) {
		this.context = context;
		this.filepath = filepath;
		this.isFromManualIdentify = isFromManualIdentify;
		this.rowId = rowId;
		this.language = language;

		setup();

		TMDb tmdb = new TMDb(context);
		if (isFromManualIdentify)
			movie = tmdb.getMovie(tmdbId, LOCALE);
		else {
			DecryptedMovie mMovie = MizLib.decryptMovie(filepath, settings.getString("ignoredTags", ""));
			movie = tmdb.searchForMovie(mMovie.getDecryptedFileName(), mMovie.getFileNameYear(), filepath, LOCALE);
		}

		download();
	}

	public TheMovieDb(Context context, String filepath, MovieLibraryUpdateCallback callback) {
		this.context = context;
		this.filepath = filepath;
		this.rowId = 0;
		this.language = "";
		this.callback = callback;

		setup();

		TMDb tmdb = new TMDb(context);
		DecryptedMovie mMovie = MizLib.decryptMovie(filepath, settings.getString("ignoredTags", ""));
		movie = tmdb.searchForMovie(mMovie.getDecryptedFileName(), mMovie.getFileNameYear(), filepath, LOCALE);

		download();
	}

	private void setup() {
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		localizedInfo = settings.getBoolean("prefsUseLocalData", false);

		if (language.isEmpty()) {
			if (localizedInfo) {
				LOCALE = Locale.getDefault().toString();
				if (LOCALE.contains("_")) LOCALE = LOCALE.substring(0, LOCALE.indexOf("_"));
				else LOCALE = "en";
			} else LOCALE = "en";
		} else {
			LOCALE = language;
		}

		thumbsFolder = MizLib.getMovieThumbFolder(context);
	}

	private void download() {
		String thumb_filepath = new File(thumbsFolder, movie.getId() + ".jpg").getAbsolutePath();

		// Download the movie.getCover() file and try again if it fails
		if (!MizLib.downloadFile(movie.getCover(), thumb_filepath))
			MizLib.downloadFile(movie.getCover(), thumb_filepath);

		// Download the backdrop file and try again if it fails
		if (!movie.getBackdrop().equals("NOIMG")) {
			String backdropFile = new File(MizLib.getMovieBackdropFolder(context), movie.getId() + "_bg.jpg").getAbsolutePath();

			if (!MizLib.downloadFile(movie.getBackdrop(), backdropFile))
				MizLib.downloadFile(movie.getBackdrop(), backdropFile);
		}

		// Download the collection image
		if (!movie.getCollectionImage().isEmpty()) {
			String collectionImage = new File(thumbsFolder, movie.getCollectionId() + ".jpg").getAbsolutePath();

			if (!MizLib.downloadFile(movie.getCollectionImage(), collectionImage))
				MizLib.downloadFile(movie.getCollectionImage(), collectionImage);
		}

		addToDatabase();
	}

	private void addToDatabase() {
		// Create and open database
		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();
		if (!isFromManualIdentify)
			dbHelper.createMovie(filepath, movie.getCover(), movie.getTitle(), movie.getPlot(), movie.getId(), movie.getImdbId(), movie.getRating(), movie.getTagline(), movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0", movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));
		else
			dbHelper.updateMovie(rowId, filepath, movie.getCover(), movie.getTitle(), movie.getPlot(), movie.getId(), movie.getImdbId(), movie.getRating(), movie.getTagline(), movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0", movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

		updateNotification();
	}

	private void updateNotification() {
		if (!isFromManualIdentify) {
			File backdropFile = new File(MizLib.getMovieBackdropFolder(context), movie.getId() + "_bg.jpg");
			if (!backdropFile.exists())
				backdropFile = new File(thumbsFolder, movie.getId() + ".jpg");

			if (callback != null)
				callback.onMovieAdded(movie.getTitle(), new File(thumbsFolder, movie.getId() + ".jpg").getAbsolutePath(), backdropFile.getAbsolutePath());
		} else {
			sendUpdateBroadcast(new Intent("mizuu-movies-identification"));
			updateWidgets();
		}

		sendUpdateBroadcast(new Intent("mizuu-movies-update"));
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