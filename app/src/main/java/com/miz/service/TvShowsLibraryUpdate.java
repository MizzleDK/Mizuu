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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.miz.abstractclasses.TvShowFileSource;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.filesources.FileTvShow;
import com.miz.filesources.SmbTvShow;
import com.miz.filesources.UpnpTvShow;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.functions.TvShowLibraryUpdateCallback;
import com.miz.identification.ShowStructure;
import com.miz.identification.TvShowIdentification;
import com.miz.mizuu.CancelLibraryUpdate;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;
import com.miz.utils.LocalBroadcastUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.miz.functions.PreferenceKeys.CLEAR_LIBRARY_TVSHOWS;
import static com.miz.functions.PreferenceKeys.REMOVE_UNAVAILABLE_FILES_TVSHOWS;
import static com.miz.functions.PreferenceKeys.SYNC_WITH_TRAKT;

public class TvShowsLibraryUpdate extends IntentService implements TvShowLibraryUpdateCallback {

	public static final String STOP_TVSHOW_LIBRARY_UPDATE = "mizuu-stop-tvshow-library-update";
	private boolean mDebugging = true;
	private ArrayList<FileSource> mFileSources;
	private ArrayList<TvShowFileSource<?>> mTvShowFileSources;
	private ArrayList<ShowStructure> mFiles;
	private HashSet<String> mUniqueShowIds = new HashSet<String>();
	private boolean mClearLibrary, mClearUnavailable, mSyncLibraries, mStopUpdate;
	private int mTotalFiles, mShowCount, mEpisodeCount;
	private SharedPreferences mSettings;
	private Editor mEditor;
	private final int NOTIFICATION_ID = 300, POST_UPDATE_NOTIFICATION = 313;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;
	private TvShowIdentification mIdentification;

	public TvShowsLibraryUpdate() {
		super("TvShowsLibraryUpdate");
	}

	public TvShowsLibraryUpdate(String name) {
		super(name);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		log("onDestroy()");

		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);

		LocalBroadcastUtils.updateTvShowLibrary(this);

		showPostUpdateNotification();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

		MizLib.scheduleShowsUpdate(this);

