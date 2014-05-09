package com.miz.functions;

import com.miz.abstractclasses.BaseMovie;

import android.content.Context;

public class SmallMovie extends BaseMovie {

	public SmallMovie(Context context, String rowId, String filepath, String title, String tmdbId, boolean ignorePrefixes, boolean ignoreNfo) {
		super(context, tmdbId, tmdbId, tmdbId, tmdbId, ignoreNfo, ignoreNfo);
		// Set up movie fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		TITLE = title;
		TMDB_ID = tmdbId;
		this.ignorePrefixes = ignorePrefixes;
		this.ignoreNfo = ignoreNfo;
	}
}