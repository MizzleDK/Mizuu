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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.json.JSONArray;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.miz.db.DbAdapter;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class TraktMoviesSyncService extends IntentService {

	private ArrayList<Movie> mMovies;
	private Multimap<String, String> mTmdbIdRowIdMap;
	private HashSet<String> mMovieCollection, mWatchedMovies, mMovieFavorites, mWatchlist;
	private StringBuilder rowId;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private DbAdapter mMovieDatabase;
	private final int NOTIFICATION_ID = 9;

	public TraktMoviesSyncService() {
		super("TraktMoviesSyncService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Variables and collections
		setup();

		// Notification
		setupNotification();

		// Internet check
		if (!MizLib.isOnline(this)) {
			showNoInternetNotification();
			return;
		}

		// Load all local movies into an ArrayList and a map of TMDb ID and
		// corresponding row ID
		loadMovieLibrary();

		updateNotification(getString(R.string.downloadingMovieCollection));

		// Download Trakt movie collection
		downloadMovieCollection();

		updateNotification(getString(R.string.updatingMovieCollection));

		// Compare the local movie collection with the one from Trakt and update
		// the Trakt collection if new items are found
		updateMovieCollection();

		updateNotification(getString(R.string.downloadingWatchedMovies));

		// Download watched movies from Trakt
		downloadWatchedMovies();

		updateNotification(getString(R.string.updatingWatchedMovies));

		// Sync locally watched movies with Trakt
		updateWatchedMovies();

		updateNotification(getString(R.string.downloadingMovieFavorites));

		// Download movie favorites from Trakt
		downloadMovieFavorites();

		updateNotification(getString(R.string.updatingMovieFavorites));

		// Sync local favorites with Trakt
		updateMovieFavorites();

		updateNotification(getString(R.string.downloadingWatchlist));

		// Download watchlist from Trakt
		downloadWatchlist();

		updateNotification(getString(R.string.updatingWatchlist));

		// Sync local watchlist with Trakt
		updateWatchlist();

		// Clean up
		mMovies.clear();
		mMovies = null;	

		// Let the application know that the sync is finished
		broadcastLibraryUpdate();

		// Let the user know that the sync is finished
		showPostUpdateNotification();
	}

	private void setup() {
		mMovies = new ArrayList<Movie>();
		mTmdbIdRowIdMap = LinkedListMultimap.create();
		rowId = new StringBuilder();
		mMovieCollection = new HashSet<String>();
		mWatchedMovies = new HashSet<String>();
		mMovieFavorites = new HashSet<String>();
		mWatchlist = new HashSet<String>();
	}

	private void setupNotification() {
		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setSmallIcon(R.drawable.ic_action_tv);
		mBuilder.setTicker(getString(R.string.syncMovies));
		mBuilder.setContentTitle(getString(R.string.syncMovies));
		mBuilder.setContentText(getString(R.string.updatingMovieInfo));
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_tv));

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);
	}

	private void loadMovieLibrary() {
		// Get movies
		mMovieDatabase = MizuuApplication.getMovieAdapter();
		Cursor cursor = mMovieDatabase.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false, false);

		try {
			while (cursor.moveToNext()) {
				mTmdbIdRowIdMap.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)), cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)));

				// Full movies
				mMovies.add(new Movie(this,
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_IMDBID)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TRAILER)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CAST)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COLLECTION)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)),
						cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
						false,
						true
						));
			}
		} catch (Exception e) {} finally {
			cursor.close();
		}
	}

	/**
	 * Get movie collection from Trakt
	 */
	private void downloadMovieCollection() {
		JSONArray jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.COLLECTION);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					mMovieCollection.add(String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id")));
				} catch (Exception e) {}
			}
		}

		jsonArray = null;
	}

	private void updateMovieCollection() {
		int count = mMovies.size();
		if (count > 0) {
			ArrayList<Movie> collection = new ArrayList<Movie>();

			for (int i = 0; i < count; i++) {
				if (!mMovieCollection.contains(mMovies.get(i).getTmdbId())) {
					collection.add(mMovies.get(i));
				}
			}

			count = collection.size();

			if (count > 0) {
				// Add to Trakt movie collection
				MizLib.addMoviesToTraktLibrary(collection, getApplicationContext());
			}

			// Clean up
			collection.clear();
			collection = null;
		}	
	}

	private void downloadWatchedMovies() {
		JSONArray jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.WATCHED);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mWatchedMovies.add(tmdbId);
					Collection<String> rowIds = mTmdbIdRowIdMap.get(tmdbId);
					for (String id : rowIds) {
						rowId.delete(0, rowId.length());
						rowId.append(id);
						if (rowId.toString() != null)
							mMovieDatabase.updateMovieSingleItem(Long.valueOf(rowId.toString()), DbAdapter.KEY_HAS_WATCHED, "1");
					}
				} catch (Exception e) {}
			}
		}
	}

	private void updateWatchedMovies() {
		ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
		int count = mMovies.size();
		for (int i = 0; i < count; i++)
			// Check that we're not uploading stuff that's already on Trakt
			if (!mWatchedMovies.contains(mMovies.get(i).getTmdbId()) && mMovies.get(i).hasWatched())
				watchedMovies.add(mMovies.get(i));

		if (watchedMovies.size() > 0)
			MizLib.markMovieAsWatched(watchedMovies, this);

		// Clean up
		watchedMovies.clear();
		watchedMovies = null;

		mWatchedMovies.clear();
		mWatchedMovies = null;
	}

	private void downloadMovieFavorites() {
		JSONArray jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.RATINGS);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mMovieFavorites.add(tmdbId);
					Collection<String> rowIds = mTmdbIdRowIdMap.get(tmdbId);
					for (String id : rowIds) {
						rowId.delete(0, rowId.length());
						rowId.append(id);
						if (rowId.toString() != null)
							mMovieDatabase.updateMovieSingleItem(Long.valueOf(rowId.toString()), DbAdapter.KEY_FAVOURITE, "1");
					}
				} catch (Exception e) {}
			}
		}
	}

	private void updateMovieFavorites() {
		ArrayList<Movie> favs = new ArrayList<Movie>();
		int count = mMovies.size();
		for (int i = 0; i < count; i++)
			// Check that we're not uploading stuff that's already on Trakt
			if (!mMovieFavorites.contains(mMovies.get(i).getTmdbId()) && mMovies.get(i).isFavourite())
				favs.add(mMovies.get(i));

		if (favs.size() > 0)
			MizLib.movieFavorite(favs, this);

		// Clean up
		favs.clear();
		favs = null;

		mMovieFavorites.clear();
		mMovieFavorites = null;
	}

	private void downloadWatchlist() {
		JSONArray jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.WATCHLIST);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mWatchlist.add(tmdbId);
					Collection<String> rowIds = mTmdbIdRowIdMap.get(tmdbId);
					for (String id : rowIds) {
						rowId.delete(0, rowId.length());
						rowId.append(id);
						if (rowId.toString() != null)
							mMovieDatabase.updateMovieSingleItem(Long.valueOf(rowId.toString()), DbAdapter.KEY_TO_WATCH, "1");
					}
				} catch (Exception e) {}
			}
		}
	}

	private void updateWatchlist() {
		ArrayList<Movie> watchlistMovies = new ArrayList<Movie>();
		int count = mMovies.size();
		for (int i = 0; i < count; i++)
			if (!mWatchlist.contains(mMovies.get(i).getTmdbId()) && mMovies.get(i).toWatch())
				watchlistMovies.add(mMovies.get(i));

		if (watchlistMovies.size() > 0)
			MizLib.movieWatchlist(watchlistMovies, this);

		// Clean up
		watchlistMovies.clear();
		watchlistMovies = null;

		mWatchlist.clear();
		mWatchlist = null;
	}

	private void broadcastLibraryUpdate() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-library-change"));
	}

	private void showPostUpdateNotification() {
		// Remove the old one
		mNotificationManager.cancel(NOTIFICATION_ID);

		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setTicker(getString(R.string.finishedTraktMovieSync));
		mBuilder.setContentTitle(getString(R.string.finishedTraktMovieSync));
		mBuilder.setSmallIcon(R.drawable.done);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.done));
		mBuilder.setOngoing(false);
		mBuilder.setAutoCancel(true);
		mBuilder.setOnlyAlertOnce(true);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID + 1000, updateNotification);
	}

	private void showNoInternetNotification() {
		// Remove the old one
		mNotificationManager.cancel(NOTIFICATION_ID);

		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setTicker(getString(R.string.traktSyncFailed));
		mBuilder.setContentTitle(getString(R.string.traktSyncFailed));
		mBuilder.setContentText(getString(R.string.noInternet));
		mBuilder.setSmallIcon(R.drawable.ic_action_wifi);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_wifi));
		mBuilder.setOngoing(false);
		mBuilder.setAutoCancel(true);
		mBuilder.setOnlyAlertOnce(true);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID + 1000, updateNotification);
	}

	private void updateNotification(String text) {
		// Setup up notification
		mBuilder.setContentText(text);

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);
	}
}