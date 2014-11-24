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
import android.text.TextUtils;

import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.FileUtils;
import com.miz.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class BaseMovie implements Comparable<BaseMovie> {

	protected ArrayList<Filepath> mFilepaths = new ArrayList<Filepath>();
	protected Context mContext;
	protected String mTitle, mTmdbId;
	protected boolean mIgnorePrefixes;

	public BaseMovie(Context context, String title, String tmdbId, boolean ignorePrefixes) {
		// Set up movie fields based on constructor
		mContext = context;
		mTitle = title;
		mTmdbId = tmdbId;
		mIgnorePrefixes = ignorePrefixes;

		// getTitle()
		if (!TextUtils.isEmpty(mTitle) && ignorePrefixes) {
			String temp = mTitle.toLowerCase(Locale.ENGLISH);
			String[] prefixes = MizLib.getPrefixes(mContext);
			int count = prefixes.length;
			for (int i = 0; i < count; i++) {
				if (temp.startsWith(prefixes[i])) {
					mTitle = mTitle.substring(prefixes[i].length());
					break;
				}
			}
		}

        List<String> paths = MizuuApplication.getMovieFilepaths(mTmdbId);
        if (paths != null)
            setFilepaths(paths);
        else
            setFilepaths(MizuuApplication.getMovieMappingAdapter().getMovieFilepaths(getTmdbId()));
	}

	public String getTitle() {
		if (TextUtils.isEmpty(mTitle))
			// If the title is empty, we'll have to use whatever filepath is available
			return StringUtils.getFilenameWithoutExtension(getFilepaths().get(0).getFilepath());

		return mTitle;
	}

	/**
	 * This is only used for the SectionIndexer in the overview
	 */
	@Override
	public String toString() {
		try {
			return getTitle().substring(0, 1);
		} catch (Exception e) {
			return "";
		}
	}

	public File getThumbnail() {
		return FileUtils.getMovieThumb(mContext, mTmdbId);
	}

	public String getTmdbId() {
		return mTmdbId;
	}

	public File getBackdrop() {
		return FileUtils.getMovieBackdrop(mContext, mTmdbId);
	}

	@Override
	public int compareTo(BaseMovie another) {
		return getTitle().compareToIgnoreCase(another.getTitle());
	}

	public void setFilepaths(List<String> paths) {
		for (String path : paths)
			mFilepaths.add(new Filepath(path));
	}

	public ArrayList<Filepath> getFilepaths() {
		return mFilepaths;
	}

	public String getAllFilepaths() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < getFilepaths().size(); i++)
			sb.append(getFilepaths().get(i).getFilepath()).append("\n");
		return sb.toString().trim();
	}
}