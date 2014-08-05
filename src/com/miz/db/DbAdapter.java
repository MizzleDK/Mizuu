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

package com.miz.db;

import com.miz.mizuu.MizuuApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	// Database fields
	public static final String KEY_TMDB_ID = "tmdbid"; // Unidentified movies and .nfo files without TMDb ID use the filepath
	public static final String KEY_TITLE = "title";
	public static final String KEY_PLOT = "plot";
	public static final String KEY_IMDB_ID = "imdbid";
	public static final String KEY_RATING = "rating";
	public static final String KEY_TAGLINE = "tagline";
	public static final String KEY_RELEASEDATE = "release";
	public static final String KEY_CERTIFICATION = "certification";
	public static final String KEY_RUNTIME = "runtime";
	public static final String KEY_TRAILER = "trailer";
	public static final String KEY_GENRES = "genres";
	public static final String KEY_FAVOURITE = "favourite";
	public static final String KEY_ACTORS = "actors";
	public static final String KEY_COLLECTION = "collection";
	public static final String KEY_COLLECTION_ID = "collection_id";
	public static final String KEY_TO_WATCH = "to_watch";
	public static final String KEY_HAS_WATCHED = "has_watched";
	public static final String KEY_DATE_ADDED = "date_added";
	public static final String KEY_EXTRA_1 = "extra1"; // Not in use
	public static final String KEY_EXTRA_2 = "extra2"; // Not in use
	public static final String KEY_EXTRA_3 = "extra3"; // Not in use
	public static final String KEY_EXTRA_4 = "extra4"; // Not in use

	public static final String DATABASE_TABLE = "movie";
	
	public static final String REMOVED_MOVIE_TITLE = "MIZ_REMOVED_MOVIE";

	public static final String[] SELECT_ALL = new String[] {KEY_TMDB_ID, KEY_TITLE, KEY_PLOT, KEY_IMDB_ID, KEY_RATING, KEY_TAGLINE, KEY_RELEASEDATE, KEY_CERTIFICATION,
		KEY_RUNTIME, KEY_TRAILER, KEY_GENRES, KEY_FAVOURITE, KEY_ACTORS, KEY_COLLECTION, KEY_COLLECTION_ID, KEY_TO_WATCH, KEY_HAS_WATCHED, KEY_DATE_ADDED};

	private SQLiteDatabase mDatabase;

	public DbAdapter(Context context) {
		mDatabase = DbHelper.getHelper(context).getWritableDatabase();
	}

	public void close() {
		mDatabase.close();
	}

	/**
	 * Create a new movie
	 * If the movie is successfully created, return the new
	 * ID for that movie, otherwise return a -1 to indicate failure.
	 */
	public void createMovie(String tmdbid, String title, String plot, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String actors, String collection,
			String collectionId, String toWatch, String hasWatched, String date) {
		
		if (movieExists(tmdbid))
			return; // It already exists, let's not create it again
		
		ContentValues initialValues = createContentValues(tmdbid, title, plot, imdbid, rating, tagline, release, certification, runtime, trailer, genres, favourite, actors, collection, collectionId, toWatch, hasWatched, date, true);
		mDatabase.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public void createOrUpdateMovie(String tmdbid, String title, String plot, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String actors, String collection,
			String collectionId, String toWatch, String hasWatched, String date) {
		if (movieExists(tmdbid)) {
			// It already exists - let's update it while keeping
			// favourite, watchlist and watched status data intact
			updateMovie(tmdbid, title, plot, imdbid, rating, tagline, release, certification, runtime, trailer, genres, actors, collection, collectionId, date);
		} else {
			// It doesn't exist! Let's create it
			createMovie(tmdbid, title, plot, imdbid, rating, tagline, release, certification, runtime, trailer, genres, favourite, actors, collection, collectionId, toWatch, hasWatched, date);
		}
	}

	/**
	 * Update the movie
	 */
	public boolean updateMovie(String tmdbid, String title, String plot, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String actors, String collection, String collectionId, String date) {
		ContentValues updateValues = createUpdateContentValues(title, plot, imdbid, rating, tagline, release, certification, runtime, trailer, genres, actors, collection, collectionId, date);
		return mDatabase.update(DATABASE_TABLE, updateValues, KEY_TMDB_ID + "='" + tmdbid + "'", null) > 0;
	}

	public boolean updateMovieSingleItem(String tmdbId, String column, String value) {
		ContentValues values = new ContentValues();
		values.put(column, value);

		return mDatabase.update(DATABASE_TABLE, values, KEY_TMDB_ID + "='" + tmdbId + "'", null) > 0;
	}

	/**
	 * Update the movie
	 */
	public boolean editUpdateMovie(String tmdbId, String title, String plot, String rating, String tagline, String release,
			String certification, String runtime, String genres, String toWatch, String hasWatched, String date) {
		ContentValues updateValues = createEditContentValues(title, plot, rating, tagline, release, certification, runtime, genres, toWatch, hasWatched, date);
		return mDatabase.update(DATABASE_TABLE, updateValues, KEY_TMDB_ID + "='" + tmdbId + "'", null) > 0;
	}

	/**
	 * Marks movie as ignored
	 */
	public boolean ignoreMovie(String tmdbId) {
		boolean result = updateMovieSingleItem(tmdbId, KEY_TITLE, REMOVED_MOVIE_TITLE);
		return result && MizuuApplication.getMovieMappingAdapter().ignoreMovie(tmdbId);
	}

	/**
	 * Deletes movie
	 */
	public boolean deleteMovie(String tmdbId) {
		boolean result = mDatabase.delete(DATABASE_TABLE, KEY_TMDB_ID + "='" + tmdbId + "'", null) > 0;
		return result && MizuuApplication.getMovieMappingAdapter().deleteMovie(tmdbId);
	}

	public boolean deleteAllMovies() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0 && MizuuApplication.getMovieMappingAdapter().deleteAllMovies();
	}

	/**
	 * Return a Cursor over the list of all movies in the database
	 */
	public Cursor fetchAllMovies(String sort, boolean includeRemoved) {
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL, includeRemoved ? null : "NOT(" + KEY_TITLE + " = '" + REMOVED_MOVIE_TITLE + "')", null, null, null, sort);
	}

	public Cursor fetchAllMoviesByActor(String actor) {
		String newQuery = actor.replace("'", "''");
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL,
				KEY_ACTORS + " like '%" + newQuery + "%'", null, KEY_TMDB_ID, null, KEY_TITLE + " ASC");
	}

	public int getMovieCountByActor(String actor) {
		String newQuery = actor.replace("'", "''");
		Cursor cursor = mDatabase.query(DATABASE_TABLE, SELECT_ALL,
				KEY_ACTORS + " like '%" + newQuery + "%'", null, KEY_TMDB_ID, null, KEY_TITLE + " ASC");
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

	public Cursor getAllIgnoredMovies() {
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL, KEY_TITLE + " = '" + REMOVED_MOVIE_TITLE + "'", null, null, null, null);
	}

	/**
	 * Return a Cursor over the list of all genres in the database
	 */
	public Cursor fetchAllGenres() {
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL,
				null, null, KEY_GENRES, null, KEY_GENRES + " ASC");
	}

	public Cursor fetchMovie(String movieId) throws SQLException {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, SELECT_ALL, KEY_TMDB_ID + "='" + movieId + "'", null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public boolean movieExists(String movieId) {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, SELECT_ALL, KEY_TMDB_ID + "='" + movieId + "'", null, null, null, null, null);
		if (mCursor == null)
			return false;
		try {
			if (mCursor.getCount() == 0) {
				mCursor.close();
				return false;
			}
		} catch (Exception e) {
			mCursor.close();
			return false;
		}

		mCursor.close();
		return true;
	}

	private ContentValues createContentValues(String tmdbid, String title,
			String plot, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String actors,
			String collection, String collectionId, String toWatch, String hasWatched, String date, boolean includeTmdbId) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, title);
		values.put(KEY_PLOT, plot);
		if (includeTmdbId)
			values.put(KEY_TMDB_ID, tmdbid);
		values.put(KEY_IMDB_ID, imdbid);
		values.put(KEY_RATING, rating);
		values.put(KEY_TAGLINE, tagline);
		values.put(KEY_RELEASEDATE, release);
		values.put(KEY_CERTIFICATION, certification);
		values.put(KEY_RUNTIME, runtime);
		values.put(KEY_TRAILER, trailer);
		values.put(KEY_GENRES, genres);
		values.put(KEY_FAVOURITE, favourite);
		values.put(KEY_ACTORS, actors);
		values.put(KEY_COLLECTION, collection);
		values.put(KEY_COLLECTION_ID, collectionId);
		values.put(KEY_TO_WATCH, toWatch);
		values.put(KEY_HAS_WATCHED, hasWatched);
		values.put(KEY_DATE_ADDED, date);
		return values;
	}
	
	private ContentValues createUpdateContentValues(String title, String plot, String imdbid,
			String rating, String tagline, String release, String certification, String runtime,
			String trailer, String genres, String actors, String collection, String collectionId, String date) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, title);
		values.put(KEY_PLOT, plot);
		values.put(KEY_IMDB_ID, imdbid);
		values.put(KEY_RATING, rating);
		values.put(KEY_TAGLINE, tagline);
		values.put(KEY_RELEASEDATE, release);
		values.put(KEY_CERTIFICATION, certification);
		values.put(KEY_RUNTIME, runtime);
		values.put(KEY_TRAILER, trailer);
		values.put(KEY_GENRES, genres);
		values.put(KEY_ACTORS, actors);
		values.put(KEY_COLLECTION, collection);
		values.put(KEY_COLLECTION_ID, collectionId);
		values.put(KEY_DATE_ADDED, date);
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
		values.put(KEY_EXTRA_1, date);
		return values;
	}

	public int count() {
		Cursor c = mDatabase.query(DATABASE_TABLE, new String[] {KEY_TITLE}, "NOT(" + KEY_TITLE + " = '" + REMOVED_MOVIE_TITLE + "')", null, null, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}

	public int countWatchlist() {
		Cursor c = mDatabase.query(DATABASE_TABLE, new String[] {KEY_TO_WATCH}, KEY_TO_WATCH + " = '1' AND NOT(" + KEY_TITLE + " = '" + REMOVED_MOVIE_TITLE + "')", null, null, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}
}