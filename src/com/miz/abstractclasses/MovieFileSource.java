package com.miz.abstractclasses;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.miz.db.DbAdapter;
import com.miz.functions.DbMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public abstract class MovieFileSource<T> extends AbstractFileSource<T> {

	protected HashMap<String, InputStream> mNfoFiles = new HashMap<String, InputStream>();
	protected List<DbMovie> mDbMovies = new ArrayList<DbMovie>();

	public MovieFileSource(Context context, FileSource fileSource, boolean ignoreRemovedFiles, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		mContext = context;
		mFileSource = fileSource;
		mIgnoreRemovedFiles = ignoreRemovedFiles;
		mSubFolderSearch = subFolderSearch;
		mClearLibrary = clearLibrary;
		mDisableEthernetWiFiCheck = disableEthernetWiFiCheck;

		mFileSizeLimit = MizLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbMovies() {
		mDbMovies.clear();
		
		// Fetch all the movies from the database
		DbAdapter db = MizuuApplication.getMovieAdapter();

		Cursor tempCursor = db.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", ignoreRemovedFiles(), true);
		try {
			while (tempCursor.moveToNext()) {
				mDbMovies.add(new DbMovie(getContext(),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
						tempCursor.getLong(tempCursor.getColumnIndex(DbAdapter.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_GENRES)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_TITLE))));
			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
		}
	}
	
	public List<DbMovie> getDbMovies() {
		if (mDbMovies.size() == 0)
			setupDbMovies();
		return mDbMovies;
	}

	public void addNfoFile(String filepath, InputStream is) {
		mNfoFiles.put(filepath, is);
	}

	public HashMap<String, InputStream> getNfoFiles() {
		return mNfoFiles;
	}

	/**
	 * Determine if this file source supports loading of .NFO files. Should be overridden if the file source doesn't support .NFO files.
	 * @return The value indicates if the file source supports loading of .NFO files or not.
	 */
	public boolean supportsNfo() {
		return true;
	}
}