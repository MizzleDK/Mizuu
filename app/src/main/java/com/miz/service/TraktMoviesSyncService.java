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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;

import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.LocalBroadcastUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;

public class TraktMoviesSyncService extends IntentService {

	private ArrayList<Movie> mMovies;
	private ArrayList<String> mTmdbIds;
	private HashSet<String> mMovieCollection, mWatchedMovies, mMovieFavorites, mWatchlist;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private DbAdapterMovies mMovieDatabase;
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
		mTmdbIds = new ArrayList<String>();
		mMovieCollection = new HashSet<String>();
		mWatchedMovies = new HashSet<String>();
		mMovieFavorites = new HashSet<String>();
		mWatchlist = new HashSet<String>();
	}

	private void setupNotification() {
		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setSmallIcon(R.drawable.ic_tv_white_24dp);
		mBuilder.setTicker(getString(R.string.syncMovies));
		mBuilder.setContentTitle(getString(R.string.syncMovies));
		mBuilder.setContentText(getString(R.string.updatingMovieInfo));
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);

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
		Cursor cursor = mMovieDatabase.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
		ColumnIndexCache cache = new ColumnIndexCache();
		
		try {
			while (cursor.moveToNext()) {
				mTmdbIds.add(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)));

				// Full movies
				mMovies.add(new Movie(this,
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TITLE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_PLOT)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TAGLINE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_IMDB_ID)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RATING)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RELEASEDATE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_CERTIFICATION)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RUNTIME)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TRAILER)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_GENRES)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_FAVOURITE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_ACTORS)),
						MizuuApplication.getCollectionsAdapter().getCollection(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID))),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TO_WATCH)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_HAS_WATCHED)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_DATE_ADDED)),
						false
						));
			}
		} catch (Exception e) {} finally {
			cursor.close();
			cache.clear();
		}
	}

	/**
	 * Get movie collection from Trakt
	 */
	private void downloadMovieCollection() {
		JSONArray jsonArray = Trakt.getMovieLibrary(this, Trakt.COLLECTION);
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
				Trakt.addMoviesToLibrary(collection, getApplicationContext());
			}

			// Clean up
			collection.clear();
			collection = null;
		}	
	}

	private void downloadWatchedMovies() {
		JSONArray jsonArray = Trakt.getMovieLibrary(this, Trakt.WATCHED);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mWatchedMovies.add(tmdbId);
					mMovieDatabase.updateMovieSingleItem(tmdbId, DbAdapterMovies.KEY_HAS_WATCHED, "1");
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
			Trakt.markMovieAsWatched(watchedMovies, this);

		// Clean up
		watchedMovies.clear();
		watchedMovies = null;

		mWatchedMovies.clear();
		mWatchedMovies = null;
	}

	private void downloadMovieFavorites() {
		JSONArray jsonArray = Trakt.getMovieLibrary(this, Trakt.RATINGS);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mMovieFavorites.add(tmdbId);
					mMovieDatabase.updateMovieSingleItem(tmdbId, DbAdapterMovies.KEY_FAVOURITE, "1");
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
			Trakt.movieFavorite(favs, this);

		// Clean up
		favs.clear();
		favs = null;

		mMovieFavorites.clear();
		mMovieFavorites = null;
	}

	private void downloadWatchlist() {
		JSONArray jsonArray = Trakt.getMovieLibrary(this, Trakt.WATCHLIST);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String tmdbId = String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id"));
					mWatchlist.add(tmdbId);
					mMovieDatabase.updateMovieSingleItem(tmdbId, DbAdapterMovies.KEY_TO_WATCH, "1");
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
			Trakt.movieWatchlist(watchlistMovies, this);

		// Clean up
		watchlistMovies.clear();
		watchlistMovies = null;

		mWatchlist.clear();
		mWatchlist = null;
	}

	private void broadcastLibraryUpdate() {
		LocalBroadcastUtils.updateMovieLibrary(this);
	}

	private void showPostUpdateNotification() {
		// Remove the old one
		mNotificationManager.cancel(NOTIFICATION_ID);

		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setTicker(getString(R.string.finishedTraktMovieSync));
		mBuilder.setContentTitle(getString(R.string.finishedTraktMovieSync));
		mBuilder.setSmallIcon(R.drawable.ic_done_white_24dp);
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
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setTicker(getString(R.string.traktSyncFailed));
		mBuilder.setContentTitle(getString(R.string.traktSyncFailed));
		mBuilder.setContentText(getString(R.string.noInternet));
		mBuilder.setSmallIcon(R.drawable.ic_signal_wifi_statusbar_connected_no_internet_2_white_24dp);
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