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

public class DbAdapterSources extends AbstractDbAdapter {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_TYPE = "type"; // movie / TV show
	public static final String KEY_FILESOURCE_TYPE = "is_smb"; // local file / NAS / other sources... Used to be 0 = local file, 1 = smb.
	public static final String KEY_USER = "user";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_DOMAIN = "domain";
	
	public static final String KEY_TYPE_MOVIE = "movie";
	public static final String KEY_TYPE_SHOW = "show";
	
	private static final String DATABASE_TABLE = "sources";

	private static final String[] ALL_ROWS = new String[]{KEY_ROWID, KEY_FILEPATH, KEY_FILESOURCE_TYPE, KEY_USER, KEY_PASSWORD, KEY_DOMAIN, KEY_TYPE};
	
	public DbAdapterSources(Context context) {
		super(context);
	}

	/**
	 * Creates a new file source.
	 * @param filepath
	 * @param type
	 * @param isSmb
	 * @param user
	 * @param password
	 * @param domain
	 * @return rowId of the newly created source
	 */
	public long createSource(String filepath, String type, int isSmb, String user, String password, String domain) {
		return mDatabase.insert(DATABASE_TABLE, null, createContentValues(filepath, type, isSmb, user, password, domain));
	}

	/**
	 * Deletes a file source.
	 * @param rowId
	 * @return Boolean whether it was successful or not
	 */
	public boolean deleteSource(long rowId) {
		return mDatabase.delete(DATABASE_TABLE, KEY_ROWID + "='" + rowId + "'", null) > 0;
	}
	
	/**
	 * Get a Cursor of all movie file sources
	 * @return
	 */
	public Cursor fetchAllMovieSources() {
		return mDatabase.query(DATABASE_TABLE, ALL_ROWS, KEY_TYPE + "='" + KEY_TYPE_MOVIE + "'", null, null, null, null);
	}
	
	/**
	 * Get a Cursor of all show file sources
	 * @return
	 */
	public Cursor fetchAllShowSources() {
		return mDatabase.query(DATABASE_TABLE, ALL_ROWS, KEY_TYPE + "='" + KEY_TYPE_SHOW + "'", null, null, null, null);
	}
	
	/**
	 * Get a Cursor of all file sources
	 * @return
	 */
	public Cursor fetchAllSources() {
		return mDatabase.query(DATABASE_TABLE, ALL_ROWS, null, null, null, null, null);
	}

	private ContentValues createContentValues(String filepath, String type, int fileSourceType, String user, String password, String domain) {
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_TYPE, type);
		values.put(KEY_FILESOURCE_TYPE, fileSourceType);
		values.put(KEY_USER, user);
		values.put(KEY_PASSWORD, password);
		values.put(KEY_DOMAIN, domain);
		return values;
	}
}