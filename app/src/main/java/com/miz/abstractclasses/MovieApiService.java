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

import android.text.TextUtils;

import com.miz.apis.tmdb.Movie;
import com.miz.functions.WebMovie;

import java.util.List;

public abstract class MovieApiService extends ApiService<Movie> {

	/**
	 * Get content based on its ID.
	 * @param id Movie or TV show ID.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return Content object based on the supplied ID.
	 */
	public abstract Movie get(String id, String json, String language);
	
	/**
	 * Get a {@link List} of URL's to cover images.
	 * @param id Movie ID.
	 * @return {@link List} containing cover image paths
	 */
	public abstract List<String> getCovers(String id);
	
	/**
	 * Get a {@link List} of URL's to backdrop images.
	 * @param id Movie ID.
	 * @return {@link List} containing backdrop image paths
	 */
	public abstract List<String> getBackdrops(String id);
	
	/**
	 * Get a {@link List} of similar content for a given movie ID.
	 * @param id Movie ID.
	 * @return {@link List} of similar content.
	 */
	public abstract List<WebMovie> getSimilarMovies(String id);
	
	/**
	 * Get the language code or a default one if
	 * the supplied one is empty or {@link null}.
	 * @param language
	 * @return Language code
	 */
	public String getLanguage(String language) {
		if (TextUtils.isEmpty(language))
			language = "en";
		return language;
	}
}