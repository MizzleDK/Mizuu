package com.miz.loader;/*
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.GridEpisode;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jcifs.smb.SmbFile;

import static com.miz.functions.PreferenceKeys.TVSHOWS_EPISODE_ORDER;

public class TvShowEpisodeLoader {

    public enum TvShowEpisodeSortType {
        ASCENDING(SORT_ASCENDING),
        DESCENDING(SORT_DESCENDING);

        private final int mType;

        TvShowEpisodeSortType(int type) {
            mType = type;
        }

        public Comparator<GridEpisode> getComparator() {
            return new Comparator<GridEpisode>() {
                @Override
                public int compare(GridEpisode lhs, GridEpisode rhs) {
                    int multiplier = mType == SORT_ASCENDING ? 1 : -1;

                    // Regular sorting
                    if (lhs.getEpisode() < rhs.getEpisode())
                        return -1 * multiplier;
                    if (lhs.getEpisode() > rhs.getEpisode())
                        return multiplier;
                    return 0;
                }
            };
        }
    }

    public enum TvShowEpisodeWatchedFilter {
        ALL, WATCHED, UNWATCHED
    }

    public static final int SORT_ASCENDING = 0, SORT_DESCENDING = 1;

    private final Context mContext;
    private final OnLoadCompletedCallback mCallback;
    private final String mShowId;
    private final int mShowSeason;
    private final DbAdapterTvShowEpisodes mTvShowEpisodeDatabase;

    private TvShowEpisodeWatchedFilter mWatchedFilter;
    private ArrayList<GridEpisode> mResults = new ArrayList<>();
    private TvShowEpisodeSortType mSortType;
    private TvShowEpisodeLoaderAsyncTask mAsyncTask;
    private boolean mShowAvailableFiles = false;

    public TvShowEpisodeLoader(Context context, String showId, int showSeason, OnLoadCompletedCallback callback) {
        mContext = context;
        mCallback = callback;
        mShowId = showId;
        mShowSeason = showSeason;
        mTvShowEpisodeDatabase = MizuuApplication.getTvEpisodeDbAdapter();

        setWatchedFilter(TvShowEpisodeWatchedFilter.ALL);
        setupSortType();
    }

    public String getShowId() {
        return mShowId;
    }

    public int getShowSeason() {
        return mShowSeason;
    }

    public TvShowEpisodeSortType getSortType() {
        return mSortType;
    }

    public void setSortType(TvShowEpisodeSortType type) {
        mSortType = type;

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString(TVSHOWS_EPISODE_ORDER, getSortType() == TvShowEpisodeSortType.ASCENDING ?
                        mContext.getString(R.string.oldestFirst) : mContext.getString(R.string.newestFirst));
        editor.apply();
    }

    public void setWatchedFilter(TvShowEpisodeWatchedFilter filter) {
        mWatchedFilter = filter;
    }

    public TvShowEpisodeWatchedFilter getWatchedFilter() {
        return mWatchedFilter;
    }

    public void setShowAvailableFiles(boolean showAvailableFiles) {
        mShowAvailableFiles = showAvailableFiles;
    }

    public boolean showAvailableFiles() {
        return mShowAvailableFiles;
    }

    public ArrayList<GridEpisode> getResults() {
        return mResults;
    }

    public void load() {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }

        mAsyncTask = new TvShowEpisodeLoaderAsyncTask();
        mAsyncTask.execute();
    }

    /**
     * Handles everything related to loading, filtering, sorting
     * and delivering the callback when everything is finished.
     */
    private class TvShowEpisodeLoaderAsyncTask extends LibrarySectionAsyncTask<Void, Void, Void> {

        private final ArrayList<GridEpisode> mEpisodeList;

        public TvShowEpisodeLoaderAsyncTask() {
            mEpisodeList = new ArrayList<GridEpisode>();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mEpisodeList.addAll(MizuuApplication.getTvEpisodeDbAdapter()
                    .getEpisodesInSeason(mContext, getShowId(), getShowSeason()));

            int totalSize = mEpisodeList.size();

            switch (getWatchedFilter()) {
                case WATCHED:
                    for (int i = 0; i < totalSize; i++) {
                        if (!mEpisodeList.get(i).hasWatched()) {
                            mEpisodeList.remove(i);
                            i--;
                            totalSize--;
                        }
                    }
                    break;

                case UNWATCHED:
                    for (int i = 0; i < totalSize; i++) {
                        if (mEpisodeList.get(i).hasWatched()) {
                            mEpisodeList.remove(i);
                            i--;
                            totalSize--;
                        }
                    }
                    break;

                default:
                    break;

            }

            if (showAvailableFiles()) {
                for (int i = 0; i < totalSize; i++) {
                    ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_SHOWS, true);

                    if (isCancelled())
                        return null;

                    boolean condition = false;

                    for (Filepath path : mEpisodeList.get(i).getFilepaths()) {
                        if (path.isNetworkFile())
                            if (FileUtils.hasOfflineCopy(mContext, path)) {
                                condition = true;
                                break; // break inner loop to continue to the next episode
                            } else {
                                if (path.getType() == FileSource.SMB) {
                                    if (MizLib.isWifiConnected(mContext)) {
                                        FileSource source = null;

                                        for (int j = 0; j < filesources.size(); j++)
                                            if (path.getFilepath().contains(filesources.get(j).getFilepath())) {
                                                source = filesources.get(j);
                                                break;
                                            }

                                        if (source == null)
                                            continue;

                                        try {
                                            final SmbFile file = new SmbFile(
                                                    MizLib.createSmbLoginString(
                                                            source.getDomain(),
                                                            source.getUser(),
                                                            source.getPassword(),
                                                            path.getFilepath(),
                                                            false
                                                    ));
                                            if (file.exists()) {
                                                condition = true;
                                                break; // break inner loop to continue to the next episode
                                            }
                                        } catch (Exception e) {
                                        }  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
                                    }
                                } else if (path.getType() == FileSource.UPNP) {
                                    if (MizLib.exists(path.getFilepath())) {
                                        condition = true;
                                        break; // break inner loop to continue to the next episode
                                    }
                                }
                            }
                        else {
                            if (new File(path.getFilepath()).exists()) {
                                condition = true;
                                break; // break inner loop to continue to the next episode
                            }
                        }
                    }

                    if (!condition && mEpisodeList.size() > i) {
                        mEpisodeList.remove(i);
                        i--;
                        totalSize--;
                    }
                }
            }

            Collections.sort(mEpisodeList, getSortType().getComparator());

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mResults = new ArrayList<>(mEpisodeList);
                mCallback.onLoadCompleted();
            } else
                mEpisodeList.clear();
        }
    }

    private void setupSortType() {
        boolean ascending = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(TVSHOWS_EPISODE_ORDER, mContext.getString(R.string.oldestFirst))
                .equals(mContext.getString(R.string.oldestFirst));

        if (ascending) {
            setSortType(TvShowEpisodeSortType.ASCENDING);
        } else {
            setSortType(TvShowEpisodeSortType.DESCENDING);
        }
    }
}