		if (Trakt.hasTraktAccount(this) && mSyncLibraries && (mEpisodeCount > 0)) {
			startService(new Intent(getApplicationContext(), TraktTvShowsSyncService.class));
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

		log("setupTvShowsFileSources()");

		// Add the different file sources to the TvShowFileSource ArrayList
		setupTvShowsFileSources(mClearLibrary);

		if (mStopUpdate)
			return;

		log("removeUnidentifiedFiles()");

		// Remove unavailable TV show files, so we can try to identify them again
		if (!mClearLibrary)
			removeUnidentifiedFiles();

		if (mStopUpdate)
			return;

		// Check if the library should be cleared
		if (mClearLibrary) {

			// Reset the preference, so it isn't checked the next
			// time the user wants to update the library
			mEditor = mSettings.edit();
			mEditor.putBoolean(CLEAR_LIBRARY_TVSHOWS, false);
			mEditor.apply();

			log("removeTvShowsFromDatabase()");

			// Remove all entries from the database
			removeTvShowsFromDatabase();
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

		// Search all folders
		searchFolders();

		if (mStopUpdate)
			return;
		log("mTotalFiles > 0 check");

		// Check if we've found any files to identify
		if (mTotalFiles > 0) {
			log("updateTvShows()");

			// Start the actual TV shows update / identification task
			updateTvShows();
		}
	}

	private void loadFileSources() {
		mFileSources = new ArrayList<FileSource>();
		DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();
		Cursor c = dbHelperSources.fetchAllShowSources();
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

	private void setupTvShowsFileSources(boolean mClearLibrary) {
		for (FileSource fileSource : mFileSources) {
			if (mStopUpdate)
				return;
			switch (fileSource.getFileSourceType()) {
			case FileSource.FILE:
				mTvShowFileSources.add(new FileTvShow(getApplicationContext(), fileSource, mClearLibrary));
				break;
			case FileSource.SMB:
				mTvShowFileSources.add(new SmbTvShow(getApplicationContext(), fileSource, mClearLibrary));
				break;
			case FileSource.UPNP:
				mTvShowFileSources.add(new UpnpTvShow(getApplicationContext(), fileSource, mClearLibrary));
				break;
			}
		}
	}

	private void removeUnidentifiedFiles() {
		for (TvShowFileSource<?> tvShowFileSource : mTvShowFileSources) {
			tvShowFileSource.removeUnidentifiedFiles();
		}
	}

	private void removeTvShowsFromDatabase() {
		// Delete all shows from the database
		DbAdapterTvShows db = MizuuApplication.getTvDbAdapter();
		db.deleteAllShowsInDatabase();

		DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();
		dbEpisodes.deleteAllEpisodes();
		
		MizuuApplication.getTvShowEpisodeMappingsDbAdapter().deleteAllFilepaths();

		// Delete all downloaded images files from the device
		FileUtils.deleteRecursive(MizuuApplication.getTvShowThumbFolder(this), false);
		FileUtils.deleteRecursive(MizuuApplication.getTvShowEpisodeFolder(this), false);
		FileUtils.deleteRecursive(MizuuApplication.getTvShowBackdropFolder(this), false);
		FileUtils.deleteRecursive(MizuuApplication.getTvShowSeasonFolder(this), false);
	}

	private void removeUnavailableFiles() {
		for (TvShowFileSource<?> tvShowFileSource : mTvShowFileSources) {
			tvShowFileSource.removeUnavailableFiles();
		}
	}

	private void searchFolders() {
		// Temporary collection
		List<String> tempList = null;

		for (int j = 0; j < mTvShowFileSources.size(); j++) {
			updateTvShowScanningNotification(mTvShowFileSources.get(j).toString());
			tempList = mTvShowFileSources.get(j).searchFolder();
			for (int i = 0; i < tempList.size(); i++) {
				mFiles.add(new ShowStructure(tempList.get(i)));
			}
		}

		// Clean up...
		if (tempList != null)
			tempList.clear();

		int episodeCount = 0;
		for (ShowStructure ss : mFiles)
			episodeCount += ss.getEpisodes().size();
		
		mTotalFiles = episodeCount;
	}

	private void setup() {
		if (!MizLib.isOnline(this)) {
			mStopUpdate = true;
			return;
		}

		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter(STOP_TVSHOW_LIBRARY_UPDATE));

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelLibraryUpdate.class);
		notificationIntent.putExtra("isMovie", false);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		mBuilder.setTicker(getString(R.string.updatingTvShows));
		mBuilder.setContentTitle(getString(R.string.updatingTvShows));
		mBuilder.setContentText(getString(R.string.gettingReady));
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.addAction(R.drawable.ic_close_white_24dp, getString(android.R.string.cancel), contentIntent);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);

		mSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mClearLibrary = mSettings.getBoolean(CLEAR_LIBRARY_TVSHOWS, false);
		mClearUnavailable = mSettings.getBoolean(REMOVE_UNAVAILABLE_FILES_TVSHOWS, false);
		mSyncLibraries = mSettings.getBoolean(SYNC_WITH_TRAKT, true);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStopUpdate = true;
			
