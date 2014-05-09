package com.miz.db;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapterTvShowEpisode {

	// Database fields
	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_SCREENSHOTPATH = "screenshot_path";
	public static final String KEY_SEASON = "season";
	public static final String KEY_EPISODE = "episode";
	public static final String KEY_DATE_ADDED = "date_added";
	public static final String KEY_TO_WATCH = "to_watch";
	public static final String KEY_HAS_WATCHED = "has_watched";
	public static final String KEY_EXTRA_1 = "extra1"; // Favorite
	public static final String KEY_EXTRA_2 = "extra2"; // Not in use
	public static final String KEY_EXTRA_3 = "extra3"; // Not in use
	public static final String KEY_EXTRA_4 = "extra4"; // Not in use
	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_EPISODE_TITLE = "episode_title";
	public static final String KEY_EPISODE_PLOT = "episode_description";
	public static final String KEY_EPISODE_AIRDATE = "episode_airdate";
	public static final String KEY_EPISODE_RATING = "episode_rating";
	public static final String KEY_EPISODE_DIRECTOR = "episode_director";
	public static final String KEY_EPISODE_WRITER = "episode_writer";
	public static final String KEY_EPISODE_GUESTSTARS = "episode_gueststars";

	private static final String DATABASE_TABLE = "tvshow_episodes";

	private final String[] allRows = new String[]{KEY_ROWID, KEY_FILEPATH, KEY_SCREENSHOTPATH, KEY_SEASON, KEY_EPISODE, KEY_DATE_ADDED, KEY_TO_WATCH, KEY_HAS_WATCHED,
			KEY_EXTRA_1, KEY_EXTRA_2, KEY_EXTRA_3, KEY_EXTRA_4, KEY_SHOW_ID, KEY_EPISODE_TITLE, KEY_EPISODE_PLOT, KEY_EPISODE_AIRDATE, KEY_EPISODE_RATING, KEY_EPISODE_DIRECTOR,
			KEY_EPISODE_WRITER, KEY_EPISODE_GUESTSTARS};

	private SQLiteDatabase database;

	public DbAdapterTvShowEpisode(Context context) {
		database = DbHelperTvShowEpisode.getHelper(context).getWritableDatabase();
	}

	public void close() {
		database.close();
	}

	public long createEpisode(String filepath, String season, String episode, String showId, String episodeTitle, String episodePlot,
			String episodeAirdate, String episodeRating, String episodeDirector, String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues initialValues = createContentValues(filepath, season,episode, showId, episodeTitle,
				episodePlot, episodeAirdate, episodeRating, episodeDirector, episodeWriter, episodeGuestStars, hasWatched, favorite);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}

	public boolean updateSingleItem(long rowId, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return database.update(DATABASE_TABLE, values, KEY_ROWID + "='" + rowId + "'", null) > 0;
	}

	public boolean updateEpisode(long rowId, String filepath, String season, String episode, String showId, String episodeTitle, String episodePlot,
			String episodeAirdate, String episodeRating, String episodeDirector, String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues initialValues = createContentValues(filepath, season,episode, showId, episodeTitle,
				episodePlot, episodeAirdate, episodeRating, episodeDirector, episodeWriter, episodeGuestStars, hasWatched, favorite);
		return database.update(DATABASE_TABLE, initialValues, KEY_ROWID + "='" + rowId + "'", null) > 0;
	}

	public boolean updateEpisode(String showId, String season, String episode, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return database.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + season + "' AND " + KEY_EPISODE + "='" + episode + "'", null) > 0;
	}

	public Cursor getEpisode(String rowId) {
		return database.query(DATABASE_TABLE, allRows, KEY_ROWID + "='" + rowId + "'", null, null, null, null);
	}

	public static int OLDEST_FIRST = 0, NEWEST_FIRST = 1;
	public Cursor getAllEpisodes(String showId, int type) {
		if (type == OLDEST_FIRST)
			return database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " asc, " + KEY_EPISODE + " asc");
		else
			return database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " desc, " + KEY_EPISODE + " desc");
	}

	public Cursor getAllEpisodesInDatabase(boolean includeRemoved) {
		if (includeRemoved)
			return database.query(DATABASE_TABLE, allRows, null, null, null, null, null);
		else
			return database.query(DATABASE_TABLE, allRows, "NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, null, null, null);
	}

	public Cursor getAllIgnoredEpisodesInDatabase() {
		return database.query(DATABASE_TABLE, allRows, KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE'", null, null, null, null);
	}

	public Cursor getAllUnidentifiedEpisodesInDatabase() {
		return database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + " = 'invalid'", null, null, null, null);
	}

	public boolean ignoreEpisode(String rowId) {
		return updateSingleItem(Long.parseLong(rowId), KEY_EPISODE_TITLE, "MIZ_REMOVED_EPISODE");
	}

	public boolean deleteEpisode(String rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "='" + rowId + "'", null) > 0;
	}

	public boolean deleteAllEpisodes(String showId) {
		return database.delete(DATABASE_TABLE, KEY_SHOW_ID + "= '" + showId + "'", null) > 0;
	}

	public boolean deleteSeaon(String showId, String season) {
		return database.delete(DATABASE_TABLE, KEY_SHOW_ID + " = '" + showId + "' and " + KEY_SEASON + " = '" + season + "'", null) > 0;
	}

	public boolean deleteAllEpisodesInDatabase() {
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean deleteAllUnidentifiedEpisodes() {
		return database.delete(DATABASE_TABLE, KEY_SHOW_ID + " = 'invalid'", null) > 0;
	}

	public int getEpisodeCount(String showId) {
		Cursor c = database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " asc, " + KEY_EPISODE + " asc");
		int count = c.getCount();
		c.close();
		return count;
	}

	public HashMap<String, Integer> getSeasons(String showId) {
		HashMap<String, Integer> results = new HashMap<String, Integer>();
		Cursor c = null;
		try {
			c = database.query(DATABASE_TABLE, new String[]{KEY_SHOW_ID, KEY_EPISODE_TITLE, KEY_SEASON}, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, null, null, KEY_SEASON + " asc");
			while (c.moveToNext()) {
				String season = c.getString(c.getColumnIndex(KEY_SEASON));
				if (results.containsKey(season)) {
					results.put(season, results.get(season) + 1);
				} else {
					results.put(season, 1);
				}
			}
		} catch (Exception e) {
		} finally {
			if (c != null)
				c.close();
		}
		return results;
	}

	public String getLatestEpisodeAirdate(String showId) {
		String s = "";
		Cursor c = database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_EPISODE_AIRDATE + " LIKE '%-%'", null, null, null, KEY_EPISODE_AIRDATE + " desc"); // %-% hack to make sure that the airdate includes a hyphen and is an actual date
		if (c.moveToFirst())
			s = c.getString((c.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)));
		c.close();
		return s;
	}

	public boolean hasUnwatchedEpisodes(String showId) {
		Cursor c = database.query(DATABASE_TABLE, allRows, KEY_SHOW_ID + " = '" + showId + "' AND " + KEY_HAS_WATCHED + " = '0'", null, null, null, null);
		return c.getCount() > 0;
	}

	public boolean markSeasonAsWatched(String showId, String season) {
		ContentValues values = new ContentValues();
		values.put(KEY_HAS_WATCHED, "1");
		return database.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + season + "'", null) > 0;
	}

	private ContentValues createContentValues(String filepath, String season, String episode,
			String showId, String episodeTitle, String episodePlot, String episodeAirdate, String episodeRating,
			String episodeDirector, String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_SEASON, season);
		values.put(KEY_EPISODE, episode);
		values.put(KEY_SHOW_ID, showId);
		values.put(KEY_EPISODE_TITLE, episodeTitle);
		values.put(KEY_EPISODE_PLOT, episodePlot);
		values.put(KEY_EPISODE_AIRDATE, episodeAirdate);
		values.put(KEY_EPISODE_RATING, episodeRating);
		values.put(KEY_EPISODE_DIRECTOR, episodeDirector);
		values.put(KEY_EPISODE_WRITER, episodeWriter);
		values.put(KEY_EPISODE_GUESTSTARS, episodeGuestStars);
		values.put(KEY_HAS_WATCHED, hasWatched);
		values.put(KEY_EXTRA_1, favorite);
		return values;
	}
}