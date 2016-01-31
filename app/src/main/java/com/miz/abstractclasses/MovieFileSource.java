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

import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.DbMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import java.util.ArrayList;
import java.util.List;

public abstract class MovieFileSource<T> extends AbstractFileSource<T> {

	protected List<DbMovie> mDbMovies = new ArrayList<DbMovie>();

	public MovieFileSource(Context context, FileSource fileSource, boolean clearLibrary) {
		mContext = context;
		mFileSource = fileSource;
		mClearLibrary = clearLibrary;

		mFileSizeLimit = MizLib.getFileSizeLimit(getContext());

		setFolder(getRootFolder());
	}

	private void setupDbMovies() {
		mDbMovies.clear();
		
		// Fetch all the movies from the database
		DbAdapterMovies db = MizuuApplication.getMovieAdapter();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor tempCursor = db.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
		try {
			while (tempCursor.moveToNext()) {
				mDbMovies.add(new DbMovie(getContext(),
						MizuuApplication.getMovieMappingAdapter().getFirstFilepathForMovie(tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovieMappings.KEY_TMDB_ID))),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_TMDB_ID)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_RUNTIME)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_RELEASEDATE)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_GENRES)),
						tempCursor.getString(cache.getColumnIndex(tempCursor, DbAdapterMovies.KEY_TITLE))));
			}
		} catch (NullPointerException e) {
		} finally {
			tempCursor.close();
			cache.clear();
		}
	}
	
	public List<DbMovie> getDbMovies() {
		if (mDbMovies.size() == 0)
			setupDbMovies();
		return mDbMovies;
	}

}