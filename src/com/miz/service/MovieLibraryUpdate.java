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

package com.miz.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.miz.abstractclasses.MovieFileSource;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterSources;
import com.miz.filesources.FileMovie;
import com.miz.filesources.SmbMovie;
import com.miz.filesources.UpnpMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.functions.MovieLibraryUpdateCallback;
import com.miz.functions.NfoMovie;
import com.miz.functions.TheMovieDb;
import com.miz.mizuu.CancelLibraryUpdate;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieBackdropWidgetProvider;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;

import static com.miz.functions.PreferenceKeys.SYNC_WITH_TRAKT;
import static com.miz.functions.PreferenceKeys.CLEAR_LIBRARY_MOVIES;
import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.miz.functions.PreferenceKeys.IGNORED_FILES_ENABLED;
import static com.miz.functions.PreferenceKeys.REMOVE_UNAVAILABLE_FILES_MOVIES;
import static com.miz.functions.PreferenceKeys.DISABLE_ETHERNET_WIFI_CHECK;
import static com.miz.functions.PreferenceKeys.ENABLE_SUBFOLDER_SEARCH;

public class MovieLibraryUpdate extends IntentService implements MovieLibraryUpdateCallback {

	public static final String STOP_MOVIE_LIBRARY_UPDATE = "mizuu-stop-movie-library-update";
	private boolean isDebugging = true;
	private ArrayList<FileSource> mFileSources;
	private ArrayList<MovieFileSource<?>> mMovieFileSources;
	private HashMap<String, InputStream> mNfoFiles = new HashMap<String, InputStream>();
	private LinkedList<String> mMovieQueue = new LinkedList<String>();
	private boolean mIgnoreRemovedFiles, mClearLibrary, mSearchSubfolders, mClearUnavailable, mIgnoreNfoFiles, mDisableEthernetWiFiCheck, mSyncLibraries, mStopUpdate;
	private int mTotalFiles, mCount;
	private SharedPreferences mSettings;
	private Editor mEditor;
	private final int NOTIFICATION_ID = 200, POST_UPDATE_NOTIFICATION = 213;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;

	public MovieLibraryUpdate() {
		super("MovieLibraryUpdate");
	}

	public MovieLibraryUpdate(String name) {
		super(name);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		log("onDestroy()");

		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);

		reloadLibrary();
		
		showPostUpdateNotification();

		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

		MizLib.scheduleMovieUpdate(this);

		if (Trakt.hasTraktAccount(this) && mSyncLibraries && ((mTotalFiles - mMovieQueue.size()) > 0)) {
			getApplicationContext().startService(new Intent(getApplicationContext(), TraktMoviesSyncService.class));
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		log("clear()");

		// Clear and set up all variables
		clear();

		log("setup()");

		// Set up Notification, variables, etc.
		setup();

		log("loadFileSources()");

		// Load all file sources from the database
		loadFileSources();

		log("setupMovieFileSources()");

		// Add the different file sources to the MovieFileSource ArrayList
		setupMovieFileSources(mIgnoreRemovedFiles, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck);

		if (mStopUpdate)
			return;

		log("removeUnidentifiedFiles()");

		// Remove unavailable movies, so we can try to identify them again
		if (!mClearLibrary)
			removeUnidentifiedFiles();

		if (mStopUpdate)
			return;

		// Check if the library should be cleared
		if (mClearLibrary) {

			// Reset the preference, so it isn't checked the next
			// time the user wants to update the library
			mEditor = mSettings.edit();
			mEditor.putBoolean(CLEAR_LIBRARY_MOVIES, false);
			mEditor.commit();

			log("removeMoviesFromDatabase()");

			// Remove all entries from the database
			removeMoviesFromDatabase();
		}

		if (mStopUpdate)
			return;

		// Check if we should remove all unavailable files.
		// Note that this only makes sense if we haven't already cleared the library.
		if (!mClearLibrary && mClearUnavailable) {

			log("removeUnavailableFiles()");

			// Remove all unavailable files from the database
			removeUnavailableFiles();
		}

		log("searchFolders()");

		if (mStopUpdate)
			return;
		
		reloadLibrary();

		// Search all folders
		searchFolders();

		if (mStopUpdate)
			return;
		log("mTotalFiles > 0 check");

		// Check if we've found any files to identify
		if (mTotalFiles > 0) {
			log("updateMovies()");

			// Start the actual movie update / identification task
			updateMovies();
		}
	}
	
	private void reloadLibrary() {
		log("reloadLibrary()");
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-movies-update"));
	}

