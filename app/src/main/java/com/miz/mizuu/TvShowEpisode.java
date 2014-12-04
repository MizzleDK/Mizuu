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

package com.miz.mizuu;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.utils.FileUtils;
import com.miz.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;

import static com.miz.functions.PreferenceKeys.TVSHOWS_EPISODE_ORDER;

public class TvShowEpisode implements Comparable<TvShowEpisode> {

	private Context mContext;
	private String mShowId, mTitle, mDescription, mSeason, mEpisode, mReleaseDate, mDirector, mWriter, mGuestStars, mRating, mHasWatched, mFavorite, mSubtitleText;
	private ArrayList<Filepath> mFilepaths = new ArrayList<Filepath>();

	public TvShowEpisode() {}
	
	public TvShowEpisode(Context context, String showId, String title, String description,
			String season, String episode, String releasedate, String director, String writer, String gueststars,
			String rating, String hasWatched, String favorite) {

		// Set up episode fields based on constructor
		mContext = context;
		mShowId = showId;
		mTitle = title;
		mDescription = description;
		mSeason = season;
		mEpisode = episode;
		mReleaseDate = releasedate;
		mDirector = director;
		mWriter = writer;
		mGuestStars = gueststars;
		mRating = rating;
		mHasWatched = hasWatched;
		mFavorite = favorite;

		// Subtitle text
		StringBuilder sb = new StringBuilder();
		sb.append(mContext.getString(R.string.showEpisode));
		sb.append(" ");
		sb.append(getEpisode());
		if (!hasWatched()) {
            sb.append(" ");
            sb.append(mContext.getString(R.string.unwatched));
        }

		mSubtitleText = sb.toString();
	}

	public String getShowId() {
		return mShowId;
	}

	public String getTitle() {
		if (TextUtils.isEmpty(mTitle)) {
			String path = getFilepaths().get(0).getFullFilepath();
			String temp = path.contains("<MiZ>") ? path.split("<MiZ>")[0] : path;
			File fileName = new File(temp);
			int pointPosition=fileName.getName().lastIndexOf(".");
			return pointPosition == -1 ? fileName.getName() : fileName.getName().substring(0, pointPosition);
		} else {
			return mTitle;
		}
	}

	public String getDescription() {
		if (TextUtils.isEmpty(mDescription)) {
			return mContext.getString(R.string.stringNoPlot);
		} else {
			return mDescription.trim();
		}
	}

	public String getSeason() {
		return mSeason;
	}

	public String getEpisode() {
		return mEpisode;
	}

	public File getEpisodePhoto() {
		return FileUtils.getTvShowEpisode(mContext, mShowId, mSeason, mEpisode);
	}

	public void setReleaseDate(int year, int month, int day) {
		mReleaseDate = year + "-" + MizLib.addIndexZero(month) + "-" + MizLib.addIndexZero(day);
	}
	
	public String getReleasedate() {
		return mReleaseDate;
	}

	public String getDirector() {
		return StringUtils.replacePipesWithCommas(mDirector);
	}

	public String getWriter() {
		return StringUtils.replacePipesWithCommas(mWriter);
	}

	public String getGuestStars() {
		return StringUtils.replacePipesWithCommas(mGuestStars);
	}

	public void setRating(int rating) {
		mRating = String.valueOf(Double.valueOf((double) rating / 10));
	}
	
	public String getRating() {
		if (TextUtils.isEmpty(mRating))
			return "0.0";
		return mRating;
	}

	public double getRawRating() {
		try {
			return Double.valueOf(mRating);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	public boolean hasWatched() {
		return (!(mHasWatched.isEmpty() || mHasWatched.equals("0")));
	}

	public String getHasWatched() {
		return mHasWatched;
	}

	public void setHasWatched(boolean hasWatched) {
		if (hasWatched)
			mHasWatched = "1";
		else
			mHasWatched = "0";
	}

	public boolean isFavorite() {
		return (!(mFavorite.isEmpty() || mFavorite.equals("0")));
	}

	public String getFavorite() {
		return mFavorite;
	}

	public boolean hasOfflineCopy(Filepath path) {
		return getOfflineCopyFile(path).exists();
	}

	public String getOfflineCopyUri(Filepath path) {
		return getOfflineCopyFile(path).getAbsolutePath();
	}

	public File getOfflineCopyFile(Filepath path) {
		return FileUtils.getOfflineFile(mContext, path.getFilepath());
	}

	/**
	 * Returns the path for the TV show thumbnail
	 * @return
	 */
	public File getThumbnail() {
		return FileUtils.getTvShowThumb(mContext, mShowId);
	}

	/**
	 * Returns the path for the TV show thumbnail
	 * @return
	 */
	public File getTvShowBackdrop() {
		return FileUtils.getTvShowBackdrop(mContext, mShowId);
	}

	public String getSubtitleText() {
		return mSubtitleText;
	}

	@Override
	public int compareTo(TvShowEpisode another) {
		String defaultOrder = mContext.getString(R.string.oldestFirst);
		boolean oldestFirst = PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_EPISODE_ORDER, defaultOrder).equals(defaultOrder);
		int multiplier = oldestFirst ? 1 : -1;

		return getEpisode().compareToIgnoreCase(another.getEpisode()) * multiplier;
	}

	public void setFilepaths(ArrayList<String> paths) {
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