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

import com.miz.mizuu.MizuuApplication;

import android.content.Context;

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
}