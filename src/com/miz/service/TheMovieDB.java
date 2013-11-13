package com.miz.service;

import java.io.File;
import java.util.Locale;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.db.DbAdapter;
import com.miz.functions.DecryptedMovie;
import com.miz.functions.MizLib;
import com.miz.functions.TMDb;
import com.miz.functions.TMDbMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieBackdropWidgetProvider;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;

public class TheMovieDB extends IntentService {

	private TMDbMovie movie;
	private String LOCALE, filepath, language;
	private boolean localizedInfo, isFromManualIdentify;
	private File thumbsFolder;
	private long rowId;
	private SharedPreferences settings;

	public TheMovieDB() {
		super("TheMovieDB");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		initializeFields();

		Bundle bundle = intent.getExtras();
		filepath = bundle.getString("filepath");
		isFromManualIdentify = bundle.getBoolean("isFromManualIdentify", false);
		rowId = bundle.getLong("rowId", 0);
		language = bundle.getString("language", "");

		setup();

		TMDb tmdb = new TMDb(this);
		if (isFromManualIdentify)
			movie = tmdb.getMovie(bundle.getString("tmdbId", "invalid"), LOCALE);
		else {
			DecryptedMovie mMovie = MizLib.decryptMovie(filepath, settings.getString("ignoredTags", ""));
			movie = tmdb.searchForMovie(mMovie.getDecryptedFileName(), mMovie.getFileNameYear(), filepath, LOCALE);
		}
		
		download();
	}

	private void setup() {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
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

		thumbsFolder = MizLib.getMovieThumbFolder(this);
	}

	private void initializeFields() {
		LOCALE = "";
		filepath = "";
		localizedInfo = false;
		isFromManualIdentify = false;
		language = "";
	}

	private void download() {
		String thumb_filepath = new File(thumbsFolder, movie.getId() + ".jpg").getAbsolutePath();

		// Download the movie.getCover() file and try again if it fails
		if (!MizLib.downloadFile(movie.getCover(), thumb_filepath))
			MizLib.downloadFile(movie.getCover(), thumb_filepath);
		
		// Download the backdrop file and try again if it fails
		if (!movie.getBackdrop().equals("NOmovie.getCover()IMG")) {
			String backdropFile = new File(MizLib.getMovieBackdropFolder(this), movie.getId() + "_bg.jpg").getAbsolutePath();
			
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
		Intent intent = null;
		if (!isFromManualIdentify) {
			intent = new Intent("mizuu-movies-object");
			intent.putExtra("movieName", movie.getTitle());
			intent.putExtra("thumbFile", new File(thumbsFolder, movie.getId() + ".jpg").getAbsolutePath());

			File backdropFile = new File(MizLib.getMovieBackdropFolder(this), movie.getId() + "_bg.jpg");

			if (!backdropFile.exists())
				backdropFile = new File(thumbsFolder, movie.getId() + ".jpg");

			intent.putExtra("backdrop", backdropFile.getAbsolutePath());
		} else {
			intent = new Intent("mizuu-movies-identification");
		}

		sendUpdateBroadcast(intent);
		sendUpdateBroadcast(new Intent("mizuu-movies-update"));
		
		updateWidgets();
	}

	private void sendUpdateBroadcast(Intent i) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
	}
	
	private void updateWidgets() {
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}
}