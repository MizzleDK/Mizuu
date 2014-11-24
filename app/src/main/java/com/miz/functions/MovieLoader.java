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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.mizuu.MizuuApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MovieLoader {

    public enum MovieLibraryType {

        ALL_MOVIES(MovieLoader.ALL_MOVIES),
        FAVORITES(MovieLoader.FAVORITES),
        NEW_RELEASES(MovieLoader.NEW_RELEASES),
        WATCHLIST(MovieLoader.WATCHLIST),
        WATCHED(MovieLoader.WATCHED),
        UNWATCHED(MovieLoader.UNWATCHED),
        COLLECTIONS(MovieLoader.COLLECTIONS);

        private final int mType;

        MovieLibraryType(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }

        public static MovieLibraryType fromInt(int type) {
            switch (type) {
                case MovieLoader.ALL_MOVIES:
                    return ALL_MOVIES;
                case MovieLoader.FAVORITES:
                    return FAVORITES;
                case MovieLoader.NEW_RELEASES:
                    return NEW_RELEASES;
                case MovieLoader.WATCHLIST:
                    return WATCHLIST;
                case MovieLoader.WATCHED:
                    return WATCHED;
                case MovieLoader.UNWATCHED:
                    return UNWATCHED;
                default:
                    return COLLECTIONS;
            }
        }
    }

    public interface OnLoadCompletedCallback {
        void onLoadCompleted(List<MediumMovie> movieList);
    }

    public static final int ALL_MOVIES = 1,
    FAVORITES = 2,
    NEW_RELEASES = 3,
    WATCHLIST = 4,
    WATCHED = 5,
    UNWATCHED = 6,
    COLLECTIONS = 7;

    private final Context mContext;
    private final MovieLibraryType mLibraryType;
    private final OnLoadCompletedCallback mCallback;
    private final DbAdapterMovies mDatabase;

    private ArrayList<MediumMovie> mResults;
    private MovieLoaderAsyncTask mAsyncTask;
    private boolean mIgnorePrefixes = false,
            mAvailableFilesOnly = false,
            mOfflineFilesOnly = false;

    public MovieLoader(Context context, MovieLibraryType libraryType, OnLoadCompletedCallback callback) {
        mContext = context;
        mLibraryType = libraryType;
        mCallback = callback;
        mDatabase = MizuuApplication.getMovieAdapter();
    }

    public MovieLibraryType getType() {
        return mLibraryType;
    }

    /**
     * Determine whether the MovieLoader should ignore title prefixes.
     * @param ignore
     */
    public void setIgnorePrefixes(boolean ignore) {
        mIgnorePrefixes = ignore;
    }

    /**
     * Determine whether to only show available files.
     * @param showAvailableFilesOnly
     */
    public void setShowAvailableFiles(boolean showAvailableFilesOnly) {
        mAvailableFilesOnly = showAvailableFilesOnly;
    }

    public void setShowOfflineFiles(boolean showOfflineFilesOnly) {
        mOfflineFilesOnly = showOfflineFilesOnly;
    }

    public void load() {
        load("");
    }

    public void search(String query) {
        load(query);
    }

    private void load(String query) {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = new MovieLoaderAsyncTask(query);
        mAsyncTask.execute();
    }

    /**
     * Creates movie objects from a Cursor and adds them to a list.
     * @param cursor
     * @return
     */
    private ArrayList<MediumMovie> listFromCursor(Cursor cursor) {

        // Normally we'd have to go through each movie and add filepaths mapped to that movie
        // one by one. This is a hacky approach that gets all filepaths at once and creates a
        // map of them. That way it's easy to get filepaths for a specific movie - and it's
        // 2-3x faster with ~750 movies.
        ArrayListMultimap<String, String> filepaths = ArrayListMultimap.create();
        Cursor paths = MizuuApplication.getMovieMappingAdapter().getAllFilepaths(false);
        if (paths != null) {
            try {
                while (paths.moveToNext()) {
                    filepaths.put(paths.getString(paths.getColumnIndex(DbAdapterMovieMappings.KEY_TMDB_ID)),
                            paths.getString(paths.getColumnIndex(DbAdapterMovieMappings.KEY_FILEPATH)));
                }
            } catch (Exception e) {} finally {
                paths.close();
                MizuuApplication.setMovieFilepaths(filepaths);
            }
        }

        HashMap<String, String> collectionsMap = MizuuApplication.getCollectionsAdapter().getCollectionsMap();
        ArrayList<MediumMovie> list = new ArrayList<MediumMovie>();

        if (cursor != null) {
            ColumnIndexCache cache = new ColumnIndexCache();

            try {
                while (cursor.moveToNext()) {
                    list.add(new MediumMovie(mContext,
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TITLE)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RATING)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RELEASEDATE)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_GENRES)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_FAVOURITE)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_ACTORS)),
                            collectionsMap.get(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID))),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TO_WATCH)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_HAS_WATCHED)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_DATE_ADDED)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_CERTIFICATION)),
                            cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RUNTIME)),
                            mIgnorePrefixes
                    ));
                }
            } catch (Exception e) {} finally {
                cursor.close();
                cache.clear();
            }
        }

        mResults = list;

        return list;
    }

    public ArrayList<MediumMovie> getResults() {
        return mResults;
    }

    private class MovieLoaderAsyncTask extends LibrarySectionAsyncTask<Void, Void, Void> {

        private final ArrayList<MediumMovie> mMovieList;
        private final String mSearchQuery;

        public MovieLoaderAsyncTask(String searchQuery) {
            // Lowercase in order to search more efficiently
            mSearchQuery = searchQuery.toLowerCase(Locale.getDefault());

            mMovieList = new ArrayList<MediumMovie>();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // We're only interested in querying the database
            // if we're not searching for a query
            if (TextUtils.isEmpty(mSearchQuery)) {
                switch (mLibraryType) {
                    case ALL_MOVIES:
                        mMovieList.addAll(listFromCursor(mDatabase.getAllMovies()));
                        break;
                    case COLLECTIONS:
                        mMovieList.addAll(listFromCursor(mDatabase.getCollections()));
                        break;
                    case FAVORITES:
                        mMovieList.addAll(listFromCursor(mDatabase.getFavorites()));
                        break;
                    case NEW_RELEASES:
                        mMovieList.addAll(listFromCursor(mDatabase.getNewReleases()));
                        break;
                    case UNWATCHED:
                        mMovieList.addAll(listFromCursor(mDatabase.getUnwatched()));
                        break;
                    case WATCHED:
                        mMovieList.addAll(listFromCursor(mDatabase.getWatched()));
                        break;
                    case WATCHLIST:
                        break;
                    default:
                        break;
                }
            }

            // If we've got a search query, we should search based on it
            if (!TextUtils.isEmpty(mSearchQuery)) {

                ArrayList<MediumMovie> tempCollection = Lists.newArrayList();

                if (mSearchQuery.startsWith("actor:")) {
                    for (int i = 0; i < mResults.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (mResults.get(i).getCast().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
                            tempCollection.add(mResults.get(i));
                    }
                } else if (mSearchQuery.equalsIgnoreCase("missing_genres")) {
                    for (int i = 0; i < mResults.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (TextUtils.isEmpty(mResults.get(i).getGenres()))
                            tempCollection.add(mResults.get(i));
                    }
                } else if (mSearchQuery.equalsIgnoreCase("multiple_versions")) {
                    DbAdapterMovieMappings db = MizuuApplication.getMovieMappingAdapter();

                    for (int i = 0; i < mResults.size(); i++) {
                        if (isCancelled())
                            return null;

                        if (db.hasMultipleFilepaths(mResults.get(i).getTmdbId()))
                            tempCollection.add(mResults.get(i));
                    }
                } else {
                    Pattern p = Pattern.compile(MizLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

                    for (int i = 0; i < mResults.size(); i++) {
                        if (isCancelled())
                            return null;

                        String lowerCaseTitle = (getType() == MovieLibraryType.COLLECTIONS) ?
                                mResults.get(i).getCollection().toLowerCase(Locale.ENGLISH) :
                                mResults.get(i).getTitle().toLowerCase(Locale.ENGLISH);

                        boolean foundInTitle = false;

                        if (lowerCaseTitle.indexOf(mSearchQuery) != -1 || p.matcher(lowerCaseTitle).replaceAll("").indexOf(mSearchQuery) != -1) {
                            tempCollection.add(mResults.get(i));
                            foundInTitle = true;
                        }

                        if (!foundInTitle) {
                            for (Filepath path : mResults.get(i).getFilepaths()) {
                                String filepath = path.getFilepath().toLowerCase(Locale.ENGLISH);
                                if (filepath.indexOf(mSearchQuery) != -1) {
                                    tempCollection.add(mResults.get(i));
                                    break; // Break the loop
                                }
                            }
                        }
                    }
                }

                // Clear the movie list
                mMovieList.clear();

                // Add all the temporary ones after search completed
                mMovieList.addAll(tempCollection);

                // Clear the temporary list
                tempCollection.clear();
            }





            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mCallback.onLoadCompleted(mMovieList);
                mResults = mMovieList;
            } else
                mMovieList.clear();
        }
    }
}