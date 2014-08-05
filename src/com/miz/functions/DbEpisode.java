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

public class DbEpisode {

	private Context mContext;
	private String mFilepath, mRowId, mShowId, mSeason, mEpisode;

	public DbEpisode(Context context, String filepath, String rowId, String showId, String season, String episode) {
		mContext = context;
		mFilepath = filepath;
		mRowId = rowId;
		mShowId = showId;
		mSeason = season;
		mEpisode = episode;
	}

	public String getFilepath() {
		if (mFilepath.contains("smb") && mFilepath.contains("@"))
			return "smb://" + mFilepath.substring(mFilepath.indexOf("@") + 1);
		return mFilepath.replace("/smb:/", "smb://");
	}

	public String getRowId() {
		return mRowId;
	}

	public String getShowId() {
		return mShowId;
	}

	public String getEpisodeCoverPath() {
		return MizLib.getTvShowEpisode(mContext, mShowId, mSeason, mEpisode).getAbsolutePath();
	}

	public String getThumbnail() {
		return MizLib.getTvShowThumb(mContext, getShowId()).getAbsolutePath();
	}

	public String getBackdrop() {
		return MizLib.getTvShowBackdrop(mContext, getShowId()).getAbsolutePath();
	}

	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}
	
	public boolean isUnidentified() {
		return getShowId().equals("invalid");
	}
}