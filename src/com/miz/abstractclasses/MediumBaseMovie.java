package com.miz.abstractclasses;

import java.io.File;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;


public abstract class MediumBaseMovie extends BaseMovie {

	protected String TO_WATCH, COLLECTION, COLLECTION_ID, RATING, FAVOURITE, HAS_WATCHED, RELEASEDATE, DATE_ADDED, GENRES, CAST, CERTIFICATION, RUNTIME;

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
	
	public boolean isFavourite() {
		if (FAVOURITE.equals("1"))
			return true;
		return false;
	}
	
	public String getRuntime() {
		return RUNTIME.replace("min", "").trim();
	}

	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public String getFilepath() {
		if (FILEPATH.contains("smb") && FILEPATH.contains("@"))
			return "smb://" + FILEPATH.substring(FILEPATH.indexOf("@") + 1);
		return FILEPATH.replace("/smb:/", "smb://");
	}
	
	public boolean hasWatched() {
		return (HAS_WATCHED.equals("0")) ? false : true;
	}
	
	public String getReleasedate() {
		return RELEASEDATE;
	}
	
	public String getReleaseYear() {
		if (RELEASEDATE != null) {
			String YEAR = RELEASEDATE.trim();
			try {
				if (YEAR.substring(4,5).equals("-") && YEAR.substring(7,8).equals("-")) {
					return "(" + YEAR.substring(0,4) + ")";
				} else {
					return CONTEXT.getString(R.string.unknownYear);
				}
			} catch (Exception e) {
				if (YEAR.length() == 4)
					return YEAR;
				return CONTEXT.getString(R.string.unknownYear);
			}
		} else {
			return CONTEXT.getString(R.string.unknownYear);
		}
	}
	
	public String getDateAdded() {
		return DATE_ADDED;
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
}