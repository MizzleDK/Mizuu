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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class DbAdapterTvShows extends AbstractDbAdapter {

	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_SHOW_TITLE = "show_title";
	public static final String KEY_SHOW_PLOT = "show_description";
	public static final String KEY_SHOW_ACTORS = "show_actors";
	public static final String KEY_SHOW_GENRES = "show_genres";
	public static final String KEY_SHOW_RATING = "show_rating";
	public static final String KEY_SHOW_CERTIFICATION = "show_certification";
	public static final String KEY_SHOW_RUNTIME = "show_runtime";
	public static final String KEY_SHOW_FIRST_AIRDATE = "show_first_airdate";
	public static final String KEY_SHOW_FAVOURITE = "favourite";

	public static final String DATABASE_TABLE = "tvshows";

	public static final String UNIDENTIFIED_ID = "invalid";
	
	public static final String[] SELECT_ALL = new String[]{KEY_SHOW_ID, KEY_SHOW_TITLE, KEY_SHOW_PLOT, KEY_SHOW_ACTORS,
		KEY_SHOW_GENRES, KEY_SHOW_RATING, KEY_SHOW_RATING, KEY_SHOW_CERTIFICATION, KEY_SHOW_RUNTIME,
		KEY_SHOW_FIRST_AIRDATE, KEY_SHOW_FAVOURITE};

	public DbAdapterTvShows(Context context) {
		super(context);
	}

	public long createShow(String showId, String showTitle, String showPlot, String showActors, String showGenres, String showRating, String showCertification,
			String showRuntime, String showFirstAirdate, String isFavorite) {
		if (getShow(showId).getCount() == 0) {
			ContentValues initialValues = createContentValues(showId, showTitle, showPlot, showActors, showGenres, showRating, showCertification, showRuntime, showFirstAirdate, isFavorite);
			return mDatabase.insert(DATABASE_TABLE, null, initialValues);
		} else {
			return -1;
		}
	}
	
	public boolean showExists(String showTitle) {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, SELECT_ALL, KEY_SHOW_TITLE + "='" + showTitle.replace("'", "''") + "'", null, KEY_SHOW_TITLE, null, null, null);
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
	
	public String getShowId(String showTitle) {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, SELECT_ALL, KEY_SHOW_TITLE + "='" + showTitle.replace("'", "''") + "'", null, KEY_SHOW_TITLE, null, null, null);
		if (mCursor == null)
			return "";
		try {
			if (mCursor.getCount() == 0) {
				mCursor.close();
				return "";
			}
		} catch (Exception e) {
			mCursor.close();
			return "";
		}
		
		mCursor.moveToFirst();
		String showId = mCursor.getString(mCursor.getColumnIndex(KEY_SHOW_ID));
		
		mCursor.close();
		return showId;
	}

	public boolean updateShowSingleItem(String showId, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + " = '" + showId + "'", null) > 0;
	}

	public Cursor getShow(String showId) {
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL, KEY_SHOW_ID + " = '" + showId + "'", null, null, null, null);
	}

	public String getShowTitle(String showId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, new String[]{KEY_SHOW_TITLE, KEY_SHOW_ID}, KEY_SHOW_ID + " = '" + showId + "'", null, null, null, null);
		if (cursor.moveToFirst()) {
			String showTitle = cursor.getString(cursor.getColumnIndex(KEY_SHOW_TITLE));
			cursor.close();
			return showTitle;
		}
		return "";
	}

	public Cursor getAllShows() {
		return mDatabase.query(DATABASE_TABLE, SELECT_ALL, "NOT(" + KEY_SHOW_ID + " = 'invalid')", null, null, null, KEY_SHOW_TITLE + " ASC");
	}

	public boolean deleteShow(String showId) {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "= '" + showId + "'", null) > 0;
	}

	public boolean deleteAllShowsInDatabase() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean deleteAllUnidentifiedShows() {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "= 'invalid'", null) > 0;
	}

	public int count() {
		Cursor c = mDatabase.query(DATABASE_TABLE, new String[] {KEY_SHOW_ID, KEY_SHOW_TITLE},
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
		values.put(KEY_SHOW_FAVOURITE, isFavorite);
		return values;
	}
	
	public ArrayList<String> getCertifications() {
		ArrayList<String> certifications = new ArrayList<String>();
		Cursor cursor = mDatabase.query(DATABASE_TABLE, new String[]{KEY_SHOW_CERTIFICATION}, null, null, KEY_SHOW_CERTIFICATION, null, null);

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					String certification = cursor.getString(cursor.getColumnIndex(KEY_SHOW_CERTIFICATION));
					if (!TextUtils.isEmpty(certification))
					certifications.add(certification);
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}
		
		return certifications;
	}
	
	public boolean editShow(String showId, String title, String description,
			String genres, String runtime, String rating, String firstAirDate, String certification) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_SHOW_TITLE, title);
		cv.put(KEY_SHOW_PLOT, description);
		cv.put(KEY_SHOW_RATING, rating);
		cv.put(KEY_SHOW_FIRST_AIRDATE, firstAirDate);
		cv.put(KEY_SHOW_CERTIFICATION, certification);
		cv.put(KEY_SHOW_RUNTIME, runtime);
		cv.put(KEY_SHOW_GENRES, genres);
		return mDatabase.update(DATABASE_TABLE, cv, KEY_SHOW_ID + "='" + showId + "'", null) > 0;
	}
}