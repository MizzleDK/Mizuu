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

package com.miz.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.miz.db.DbAdapterMovies;
import com.miz.functions.TheMovieDb;

public class TheMovieDB extends IntentService {

	public TheMovieDB() {
		super("TheMovieDB");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();

		new TheMovieDb(this, bundle.getString("filepath"), bundle.getBoolean("isFromManualIdentify", false),
				bundle.getString("language", ""), bundle.getString("oldTmdbId"), bundle.getString("tmdbId", DbAdapterMovies.UNIDENTIFIED_ID));
	}
}