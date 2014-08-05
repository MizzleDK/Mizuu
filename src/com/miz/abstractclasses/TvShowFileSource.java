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

package com.miz.abstractclasses;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.ColumnIndexCache;
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

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor tempCursor = db.getAllEpisodesInDatabase(ignoreRemovedFiles());
		try {
			while (tempCursor.moveToNext()) {
				mDbEpisode.add(new DbEpisode(getContext(),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisode.KEY_FILEPATH)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisode.KEY_ROWID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisode.KEY_SEASON)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisode.KEY_EPISODE))
						)
				);

			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
			cache.clear();
		}
	}

	public List<DbEpisode> getDbEpisodes() {
		if (mDbEpisode.size() == 0)
			setupDbEpisodes();
		return mDbEpisode;
	}
}