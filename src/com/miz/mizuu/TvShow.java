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

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;

import com.miz.functions.MizLib;
import com.miz.functions.SortingKeys;
import com.miz.utils.StringUtils;

public class TvShow implements Comparable<TvShow> {

	private Context mContext;
	private String TITLE, DESCRIPTION, RATING, GENRES, ACTORS, CERTIFICATION, FIRST_AIR_DATE, RUNTIME, LATEST_EPISODE_AIR_DATE;
	private String mId, mRuntime, mWeightedCompatibility, mReleaseDate, mGetReleaseYear, mTitle, mLatestEpisodeAirDate;
	private boolean mFavorite;
	private File mThumbnail;

	public TvShow(Context context, String id, String title, String description, String rating, String genres, String actors, String certification, String firstAirdate, String runtime, boolean ignorePrefixes, String isFavorite, String latestEpisodeAirDate) {

		// Set up episode fields based on constructor
		mContext = context;
		mId = id;
		TITLE = title;
		DESCRIPTION = description;
		RATING = rating;
		GENRES = genres;
		ACTORS = actors;
		CERTIFICATION = certification;
		FIRST_AIR_DATE = firstAirdate;
		RUNTIME = runtime;
		LATEST_EPISODE_AIR_DATE = latestEpisodeAirDate;
		mFavorite = !(isFavorite.equals("0") || isFavorite.isEmpty());

		// Thumbnail
		mThumbnail = MizLib.getTvShowThumb(mContext, mId);

		// Title		
		if (TextUtils.isEmpty(TITLE)) {
			mTitle = "";
		} else {
			mTitle = TITLE;
			if (ignorePrefixes) {
				String temp = TITLE.toLowerCase(Locale.ENGLISH);
				String[] prefixes = MizLib.getPrefixes(mContext);
				int count = prefixes.length;
				for (int i = 0; i < count; i++) {
					if (temp.startsWith(prefixes[i])) {
						mTitle = TITLE.substring(prefixes[i].length());
						break;
					}
				}
			}
		}

		// getReleaseYear()
		if (!TextUtils.isEmpty(FIRST_AIR_DATE)) {
			String YEAR = FIRST_AIR_DATE.trim();
			try {
				if (YEAR.substring(4,5).equals("-") && YEAR.substring(7,8).equals("-")) {
					mGetReleaseYear = YEAR.substring(0,4);
				} else {
					mGetReleaseYear = mContext.getString(R.string.unknownYear);
				}
			} catch (Exception e) {
				if (YEAR.length() == 4)
					mGetReleaseYear = YEAR;
				else
					mGetReleaseYear = mContext.getString(R.string.unknownYear);
			}
		} else {
			mGetReleaseYear = mContext.getString(R.string.unknownYear);
		}

		// Weighted compatibility
		mWeightedCompatibility = (int) (getWeightedRating() * 10) + "% " + mContext.getString(R.string.compatibility);

		// Runtime
		mRuntime = MizLib.getPrettyTime(mContext, Integer.parseInt(getRuntime()));

		// Release date
		mReleaseDate = MizLib.getPrettyDate(mContext, getFirstAirdate());
		
		// Latest episode airdate
		mLatestEpisodeAirDate = MizLib.getPrettyDate(mContext, getLatestEpisodeAirdate());;
	}

	public String getTitle() {
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

	/**
	 * Get the TV show ID
	 * @param includeHacks Include information about the service provider in the ID, i.e. "tmdb_" at the start of the ID.
	 * @return
	 */
	public String getId() {
		if (mId == null || mId.isEmpty()) {
			return "NOSHOW";
		} else {
			return mId;
		}
	}
	
	public String getIdWithoutHack() {
		return mId.replace("tmdb_", "");
	}
	
	public static final int TMDB = 1, THETVDB = 2;
	
	/**
	 * Get the service provider for the ID.
	 * @return
	 */
	public int getIdType() {
		if (mId.startsWith("tmdb_"))
			return TMDB;
		return THETVDB;
	}

	public String getDescription() {
		if (DESCRIPTION == null || DESCRIPTION.isEmpty()) {
			return mContext.getString(R.string.stringNoPlot);
		} else {
			return DESCRIPTION;
		}
	}

	public File getCoverPhoto() {
		return getThumbnail();
	}

	public File getThumbnail() {
		return mThumbnail;
	}

	public String getBackdrop() {
		return MizLib.getTvShowBackdrop(mContext, mId).getAbsolutePath();
	}

	public String getRating() {
		return RATING + "/10";
	}

	public double getRawRating() {
		try {
			return Double.valueOf(RATING);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	public double getWeightedRating() {
		if (isFavorite())
			return (10 + getRawRating()) / 2;
		return (getRawRating() + 5) / 2;
	}

	public String getGenres() {
		return StringUtils.replacePipesWithCommas(GENRES);
	}

	public String getActors() {
		return StringUtils.replacePipesWithCommas(ACTORS);
	}

	public String getCertification() {
		return CERTIFICATION;
	}

	public String getFirstAirdate() {
		return FIRST_AIR_DATE;
	}
	
	public String getLatestEpisodeAirdate() {
		return LATEST_EPISODE_AIR_DATE;
	}

	public String getFirstAirdateYear() {
		try {
			return FIRST_AIR_DATE.substring(0, 4);
		} catch (Exception e) {
			return "N/A";
		}
	}

	public String getRuntime() {
		if (!MizLib.isNumber(RUNTIME) || TextUtils.isEmpty(RUNTIME))
			return "0";
		return RUNTIME;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	public void setFavorite(boolean fav) {
		mFavorite = fav;
	}

	public String getFavorite() {
		return mFavorite ? "1" : "0";
	}

	@Override
	public int compareTo(TvShow another) {
		return getTitle().compareToIgnoreCase(another.getTitle());
	}

	public String getPrettyRuntime() {
		return mRuntime;
	}

	public String getWeightedCompatibility() {
		return mWeightedCompatibility;
	}

	public String getPrettyReleaseDate() {
		return mReleaseDate;
	}
	
	public String getPrettyLatestEpisodeAirDate() {
		return mLatestEpisodeAirDate;
	}

	public String getReleaseYear() {
		return mGetReleaseYear;
	}

	public String getSubText(int sort) {
		switch (sort) {
		case SortingKeys.DURATION:
			return getPrettyRuntime();
		case SortingKeys.RATING:
			return (int) (getRawRating() * 10) + "%";
		case SortingKeys.WEIGHTED_RATING:
			return getWeightedCompatibility();
		case SortingKeys.NEWEST_EPISODE:
			return getPrettyLatestEpisodeAirDate();
		case SortingKeys.RELEASE:
			return getPrettyReleaseDate();
		default:
			return getReleaseYear();
		}
	}
}