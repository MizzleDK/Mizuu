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
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;

import java.io.File;


public abstract class MediumBaseMovie extends BaseMovie {

	protected String TO_WATCH, COLLECTION, COLLECTION_ID, RATING, FAVOURITE, HAS_WATCHED, RELEASEDATE, DATE_ADDED, GENRES, CAST, CERTIFICATION, RUNTIME;
	protected String mGetReleaseYear, mWeightedCompatibility, mDateAdded, mRuntime, mReleaseDate;
	
	public MediumBaseMovie(Context context, String title, String tmdbId, String rating, String releasedate,
			String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String date_added, String certification, String runtime, boolean ignorePrefixes) {
		super(context, title, tmdbId, ignorePrefixes);
		
		RATING = rating;
		RELEASEDATE = releasedate;
		GENRES = genres;
		FAVOURITE = favourite;
		CAST = cast;
		COLLECTION = collection;
		COLLECTION_ID = collectionId;
		TO_WATCH = toWatch;
		HAS_WATCHED = hasWatched;
		DATE_ADDED = date_added;
		CERTIFICATION = certification;
		RUNTIME = runtime.replace("min", "").trim();
		
		// getReleaseYear()
		if (!TextUtils.isEmpty(RELEASEDATE)) {
			String YEAR = RELEASEDATE.trim();
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
		
		// Date added
		mDateAdded = MizLib.getPrettyDate(mContext, Long.valueOf(getDateAdded()));
		
		// Runtime
		mRuntime = MizLib.getPrettyTime(mContext, Integer.parseInt(getRuntime()));
		
		// Release date
		mReleaseDate = MizLib.getPrettyDate(mContext, getReleasedate());
	}
	
	public boolean toWatch() {
		return (!TO_WATCH.equals("0"));
	}
	
	public String getCollection() {
		return COLLECTION;
	}

	public String getCollectionId() {
		return COLLECTION_ID;
	}
	
	public File getCollectionPoster() {
		File collectionImage = FileUtils.getMovieThumb(mContext, COLLECTION_ID);
		if (collectionImage.exists() && collectionImage.length() > 0)
			return collectionImage;
		return getThumbnail();
	}
	
	public void setRating(int rating) {
		RATING = String.valueOf(Double.valueOf((double) rating / 10));
	}
	
	public double getRawRating() {
		try {
			return Double.valueOf(RATING);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	public double getWeightedRating() {
		if (isFavourite())
			return (10 + getRawRating()) / 2;
		if (hasWatched())
			return (6 + getRawRating()) / 2;
		return (getRawRating() + 5) / 2;
	}
	
	public String getWeightedCompatibility() {
		return mWeightedCompatibility;
	}
	
	public boolean isFavourite() {
        return FAVOURITE.equals("1");
    }
	
	public void setRuntime(int runtime) {
		RUNTIME = String.valueOf(runtime);
	}
	
	public String getRuntime() {
        if (TextUtils.isEmpty(RUNTIME))
            return "0";
		return RUNTIME;
	}
	
	public String getPrettyRuntime() {
		return mRuntime;
	}
	
	public boolean hasWatched() {
		return (HAS_WATCHED.equals("0")) ? false : true;
	}
	
	public void setReleaseDate(int year, int month, int day) {
		RELEASEDATE = year + "-" + MizLib.addIndexZero(month) + "-" + MizLib.addIndexZero(day);
	}
	
	public String getReleasedate() {
		return RELEASEDATE;
	}
	
	public String getPrettyReleaseDate() {
		return mReleaseDate;
	}
	
	public String getReleaseYear() {
		return mGetReleaseYear;
	}
	
	public String getDateAdded() {
		if (TextUtils.isEmpty(DATE_ADDED) || !MizLib.isNumber(DATE_ADDED))
			return "0";
		return DATE_ADDED;
	}
	
	public String getPrettyDateAdded() {
		return mDateAdded;
	}
	
	public String getGenres() {
		return GENRES;
	}
	
	public String getCast() {
		return CAST;
	}
	
	public void setCertification(String certification) {
		CERTIFICATION = certification;
	}
	
	public String getCertification() {
		return CERTIFICATION;
	}
	
	public boolean isUnidentified() {
		return 	getRuntime().equals("0") &&
				getReleaseYear().equals(mContext.getString(R.string.unknownYear)) &&
				TextUtils.isEmpty(getGenres());
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
	
	public String getSubText(int sort) {
        return getReleaseYear();
	}
}