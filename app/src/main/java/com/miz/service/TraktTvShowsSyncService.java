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

import com.google.common.collect.Multimap;
import com.miz.apis.trakt.Trakt;
import com.miz.apis.trakt.TraktTvShow;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.utils.LocalBroadcastUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class TraktTvShowsSyncService extends IntentService {

	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private ArrayList<TvShow> mShows;
	private ArrayList<TraktTvShow> mLocalCollection, mLocalWatched;
	private LinkedHashMap<String, TraktTvShow> mTraktCollection, mTraktWatched;
	private HashSet<String> mShowIds, mTraktFavorites, mLocalFavorites;
	private DbAdapterTvShows mShowDatabase;
	private DbAdapterTvShowEpisodes mEpisodeDatabase;
	private final int NOTIFICATION_ID = 8;

	public TraktTvShowsSyncService() {
		super("TraktTvShowsSyncService");
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

		// Load all local TV shows into an ArrayList and a map of TVDB ID and
		// corresponding row ID
		loadTvShowLibrary();

		updateNotification(getString(R.string.downloadingTvShowCollection));

		// Download Trakt TV show collection
		downloadTvShowCollection();

		updateNotification(getString(R.string.updatingTvShowCollection));

		// Compare the local TV show collection with the one from Trakt and update
		// the Trakt collection if new items are found
		updateTvShowCollection();

		updateNotification(getString(R.string.downloadingWatchedTvShows));

		// Download watched TV shows from Trakt
		downloadWatchedTvShows();

		updateNotification(getString(R.string.updatingWatchedTvShows));

		// Sync locally watched TV shows with Trakt
		updateWatchedTvShows();

		updateNotification(getString(R.string.downloadingTvShowFavorites));

		// Download TV show favorites from Trakt
		downloadTvShowFavorites();

		updateNotification(getString(R.string.updatingTvShowFavorites));

		// Sync local favorites with Trakt
		updateTvShowFavorites();

		// Clean up
		mShows.clear();
		mShows = null;	

		// Let the application know that the sync is finished
		broadcastLibraryUpdate();

		// Let the user know that the sync is finished
		showPostUpdateNotification();
	}

	private void setup() {
		mShows = new ArrayList<TvShow>();
		mLocalCollection = new ArrayList<TraktTvShow>();
		mLocalWatched = new ArrayList<TraktTvShow>();
		mTraktCollection = new LinkedHashMap<String, TraktTvShow>();
		mTraktWatched = new LinkedHashMap<String, TraktTvShow>();
		mShowIds = new HashSet<String>();
		mTraktFavorites = new HashSet<String>();
		mLocalFavorites = new HashSet<String>();
	}

	private void setupNotification() {
		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setSmallIcon(R.drawable.ic_tv_white_24dp);
		mBuilder.setTicker(getString(R.string.syncTvShows));
		mBuilder.setContentTitle(getString(R.string.syncTvShows));
		mBuilder.setContentText(getString(R.string.updatingShowInfo));
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

	private void loadTvShowLibrary() {
		// Get shows
		mShowDatabase = MizuuApplication.getTvDbAdapter();
		Cursor cursor = mShowDatabase.getAllShows();
		ColumnIndexCache cache = new ColumnIndexCache();
		
		try {
			while (cursor.moveToNext()) {
				mShowIds.add(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)));

				// Full TV shows
				mShows.add(new TvShow(
						this,
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_TITLE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_PLOT)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RATING)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_GENRES)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ACTORS)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RUNTIME)),
						false,
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
						MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)))
						));

				if (cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FAVOURITE)).equals("1"))
					mLocalFavorites.add(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)));
			}
		} catch (Exception e) {} finally {
			cursor.close();
			cache.clear();
		}

		mEpisodeDatabase = MizuuApplication.getTvEpisodeDbAdapter();
		int count = mShows.size();
		for (int i = 0; i < count; i++) {
			TraktTvShow collectionShow = new TraktTvShow(mShows.get(i).getId(), mShows.get(i).getTitle());
			TraktTvShow watchedShow = new TraktTvShow(mShows.get(i).getId(), mShows.get(i).getTitle());

			Cursor c = mEpisodeDatabase.getEpisodes(mShows.get(i).getId());
			try {
				while (c.moveToNext()) {
					collectionShow.addEpisode(c.getString(cache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_SEASON)),
							c.getString(cache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_EPISODE)));

					if (c.getString(cache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)).equals("1")) {
						watchedShow.addEpisode(c.getString(cache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_SEASON)),
								c.getString(cache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_EPISODE)));
					}
				}
			} catch (Exception ignored) {
			} finally {
				c.close();
			}

			if (collectionShow.getSeasons().size() > 0)
				mLocalCollection.add(collectionShow);

			if (watchedShow.getSeasons().size() > 0)
				mLocalWatched.add(watchedShow);
		}
	}

	private void downloadTvShowCollection() {
		JSONArray jsonArray = Trakt.getTvShowLibrary(this, Trakt.COLLECTION);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String id = String.valueOf(jsonArray.getJSONObject(i).getString("tvdb_id"));
					TraktTvShow show = new TraktTvShow(id, jsonArray.getJSONObject(i).getString("title"));

					JSONArray seasons = jsonArray.getJSONObject(i).getJSONArray("seasons");
					for (int j = 0; j < seasons.length(); j++) {
						String season = seasons.getJSONObject(j).getString("season");
						JSONArray episodes = seasons.getJSONObject(j).getJSONArray("episodes");
						for (int k = 0; k < episodes.length(); k++) {
							show.addEpisode(season, episodes.getString(k));
						}
					}

					mTraktCollection.put(id, show);
				} catch (Exception e) {}
			}
		}
	}

	private void updateTvShowCollection() {
		int count = mLocalCollection.size();
		if (count > 0) {
			ArrayList<TraktTvShow> collection = new ArrayList<TraktTvShow>();

			for (int i = 0; i < count; i++) {
				if (!mTraktCollection.containsKey(mLocalCollection.get(i).getId())) {					
					// This show isn't in the Trakt library, so we'll add everything
					collection.add(mLocalCollection.get(i));
				} else {

					TraktTvShow traktTemp = mTraktCollection.get(mLocalCollection.get(i).getId());
					TraktTvShow temp = new TraktTvShow(mLocalCollection.get(i).getId(), mLocalCollection.get(i).getTitle());

					// This show is in the Trakt library, so we'll have to check each episode in each season
					Multimap<String, String> seasonsMap = mLocalCollection.get(i).getSeasons();
					Set<String> seasons = seasonsMap.keySet();
					for (String season : seasons) {
						Collection<String> episodes = seasonsMap.get(season);
						for (String episode : episodes) {
							if (!traktTemp.contains(MizLib.removeIndexZero(season), MizLib.removeIndexZero(episode))) {
								temp.addEpisode(season, episode);
							}
						}
					}

					if (temp.getSeasons().size() > 0)
						collection.add(temp);
				}
			}

			count = collection.size();
			for (int i = 0; i < count; i++) {
				Trakt.addTvShowToLibrary(collection.get(i), getApplicationContext());
			}

			// Clean up
			collection.clear();
			collection = null;

			mTraktCollection.clear();
			mTraktCollection = null;
		}
	}

	private void downloadWatchedTvShows() {
		JSONArray jsonArray = Trakt.getTvShowLibrary(this, Trakt.WATCHED);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String showId = String.valueOf(jsonArray.getJSONObject(i).getString("tvdb_id"));
					TraktTvShow show = new TraktTvShow(showId, jsonArray.getJSONObject(i).getString("title"));

					JSONArray seasons = jsonArray.getJSONObject(i).getJSONArray("seasons");
					for (int j = 0; j < seasons.length(); j++) {
						String season = seasons.getJSONObject(j).getString("season");
						JSONArray episodes = seasons.getJSONObject(j).getJSONArray("episodes");
						for (int k = 0; k < episodes.length(); k++) {
							show.addEpisode(season, episodes.getString(k));
						}
					}
					mTraktWatched.put(showId, show);


					if (mShowIds.contains(String.valueOf(jsonArray.getJSONObject(i).get("tvdb_id")))) {
						for (int j = 0; j < seasons.length(); j++) {
							JSONObject season = seasons.getJSONObject(j);
							String seasonNumber = MizLib.addIndexZero(String.valueOf(season.get("season")));
							JSONArray seasonEpisodes = season.getJSONArray("episodes");
							for (int k = 0; k < seasonEpisodes.length(); k++) {
								mEpisodeDatabase.updateEpisode(showId, seasonNumber, MizLib.addIndexZero(String.valueOf(seasonEpisodes.get(k))), DbAdapterTvShowEpisodes.KEY_HAS_WATCHED, "1");
							}
						}
					}
				} catch (Exception e) {}
			}
		}
	}

	private void updateWatchedTvShows() {
		int count = mLocalWatched.size();		
		if (count > 0) {
			ArrayList<TraktTvShow> watched = new ArrayList<TraktTvShow>();

			for (int i = 0; i < count; i++) {
				if (!mTraktWatched.containsKey(mLocalWatched.get(i).getId())) {					
					// This show isn't in the Trakt library, so we'll add everything
					watched.add(mLocalWatched.get(i));
				} else {

					TraktTvShow traktTemp = mTraktWatched.get(mLocalWatched.get(i).getId());
					TraktTvShow temp = new TraktTvShow(mLocalWatched.get(i).getId(), mLocalWatched.get(i).getTitle());

					// This show is in the Trakt library, so we'll have to check each episode in each season
					Multimap<String, String> seasonsMap = mLocalWatched.get(i).getSeasons();
					Set<String> seasons = seasonsMap.keySet();
					for (String season : seasons) {
						Collection<String> episodes = seasonsMap.get(season);
						for (String episode : episodes) {
							if (!traktTemp.contains(MizLib.removeIndexZero(season), MizLib.removeIndexZero(episode))) {
								temp.addEpisode(MizLib.removeIndexZero(season), MizLib.removeIndexZero(episode));
							}
						}
					}

					if (temp.getSeasons().size() > 0)
						watched.add(temp);
				}
			}

			count = watched.size();
			for (int i = 0; i < count; i++) {
				Trakt.markTvShowAsWatched(watched.get(i), getApplicationContext());
			}

			// Clean up
			watched.clear();
			watched = null;

			mTraktWatched.clear();
			mTraktWatched = null;
		}
	}

	private void downloadTvShowFavorites() {
		JSONArray jsonArray = Trakt.getTvShowLibrary(this, Trakt.RATINGS);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					String showId = String.valueOf(jsonArray.getJSONObject(i).getString("tvdb_id"));
					mTraktFavorites.add(showId);

					if (!mLocalFavorites.contains(showId))
						mShowDatabase.updateShowSingleItem(showId, DbAdapterTvShows.KEY_SHOW_FAVOURITE, "1");
				} catch (Exception e) {}
			}
		}
	}

	private void updateTvShowFavorites() {
		ArrayList<TvShow> favs = new ArrayList<TvShow>();
		int count = mShows.size();
		for (int i = 0; i < count; i++)
			if (mShows.get(i).isFavorite() && !mTraktFavorites.contains(mShows.get(i).getId()))
				favs.add(mShows.get(i));

		Trakt.tvShowFavorite(favs, this);

		favs.clear();
		favs = null;
	}

	private void broadcastLibraryUpdate() {
		LocalBroadcastUtils.updateTvShowLibrary(this);
	}

	private void showPostUpdateNotification() {
		// Remove the old one
		mNotificationManager.cancel(NOTIFICATION_ID);

		mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setTicker(getString(R.string.finishedTraktTvShowSync));
		mBuilder.setContentTitle(getString(R.string.finishedTraktTvShowSync));
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