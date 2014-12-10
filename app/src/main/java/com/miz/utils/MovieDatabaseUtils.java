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
import android.text.TextUtils;

import com.miz.db.DbAdapterMovies;
import com.miz.mizuu.MizuuApplication;

public class MovieDatabaseUtils {

	private MovieDatabaseUtils() {} // No instantiation

	/**
	 * Delete all movies in the various database tables and remove all related image files.
	 * @param context
	 */
	public static void deleteAllMovies(Context context) {
		// Delete all movies
		MizuuApplication.getMovieAdapter().deleteAllMovies();
		
		// Delete all movie filepath mappings
		MizuuApplication.getMovieMappingAdapter().deleteAllMovies();
		
		// Delete all movie collections
		MizuuApplication.getCollectionsAdapter().deleteAllCollections();

		// Delete all downloaded image files from the device
		FileUtils.deleteRecursive(MizuuApplication.getMovieThumbFolder(context), false);
		FileUtils.deleteRecursive(MizuuApplication.getMovieBackdropFolder(context), false);
	}

	public static void removeAllUnidentifiedFiles() {
		MizuuApplication.getMovieMappingAdapter().deleteAllUnidentifiedFilepaths();
	}

    public static void deleteMovie(Context context, String tmdbId) {
        if (TextUtils.isEmpty(tmdbId))
            return;

        DbAdapterMovies db = MizuuApplication.getMovieAdapter();

        // Delete the movie details
        db.deleteMovie(tmdbId);

        // When deleting a movie, check if there's other movies
        // linked to the collection - if not, delete the collection entry
        String collectionId = db.getCollectionId(tmdbId);
        if (MizuuApplication.getCollectionsAdapter().getMovieCount(collectionId) == 1)
            MizuuApplication.getCollectionsAdapter().deleteCollection(collectionId);

        // Finally, delete all filepath mappings to this movie ID
        MizuuApplication.getMovieMappingAdapter().deleteMovie(tmdbId);
    }

    public static void ignoreMovie(String tmdbId) {
        if (TextUtils.isEmpty(tmdbId))
            return;

        DbAdapterMovies db = MizuuApplication.getMovieAdapter();

        // We delete the movie...
        db.deleteMovie(tmdbId);

        // ... check if there's other movies linked to the collection
        // - if not, we delete the collection entry as well
        String collectionId = db.getCollectionId(tmdbId);
        if (MizuuApplication.getCollectionsAdapter().getMovieCount(collectionId) == 1)
            MizuuApplication.getCollectionsAdapter().deleteCollection(collectionId);

        // And finally we make sure that Mizuu ignores the filepath in the future
        MizuuApplication.getMovieMappingAdapter().ignoreMovie(tmdbId);
    }
}