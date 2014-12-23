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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.mizuu.MizuuApplication;

public class LocalBroadcastUtils {

	public static final String UPDATE_MOVIE_LIBRARY = "mizuu-movies-update";
	public static final String UPDATE_TV_SHOW_LIBRARY = "mizuu-tvshows-update";
	public static final String CLEAR_IMAGE_CACHE = "clear-image-cache";
    public static final String UPDATE_TV_SHOW_SEASONS_OVERVIEW = "mizuu-tvshows-seasons-update";
    public static final String UPDATE_TV_SHOW_EPISODES_OVERVIEW = "mizuu-tvshows-episodes-update";
    public static final String UPDATE_TV_SHOW_EPISODE_DETAILS_OVERVIEW = "mizuu-tvshows-episode-details-update";
	
	private LocalBroadcastUtils() {} // No instantiation
	
	/**
	 * Force the movie library to clear the cache and reload everything.
	 * @param context
	 */
	public static void updateMovieLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_MOVIE_LIBRARY));
	}
	
	/**
	 * Force the TV show library to clear the cache and reload everything.
	 * @param context
	 */
	public static void updateTvShowLibrary(Context context) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_LIBRARY));
	}
	
	/**
	 * Clear the image cache.
	 * @param context
	 */
	public static void clearImageCache(Context context) {
		MizuuApplication.clearLruCache(context);
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(CLEAR_IMAGE_CACHE));
	}

    /**
     * Force the TV show seasons overview to reload.
     * @param context
     */
    public static void updateTvShowSeasonsOverview(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_SEASONS_OVERVIEW));
    }

    /**
     * Force the TV show episodes overview to reload.
     * @param context
     */
    public static void updateTvShowEpisodesOverview(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_EPISODES_OVERVIEW));
    }

    /**
     * Force the TV show details view to reload.
     * @param context
     */
    public static void updateTvShowEpisodeDetailsView(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(UPDATE_TV_SHOW_EPISODE_DETAILS_OVERVIEW));
    }
}