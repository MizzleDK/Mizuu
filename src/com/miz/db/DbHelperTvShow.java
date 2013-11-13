package com.miz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelperTvShow extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "mizuu_tv_show_data";
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_CREATE = "create table tvshows(_id INTEGER PRIMARY KEY AUTOINCREMENT, show_id TEXT, show_title TEXT, show_description TEXT," +
			"show_actors TEXT, show_genres TEXT, show_rating TEXT, show_certification TEXT, show_runtime TEXT, show_first_airdate TEXT, extra1 TEXT, extra2 TEXT, extra3 TEXT, extra4 TEXT);";
	
	private static final String DATABASE_CREATE_SHOW_ID_INDEX = "create index show_id_index on tvshows(show_id);";

	private static DbHelperTvShow instance;
	
	private DbHelperTvShow(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public static synchronized DbHelperTvShow getHelper(Context context) {
		if (instance == null)
            instance = new DbHelperTvShow(context);

        return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(DATABASE_CREATE_SHOW_ID_INDEX);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS tvshows");
		onCreate(db);
	}

}
