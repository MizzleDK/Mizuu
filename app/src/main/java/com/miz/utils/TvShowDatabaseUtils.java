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
import android.database.Cursor;
import android.widget.Toast;

import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.GridEpisode;
import com.miz.functions.MizLib;
import com.miz.functions.TvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;

import java.util.ArrayList;
import java.util.List;

public class TvShowDatabaseUtils {

	private TvShowDatabaseUtils() {} // No instantiation

    /**
     * Delete all TV shows in the various database tables and remove all related image files.
     * @param context
     */
    public static void deleteAllTvShows(Context context) {
        // Delete all movies
        MizuuApplication.getTvDbAdapter().deleteAllShowsInDatabase();

        // Delete all episodes
        MizuuApplication.getTvEpisodeDbAdapter().deleteAllEpisodes();

        // Delete all episode filepath mappings
        MizuuApplication.getTvShowEpisodeMappingsDbAdapter().deleteAllFilepaths();

        // Delete all downloaded image files from the device
        FileUtils.deleteRecursive(MizuuApplication.getTvShowThumbFolder(context), false);
        FileUtils.deleteRecursive(MizuuApplication.getTvShowBackdropFolder(context), false);
        FileUtils.deleteRecursive(MizuuApplication.getTvShowSeasonFolder(context), false);
        FileUtils.deleteRecursive(MizuuApplication.getTvShowEpisodeFolder(context), false);
    }

	/**
	 * Remove all database entries and related images for a given TV show season.
	 * This also checks if the TV show has any other seasons - if not, it'll remove
	 * the TV show database entry as well.
	 */
	public static void deleteSeason(Context context, String showId, int season) {
		// Database adapters
		DbAdapterTvShows showAdapter = MizuuApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = MizuuApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

		// Get a List of episodes in the season
		List<GridEpisode> episodesInSeason = episodeAdapter.getEpisodesInSeason(context, showId, season);

		// Remove all episodes from the season
		episodeAdapter.deleteSeason(showId, season);

		// Remove all filepath mappings to the season
		episodeMappingsAdapter.removeSeason(showId, season);

		// Remove all episode images
		for (GridEpisode episode : episodesInSeason) {
			episode.getCover().delete();
		}

		// Remove season image
		FileUtils.getTvShowSeason(context, showId, season).delete();

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {
			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}
	}

	public static void deleteEpisode(Context context, String showId, String filepath) {
		// Database adapters
		DbAdapterTvShows showAdapter = MizuuApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = MizuuApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

		// Go through each filepath and remove the database entries
		Cursor cursor = episodeMappingsAdapter.getAllFilepathInfo(filepath);
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					String season = cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_SEASON));
					String episode = cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_EPISODE));

					// Remove the filepath mapping
					episodeMappingsAdapter.deleteFilepath(filepath);

					// Check if there are any more filepaths mapped to the season / episode
					if (!episodeMappingsAdapter.hasMultipleFilepaths(showId, season, episode)) {

						episodeAdapter.deleteEpisode(showId, MizLib.getInteger(season), MizLib.getInteger(episode));

						// Delete the episode photo
						FileUtils.getTvShowEpisode(context, showId, season, episode).delete();

						// Check if the season contains any more mapped filepaths
						if (episodeAdapter.getEpisodesInSeason(context, showId, MizLib.getInteger(season)).size() == 0) {

							// Remove season image
							FileUtils.getTvShowSeason(context, showId, season).delete();
						}
					}
				}
			} catch (Exception ignored) {} finally {
				cursor.close();
			}
		}

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {

			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}	
	}

	public static void deleteEpisode(Context context, String showId, int season, int episode) {
		// Database adapters
		DbAdapterTvShows showAdapter = MizuuApplication.getTvDbAdapter();
		DbAdapterTvShowEpisodes episodeAdapter = MizuuApplication.getTvEpisodeDbAdapter();
		DbAdapterTvShowEpisodeMappings episodeMappingsAdapter = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

		ArrayList<String> filepaths = episodeMappingsAdapter.getFilepathsForEpisode(showId, MizLib.addIndexZero(season), MizLib.addIndexZero(episode));

		for (int i = 0; i < filepaths.size(); i++) {
			String filepath = filepaths.get(i);
			// Remove the filepath mapping
			episodeMappingsAdapter.deleteFilepath(filepath);
		}

        // Delete the episode
        episodeAdapter.deleteEpisode(showId, season, episode);

        // Delete the episode photo
        FileUtils.getTvShowEpisode(context, showId, season, episode).delete();

        // Check if the season contains any more mapped filepaths
        if (episodeAdapter.getEpisodesInSeason(context, showId, season).size() == 0) {

            // Remove season image
            FileUtils.getTvShowSeason(context, showId, season).delete();
        }

		// Check if we've removed all episodes for the given TV show
		if (episodeAdapter.getEpisodeCount(showId) == 0) {

			// Remove the TV show from the TV show database
			showAdapter.deleteShow(showId);

			// Remove the TV show thumbnail image
			FileUtils.getTvShowThumb(context, showId).delete();

			// Remove the TV show backdrop image
			FileUtils.getTvShowBackdrop(context, showId).delete();
		}	
	}
	
	public static void deleteAllUnidentifiedFiles() {
		MizuuApplication.getTvShowEpisodeMappingsDbAdapter().deleteAllUnidentifiedFilepaths();
	}

    public static void setTvShowsFavourite(final Context context,
                                          final List<TvShow> shows,
                                          boolean favourite) {
        boolean success = true;

        for (TvShow show : shows)
            success = success && MizuuApplication.getTvDbAdapter().updateShowSingleItem(show.getId(),
                DbAdapterTvShows.KEY_SHOW_FAVOURITE, favourite ? "1" : "0");

        if (success)
            if (favourite)
                Toast.makeText(context, context.getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                Trakt.tvShowFavorite(shows, context);
            }
        }.start();
    }

    public static void setTvShowsWatched(final Context context,
                                           final List<TvShow> shows,
                                           final boolean watched) {
        boolean success = true;

        for (TvShow show : shows)
            success = success && MizuuApplication.getTvEpisodeDbAdapter().setShowWatchStatus(show.getId(), watched);

        if (success)
            if (watched)
                Toast.makeText(context, context.getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, context.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                for (TvShow show : shows) {
                    Cursor cursor = MizuuApplication.getTvEpisodeDbAdapter().getEpisodes(show.getId());
                    if (cursor != null) {
                        try {
                            List<TvShowEpisode> episodes = new ArrayList<>();
                            while (cursor.moveToNext()) {
                                episodes.add(new TvShowEpisode(show.getId(),
                                        MizLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))),
                                        MizLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)))
                                ));
                            }
                            Trakt.markEpisodeAsWatched(show.getId(), episodes, context, watched);
                        } catch (Exception e) {

                        } finally {
                            cursor.close();
                        }
                    }
                }
            }
        }.start();
    }
}