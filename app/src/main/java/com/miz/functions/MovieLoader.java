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

import com.miz.db.DbAdapterMovies;
import com.miz.mizuu.MizuuApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MovieLoader {

    public enum MovieLibraryType {
        ALL_MOVIES,
        FAVORITES,
        NEW_RELEASES,
        WATCHLIST,
        WATCHED,
        UNWATCHED,
        COLLECTIONS
    }

    public interface OnLoadCompletedCallback {
        void onLoadCompleted(List<MediumMovie> movieList);
    }

    private final Context mContext;
    private final MovieLibraryType mLibraryType;
    private final OnLoadCompletedCallback mCallback;
    private final DbAdapterMovies mDatabase;

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
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = new MovieLoaderAsyncTask();
        mAsyncTask.execute();
    }

    /**
     * Creates movie objects from a Cursor and adds them to a list.
     * @param cursor
     * @return
     */
    private ArrayList<MediumMovie> listFromCursor(Cursor cursor) {
        ArrayList<MediumMovie> list = new ArrayList<MediumMovie>();

        if (cursor != null) {
            HashMap<String, String> collectionsMap = MizuuApplication.getCollectionsAdapter().getCollectionsMap();
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

        return list;
    }

    private class MovieLoaderAsyncTask extends LibrarySectionAsyncTask<Void, Void, Void> {

        private final ArrayList<MediumMovie> mMovieList;

        public MovieLoaderAsyncTask() {
            mMovieList = new ArrayList<MediumMovie>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            switch (mLibraryType) {
                case ALL_MOVIES:
                    mMovieList.addAll(listFromCursor(mDatabase.getAllMovies()));
                    break;
                case COLLECTIONS:
                    mMovieList.addAll(listFromCursor(mDatabase.getCollections()));
                    break;
                case FAVORITES:
                    break;
                case NEW_RELEASES:
                    break;
                case UNWATCHED:
                    break;
                case WATCHED:
                    break;
                case WATCHLIST:
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            if (!isCancelled())
                mCallback.onLoadCompleted(mMovieList);
            else
                mMovieList.clear();
        }
    }
}