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

package com.miz.utils;

import com.miz.functions.Actor;
import com.miz.functions.WebMovie;
import com.miz.mizuu.ActorBrowser;
import com.miz.mizuu.ActorBrowserTv;
import com.miz.mizuu.ActorDetails;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.SimilarMovies;
import com.miz.mizuu.TMDbMovieDetails;

import android.content.Context;
import android.content.Intent;

public class IntentUtils {

	private IntentUtils() {} // No instantiation
	
	/**
	 * Intent for actor details.
	 * @param context
	 * @param name
	 * @param id
	 * @param thumbnail
	 * @return
	 */
	public static Intent getActorIntent(Context context, String name, String id, String thumbnail) {
		Intent actorIntent = new Intent(context, ActorDetails.class);
		actorIntent.putExtra("actorName", name);
		actorIntent.putExtra("actorID", id);
		actorIntent.putExtra("thumb", thumbnail);
		return actorIntent;
	}
	
	/**
	 * Intent for actor details. Uses the supplied {@link Actor} object to get details.
	 * @param context
	 * @param actor
	 * @return
	 */
	public static Intent getActorIntent(Context context, Actor actor) {
		return getActorIntent(context, actor.getName(), actor.getId(), actor.getUrl());
	}
	
	/**
	 * Intent for the movie actor browser.
	 * @param context
	 * @param title
	 * @param movieId
	 * @return
	 */
	public static Intent getActorBrowserMovies(Context context, String title, String movieId) {
		Intent actorIntent = new Intent(context, ActorBrowser.class);
		actorIntent.putExtra("title", title);
		actorIntent.putExtra("movieId", movieId);
		return actorIntent;
	}
	
	/**
	 * Intent for the movie actor browser.
	 * @param context
	 * @param title
	 * @param movieId
	 * @return
	 */
	public static Intent getActorBrowserTvShows(Context context, String title, String showId) {
		Intent actorIntent = new Intent(context, ActorBrowserTv.class);
		actorIntent.putExtra("title", title);
		actorIntent.putExtra("showId", showId);
		return actorIntent;
	}
	
	/**
	 * Intent for the similar movies browser.
	 * @param context
	 * @param title
	 * @param movieId
	 * @return
	 */
	public static Intent getSimilarMovies(Context context, String title, String movieId) {
		Intent similarMoviesIntent = new Intent(context, SimilarMovies.class);
		similarMoviesIntent.putExtra("title", title);
		similarMoviesIntent.putExtra("movieId", movieId);
		return similarMoviesIntent;
	}
	
	public static Intent getTmdbMovieDetails(Context context, WebMovie movie) {
		Intent movieDetailsIntent = new Intent(context, movie.isInLibrary() ? MovieDetails.class : TMDbMovieDetails.class);
		movieDetailsIntent.putExtra("tmdbId", movie.getId());
		movieDetailsIntent.putExtra("title", movie.getTitle());
		return movieDetailsIntent;
	}
}
