package com.miz.mizuu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	// Database fields
	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_COVERPATH = "coverpath";
	public static final String KEY_TITLE = "title";
	public static final String KEY_PLOT = "plot";
	public static final String KEY_TMDBID = "tmdbid";
	public static final String KEY_IMDBID = "imdbid";
	public static final String KEY_RATING = "rating";
	public static final String KEY_TAGLINE = "tagline";
	public static final String KEY_RELEASEDATE = "release";
	public static final String KEY_CERTIFICATION = "certification";
	public static final String KEY_RUNTIME = "runtime";
	public static final String KEY_TRAILER = "trailer";
	public static final String KEY_GENRES = "genres";
	public static final String KEY_FAVOURITE = "favourite";
	public static final String KEY_CAST = "watched"; // This should really be KEY_CAST, but I'll leave that to future updates
	public static final String KEY_COLLECTION = "collection";
	public static final String KEY_TO_WATCH = "to_watch";
	public static final String KEY_HAS_WATCHED = "has_watched";
	public static final String KEY_EXTRA_1 = "extra1"; // date added
	public static final String KEY_EXTRA_2 = "extra2"; // Collection ID
	public static final String KEY_EXTRA_3 = "extra3"; // Not in use
	public static final String KEY_EXTRA_4 = "extra4"; // Not in use

	public static final String DATABASE_TABLE = "movie";

	public static final String[] SELECT_ALL = new String[] {KEY_ROWID, KEY_FILEPATH, KEY_COVERPATH,
		KEY_TITLE, KEY_PLOT, KEY_TMDBID, KEY_IMDBID, KEY_RATING, KEY_TAGLINE, KEY_RELEASEDATE, KEY_CERTIFICATION,
		KEY_RUNTIME, KEY_TRAILER, KEY_GENRES, KEY_FAVOURITE, KEY_CAST, KEY_COLLECTION, KEY_TO_WATCH, KEY_HAS_WATCHED,
		KEY_EXTRA_1, KEY_EXTRA_2, KEY_EXTRA_3, KEY_EXTRA_4};

	private SQLiteDatabase database;

	public DbAdapter(Context context) {
		database = DbHelper.getHelper(context).getWritableDatabase();
	}
	
	public void close() {
		database.close();
	}

	/**
	 * Create a new movie
	 * If the movie is successfully created, return the new
	 * rowId for that movie, otherwise return a -1 to indicate failure.
	 */
	public long createMovie(String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched, String collection, String collectionId, String toWatch, String hasWatched, String date) {
		ContentValues initialValues = createContentValues(filepath, coverpath, title, plot, tmdbid, imdbid,
				rating, tagline, release, certification, runtime, trailer, genres, favourite, watched, collection, collectionId, toWatch, hasWatched, date);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Update the movie
	 */
	public boolean updateMovie(long rowId, String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched,
			String collection, String collectionId, String toWatch, String hasWatched, String date) {
		ContentValues updateValues = createContentValues(filepath, coverpath, title, plot, tmdbid, imdbid,
				rating, tagline, release, certification, runtime, trailer, genres, favourite, watched, collection, collectionId, toWatch, hasWatched, date);
		return database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	public boolean updateMovieSingleItem(long rowId, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);

		return database.update(DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Update the movie
	 */
	public boolean editUpdateMovie(long rowId, String title, String plot, String rating, String tagline, String release,
			String certification, String runtime, String genres, String toWatch, String hasWatched, String date) {
		ContentValues updateValues = createEditContentValues(title, plot, rating, tagline, release, certification, runtime, genres, toWatch, hasWatched, date);
		return database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Marks movie as ignored
	 */
	public boolean ignoreMovie(long rowId) {
		ContentValues updateValues = createEditContentValues("MIZ_REMOVED_MOVIE", "", "", "", "", "", "", "", "", "", "");
		return database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Deletes movie
	 */
	public boolean deleteMovie(long rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteAllMovies() {
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all movies in the database
	 */
	public Cursor fetchAllMovies(String sort, boolean includeRemoved) {
		if (includeRemoved)
			return database.query(DATABASE_TABLE, SELECT_ALL, null, null, null, null, sort);
		else
			return database.query(DATABASE_TABLE, SELECT_ALL, "NOT(" + KEY_TITLE + " = 'MIZ_REMOVED_MOVIE')", null, null, null, sort);
	}

	public Cursor fetchAllMoviesByActor(String actor) {
		String newQuery = actor.replace("'", "''");
		return database.query(DATABASE_TABLE, SELECT_ALL,
				KEY_CAST + " like '%" + newQuery + "%'", null, null, null, KEY_TITLE + " ASC");
	}
	
	public int getMovieCountByActor(String actor) {
		String newQuery = actor.replace("'", "''");
		Cursor cursor = database.query(DATABASE_TABLE, SELECT_ALL,
				KEY_CAST + " like '%" + newQuery + "%'", null, null, null, KEY_TITLE + " ASC");
		return cursor.getCount();
	}

	public Cursor getAllIgnoredMovies() {
		return database.query(DATABASE_TABLE, SELECT_ALL, KEY_TITLE + " = 'MIZ_REMOVED_MOVIE'", null, null, null, null);
	}

	/**
	 * Return a Cursor over the list of all genres in the database
	 */
	public Cursor fetchAllGenres() {
		return database.query(DATABASE_TABLE, SELECT_ALL,
				null, null, KEY_GENRES, null, KEY_GENRES + " ASC");
	}

	/**
	 * Return a Cursor positioned at the defined movie
	 */
	public Cursor fetchMovie(long rowId) throws SQLException {
		Cursor mCursor = database.query(true, DATABASE_TABLE, SELECT_ALL, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public Cursor fetchMovie(String movieId) throws SQLException {
		Cursor mCursor = database.query(true, DATABASE_TABLE, SELECT_ALL, KEY_TMDBID + "=" + movieId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public boolean movieExists(String movieId) {
		Cursor mCursor = database.query(true, DATABASE_TABLE, SELECT_ALL, KEY_TMDBID + "=" + movieId, null, null, null, null, null);
		if (mCursor == null)
			return false;
		if (mCursor.getCount() == 0)
			return false;
		return true;
	}

	public Cursor fetchRowId(String movieUrl) {
		movieUrl = movieUrl.replaceAll("\'", "\'\'");
		Cursor mCursor = database.query(DATABASE_TABLE, new String[] {KEY_ROWID,},
				KEY_FILEPATH + " = \'" + movieUrl + "\'", null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	private ContentValues createContentValues(String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched,
			String collection, String collectionId, String toWatch, String hasWatched, String date) {
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_COVERPATH, coverpath);
		values.put(KEY_TITLE, title);
		values.put(KEY_PLOT, plot);
		values.put(KEY_TMDBID, tmdbid);
		values.put(KEY_IMDBID, imdbid);
		values.put(KEY_RATING, rating);
		values.put(KEY_TAGLINE, tagline);
		values.put(KEY_RELEASEDATE, release);
		values.put(KEY_CERTIFICATION, certification);
		values.put(KEY_RUNTIME, runtime);
		values.put(KEY_TRAILER, trailer);
		values.put(KEY_GENRES, genres);
		values.put(KEY_FAVOURITE, favourite);
		values.put(KEY_CAST, watched);
		values.put(KEY_COLLECTION, collection);
		values.put(KEY_TO_WATCH, toWatch);
		values.put(KEY_HAS_WATCHED, hasWatched);
		values.put(KEY_EXTRA_1, date);
		values.put(KEY_EXTRA_2, collectionId);
		return values;
	}

	private ContentValues createEditContentValues(String title, String plot, String rating, String tagline,
			String release, String certification, String runtime, String genres, String toWatch, String hasWatched, String date) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, title);
		values.put(KEY_PLOT, plot);
		values.put(KEY_RATING, rating);
		values.put(KEY_TAGLINE, tagline);
		values.put(KEY_RELEASEDATE, release);
		values.put(KEY_CERTIFICATION, certification);
		values.put(KEY_RUNTIME, runtime);
		values.put(KEY_GENRES, genres);
		values.put(KEY_TO_WATCH, toWatch);
		values.put(KEY_HAS_WATCHED, hasWatched);
		values.put(KEY_EXTRA_2, date);
		return values;
	}

	public int count() {
		Cursor c = database.query(DATABASE_TABLE, new String[] {KEY_TITLE}, "NOT(" + KEY_TITLE + " = 'MIZ_REMOVED_MOVIE')", null, null, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}

	public int countWatchlist() {
		Cursor c = database.query(DATABASE_TABLE, new String[] {KEY_TO_WATCH}, KEY_TO_WATCH + " = '1' AND NOT(" + KEY_TITLE + " = 'MIZ_REMOVED_MOVIE')", null, null, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}
}