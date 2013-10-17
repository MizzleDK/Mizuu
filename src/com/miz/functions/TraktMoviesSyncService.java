package com.miz.functions;

import java.util.ArrayList;
import java.util.HashMap;

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

import com.miz.mizuu.DbAdapter;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class TraktMoviesSyncService extends IntentService {

	private ArrayList<Movie> fullMovies;
	private HashMap<String, String> movies;
	private String rowId;

	public TraktMoviesSyncService() {
		super("TraktMoviesSyncService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		fullMovies = new ArrayList<Movie>();
		movies = new HashMap<String, String>();

		// Setup up notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.ic_action_tv);
		builder.setTicker(getString(R.string.syncMovies));
		builder.setContentTitle(getString(R.string.syncMovies));
		builder.setContentText(getString(R.string.updatingMovieInfo));
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_tv));

		// Build notification
		Notification updateNotification = builder.build();

		// Show the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(9, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(9, updateNotification);

		// Get movies
		DbAdapter movieDb = MizuuApplication.getMovieAdapter();
		Cursor cursor = movieDb.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false);

		try {
			while (cursor.moveToNext()) {
				movies.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)), cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)));

				// Full movies
				fullMovies.add(new Movie(this,
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
		
		// Upload watched movies from Mizuu to Trakt
		ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
		int count = fullMovies.size();
		for (int i = 0; i < count; i++)
			if (fullMovies.get(i).hasWatched())
				watchedMovies.add(fullMovies.get(i));
		MizLib.markMovieAsWatched(watchedMovies, this);
		
		// Clean up
		watchedMovies.clear();
		watchedMovies = null;

		// Get watched movies from Trakt to Mizuu
		JSONArray jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.WATCHED);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					rowId = movies.get(String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id")));
					if (rowId != null)
						movieDb.updateMovieSingleItem(Long.valueOf(rowId), DbAdapter.KEY_HAS_WATCHED, "1");
				} catch (Exception e) {}
			}
		}

		// Upload favourite movies from Mizuu to Trakt
		ArrayList<Movie> favs = new ArrayList<Movie>();
		for (int i = 0; i < count; i++)
			if (fullMovies.get(i).isFavourite())
				favs.add(fullMovies.get(i));
		MizLib.movieFavorite(favs, this);
		
		// Clean up
		favs.clear();
		favs = null;
		
		// Get favourite movies from Trakt to Mizuu
		jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.RATINGS);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					rowId = movies.get(String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id")));
					if (rowId != null)
						movieDb.updateMovieSingleItem(Long.valueOf(rowId), DbAdapter.KEY_FAVOURITE, "1");
				} catch (Exception e) {}
			}
		}
		
		// Upload favourite movies from Mizuu to Trakt
		ArrayList<Movie> watchlistMovies = new ArrayList<Movie>();
		for (int i = 0; i < count; i++)
			if (fullMovies.get(i).toWatch())
				watchlistMovies.add(fullMovies.get(i));
		MizLib.movieWatchlist(watchlistMovies, this);
		
		// Clean up
		watchlistMovies.clear();
		watchlistMovies = null;

		// Get watchlist from Trakt to Mizuu
		jsonArray = MizLib.getTraktMovieLibrary(this, MizLib.WATCHLIST);
		if (jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					rowId = movies.get(String.valueOf(jsonArray.getJSONObject(i).get("tmdb_id")));
					if (rowId != null)
						movieDb.updateMovieSingleItem(Long.valueOf(rowId), DbAdapter.KEY_TO_WATCH, "1");
				} catch (Exception e) {}
			}
		}

		// Clean up
		movies.clear();
		movies = null;
		fullMovies.clear();
		fullMovies = null;
		jsonArray = null;
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-library-change"));
	}
}