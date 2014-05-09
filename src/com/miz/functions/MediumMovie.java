package com.miz.functions;

import com.miz.abstractclasses.MediumBaseMovie;

import android.content.Context;

public class MediumMovie extends MediumBaseMovie {
	
	public MediumMovie(Context context, String rowId, String filepath, String title, String tmdbId, String rating, String releasedate,
			String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String date_added, String certification, String runtime, boolean ignorePrefixes, boolean ignoreNfo) {
		
		super(context, rowId, filepath, title, tmdbId, rating, releasedate,
				genres, favourite, cast, collection, collectionId, toWatch, hasWatched,
				date_added, certification, runtime, ignorePrefixes, ignoreNfo);
	}
	
}