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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;

public class DbAdapterMovieMappings extends AbstractDbAdapter {

	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_TMDB_ID = "tmdbid";
	public static final String KEY_IGNORED = "ignored";

	public static final String DATABASE_TABLE = "moviemap";

	public static final String[] ALL_COLUMNS = new String[]{KEY_FILEPATH, KEY_TMDB_ID, KEY_IGNORED};

	public DbAdapterMovieMappings(Context context) {
		super(context);
	}

	public long createFilepathMapping(String filepath, String tmdbId) {
        // We only want to create a filepath mapping if
        // the filepath doesn't already exist
		if (!filepathExists(tmdbId, filepath)) {
			// Add values to a ContentValues object
			ContentValues values = new ContentValues();
			values.put(KEY_FILEPATH, filepath);
			values.put(KEY_TMDB_ID, tmdbId);
			values.put(KEY_IGNORED, 0);

			// Insert into database
            return mDatabase.insert(DATABASE_TABLE, null, values);
		}
		return -1;
	}

	public Cursor getAllFilepaths(boolean includeRemoved) {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, includeRemoved ? null : "NOT(" + KEY_IGNORED + " = '1')", null, null, null, null);
	}

	public Cursor getAllUnidentifiedFilepaths() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, "NOT(" + KEY_IGNORED + " = '1') AND " + KEY_TMDB_ID + "='" + DbAdapterMovies.UNIDENTIFIED_ID + "'", null, null, null, null);
	}
	
	public boolean deleteAllUnidentifiedFilepaths() {
		return mDatabase.delete(DATABASE_TABLE, KEY_TMDB_ID + " = ?", new String[]{DbAdapterMovies.UNIDENTIFIED_ID}) > 0;
	}

	public String getFirstFilepathForMovie(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, new String[]{KEY_TMDB_ID, KEY_FILEPATH}, KEY_TMDB_ID + " = ?", new String[]{tmdbId}, null, null, null);
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

	public boolean updateTmdbId(String filepath, String currentId, String newId) {
		ContentValues values = new ContentValues();
		values.put(KEY_TMDB_ID, newId);

		return mDatabase.update(DATABASE_TABLE, values, KEY_FILEPATH + " = ? AND " + KEY_TMDB_ID + " = ?", new String[]{filepath, currentId}) > 0;
	}

    public boolean updateTmdbId(String filepath, String newId) {
        ContentValues values = new ContentValues();
        values.put(KEY_TMDB_ID, newId);

        return mDatabase.update(DATABASE_TABLE, values, KEY_FILEPATH + " = ?", new String[]{filepath}) > 0;
    }

	public boolean exists(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_TMDB_ID + " = ?", new String[]{tmdbId}, null, null, null);
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

	public boolean filepathExists(String tmdbId, String filepath) {
		String[] selectionArgs = new String[]{tmdbId, filepath};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_TMDB_ID + " = ? AND " + KEY_FILEPATH + " = ?", selectionArgs, null, null, null);
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
		return mDatabase.delete(DATABASE_TABLE, KEY_TMDB_ID + " = ?", new String[]{tmdbId}) > 0;
	}

	public boolean deleteAllMovies() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean hasMultipleFilepaths(String tmdbId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_TMDB_ID + " = ?", new String[]{tmdbId}, null, null, null);
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

    /**
     * Gets all un-ignored filepaths mapped to a given movie ID
     * @param tmdbId
     * @return
     */
	public ArrayList<String> getMovieFilepaths(String tmdbId) {
        if (TextUtils.isEmpty(tmdbId))
            return new ArrayList<>();

		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_TMDB_ID + " = ? AND " + KEY_IGNORED + " = '0'", new String[]{tmdbId}, null, null, null);
		ArrayList<String> paths = new ArrayList<String>();

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					paths.add(cursor.getString(cursor.getColumnIndex(DbAdapterMovieMappings.KEY_FILEPATH)));
				}
			} catch (Exception e) {	
			} finally {
				cursor.close();
			}
		}

		return paths;
	}

	public String getIdForFilepath(String filepath) {
		String[] selectionArgs = new String[]{filepath};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, new String[]{KEY_TMDB_ID, KEY_FILEPATH}, KEY_FILEPATH + " = ?", selectionArgs, null, null, null);
		String id = "";

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					id = cursor.getString(cursor.getColumnIndex(KEY_TMDB_ID));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return id;
	}

    /**
     * Used for unit testing
     * @return
     */
    public int count() {
        Cursor c = mDatabase.query(DATABASE_TABLE, new String[]{KEY_FILEPATH}, null, null, null, null, null);
        int count = c.getCount();
        c.close();
        return count;
    }
}