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

import java.util.List;

import android.content.Context;
import android.preference.PreferenceManager;

import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.GridEpisode;
import com.miz.functions.MizLib;
import com.miz.functions.PreferenceKeys;
import com.miz.mizuu.MizuuApplication;

public class TvShowDatabaseUtils {

	private TvShowDatabaseUtils() {} // No instantiation

	/**
	 * Remove all database entries and related images for a given TV show season.
	 * This also checks if the TV show has any other seasons - if not, it'll remove
	 * the TV show database entry as well.
	 */
	public static void removeSeason(Context context, String showId, int season) {
		// Should filepaths be removed completely or ignored in future library updates?
		boolean ignoreFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.IGNORED_FILES_ENABLED, false);

		// Database adapters
		DbAdapterTvShows showAdapter = MizuuApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = MizuuApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

		// Get a List of episodes in the season
		List<GridEpisode> episodesInSeason = episodeAdapter.getEpisodesInSeason(context, showId, season);

		// Remove all episodes from the season
		episodeAdapter.removeSeason(showId, season);

		// Remove / ignore all filepath mappings to the season
		if (ignoreFiles)
			episodeMappingsAdapter.ignoreSeason(showId, season);
		else
			episodeMappingsAdapter.removeSeason(showId, season);

		// Remove all episode images
		for (GridEpisode episode : episodesInSeason) {
			episode.getCover().delete();
		}

		// Remove season image
		MizLib.getTvShowSeason(context, showId, season).delete();

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {
			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			MizLib.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			MizLib.getTvShowBackdrop(context, showId).delete();
		}	
	}
}