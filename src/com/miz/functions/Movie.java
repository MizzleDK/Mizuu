package com.miz.functions;

import java.io.File;
import java.util.Locale;

import android.content.Context;

import com.miz.abstractclasses.MediumBaseMovie;
import com.miz.mizuu.R;

public class Movie extends MediumBaseMovie {

	private MovieVersion[] mVersions = new MovieVersion[0];
	private String PLOT, TAGLINE, IMDB_ID, TRAILER, COVER;

	public Movie(Context context, String rowId, String filepath, String title, String plot, String tagline, String tmdbId, String imdbId, String rating, String releasedate,
			String certification, String runtime, String trailer, String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String cover, String date_added, boolean ignorePrefixes, boolean ignoreNfo) {

		// Set up movie fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		TITLE = title;
		PLOT = plot;
		TAGLINE = tagline;
		TMDB_ID = tmdbId;
		IMDB_ID = imdbId;
		RATING = rating;
		RELEASEDATE = releasedate;
		CERTIFICATION = certification;
		RUNTIME = runtime;
		TRAILER = trailer;
		GENRES = genres;
		FAVOURITE = favourite;
		CAST = cast;
		COLLECTION = collection;
		COLLECTION_ID = collectionId;
		TO_WATCH = toWatch;
		HAS_WATCHED = hasWatched;
		COVER = cover;
		DATE_ADDED = date_added;
		this.ignorePrefixes = ignorePrefixes;
		this.ignoreNfo = ignoreNfo;
	}

	public String getFullFilepath() {
		return FILEPATH;
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

	public String getPlot() {
		if (PLOT == null || PLOT.isEmpty()) {
			return CONTEXT.getString(R.string.stringNoPlot);
		} else {
			return PLOT;
		}
	}

	public String getTagline() {
		if (TAGLINE == null) {
			return "";
		} else {
			return TAGLINE;
		}
	}

	public String getPoster() {
		return getThumbnail();
	}

	public String getCoverUrl() {
		return COVER;
	}

	public String getImdbId() {
		return IMDB_ID;
	}

	public String getRating() {
		if (!MizLib.isEmpty(RATING))
			return RATING + "/10";
		return "0.0/10";
	}

	public String getTrailer() {
		return TRAILER;
	}

	public String getFavourite() {
		return FAVOURITE;
	}

	public void setFavourite(String fav) {
		FAVOURITE = fav;
	}

	public void setFavourite(boolean isFavourite) {
		if (isFavourite)
			FAVOURITE = "1";
		else
			FAVOURITE = "0";
	}

	public String getHasWatched() {
		return HAS_WATCHED;
	}

	public void setHasWatched(boolean hasWatched) {
		if (hasWatched)
			HAS_WATCHED = "1";
		else
			HAS_WATCHED = "0";
	}

	public String getToWatch() {
		return TO_WATCH;
	}

	public void setToWatch(boolean toWatch) {
		if (toWatch)
			TO_WATCH = "1";
		else
			TO_WATCH = "0";
	}

	public boolean isSplitFile() {
		return getFilepath().matches(".*(cd1|part1).*");
	}

	public boolean isPartOfCollection() {
		return !COLLECTION_ID.isEmpty() || COLLECTION_ID == null;
	}
	
	public String getLocalTrailer() {
		try {
			// Check if there's a custom cover art image
			String filename = FILEPATH.substring(0, FILEPATH.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
			File parentFolder = new File(FILEPATH).getParentFile();

			if (parentFolder != null) {
				File[] list = parentFolder.listFiles();

				if (list != null) {
					String name, absolutePath;
					int count = list.length;
					for (int i = 0; i < count; i++) {
						name = list[i].getName();
						absolutePath = list[i].getAbsolutePath();
						if (absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + "-trailer.") ||
								absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + "_trailer.") ||
								absolutePath.toLowerCase(Locale.ENGLISH).startsWith(filename.toLowerCase(Locale.ENGLISH) + " trailer.") ||
								name.toLowerCase(Locale.ENGLISH).startsWith("trailer.")) {
							return absolutePath;
						}
					}
				}
			}
		} catch (Exception e) {}
		return "";
	}
	
	public void setMultipleVersions(MovieVersion[] rowIds) {
		mVersions = rowIds;
	}
	
	public MovieVersion[] getMultipleVersions() {
		return mVersions;
	}
	
	public boolean hasMultipleVersions() {
		return mVersions.length > 1;
	}
}