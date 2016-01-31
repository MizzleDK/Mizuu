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

import android.content.Context;
import android.database.Cursor;

import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.DbEpisode;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import java.util.ArrayList;
import java.util.List;

public abstract class TvShowFileSource<T> extends AbstractFileSource<T> {

	protected List<DbEpisode> mDbEpisode = new ArrayList<DbEpisode>();

	public TvShowFileSource(Context context, FileSource fileSource, boolean clearLibrary) {
		mContext = context;
		mFileSource = fileSource;
		mClearLibrary = clearLibrary;

		mFileSizeLimit = MizLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbEpisodes() {
		mDbEpisode.clear();

		DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor tempCursor = db.getAllEpisodes();
		try {
			while (tempCursor.moveToNext()) {
				mDbEpisode.add(new DbEpisode(getContext(),
						MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFirstFilepath(tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SHOW_ID)), tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SEASON)), tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_EPISODE))),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_SEASON)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterTvShowEpisodes.KEY_EPISODE))
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