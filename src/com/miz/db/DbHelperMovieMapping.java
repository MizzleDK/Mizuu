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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelperMovieMapping extends SQLiteOpenHelper {

	// Database name
	private static final String DATABASE_NAME = "mizuu_movie_map";

	// Database version
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table " + DbAdapterMovieMapping.DATABASE_TABLE + " (" + DbAdapterMovieMapping.KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			DbAdapterMovieMapping.KEY_FILEPATH + " TEXT, " + DbAdapterMovieMapping.KEY_TMDB_ID + " TEXT, " + DbAdapterMovieMapping.KEY_IGNORED + " INTEGER);";
	
	private static final String INDEX_CREATE = "create index tmdbid_index on " + DbAdapterMovieMapping.DATABASE_TABLE + "(" + DbAdapterMovieMapping.KEY_TMDB_ID + ");";

	private static DbHelperMovieMapping instance;

	private DbHelperMovieMapping(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized DbHelperMovieMapping getHelper(Context context) {
		if (instance == null)
			instance = new DbHelperMovieMapping(context);

		return instance;
	}

	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(INDEX_CREATE);
	}

	// Method is called during an upgrade of the database, e.g. if you increase
	// the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(DbHelperMovieMapping.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS sources");
		onCreate(database);
	}

}