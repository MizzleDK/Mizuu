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

import com.miz.functions.Actor;

import java.util.List;

public abstract class ApiService<T> {

	/**
	 * Search based on the query.
	 * @param query Search query.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return {@link List} of search results. If no results were found,
	 * the list will be empty.
	 */
	public abstract List<T> search(String query, String language);
	
	/**
	 * Search based on the query.
	 * @param query Search query.
	 * @param query Release year (or first air date year). Can be {@link null}.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return {@link List} of search results. If no results were found,
	 * the list will be empty.
	 */
	public abstract List<T> search(String query, String year, String language);
	
	/**
	 * Search based on the query using N-gram search.
	 * @param query Search query.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return {@link List} of search results. If no results were found,
	 * the list will be empty.
	 */
	public abstract List<T> searchNgram(String query, String language);
	
	/**
	 * Search based on a IMDb ID.
	 * @param query IMDb ID.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return {@link List} of search results. If no results were found,
	 * the list will be empty.
	 */
	public abstract List<T> searchByImdbId(String imdbId, String language);
	
	/**
	 * Get content based on its ID.
	 * @param id Movie or TV show ID.
	 * @param language Two-letter ISO 639-1 language code. Can be {@link null}.
	 * @return Content object based on the supplied ID.
	 */
	public abstract T get(String id, String language);
	
	/**
	 * Get actors for the given content ID.
	 * @param id Movie or TV show ID.
	 * @return {@link List} of {@link Actor} objects.
	 */
	public abstract List<Actor> getActors(String id);
}
