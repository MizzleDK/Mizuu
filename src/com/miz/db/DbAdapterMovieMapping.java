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

import java.util.HashMap;

import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MovieFilepath;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapterMovieMapping {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_TMDB_ID = "tmdbid";
	public static final String KEY_IGNORED = "ignored";

	public static final String DATABASE_TABLE = "moviemap";

	private static final String[] ALL_ROWS = new String[]{KEY_ROWID, KEY_FILEPATH, KEY_TMDB_ID, KEY_IGNORED};

	private SQLiteDatabase mDatabase;

	public DbAdapterMovieMapping(Context context) {
		mDatabase = DbHelperMovieMapping.getHelper(context).getWritableDatabase();
	}

	public void close() {
		mDatabase.close();
	}

	public long createFilepathMapping(String filepath, String tmdbId) {

		// Add values to a ContentValues object
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_TMDB_ID, tmdbId);

		// Insert into database
		return mDatabase.insert(DATABASE_TABLE, null, values);
	}

	public Cursor getAllFilepaths(boolean includeRemoved) {
		return mDatabase.query(DATABASE_TABLE, ALL_ROWS, includeRemoved ? null : "NOT(" + KEY_IGNORED + " = '1')", null, null, null, null);
	}

	public HashMap<String, String> getFilepathMap() {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_ROWS, null, null, null, null, null);

		ColumnIndexCache cache = new ColumnIndexCache();
		HashMap<String, String> map = new HashMap<String, String>();

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					map.put(cursor.getString(cache.getColumnIndex(cursor, KEY_TMDB_ID)), cursor.getString(cache.getColumnIndex(cursor, KEY_FILEPATH)));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return map;
	}

	public String getFirstFilepathForMovie(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, new String[]{KEY_TMDB_ID, KEY_FILEPATH}, KEY_TMDB_ID + "='" + tmdbId + "'", null, null, null, null);
		String filepath = "";

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					filepath = cursor.getString(cursor.getColumnIndex(KEY_FILEPATH));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return filepath;
	}

	public boolean updateTmdbId(String filepath, String newTmdbId) {
		ContentValues values = new ContentValues();
		values.put(KEY_TMDB_ID, newTmdbId);

		return mDatabase.update(DATABASE_TABLE, values, KEY_FILEPATH + "='" + filepath + "'", null) > 0;
	}
	
	public boolean exists(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_ROWS, KEY_TMDB_ID + "='" + tmdbId + "'", null, null, null, null);
		boolean result = false;

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0)
					result = true;
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return result;
	}
	
	public boolean deleteMovie(String tmdbId) {
		return mDatabase.delete(DATABASE_TABLE, KEY_TMDB_ID + "='" + tmdbId + "'", null) > 0;
	}
	
	public boolean deleteAllMovies() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean ignoreMovie(String tmdbId) {
		ContentValues ignore = new ContentValues();
		ignore.put(KEY_IGNORED, 1);
		return mDatabase.update(DATABASE_TABLE, ignore, KEY_TMDB_ID + "='" + tmdbId + "'", null) > 0;
	}

	public boolean hasMultipleFilepaths(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_ROWS, KEY_TMDB_ID + "='" + tmdbId + "'", null, null, null, null);
		boolean result = false;

		if (cursor != null) {
			try {
				if (cursor.getCount() > 1)
					result = true;
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return result;
	}

	public MovieFilepath[] getMovieFilepaths(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_ROWS, KEY_TMDB_ID + "='" + tmdbId + "'", null, null, null, null);
		MovieFilepath[] paths = new MovieFilepath[]{};

		if (cursor != null) {
			try {
				paths = new MovieFilepath[cursor.getCount()];
				int counter = 0;
				while (cursor.moveToNext()) {
					paths[counter] = new MovieFilepath(cursor.getInt(cursor.getColumnIndex(DbAdapterMovieMapping.KEY_ROWID)),
							cursor.getString(cursor.getColumnIndex(DbAdapterMovieMapping.KEY_FILEPATH)),
							cursor.getInt(cursor.getColumnIndex(DbAdapterMovieMapping.KEY_IGNORED)) == 1);
					counter++;
				}
			} catch (Exception e) {	
			} finally {
				cursor.close();
			}
		}

		return paths;
	}
}