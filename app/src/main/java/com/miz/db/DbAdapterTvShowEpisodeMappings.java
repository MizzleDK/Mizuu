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

import com.miz.functions.MizLib;

import java.util.ArrayList;

public class DbAdapterTvShowEpisodeMappings extends AbstractDbAdapter {

	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_SEASON = "season";
	public static final String KEY_EPISODE = "episode";
	public static final String KEY_IGNORED = "ignored";

	public static final String DATABASE_TABLE = "episodes_map";

	public static final String[] ALL_COLUMNS = new String[]{KEY_FILEPATH, KEY_SHOW_ID, KEY_SEASON, KEY_EPISODE, KEY_IGNORED};

	public DbAdapterTvShowEpisodeMappings(Context context) {
		super(context);
	}

	public long createFilepathMapping(String filepath, String showId, String season, String episode) {
		if (!filepathExists(showId, season, episode, filepath)) {
			// Add values to a ContentValues object
			ContentValues values = new ContentValues();
			values.put(KEY_FILEPATH, filepath);
			values.put(KEY_SHOW_ID, showId);
			values.put(KEY_SEASON, season);
			values.put(KEY_EPISODE, episode);
			values.put(KEY_IGNORED, 0);

			// Insert into database
			return mDatabase.insert(DATABASE_TABLE, null, values);
		}
		return -1;
	}

	public boolean filepathExists(String showId, String season, String episode, String filepath) {
		String[] selectionArgs = new String[]{showId, filepath, season, episode};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ? AND " + KEY_FILEPATH + " = ? AND "
				+ KEY_SEASON + " = ? AND " + KEY_EPISODE + " = ?", selectionArgs, null, null, null);
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

	public String getFirstFilepath(String showId, String season, String episode) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ? AND " + KEY_SEASON + " = ? AND " + KEY_EPISODE + " = ?",
				new String[]{showId, season, episode}, null, null, null);
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

	public ArrayList<String> getFilepathsForEpisode(String showId, String season, String episode) {
		ArrayList<String> paths = new ArrayList<String>();

		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ? AND " + KEY_SEASON + " = ? AND " + KEY_EPISODE + " = ?",
				new String[]{showId, season, episode}, null, null, null);

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					paths.add(cursor.getString(cursor.getColumnIndex(KEY_FILEPATH)));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return paths;
	}

    public ArrayList<String> getFilepathsForShow(String showId) {
        ArrayList<String> paths = new ArrayList<String>();

        Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ?" ,
                new String[]{showId}, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    paths.add(cursor.getString(cursor.getColumnIndex(KEY_FILEPATH)));
                }
            } catch (Exception e) {
            } finally {
                cursor.close();
            }
        }

        return paths;
    }

	public Cursor getAllUnidentifiedFilepaths() {
		String[] selectionArgs = new String[]{DbAdapterTvShows.UNIDENTIFIED_ID};
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ?", selectionArgs, null, null, null);
	}
	
	public Cursor getAllFilepaths() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, null, null, null, null, null);
	}

	public Cursor getAllFilepathInfo(String filepath) {
		String[] selectionArgs = new String[]{filepath};
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_FILEPATH + " = ?", selectionArgs, null, null, null);
	}

	public Cursor getAllFilepaths(String showId) {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ?", new String[]{showId}, null, null, null);
	}

	public boolean deleteFilepath(String filepath) {
		String[] selectionArgs = new String[]{filepath};
		return mDatabase.delete(DATABASE_TABLE, KEY_FILEPATH + " = ?", selectionArgs) > 0;
	}

	public boolean deleteAllFilepaths(String showId) {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + " = ?", new String[]{showId}) > 0;
	}

	public boolean deleteAllFilepaths() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	public boolean deleteAllUnidentifiedFilepaths() {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + " = ?", new String[]{DbAdapterTvShows.UNIDENTIFIED_ID}) > 0;
	}

	public boolean hasMultipleFilepaths(String showId, String season, String episode) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ? AND " + KEY_SEASON + " = ? AND " + KEY_EPISODE + " = ?",
				new String[]{showId, season, episode}, null, null, null);
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

	public boolean removeSeason(String showId, int season) {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + " = ? AND " + KEY_SEASON + " = ?",
				new String[]{showId, MizLib.addIndexZero(season)}) > 0;
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