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

package com.miz.functions;

import android.content.Context;
import android.text.TextUtils;

import com.miz.utils.FileUtils;

import java.io.File;

public class DbMovie {

	private final Context mContext;
	private final String mFilepath, mTmdbId, mRuntime, mYear, mGenres, mTitle;

	public DbMovie(Context context, String filepath, String tmdbId, String runtime, String year, String genres, String title) {
		mContext = context;
		mFilepath = filepath;
		mTmdbId = tmdbId;
		mRuntime = runtime;
		mYear = year;
		mGenres = genres;
		mTitle = title;
	}

	public String getFilepath() {
		if (mFilepath.contains("smb") && mFilepath.contains("@"))
			return "smb://" + mFilepath.substring(mFilepath.indexOf("@") + 1);
		return mFilepath.replace("/smb:/", "smb://");
	}

	public String getThumbnail() {
		return FileUtils.getMovieThumb(mContext, mTmdbId).getAbsolutePath();
	}

	public String getBackdrop() {
		return FileUtils.getMovieBackdrop(mContext, mTmdbId).getAbsolutePath();
	}

	public String getRuntime() {
		return mRuntime;
	}

	public String getReleaseYear() {
		return mYear;
	}

	public String getGenres() {
		return mGenres;
	}

	public String getTitle() {
		return mTitle;
	}

	public boolean isUnidentified() {
        return getRuntime().equals("0")
                && TextUtils.isEmpty(getReleaseYear())
                && TextUtils.isEmpty(getGenres())
                && TextUtils.isEmpty(getTitle());
    }

	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}

	public String getTmdbId() {
		return mTmdbId;
	}

	public boolean hasOfflineCopy() {
		return getOfflineCopyFile().exists();
	}

	public File getOfflineCopyFile() {
		return FileUtils.getOfflineFile(mContext, mFilepath);
	}
}