package com.miz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelperSources extends SQLiteOpenHelper {

	// Database name
	private static final String DATABASE_NAME = "mizuu_sources";

	// Database version
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table sources (_id INTEGER PRIMARY KEY AUTOINCREMENT, filepath TEXT," +
			"type TEXT, is_smb INTEGER, user TEXT, password TEXT, domain TEXT);";
	
	private static DbHelperSources instance;
	
	private DbHelperSources(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public static synchronized DbHelperSources getHelper(Context context) {
		if (instance == null)
            instance = new DbHelperSources(context);

        return instance;
	}

	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	// Method is called during an upgrade of the database, e.g. if you increase
	// the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(DbHelperSources.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS sources");
		onCreate(database);
	}

}