package com.miz.functions;

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.mizuu.DbAdapterTvShow;
import com.miz.mizuu.DbAdapterTvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowEpisode;

public class TraktTvShowsSyncService extends IntentService {

	private ArrayList<TvShow> fullShows;
	private HashSet<String> shows;

	public TraktTvShowsSyncService() {
		super("TraktTvShowsSyncService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		fullShows = new ArrayList<TvShow>();
		shows = new HashSet<String>();
		
		// Setup up notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.ic_action_tv);
		builder.setTicker(getString(R.string.syncTvShows));
		builder.setContentTitle(getString(R.string.syncTvShows));
		builder.setContentText(getString(R.string.updatingShowInfo));
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_tv));

		// Build notification
		Notification updateNotification = builder.build();

		// Show the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(8, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(8, updateNotification);

		// Get shows
		DbAdapterTvShow showDb = MizuuApplication.getTvDbAdapter();
		Cursor cursor = showDb.getAllShows();

		try {
			while (cursor.moveToNext()) {
				shows.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)));

				// Full TV shows
				fullShows.add(new TvShow(
						this,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
						false,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1))
						));
			}
		} catch (Exception e) {} finally {
			cursor.close();
		}

		// Upload favourites from Mizuu to Trakt
		ArrayList<TvShow> favs = new ArrayList<TvShow>();
		int count = fullShows.size();
		for (int i = 0; i < count; i++)
			if (fullShows.get(i).isFavorite())
				favs.add(fullShows.get(i));

		MizLib.tvShowFavorite(favs, this);

		favs.clear();
		favs = null;

		// Get favourites from Trakt to Mizuu
		JSONArray jsonArray = MizLib.getTraktTvShowLibrary(this, MizLib.RATINGS);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					if (shows.contains(String.valueOf(jsonArray.getJSONObject(i).get("tvdb_id"))))
						showDb.updateShowSingleItem(String.valueOf(jsonArray.getJSONObject(i).get("tvdb_id")), DbAdapterTvShow.KEY_SHOW_EXTRA1, "1");
				} catch (Exception e) {}
			}
		}

		// Upload watched episodes from Mizuu to Trakt
		DbAdapterTvShowEpisode episodeDb = MizuuApplication.getTvEpisodeDbAdapter();
		ArrayList<TvShowEpisode> watchedEpisodes = new ArrayList<TvShowEpisode>();

		for (int i = 0; i < count; i++) {
			watchedEpisodes.clear();
			cursor = episodeDb.getAllEpisodes(fullShows.get(i).getId(), DbAdapterTvShowEpisode.OLDEST_FIRST);

			while (cursor.moveToNext())
				if (cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)).equals("1"))
					watchedEpisodes.add(new TvShowEpisode(this,
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_TITLE)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_PLOT)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_DIRECTOR)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_WRITER)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_GUESTSTARS)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_RATING)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)),
							cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EXTRA_1))
							));

			MizLib.markEpisodeAsWatched(watchedEpisodes, this);
		}
		
		// Clean up
		watchedEpisodes.clear();
		watchedEpisodes = null;

		// Get watched episodes from Trakt to Mizuu		
		jsonArray = MizLib.getTraktTvShowLibrary(this, MizLib.WATCHED);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					if (shows.contains(String.valueOf(jsonArray.getJSONObject(i).get("tvdb_id")))) {
						String showId = String.valueOf(jsonArray.getJSONObject(i).get("tvdb_id"));
						JSONArray seasons = jsonArray.getJSONObject(i).getJSONArray("seasons");
						for (int j = 0; j < seasons.length(); j++) {
							JSONObject season = seasons.getJSONObject(j);
							String seasonNumber = MizLib.addIndexZero(String.valueOf(season.get("season")));
							JSONArray seasonEpisodes = season.getJSONArray("episodes");
							for (int k = 0; k < seasonEpisodes.length(); k++) {
								episodeDb.updateEpisode(showId, seasonNumber, MizLib.addIndexZero(String.valueOf(seasonEpisodes.get(k))), DbAdapterTvShowEpisode.KEY_HAS_WATCHED, "1");
							}
						}
					}
				} catch (Exception e) {}
			}
		}

		// Clean up
		shows.clear();
		shows = null;
		jsonArray = null;
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-shows-update"));
	}
}