package com.miz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapterTvShow {

	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_SHOW_TITLE = "show_title";
	public static final String KEY_SHOW_PLOT = "show_description";
	public static final String KEY_SHOW_ACTORS = "show_actors";
	public static final String KEY_SHOW_GENRES = "show_genres";
	public static final String KEY_SHOW_RATING = "show_rating";
	public static final String KEY_SHOW_CERTIFICATION = "show_certification";
	public static final String KEY_SHOW_RUNTIME = "show_runtime";
	public static final String KEY_SHOW_FIRST_AIRDATE = "show_first_airdate";
	public static final String KEY_SHOW_EXTRA1 = "extra1"; // Favorite
	public static final String KEY_SHOW_EXTRA2 = "extra2"; // Not in use
	public static final String KEY_SHOW_EXTRA3 = "extra3"; // Not in use
	public static final String KEY_SHOW_EXTRA4 = "extra4"; // Not in use
	
	public static final String DATABASE_TABLE = "tvshows";

	public static final String[] SELECT_ALL = new String[]{KEY_SHOW_ID, KEY_SHOW_TITLE, KEY_SHOW_PLOT, KEY_SHOW_ACTORS,
			KEY_SHOW_GENRES, KEY_SHOW_RATING, KEY_SHOW_RATING, KEY_SHOW_CERTIFICATION, KEY_SHOW_RUNTIME,
			KEY_SHOW_FIRST_AIRDATE, KEY_SHOW_EXTRA1/*, KEY_SHOW_EXTRA2, KEY_SHOW_EXTRA3, KEY_SHOW_EXTRA4*/};

	private SQLiteDatabase database;

	public DbAdapterTvShow(Context context) {
		database = DbHelperTvShow.getHelper(context).getWritableDatabase();
	}
	
	public void close() {
		database.close();
	}

	public long createShow(String showId, String showTitle, String showPlot, String showActors, String showGenres, String showRating, String showCertification,
			String showRuntime, String showFirstAirdate, String isFavorite) {
		if (getShow(showId).getCount() == 0) {
			ContentValues initialValues = createContentValues(showId, showTitle, showPlot, showActors, showGenres, showRating, showCertification, showRuntime, showFirstAirdate, isFavorite);
			return database.insert(DATABASE_TABLE, null, initialValues);
		} else {
			return -1;
		}
	}
	
	public boolean updateShowSingleItem(String showId, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return database.update(DATABASE_TABLE, values, KEY_SHOW_ID + " = '" + showId + "'", null) > 0;
	}

	public Cursor getShow(String showId) {
		return database.query(DATABASE_TABLE, SELECT_ALL, KEY_SHOW_ID + " = '" + showId + "'", null, null, null, null);
	}

	public Cursor getAllShows() {
		return database.query(DATABASE_TABLE, SELECT_ALL, "NOT(" + KEY_SHOW_ID + " = 'invalid')", null, null, null, KEY_SHOW_TITLE + " ASC");
	}

	public boolean deleteShow(String showId) {
		return database.delete(DATABASE_TABLE, KEY_SHOW_ID + "= '" + showId + "'", null) > 0;
	}

	public boolean deleteAllShowsInDatabase() {
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	public boolean deleteAllUnidentifiedShows() {
		return database.delete(DATABASE_TABLE, KEY_SHOW_ID + "= 'invalid'", null) > 0;
	}

	public int count() {
		Cursor c = database.query(DATABASE_TABLE, new String[] {KEY_SHOW_ID, KEY_SHOW_TITLE},
				"not (" + KEY_SHOW_ID + " = 'invalid') and not (" + KEY_SHOW_TITLE + " like '%MizUnidentified%'" + ") and not (" + KEY_SHOW_ID + " = ''" + ")", null, KEY_SHOW_ID, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}

	private ContentValues createContentValues(String showId, String showTitle, String showPlot, String showActors, String showGenres, String showRating, String showCertification,
			String showRuntime, String showFirstAirdate, String isFavorite) {
		ContentValues values = new ContentValues();
		values.put(KEY_SHOW_ID, showId);
		values.put(KEY_SHOW_TITLE, showTitle);
		values.put(KEY_SHOW_PLOT, showPlot);
		values.put(KEY_SHOW_ACTORS, showActors);
		values.put(KEY_SHOW_GENRES, showGenres);
		values.put(KEY_SHOW_RATING, showRating);
		values.put(KEY_SHOW_CERTIFICATION, showCertification);
		values.put(KEY_SHOW_RUNTIME, showRuntime);
		values.put(KEY_SHOW_FIRST_AIRDATE, showFirstAirdate);
		values.put(KEY_SHOW_EXTRA1, isFavorite);
		return values;
	}
}