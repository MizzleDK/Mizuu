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

package com.miz.functions;

import android.content.Context;

import com.miz.abstractclasses.MediumBaseMovie;

public class MediumMovie extends MediumBaseMovie {
	
	public MediumMovie(Context context, String title, String tmdbId, String rating, String releasedate,
			String genres, String favourite, String cast, String collection, String collectionId, String toWatch, String hasWatched,
			String date_added, String certification, String runtime, boolean ignorePrefixes) {
		
		super(context, title, tmdbId, rating, releasedate, genres, favourite, cast, collection, collectionId,
				toWatch, hasWatched, date_added, certification, runtime, ignorePrefixes);
	}
	
}