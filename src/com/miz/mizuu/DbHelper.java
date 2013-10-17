package com.miz.mizuu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

	// Database name
	private static final String DATABASE_NAME = "mizuu_data";

	// Database version
	private static final int DATABASE_VERSION = 4;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table movie (_id INTEGER PRIMARY KEY AUTOINCREMENT, filepath TEXT, " +
			"coverpath TEXT, title TEXT, plot TEXT, tmdbid TEXT," +
			"imdbid TEXT, rating TEXT, tagline TEXT, release TEXT, certification TEXT," +
			"runtime TEXT, trailer TEXT, genres TEXT, favourite INTEGER, watched TEXT, collection TEXT," +
			"to_watch TEXT, has_watched TEXT, extra1 TEXT, extra2 TEXT, extra3 TEXT, extra4 TEXT);";

	private static final String DATABASE_CREATE_INDEX_TITLE = "create index title_index on movie(title);";
	private static final String DATABASE_CREATE_INDEX_GENRE = "create index genre_index on movie(genres);";

	private static DbHelper instance;

	private DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized DbHelper getHelper(Context context) {
		if (instance == null)
			instance = new DbHelper(context);

		return instance;
	}

	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(DATABASE_CREATE_INDEX_TITLE);
		database.execSQL(DATABASE_CREATE_INDEX_GENRE);
	}

	// Method is called during an upgrade of the database, e.g. if you increase
	// the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {

		database.execSQL("DROP TABLE IF EXISTS movie");
		onCreate(database);
	}

}