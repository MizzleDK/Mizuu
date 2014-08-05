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

import java.util.ArrayList;

import com.miz.mizuu.MizuuApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

	// Database name
	private static final String DATABASE_NAME = "mizuu_data";

	// Database version
	private static final int DATABASE_VERSION = 5;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table movie (tmdbid TEXT PRIMARY KEY, title TEXT, plot TEXT," +
			"imdbid TEXT, rating TEXT, tagline TEXT, release TEXT, certification TEXT, runtime TEXT, trailer TEXT, genres TEXT," +
			"favourite INTEGER, actors TEXT, collection TEXT, to_watch TEXT, has_watched TEXT, date_added TEXT, collection_id TEXT," +
			"extra1 TEXT, extra2 TEXT, extra3 TEXT, extra4 TEXT);";

	private static DbHelper instance;

	private DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized DbHelper getHelper(Context context) {
		if (instance == null)
			instance = new DbHelper(context);
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {	
		
		Log.d("Mizuu", "Upgrading database...");
		
		if (oldVersion == 4) { // Upgrade to the new database structure
			
			// ArrayList to store the old database values
			ArrayList<ContentValues> mOldValues = new ArrayList<ContentValues>();

			// Old database table columns
			String[] oldColumns = new String[] {"filepath", "coverpath", "title", "plot", "tmdbid", "imdbid", "rating", "tagline", "release", "certification",
					"runtime", "trailer", "genres", "favourite", "watched" /* cast / actors */, "collection", "to_watch", "has_watched",
					"extra1" /* date added */, "extra2" /* collection ID */};

			Cursor cursor = database.query(DbAdapter.DATABASE_TABLE, oldColumns, null, null, null, null, null);
			
			// Let's load all the old data
			try {
				while (cursor.moveToNext()) {
					// Fetch it
					String filepath = cursor.getString(cursor.getColumnIndex("filepath"));
					String title = cursor.getString(cursor.getColumnIndex("title"));
					String plot = cursor.getString(cursor.getColumnIndex("plot"));
					String tmdbId = cursor.getString(cursor.getColumnIndex("tmdbid"));
					String imdbId = cursor.getString(cursor.getColumnIndex("imdbid"));
					String rating = cursor.getString(cursor.getColumnIndex("rating"));
					String tagline = cursor.getString(cursor.getColumnIndex("tagline"));
					String releaseDate = cursor.getString(cursor.getColumnIndex("release"));
					String certification = cursor.getString(cursor.getColumnIndex("certification"));
					String runtime = cursor.getString(cursor.getColumnIndex("runtime"));
					String trailer = cursor.getString(cursor.getColumnIndex("trailer"));
					String genres = cursor.getString(cursor.getColumnIndex("genres"));
					String favourite = cursor.getString(cursor.getColumnIndex("favourite"));
					String cast = cursor.getString(cursor.getColumnIndex("watched")); // a mistake in previous version of the database structure
					String collection = cursor.getString(cursor.getColumnIndex("collection"));
					String collectionId = cursor.getString(cursor.getColumnIndex("extra2"));
					String toWatch = cursor.getString(cursor.getColumnIndex("to_watch"));
					String hasWatched = cursor.getString(cursor.getColumnIndex("has_watched"));
					String dateAdded = cursor.getString(cursor.getColumnIndex("extra1"));
					
					// Add it to a ContentValues object
					ContentValues values = new ContentValues();
					values.put(DbAdapterMovieMapping.KEY_FILEPATH, filepath);
					values.put(DbAdapter.KEY_TMDB_ID, tmdbId);
					values.put(DbAdapter.KEY_TITLE, title);
					values.put(DbAdapter.KEY_PLOT, plot);
					values.put(DbAdapter.KEY_IMDB_ID, imdbId);
					values.put(DbAdapter.KEY_RATING, rating);
					values.put(DbAdapter.KEY_TAGLINE, tagline);
					values.put(DbAdapter.KEY_RELEASEDATE, releaseDate);
					values.put(DbAdapter.KEY_CERTIFICATION, certification);
					values.put(DbAdapter.KEY_RUNTIME, runtime);
					values.put(DbAdapter.KEY_TRAILER, trailer);
					values.put(DbAdapter.KEY_GENRES, genres);
					values.put(DbAdapter.KEY_FAVOURITE, favourite);
					values.put(DbAdapter.KEY_ACTORS, cast);
					values.put(DbAdapter.KEY_COLLECTION, collection);
					values.put(DbAdapter.KEY_TO_WATCH, toWatch);
					values.put(DbAdapter.KEY_HAS_WATCHED, hasWatched);
					values.put(DbAdapter.KEY_DATE_ADDED, dateAdded);
					values.put(DbAdapter.KEY_COLLECTION_ID, collectionId);

					// Add it to the array
					mOldValues.add(values);
				}
			} catch (Exception e) {
			} finally {
				// Close the cursor
				if (cursor != null)
					cursor.close();
			}

			// We have a copy of all the old data - now let's drop the old table...
			database.execSQL("DROP TABLE IF EXISTS movie");
			
			// ... and create the new one
			onCreate(database);

			// ... and add data to it and the filepath mapping database
			DbAdapterMovieMapping dbMapping = MizuuApplication.getMovieMappingAdapter();
			for (ContentValues values : mOldValues) {
				
				// Check if movie exists - if not, create it
				if (!movieExists(database, values.getAsString(DbAdapter.KEY_TMDB_ID))) {

					ContentValues contentValues = new ContentValues();
					contentValues.put(DbAdapter.KEY_TMDB_ID, values.getAsString(DbAdapter.KEY_TMDB_ID).isEmpty() ? values.getAsString(DbAdapterMovieMapping.KEY_FILEPATH) : values.getAsString(DbAdapter.KEY_TMDB_ID));
					contentValues.put(DbAdapter.KEY_TITLE, values.getAsString(DbAdapter.KEY_TITLE));
					contentValues.put(DbAdapter.KEY_PLOT, values.getAsString(DbAdapter.KEY_PLOT));
					contentValues.put(DbAdapter.KEY_IMDB_ID, values.getAsString(DbAdapter.KEY_IMDB_ID));
					contentValues.put(DbAdapter.KEY_RATING, values.getAsString(DbAdapter.KEY_RATING));
					contentValues.put(DbAdapter.KEY_TAGLINE, values.getAsString(DbAdapter.KEY_TAGLINE));
					contentValues.put(DbAdapter.KEY_RELEASEDATE, values.getAsString(DbAdapter.KEY_RELEASEDATE));
					contentValues.put(DbAdapter.KEY_CERTIFICATION, values.getAsString(DbAdapter.KEY_CERTIFICATION));
					contentValues.put(DbAdapter.KEY_RUNTIME, values.getAsString(DbAdapter.KEY_RUNTIME));
					contentValues.put(DbAdapter.KEY_TRAILER, values.getAsString(DbAdapter.KEY_TRAILER));
					contentValues.put(DbAdapter.KEY_GENRES, values.getAsString(DbAdapter.KEY_GENRES));
					contentValues.put(DbAdapter.KEY_FAVOURITE, values.getAsString(DbAdapter.KEY_FAVOURITE));
					contentValues.put(DbAdapter.KEY_ACTORS, values.getAsString(DbAdapter.KEY_ACTORS));
					contentValues.put(DbAdapter.KEY_COLLECTION, values.getAsString(DbAdapter.KEY_COLLECTION));
					contentValues.put(DbAdapter.KEY_TO_WATCH, values.getAsString(DbAdapter.KEY_TO_WATCH));
					contentValues.put(DbAdapter.KEY_HAS_WATCHED, values.getAsString(DbAdapter.KEY_HAS_WATCHED));
					contentValues.put(DbAdapter.KEY_DATE_ADDED, values.getAsString(DbAdapter.KEY_DATE_ADDED));
					contentValues.put(DbAdapter.KEY_COLLECTION_ID, values.getAsString(DbAdapter.KEY_COLLECTION_ID));

					database.insert(DbAdapter.DATABASE_TABLE, null, contentValues);
				}

				// Create filepath mapping to movie
				dbMapping.createFilepathMapping(values.getAsString(DbAdapterMovieMapping.KEY_FILEPATH), values.getAsString(DbAdapter.KEY_TMDB_ID).isEmpty() ? values.getAsString(DbAdapterMovieMapping.KEY_FILEPATH) : values.getAsString(DbAdapter.KEY_TMDB_ID));
			}
		} else {
			database.execSQL("DROP TABLE IF EXISTS movie");
			onCreate(database);
		}
	}

	private static boolean movieExists(SQLiteDatabase db, String movieId) {
		boolean result = false;

		Cursor mCursor = db.query(true, DbAdapter.DATABASE_TABLE, new String[]{DbAdapter.KEY_TMDB_ID}, DbAdapter.KEY_TMDB_ID + "='" + movieId + "'", null, DbAdapter.KEY_TMDB_ID, null, null, null);
		if (mCursor == null)
			return false;

		try {
			result = mCursor.getCount() > 0;
		} catch (Exception e) {
		} finally {
			mCursor.close();
		}

		return result;
	}
}