			if (mIdentification != null)
				mIdentification.cancel();
		}
	};

	private void updateTvShows() {

		// Show the "Analyzing files..." notification
		showTvShowAnalyzingNotification();
		
		mIdentification = new TvShowIdentification(getApplicationContext(), this, mFiles);
		mIdentification.start();
	}

	private void clear() {
		// Lists
		mFileSources = new ArrayList<FileSource>();
		mTvShowFileSources = new ArrayList<TvShowFileSource<?>>();
		mFiles = new ArrayList<ShowStructure>();
		mUniqueShowIds = new HashSet<String>();

		// Booleans
		mClearLibrary = false;
		mClearUnavailable = false;
		mSyncLibraries = true;
		mStopUpdate = false;

		// Other variables
		mEditor = null;
		mSettings = null;
		mTotalFiles = 0;
		mShowCount = 0;
		mNotificationManager = null;
		mBuilder = null;
	}

	private void log(String msg) {
		if (mDebugging)
			Log.d("TvShowsLibraryUpdate", msg);
	}

	@Override
	public void onTvShowAdded(String showId, String title, Bitmap cover, Bitmap backdrop, int count) {
		if (!showId.equals(DbAdapterTvShows.UNIDENTIFIED_ID)) {
			mUniqueShowIds.add(showId);
		}
		updateTvShowAddedNotification(showId, title, cover, backdrop, count);
	}

	@Override
	public void onEpisodeAdded(String showId, String title, Bitmap cover, Bitmap photo) {
		if (!showId.equals(DbAdapterTvShows.UNIDENTIFIED_ID))
			mEpisodeCount++;
		updateEpisodeAddedNotification(showId, title, cover, photo);
	}

	private void updateEpisodeAddedNotification(String showId, String title, Bitmap cover, Bitmap backdrop) {
		String contentText;
		if (showId.isEmpty() || showId.equalsIgnoreCase(DbAdapterTvShows.UNIDENTIFIED_ID))
			contentText = getString(R.string.unidentified) + ": " + title;
		else
			contentText = getString(R.string.stringJustAdded) + ": " + title;

		mBuilder.setLargeIcon(cover);
		mBuilder.setContentTitle(getString(R.string.updatingTvShows) + " (" + (int) ((100.0 / (double) mTotalFiles) * (double) mEpisodeCount) + "%)");
		mBuilder.setContentText(contentText);
		mBuilder.setStyle(
				new NotificationCompat.BigPictureStyle()
				.setSummaryText(contentText)
				.bigPicture(backdrop)
				);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void updateTvShowAddedNotification(String showId, String title, Bitmap cover, Bitmap backdrop, int count) {
		String contentText;
		if (showId.isEmpty() || showId.equalsIgnoreCase(DbAdapterTvShows.UNIDENTIFIED_ID))
			contentText = getString(R.string.unidentified) + ": " + title + " (" + count + " " + getResources().getQuantityString(R.plurals.episodes, count, count) + ")";
		else
			contentText = getString(R.string.stringJustAdded) + ": " + title + " (" + count + " " + getResources().getQuantityString(R.plurals.episodes, count, count) + ")";

		mBuilder.setLargeIcon(cover);
		mBuilder.setContentTitle(getString(R.string.updatingTvShows) + " (" + (int) ((100.0 / (double) mTotalFiles) * (double) mEpisodeCount) + "%)");
		mBuilder.setContentText(contentText);
		mBuilder.setStyle(
				new NotificationCompat.BigPictureStyle()
				.setSummaryText(contentText)
				.bigPicture(backdrop)
				);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void updateTvShowScanningNotification(String filesource) {
		mBuilder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		mBuilder.setContentTitle(getString(R.string.updatingTvShows));
		mBuilder.setContentText(getString(R.string.scanning) + ": " + filesource);

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void showTvShowAnalyzingNotification() {
		mBuilder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		mBuilder.setContentTitle(getString(R.string.updatingTvShows));
		mBuilder.setContentText(getString(R.string.analyzing_files));

		// Show the updated notification
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void showPostUpdateNotification() {
		mShowCount = mUniqueShowIds.size();

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, Main.class);
		notificationIntent.putExtra("fromUpdate", true);
		notificationIntent.putExtra("startup", "2");
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		if (!mStopUpdate) {
			mBuilder.setSmallIcon(R.drawable.ic_done_white_24dp);
			mBuilder.setTicker(getString(R.string.finishedTvShowsLibraryUpdate));
			mBuilder.setContentTitle(getString(R.string.finishedTvShowsLibraryUpdate));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + mShowCount + " " + getResources().getQuantityString(R.plurals.showsInLibrary, mShowCount, mShowCount) + " (" + mEpisodeCount + " " + getResources().getQuantityString(R.plurals.episodes, mEpisodeCount, mEpisodeCount) + ")");
		} else {
			mBuilder.setSmallIcon(R.drawable.ic_cancel_white_24dp);
			mBuilder.setTicker(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentTitle(getString(R.string.stringUpdateCancelled));
			mBuilder.setContentText(getString(R.string.stringJustAdded) + " " + mShowCount + " " + getResources().getQuantityString(R.plurals.showsInLibrary, mShowCount, mShowCount) + " (" + mEpisodeCount + " " + getResources().getQuantityString(R.plurals.episodes, mEpisodeCount, mEpisodeCount) + ")");
		}
		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (mEpisodeCount > 0)
			mNotificationManager.notify(POST_UPDATE_NOTIFICATION, updateNotification);
	}
}