package com.miz.abstractclasses;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.DbEpisode;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public abstract class TvShowFileSource<T> extends AbstractFileSource<T> {

	protected List<DbEpisode> mDbEpisode = new ArrayList<DbEpisode>();

	public TvShowFileSource(Context context, FileSource fileSource, boolean ignoreRemovedFiles, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		mContext = context;
		mFileSource = fileSource;
		mIgnoreRemovedFiles = ignoreRemovedFiles;
		mSubFolderSearch = subFolderSearch;
		mClearLibrary = clearLibrary;
		mDisableEthernetWiFiCheck = disableEthernetWiFiCheck;

		mFileSizeLimit = MizLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbEpisodes() {
		mDbEpisode.clear();

		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor tempCursor = db.getAllEpisodesInDatabase(ignoreRemovedFiles());
		try {
			while (tempCursor.moveToNext()) {
				mDbEpisode.add(new DbEpisode(getContext(),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)) + "_S" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)) + "E" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)) + ".jpg"));

			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
		}
	}

	public List<DbEpisode> getDbEpisodes() {
		if (mDbEpisode.size() == 0)
			setupDbEpisodes();
		return mDbEpisode;
	}
}