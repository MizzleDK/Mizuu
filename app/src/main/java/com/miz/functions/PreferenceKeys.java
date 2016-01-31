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

/**
 * Simple class to hold keys for application preferences. This should
 * always be used when dealing with preferences. Scattered keys
 * across the app? No thank you.
 * @author Michell
 *
 */
public class PreferenceKeys {
	
	private PreferenceKeys() {} // No instantiation

	public static final String TRAKT_USERNAME = "traktUsername";
	public static final String TRAKT_PASSWORD = "traktPassword";
	public static final String TRAKT_FULL_NAME = "traktFullName";
	public static final String SYNC_WITH_TRAKT = "syncLibrariesWithTrakt";
	public static final String SCHEDULED_UPDATES_MOVIE = "scheduleUpdatesMovies";
	public static final String SCHEDULED_UPDATES_TVSHOWS = "scheduleUpdatesShows";
	public static final String NEXT_SCHEDULED_MOVIE_UPDATE = "nextScheduledMovieUpdate";
	public static final String NEXT_SCHEDULED_TVSHOWS_UPDATE = "nextScheduledShowsUpdate";
	public static final String INCLUDE_ADULT_CONTENT = "prefsIncludeAdultContent";
	public static final String MOVIE_RATINGS_SOURCE = "prefsMovieRatingsSource";
	public static final String TVSHOWS_RATINGS_SOURCE = "prefsShowsRatingsSource";
	public static final String TMDB_BASE_URL = "tmdbBaseUrl";
	public static final String TMDB_BASE_URL_TIME = "tmdbBaseUrlTime";
	public static final String CLEAR_LIBRARY_TVSHOWS = "prefsClearLibraryTv";
	public static final String CLEAR_LIBRARY_MOVIES = "prefsClearLibrary";
	public static final String REMOVE_UNAVAILABLE_FILES_TVSHOWS = "prefsRemoveUnavailableTv";
	public static final String REMOVE_UNAVAILABLE_FILES_MOVIES = "prefsRemoveUnavailable";
	public static final String IGNORE_VIDEO_FILE_TYPE = "prefsIgnoreFileType";
	public static final String IGNORED_TITLE_PREFIXES = "prefsIgnorePrefixesInTitles";
	public static final String TVSHOWS_COLLECTION_LAYOUT = "prefsSeasonsLayout";
	public static final String TVSHOWS_EPISODE_ORDER = "prefsEpisodesOrder";
	public static final String TVSHOWS_SEASON_ORDER = "prefsSeasonsOrder";
	public static final String SHOW_TITLES_IN_GRID = "prefsShowGridTitles";
	public static final String GRID_ITEM_SIZE = "prefsGridItemSize";
	public static final String SORTING_MOVIES = "prefsSorting";
	public static final String SORTING_TVSHOWS = "prefsSortingTv";
	public static final String STARTUP_SELECTION = "prefsStartup";
	public static final String ALWAYS_DELETE_FILE = "prefsAlwaysDeleteFile";
	public static final String BUFFER_SIZE = "prefsBufferSize";
	public static final String HAS_SHOWN_FILEBROWSER_MESSAGE = "hasShownBrowserHelpMessage";
	public static final String IGNORE_FILESIZE_CHECK = "prefsIgnoreFilesizeCheck";
	public static final String SHOW_FILE_LOCATION = "prefsShowFileLocation";
	public static final String LANGUAGE_PREFERENCE = "prefsLanguagePreference";
	public static final String TV_SHOW_DATA_SOURCE = "prefsShowsDataSource";
	public static final String BLUR_BACKDROPS = "prefsBlurBackdrops";
    public static final String DVD_ORDERING = "prefsDvdOrdering";
    public static final String CHROMECAST_BETA_SUPPORT = "prefsChromecastBeta";
    public static final String USE_FAST_BLUR = "prefsUseFastBlur";

}