	private void loadFileSources() {
		mFileSources = new ArrayList<FileSource>();
		DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();
		Cursor c = dbHelperSources.fetchAllMovieSources();
		try {
			while (c.moveToNext()) {
				mFileSources.add(new FileSource(
						c.getLong(c.getColumnIndex(DbAdapterSources.KEY_ROWID)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
						c.getInt(c.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_USER)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_TYPE))
						));
			}
		} catch (Exception e) {
		} finally {
			c.close();
		}
	}

	private void setupMovieFileSources(boolean mIgnoreRemovedFiles, boolean mSearchSubfolders, boolean mClearLibrary, boolean mDisableEthernetWiFiCheck) {
		for (FileSource fileSource : mFileSources) {
			if (mStopUpdate)
				return;
			switch (fileSource.getFileSourceType()) {
			case FileSource.FILE:
				mMovieFileSources.add(new FileMovie(getApplicationContext(), fileSource, mIgnoreRemovedFiles, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			case FileSource.SMB:
				mMovieFileSources.add(new SmbMovie(getApplicationContext(), fileSource, mIgnoreRemovedFiles, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			case FileSource.UPNP:
				mMovieFileSources.add(new UpnpMovie(getApplicationContext(), fileSource, mIgnoreRemovedFiles, mSearchSubfolders, mClearLibrary, mDisableEthernetWiFiCheck));
				break;
			}
		}
	}

	private void removeUnidentifiedFiles() {
		for (MovieFileSource<?> movieFileSource : mMovieFileSources) {
			movieFileSource.removeUnidentifiedFiles();
		}
	}

	private void removeMoviesFromDatabase() {
		// Delete all movies from the database
		DbAdapterMovies db = MizuuApplication.getMovieAdapter();
		db.deleteAllMovies();

		// Delete all downloaded images files from the device
		MizLib.deleteRecursive(MizuuApplication.getMovieThumbFolder(this), false);
		MizLib.deleteRecursive(MizuuApplication.getMovieBackdropFolder(this), false);
	}

	private void removeUnavailableFiles() {
		for (MovieFileSource<?> movieFileSource : mMovieFileSources) {
			movieFileSource.removeUnavailableFiles();
		}
	}

	private void searchFolders() {

		// Temporary collections
		List<String> tempList = null;
		HashMap<String, InputStream> tempNfoMap = null;

		for (int j = 0; j < mMovieFileSources.size(); j++) {
			updateMovieScaningNotification(mMovieFileSources.get(j).toString());
			tempList = mMovieFileSources.get(j).searchFolder();
			for (int i = 0; i < tempList.size(); i++)
				mMovieQueue.add(tempList.get(i));

			if (!mIgnoreNfoFiles) {
				tempNfoMap = mMovieFileSources.get(j).getNfoFiles();
				mNfoFiles.putAll(tempNfoMap);
			}
		}

		// Clean up...
		if (tempList != null)
			tempList.clear();

		if (tempNfoMap != null)
			tempNfoMap.clear();

		mTotalFiles = mMovieQueue.size();
	}

	private void setup() {
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter(STOP_MOVIE_LIBRARY_UPDATE));

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelLibraryUpdate.class);
		notificationIntent.putExtra("isMovie", true);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
		mBuilder.setSmallIcon(R.drawable.refresh);
		mBuilder.setTicker(getString(R.string.updatingMovies));
		mBuilder.setContentTitle(getString(R.string.updatingMovies));
		mBuilder.setContentText(getString(R.string.gettingReady));
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.refresh));
		mBuilder.addAction(R.drawable.ic_action_discard, getString(android.R.string.cancel), contentIntent);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);

		mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mClearLibrary = mSettings.getBoolean(CLEAR_LIBRARY_MOVIES, false);
		mSearchSubfolders = mSettings.getBoolean(ENABLE_SUBFOLDER_SEARCH, true);
		mClearUnavailable = mSettings.getBoolean(REMOVE_UNAVAILABLE_FILES_MOVIES, false);
		mIgnoreNfoFiles = mSettings.getBoolean(IGNORED_NFO_FILES, true);
		mDisableEthernetWiFiCheck = mSettings.getBoolean(DISABLE_ETHERNET_WIFI_CHECK, false);
		mIgnoreRemovedFiles = mSettings.getBoolean(IGNORED_FILES_ENABLED, false);
		mSyncLibraries = mSettings.getBoolean(SYNC_WITH_TRAKT, true);

		mEditor = mSettings.edit();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStopUpdate = true;
		}
	};

	private void updateMovies() {
		// Temporary StringBuilder objects to re-use memory and reduce GC's
		StringBuilder sb = new StringBuilder(), sb1 = new StringBuilder(), sb2 = new StringBuilder();

		// Let's sort the files by their file name before updating
		Collections.sort(mMovieQueue);

		while (mMovieQueue.peek() != null) {
			if (mStopUpdate)
				break;

			sb.delete(0, sb.length());
			sb.append(mMovieQueue.pop());
			
			mCount++;

			if (mIgnoreNfoFiles) {
				newMovieDbObject(sb.toString());
			} else {				
				sb1.delete(0, sb1.length());
				sb1.append(MizLib.removeExtension(sb.toString().replaceAll("part[1-9]|cd[1-9]", "").trim()));

				sb2.delete(0, sb2.length());
				sb2.append(MizLib.convertToGenericNfo(sb.toString()));

				if (mNfoFiles.containsKey(sb1.toString())) {
					new NfoMovie(sb.toString(), mNfoFiles.get(sb1.toString()), getApplicationContext(), this);
				} else if (mNfoFiles.containsKey(sb2.toString())) {
					new NfoMovie(sb.toString(), mNfoFiles.get(sb2.toString()), getApplicationContext(), this);
				} else {
					newMovieDbObject(sb.toString());
				}
			}
		}
	}

	/**
	 * Used in the updateMovies() method.
	 * @param file Path of the file we want to identify
	 */
	private void newMovieDbObject(String file) {
		if (MizLib.isOnline(getApplicationContext())) {
			new TheMovieDb(this, file, this);
		}
	}

	private void clear() {
		// Lists
		mFileSources = new ArrayList<FileSource>();
		mMovieFileSources = new ArrayList<MovieFileSource<?>>();
		mMovieQueue = new LinkedList<String>();
		mNfoFiles = new HashMap<String, InputStream>();

		// Booleans
		mIgnoreRemovedFiles = false;
		mClearLibrary = false;
		mSearchSubfolders = true;
		mClearUnavailable = false;
		mIgnoreNfoFiles = true;
		mDisableEthernetWiFiCheck = false;
		mSyncLibraries = true;
		mStopUpdate = false;

		// Other variables
		mEditor = null;
		mSettings = null;
		mTotalFiles = 0;
		mCount = 0;
		mNotificationManager = null;
		mBuilder = null;
	}

	private void log(String msg) {
		if (isDebugging)
			Log.d("MovieLibraryUpdate", msg);
	}

	@Override
	public void onMovieAdded(String title, String cover, String backdrop) {
		updateMovieAddedNotification(title, cover, backdrop);
	}

	private void updateMovieAddedNotification(String title, String cover, String backdrop) {
		mBuilder.setLargeIcon(MizLib.getNotificationImageThumbnail(this, cover));
		mBuilder.setContentTitle(getString(R.string.updatingMovies) + " (" + (int) ((100.0 / (double) mTotalFiles) * (double) mCount) + "%)");
		mBuilder.setContentText(getString(R.string.stringJustAdded) + ": " + title);
		mBuilder.setStyle(
				new NotificationCompat.BigPictureStyle()
				.setSummaryText(getString(R.string.stringJustAdded) + ": " + title)
				.bigPicture(
						MizLib.decodeSampledBitmapFromFile(backdrop, getNotificationImageWidth(), getNotificationImageHeight())
						)
				);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void updateMovieScaningNotification(String filesource) {
		mBuilder.setSmallIcon(R.drawable.refresh);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.refresh));
		mBuilder.setContentTitle(getString(R.string.updatingMovies));
		mBuilder.setContentText(getString(R.string.scanning) + ": " + filesource);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void showPostUpdateNotification() {
		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, Main.class);
		notificationIntent.putExtra("fromUpdate", true);
		notificationIntent.putExtra("startup", "1");
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		if (!mStopUpdate) {
			mBuilder.setSmallIcon(R.drawable.done);
			mBuilder.setTicker(getString(R.string.finishedMovieLibraryUpdate));
			mBuilder.setContentTitle(getString(R.string.finishedMovieLibraryUpdate));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + (mTotalFiles - mMovieQueue.size()) + " " + getResources().getQuantityString(R.plurals.moviesInLibrary, (mTotalFiles - mMovieQueue.size()), (mTotalFiles - mMovieQueue.size())));
			mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.done));
		} else {
			mBuilder.setSmallIcon(R.drawable.ignoresmallfiles);
			mBuilder.setTicker(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentTitle(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + (mTotalFiles - mMovieQueue.size()) + " " + getResources().getQuantityString(R.plurals.moviesInLibrary, (mTotalFiles - mMovieQueue.size()), (mTotalFiles - mMovieQueue.size())));
			mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ignoresmallfiles));
		}
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if ((mTotalFiles - mMovieQueue.size()) > 0)
			mNotificationManager.notify(POST_UPDATE_NOTIFICATION, updateNotification);
	}

	// These variables don't need to be re-initialized
	private int widgetWidth = 0, widgetHeight = 0;

	private int getNotificationImageWidth() {
		if (widgetWidth == 0)
			widgetWidth = MizLib.getLargeNotificationWidth(this);
		return widgetWidth;
	}

	private int getNotificationImageHeight() {
		if (widgetHeight == 0)
			widgetHeight = MizLib.getLargeNotificationWidth(this) / 2;
		return widgetHeight;
	}
}