package com.miz.mizuu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapterSources {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_TYPE = "type";
	public static final String KEY_IS_SMB = "is_smb";
	public static final String KEY_USER = "user";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_DOMAIN = "domain";
	
	public static final String KEY_TYPE_MOVIE = "movie";
	public static final String KEY_TYPE_SHOW = "show";
	
	private static final String DATABASE_TABLE = "sources";

	private SQLiteDatabase database;

	public DbAdapterSources(Context context) {
		database = DbHelperSources.getHelper(context).getWritableDatabase();
	}
	
	public void close() {
		database.close();
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
		return database.insert(DATABASE_TABLE, null, createContentValues(filepath, type, isSmb, user, password, domain));
	}

	/**
	 * Deletes a file source.
	 * @param rowId
	 * @return Boolean whether it was successful or not
	 */
	public boolean deleteSource(long rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Get a Cursor of all movie file sources
	 * @return
	 */
	public Cursor fetchAllMovieSources() {
		return database.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_FILEPATH, KEY_IS_SMB, KEY_USER, KEY_PASSWORD, KEY_DOMAIN, KEY_TYPE}, KEY_TYPE + "='" + KEY_TYPE_MOVIE + "'", null, null, null, null);
	}
	
	/**
	 * Get a Cursor of all show file sources
	 * @return
	 */
	public Cursor fetchAllShowSources() {
		return database.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_FILEPATH, KEY_IS_SMB, KEY_USER, KEY_PASSWORD, KEY_DOMAIN, KEY_TYPE}, KEY_TYPE + "='" + KEY_TYPE_SHOW + "'", null, null, null, null);
	}
	
	/**
	 * Get a Cursor of all file sources
	 * @return
	 */
	public Cursor fetchAllSources() {
		return database.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_FILEPATH, KEY_IS_SMB, KEY_USER, KEY_PASSWORD, KEY_DOMAIN, KEY_TYPE}, null, null, null, null, null);
	}

	private ContentValues createContentValues(String filepath, String type, int isSmb, String user, String password, String domain) {
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_TYPE, type);
		values.put(KEY_IS_SMB, isSmb);
		values.put(KEY_USER, user);
		values.put(KEY_PASSWORD, password);
		values.put(KEY_DOMAIN, domain);
		return values;
	}
}