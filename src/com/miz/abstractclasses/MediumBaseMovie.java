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

import java.io.File;

import android.content.Context;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;


public abstract class MediumBaseMovie extends BaseMovie {

	protected String TO_WATCH, COLLECTION, COLLECTION_ID, RATING, FAVOURITE, HAS_WATCHED, RELEASEDATE, DATE_ADDED, GENRES, CAST, CERTIFICATION, RUNTIME;
	protected String mGetReleaseYear, mGetFilePath, mWeightedCompatibility, mDateAdded, mRuntime, mReleaseDate;
	protected boolean mIsNetworkFile;
	
	public MediumBaseMovie(Context context, String rowId, String filepath, String title, String tmdbId, String rating, String releasedate,
			String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String date_added, String certification, String runtime, boolean ignorePrefixes, boolean ignoreNfo) {
		super(context, rowId, filepath, title, tmdbId, ignorePrefixes, ignoreNfo);
		
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
		if (!MizLib.isEmpty(RELEASEDATE)) {
			String YEAR = RELEASEDATE.trim();
			try {
				if (YEAR.substring(4,5).equals("-") && YEAR.substring(7,8).equals("-")) {
					mGetReleaseYear = YEAR.substring(0,4);
				} else {
					mGetReleaseYear = CONTEXT.getString(R.string.unknownYear);
				}
			} catch (Exception e) {
				if (YEAR.length() == 4)
					mGetReleaseYear = YEAR;
				else
					mGetReleaseYear = CONTEXT.getString(R.string.unknownYear);
			}
		} else {
			mGetReleaseYear = CONTEXT.getString(R.string.unknownYear);
		}
		
		// getFilePath()		
		String temp = FILEPATH.contains("<MiZ>") ? FILEPATH.split("<MiZ>")[1] : FILEPATH;
		mGetFilePath = MizLib.transformSmbPath(temp);
		
		// isNetworkFile()
		mIsNetworkFile = getFilepath().contains("smb:/");
		
		// Weighted compatibility
		mWeightedCompatibility = (int) (getWeightedRating() * 10) + "% " + CONTEXT.getString(R.string.compatibility);
		
		// Date added
		mDateAdded = MizLib.getPrettyDate(CONTEXT, Long.valueOf(getDateAdded()));
		
		// Runtime
		mRuntime = MizLib.getPrettyTime(CONTEXT, Integer.parseInt(getRuntime()));
		
		// Release date
		mReleaseDate = MizLib.getPrettyDate(CONTEXT, getReleasedate());
	}
	
	public boolean toWatch() {
		return (TO_WATCH.equals("0")) ? false : true;
	}
	
	public String getCollection() {
		return COLLECTION;
	}

	public String getCollectionId() {
		return COLLECTION_ID;
	}
	
	public String getCollectionPoster() {
		File collectionImage = new File(MizLib.getMovieThumbFolder(CONTEXT), COLLECTION_ID + ".jpg");
		if (collectionImage.exists() && collectionImage.length() > 0)
			return collectionImage.getAbsolutePath();
		return getThumbnail();
	}
	
	public String getFullFilepath() {
		return FILEPATH;
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
		if (FAVOURITE.equals("1"))
			return true;
		return false;
	}
	
	public String getRuntime() {
		return RUNTIME;
	}
	
	public String getPrettyRuntime() {
		return mRuntime;
	}

	public boolean isNetworkFile() {
		return mIsNetworkFile;
	}
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}
	
	public String getFilepath() {
		return mGetFilePath;
	}
	
	public String getManualIdentificationQuery() {
		String temp = FILEPATH.contains("<MiZ>") ? FILEPATH.split("<MiZ>")[0] : FILEPATH;
		return temp;
	}
	
	public boolean hasWatched() {
		return (HAS_WATCHED.equals("0")) ? false : true;
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
		if (!MizLib.isNumber(DATE_ADDED))
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
	
	public String getTmdbId() {
		return TMDB_ID;
	}
	
	public String getCertification() {
		return CERTIFICATION;
	}
	
	public boolean isUnidentified() {
		if (getRuntime().equals("0")
				&& getReleaseYear().equals(CONTEXT.getString(R.string.unknownYear))
				&& MizLib.isEmpty(getGenres())
				&& MizLib.isEmpty(TITLE))
			return true;
		
		return false;
	}
	
	public boolean hasOfflineCopy() {
		return getOfflineCopyFile().exists();
	}
	
	public String getOfflineCopyUri() {
		return getOfflineCopyFile().getAbsolutePath();
	}
	
	public File getOfflineCopyFile() {
		if (FILEPATH.contains("<MiZ>"))
			return new File(MizLib.getAvailableOfflineFolder(CONTEXT), MizLib.md5(getFilepath()) + "." + MizLib.getFileExtension(getFilepath()));
		return new File(MizLib.getAvailableOfflineFolder(CONTEXT), MizLib.md5(getFullFilepath()) + "." + MizLib.getFileExtension(getFullFilepath()));
	}
}