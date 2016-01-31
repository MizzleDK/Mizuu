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

package com.miz.identification;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseBooleanArray;

import com.miz.abstractclasses.MovieApiService;
import com.miz.apis.tmdb.Movie;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.MizLib;
import com.miz.functions.MovieLibraryUpdateCallback;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.FileUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.MovieDatabaseUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class MovieIdentification {

    private final Picasso mPicasso;
    private final MovieLibraryUpdateCallback mCallback;
    private final Context mContext;
    private final ArrayList<MovieStructure> mMovieStructures;

    private SparseBooleanArray mImdbMap = new SparseBooleanArray();
    private String mMovieId = null, mCurrentMovieId = null, mLocale = null;
    private boolean mCancel = false;
    private int mCount = 0;

    public MovieIdentification(Context context, MovieLibraryUpdateCallback callback, ArrayList<MovieStructure> files) {
        mContext = context;
        mCallback = callback;
        mMovieStructures = new ArrayList<MovieStructure>(files);

        mPicasso = Picasso.with(mContext);

        // Get the language preference
        getLanguagePreference();
    }

    private void getLanguagePreference() {
        mLocale = PreferenceManager.getDefaultSharedPreferences(mContext).getString(LANGUAGE_PREFERENCE, "en");
    }

    /**
     * Use this to disable movie searching
     * and attempt identification based on the
     * provided movie ID.
     * @param movieId
     */
    public void setMovieId(String movieId) {
        mMovieId = movieId;
    }

    public void setCurrentMovieId(String oldMovieId) {
        if (!TextUtils.isEmpty(oldMovieId))
            mCurrentMovieId = oldMovieId;
    }

    private boolean overrideMovieId() {
        return null != getMovieId();
    }

    private String getMovieId() {
        return mMovieId;
    }

    private String getCurrentMovieId() {
        return mCurrentMovieId;
    }

    /**
     * Accepts two-letter ISO 639-1 language codes, i.e. "en".
     * @param language
     */
    public void setLanguage(String language) {
        mLocale = language;
    }

    public void cancel() {
        mCancel = true;
    }

    public void start() {

        // Go through all files
        for (int i = 0; i < mMovieStructures.size(); i++) {
            if (mCancel)
                return;

            MovieStructure ms = mMovieStructures.get(i);

            mImdbMap.put(i, ms.hasImdbId());
        }

        MovieApiService service = MizuuApplication.getMovieService(mContext);

        for (MovieStructure ms : mMovieStructures) {
            if (mCancel)
                return;

            mCount++;

            Movie movie = null;
            List<Movie> results = new ArrayList<Movie>();

            if (!overrideMovieId()) {
                // Check if there's an IMDb ID and attempt to search based on it
                if (ms.hasImdbId()) {
                    results = service.searchByImdbId(ms.getImdbId(), null);
                }

                // If there's no results, attempt to search based on the movie file name and year
                if (results.size() == 0) {
                    int year = ms.getReleaseYear();
                    if (year >= 0)
                        results = service.search(ms.getDecryptedFilename(), String.valueOf(year), null);
                }

                // If there's still no results, attempt to search based on the movie file name without year
                if (results.size() == 0)
                    results = service.search(ms.getDecryptedFilename(), null);

                // If there's still no results, attempt to search based on the parent folder name and year
                if (results.size() == 0) {
                    int year = ms.getReleaseYear();
                    if (year >= 0)
                        results = service.search(ms.getDecryptedParentFolderName(), String.valueOf(year), null);
                }

                // If there's still no results, search based on the parent folder name only
                if (results.size() == 0)
                    results = service.search(ms.getDecryptedParentFolderName(), null);
            } else {
                movie = service.get(getMovieId(), mLocale);
            }

            if (!overrideMovieId() && results.size() > 0) {
                // Automatic library update
                movie = service.get(results.get(0).getId(), mLocale);
            }

            // Last check - is movie still null?
            if (movie == null)
                movie = new Movie();

            createMovie(ms, movie);
        }
    }

    private void createMovie(MovieStructure ms, Movie movie) {
        boolean downloadCovers = true;

        if (!movie.getId().equals(DbAdapterMovies.UNIDENTIFIED_ID) && !TextUtils.isEmpty(movie.getId()))
            // We only want to download covers if the movie doesn't already exist
            downloadCovers = !MizuuApplication.getMovieAdapter().movieExists(movie.getId());

        if (downloadCovers) {
            String thumb_filepath = FileUtils.getMovieThumb(mContext, movie.getId()).getAbsolutePath();

            // Download the cover image and try again if it fails
            if (!MizLib.downloadFile(movie.getCover(), thumb_filepath))
                MizLib.downloadFile(movie.getCover(), thumb_filepath);

            // Download the backdrop image and try again if it fails
            if (!TextUtils.isEmpty(movie.getBackdrop())) {
                String backdropFile = FileUtils.getMovieBackdrop(mContext, movie.getId()).getAbsolutePath();

                if (!MizLib.downloadFile(movie.getBackdrop(), backdropFile))
                    MizLib.downloadFile(movie.getBackdrop(), backdropFile);
            }

            // Download the collection image
            if (!TextUtils.isEmpty(movie.getCollectionImage())) {
                String collectionImage = FileUtils.getMovieThumb(mContext, movie.getCollectionId()).getAbsolutePath();

                if (!MizLib.downloadFile(movie.getCollectionImage(), collectionImage))
                    MizLib.downloadFile(movie.getCollectionImage(), collectionImage);
            }
        }

        addToDatabase(ms, movie);
    }

    private void addToDatabase(MovieStructure ms, Movie movie) {
        DbAdapterMovieMappings dbHelperMovieMapping = MizuuApplication.getMovieMappingAdapter();
        DbAdapterMovies dbHelper = MizuuApplication.getMovieAdapter();

        // Check if this is manual identification by the user
        if (overrideMovieId()) {

            // How many filepaths are mapped to the current movie ID?
            int currentCount = dbHelperMovieMapping.getMovieFilepaths(getCurrentMovieId()).size();

            if (currentCount > 1) {
                // This movie has more than one filepath mapping, so we don't want
                // to remove the movie entry nor any images, etc.

                // Update the ID currently used to map the filepath to the movie
                dbHelperMovieMapping.updateTmdbId(ms.getFilepath(), getCurrentMovieId(), getMovieId());
            } else {
                if (TextUtils.isEmpty(getCurrentMovieId())) {
                    // We're dealing with an unidentified movie, so we update
                    // the mapped TMDb ID of the filepath to the new one
                    dbHelperMovieMapping.updateTmdbId(ms.getFilepath(), getMovieId());
                } else {
                    // This movie only has one filepath mapping, i.e. the one we're
                    // currently re-assigning. It's safe to delete all movie data and
                    // create the filepath mapping for the new movie ID.

                    // Delete the old movie and everything related to it
                    MovieDatabaseUtils.deleteMovie(mContext, getCurrentMovieId());

                    // Create the new filepath mapping
                    dbHelperMovieMapping.createFilepathMapping(ms.getFilepath(), getMovieId());
                }
            }

        } else {
            // This is Mizuu's automatic library update...

            // Just create the filepath mapping - if the filepath / movie
            // combination already exists, it won't do anything
            dbHelperMovieMapping.createFilepathMapping(ms.getFilepath(), movie.getId());
        }

        // Finally, create or update the movie
        dbHelper.createOrUpdateMovie(movie.getId(), movie.getTitle(), movie.getPlot(), movie.getImdbId(), movie.getRating(), movie.getTagline(),
                movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0",
                movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

        updateNotification(movie);
    }

    private void updateNotification(Movie movie) {
        File backdropFile = FileUtils.getMovieBackdrop(mContext, movie.getId());
        if (!backdropFile.exists())
            backdropFile = FileUtils.getMovieThumb(mContext, movie.getId());

        if (mCallback != null) {
            try {
                mCallback.onMovieAdded(movie.getTitle(),
                        mPicasso.load(FileUtils.getMovieThumb(mContext, movie.getId())).resize(getNotificationImageSizeSmall(), (int) (getNotificationImageSizeSmall() * 1.5)).get(),
                        mPicasso.load(backdropFile).resize(getNotificationImageWidth(), getNotificationImageHeight()).skipMemoryCache().get(), mCount);
            } catch (Exception e) {
                mCallback.onMovieAdded(movie.getTitle(), null, null, mCount);
            }
        }

        LocalBroadcastUtils.updateMovieLibrary(mContext);
    }

    // These variables don't need to be re-initialized
    private int widgetWidth = 0, widgetHeight = 0, smallSize = 0;

    private int getNotificationImageWidth() {
        if (widgetWidth == 0)
            widgetWidth = MizLib.getLargeNotificationWidth(mContext);
        return widgetWidth;
    }

    private int getNotificationImageHeight() {
        if (widgetHeight == 0)
            widgetHeight = (int) (MizLib.getLargeNotificationWidth(mContext) / 1.778);
        return widgetHeight;
    }

    private int getNotificationImageSizeSmall() {
        if (smallSize == 0)
            smallSize = MizLib.getThumbnailNotificationSize(mContext);
        return smallSize;
    }
}