package com.miz.functions;

import com.miz.abstractclasses.MediumBaseMovie;

import android.content.Context;

public class MediumMovie extends MediumBaseMovie {
	
	public MediumMovie(Context context, String rowId, String filepath, String title, String tmdbId, String rating, String releasedate,
			String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String date_added, String certification, boolean ignorePrefixes, boolean ignoreNfo) {

		// Set up movie fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		TITLE = title;
		TMDB_ID = tmdbId;
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
		this.ignorePrefixes = ignorePrefixes;
		this.ignoreNfo = ignoreNfo;
	}
	
}