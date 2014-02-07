package com.miz.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.miz.functions.TheMovieDb;

public class TheMovieDB extends IntentService {

	public TheMovieDB() {
		super("TheMovieDB");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();

		new TheMovieDb(this, bundle.getString("filepath"), bundle.getBoolean("isFromManualIdentify", false),
				bundle.getLong("rowId", 0), bundle.getString("language", ""), bundle.getString("tmdbId", "invalid"));
	}
}