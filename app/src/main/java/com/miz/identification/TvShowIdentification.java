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
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseBooleanArray;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.miz.abstractclasses.TvShowApiService;
import com.miz.apis.thetvdb.Episode;
import com.miz.apis.thetvdb.TvShow;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.MizLib;
import com.miz.functions.TvShowLibraryUpdateCallback;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.FileUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class TvShowIdentification {

    private final Picasso mPicasso;
    private final TvShowLibraryUpdateCallback mCallback;
    private final Context mContext;

    private ArrayList<ShowStructure> mShowStructures = new ArrayList<ShowStructure>();
    private Multimap<String, Integer> mShowFolderNameMap = LinkedListMultimap.create();
    private SparseBooleanArray mImdbMap = new SparseBooleanArray();
    private String mShowId = null, mLocale = null;
    private int mSeason = -1, mEpisode = -1;
    private boolean mCancel = false;

    public TvShowIdentification(Context context, TvShowLibraryUpdateCallback callback, ArrayList<ShowStructure> files) {
        mContext = context;
        mCallback = callback;
        mShowStructures = new ArrayList<ShowStructure>(files);

        mPicasso = MizuuApplication.getPicasso(mContext);

        // Get the language preference
        getLanguagePreference();
    }

    private void getLanguagePreference() {
        mLocale = PreferenceManager.getDefaultSharedPreferences(mContext).getString(LANGUAGE_PREFERENCE, "en");
    }

    /**
     * Use this to disable TV show searching
     * and attempt identification based on the
     * provided TV show ID.
     * @param showId
     */
    public void setShowId(String showId) {
        mShowId = showId;
    }

    public boolean overrideShowId() {
        return null != getShowId();
    }

    public String getShowId() {
        return mShowId;
    }

    /**
     * Use this to override the season
     * and episode number of a given file.
     * @param season
     * @param episode
     */
    public void setSeasonAndEpisode(int season, int episode) {
        mSeason = season;
        mEpisode = episode;
    }

    public int getSeason() {
        return mSeason;
    }

    public int getEpisode() {
        return mEpisode;
    }

    public boolean overrideSeasonAndEpisode() {
        return getSeason() >= 0 && getEpisode() >= 0;
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
        for (int i = 0; i < mShowStructures.size(); i++) {
            if (mCancel)
                return;

            ShowStructure ss = mShowStructures.get(i);

            mShowFolderNameMap.put(ss.getDecryptedShowFolderName(), i);
            mImdbMap.put(i, ss.hasImdbId());
        }

        TvShowApiService service = MizuuApplication.getTvShowService(mContext);

        for (String showFolderName : mShowFolderNameMap.keySet()) {

            if (mCancel)
                return;

            TvShow show = null;
            List<TvShow> results = new ArrayList<TvShow>();
            if (!overrideShowId()) {

                // Get the first ShowStructure element
                ShowStructure ss = mShowStructures.get(mShowFolderNameMap.get(showFolderName).iterator().next());

                // Check if there's an IMDb ID and attempt to search based on it
                if (ss.hasImdbId())
                    results = service.searchByImdbId(ss.getImdbId(), null);

                // If there's no results, attempt to search based on the show folder name and year
                if (results.size() == 0) {
                    int year = mShowStructures.get(mShowFolderNameMap.get(showFolderName).iterator().next()).getReleaseYear();
                    if (year >= 0)
                        results = service.search(showFolderName + " " + year, null);
                }

                // If there's still no results, search based on the show folder name only
                if (results.size() == 0)
                    results = service.search(showFolderName, null);
            } else
                show = service.get(getShowId(), mLocale);

            // Check if the show folder name results in any matches
            // - if it does, use that to identify all files
            if (results.size() > 0 || overrideShowId()) {

                // Get the TV show and create it in the database
                if (!overrideShowId())
                    show = service.get(results.get(0).getId(), mLocale);
                createShow(show);

                int episodeCount = 0;
                for (Integer value : mShowFolderNameMap.get(showFolderName)) {
                    if (mCancel)
                        continue;

                    ShowStructure ss = mShowStructures.get(value);
                    for (com.miz.identification.Episode ep : ss.getEpisodes()) {
                        if (mCancel)
                            continue;

                        downloadEpisode(show, ep.getSeason(), ep.getEpisode(), ss.getFilepath());
                        episodeCount++;
                    }
                }

                showAddedShowNotification(show, episodeCount);

            } else {
                // else go through each file and identify based on the filename

                for (Integer value : mShowFolderNameMap.get(showFolderName)) {
                    if (mCancel)
                        continue;

                    ShowStructure ss = mShowStructures.get(value);

                    show = null;
                    results = new ArrayList<TvShow>();

                    // Check if there's an IMDb ID and attempt to search based on it
                    if (ss.hasImdbId())
                        results = service.searchByImdbId(ss.getImdbId(), null);

                    // If there's no results, attempt to search based on the show folder name and year
                    if (results.size() == 0) {
                        int year = ss.getReleaseYear();
                        if (year >= 0)
                            results = service.search(ss.getDecryptedFilename() + " " + year, null);
                    }

                    // If there's still no results, search based on the show folder name only
                    if (results.size() == 0)
                        results = service.search(ss.getDecryptedFilename(), null);

                    if (results.size() == 0) {
                        show = new TvShow();
                        show.setId(DbAdapterTvShows.UNIDENTIFIED_ID);
                    } else {
                        show = service.get(results.get(0).getId(), mLocale);
                    }

                    createShow(show);

                    for (com.miz.identification.Episode ep : ss.getEpisodes()) {
                        if (mCancel)
                            continue;

                        downloadEpisode(show, ep.getSeason(), ep.getEpisode(), ss.getFilepath());
                    }

                    showAddedShowNotification(show, ss.getEpisodes().size());
                }
            }
        }
    }

    private void showAddedShowNotification(TvShow show, int episodeCount) {
        if (show == null)
            return;

        File coverFile = FileUtils.getTvShowThumb(mContext, show.getId());
        File backdropFile = FileUtils.getTvShowBackdrop(mContext, show.getId());
        if (!backdropFile.exists())
            backdropFile = coverFile;

        try {
            mCallback.onTvShowAdded(show.getId(), show.getTitle(), mPicasso.load(coverFile).resize(getNotificationImageSizeSmall(), (int) (getNotificationImageSizeSmall() * 1.5)).get(),
                    mPicasso.load(backdropFile).skipMemoryCache().resize(getNotificationImageWidth(), (getNotificationImageWidth() / 16) * 9).get(), episodeCount);
        } catch (IOException e) {
            mCallback.onTvShowAdded(show.getId(), show.getTitle(), null, null, episodeCount);
        }

        LocalBroadcastUtils.updateTvShowLibrary(mContext);
        LocalBroadcastUtils.updateTvShowSeasonsOverview(mContext);
    }

    private void createShow(TvShow thisShow) {

        boolean downloadCovers = true;

        // Check if the show already exists before downloading the show info
        if (!TextUtils.isEmpty(thisShow.getId()) && !thisShow.getId().equals(DbAdapterTvShows.UNIDENTIFIED_ID)) {
            DbAdapterTvShows db = MizuuApplication.getTvDbAdapter();
            Cursor cursor = db.getShow(thisShow.getId());
            if (cursor.getCount() > 0) {
                downloadCovers = false;
            }
        }

        if (downloadCovers) {
            if (!TextUtils.isEmpty(thisShow.getId()) && !thisShow.getId().equals(DbAdapterTvShows.UNIDENTIFIED_ID)) {
                String thumb_filepath = FileUtils.getTvShowThumb(mContext, thisShow.getId()).getAbsolutePath();
                String backdrop_filepath = FileUtils.getTvShowBackdrop(mContext, thisShow.getId()).getAbsolutePath();

                // Download the cover file and try again if it fails
                if (!TextUtils.isEmpty(thisShow.getCoverUrl()))
                    if (!MizLib.downloadFile(thisShow.getCoverUrl(), thumb_filepath))
                        MizLib.downloadFile(thisShow.getCoverUrl(), thumb_filepath);

                MizLib.resizeBitmapFileToCoverSize(mContext, thumb_filepath);

                // Download the backdrop image file and try again if it fails
                if (!TextUtils.isEmpty(thisShow.getBackdropUrl()))
                    if (!MizLib.downloadFile(thisShow.getBackdropUrl(), backdrop_filepath))
                        MizLib.downloadFile(thisShow.getBackdropUrl(), backdrop_filepath);

                DbAdapterTvShows dbHelper = MizuuApplication.getTvDbAdapter();
                dbHelper.createShow(thisShow.getId(), thisShow.getTitle(), thisShow.getDescription(), thisShow.getActors(), thisShow.getGenres(),
                        thisShow.getRating(), thisShow.getCertification(), thisShow.getRuntime(), thisShow.getFirstAired(), "0");
            }
        }
    }

    private void downloadEpisode(TvShow thisShow, int season, int episode, String filepath) {
        Episode thisEpisode = new Episode();

        if (overrideSeasonAndEpisode()) {
            season = getSeason();
            episode = getEpisode();
        }

        ArrayList<Episode> episodes = thisShow.getEpisodes();
        int count = episodes.size();
        for (int i = 0; i < count; i++) {
            if (episodes.get(i).getSeason() == season && episodes.get(i).getEpisode() == episode) {
                thisEpisode = episodes.get(i);
                break;
            }
        }

        if (thisEpisode.getEpisode() == -1) {
            thisEpisode.setEpisode(episode);
            thisEpisode.setSeason(season);
        }

        // Download the episode screenshot file and try again if it fails
        if (!TextUtils.isEmpty(thisEpisode.getScreenshotUrl())) {
            String screenshotFile = FileUtils.getTvShowEpisode(mContext, thisShow.getId(), season, episode).getAbsolutePath();
            if (!MizLib.downloadFile(thisEpisode.getScreenshotUrl(), screenshotFile))
                MizLib.downloadFile(thisEpisode.getScreenshotUrl(), screenshotFile);
        }

        // Download season cover if it hasn't already been downloaded
        if (thisShow.hasSeason(thisEpisode.getSeason())) {
            File seasonFile = FileUtils.getTvShowSeason(mContext, thisShow.getId(), season);
            if (!seasonFile.exists()) {
                if (!MizLib.downloadFile(thisShow.getSeason(thisEpisode.getSeason()).getCoverPath(), seasonFile.getAbsolutePath()))
                    MizLib.downloadFile(thisShow.getSeason(thisEpisode.getSeason()).getCoverPath(), seasonFile.getAbsolutePath());
            }
        }

        addToDatabase(thisShow, thisEpisode, filepath);
    }

    private void addToDatabase(TvShow thisShow, Episode ep, String filepath) {
        DbAdapterTvShowEpisodes dbHelper = MizuuApplication.getTvEpisodeDbAdapter();

        if (thisShow.getId().equals(DbAdapterTvShows.UNIDENTIFIED_ID)) {
            // If it's an unidentified file, we shouldn't create a episode entry in the database
            MizuuApplication.getTvShowEpisodeMappingsDbAdapter().createFilepathMapping(filepath,
                    thisShow.getId(), MizLib.addIndexZero(ep.getSeason()), MizLib.addIndexZero(ep.getEpisode()));
        } else {
            dbHelper.createEpisode(filepath, MizLib.addIndexZero(ep.getSeason()),
                    MizLib.addIndexZero(ep.getEpisode()), thisShow.getId(), ep.getTitle(),
                    ep.getDescription(), ep.getAirdate(), ep.getRating(), ep.getDirector(),
                    ep.getWriter(), ep.getGueststars(), "0", "0");
        }

        updateNotification(thisShow, ep, filepath);
    }

    private void updateNotification(TvShow thisShow, Episode ep, String filepath) {
        File coverFile = FileUtils.getTvShowThumb(mContext, thisShow.getId());
        File backdropFile = FileUtils.getTvShowEpisode(mContext, thisShow.getId(), MizLib.addIndexZero(ep.getSeason()), MizLib.addIndexZero(ep.getEpisode()));
        if (!backdropFile.exists())
            backdropFile = coverFile;

        try {
            mCallback.onEpisodeAdded(thisShow.getId(), thisShow.getId().equals(DbAdapterTvShows.UNIDENTIFIED_ID) ? filepath : thisShow.getTitle() + " S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()),
                    mPicasso.load(coverFile).resize(getNotificationImageSizeSmall(), (int) (getNotificationImageSizeSmall() * 1.5)).get(),
                    mPicasso.load(backdropFile).skipMemoryCache().get());
        } catch (IOException e) {
            mCallback.onEpisodeAdded(thisShow.getId(), thisShow.getId().equals(DbAdapterTvShows.UNIDENTIFIED_ID) ? filepath : thisShow.getTitle() + " S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()),
                    null, null);
        }
    }

    // These variables don't need to be re-initialized
    private int widgetWidth = 0, smallSize = 0;

    private int getNotificationImageWidth() {
        if (widgetWidth == 0)
            widgetWidth = MizLib.getLargeNotificationWidth(mContext);
        return widgetWidth;
    }

    private int getNotificationImageSizeSmall() {
        if (smallSize == 0)
            smallSize = MizLib.getThumbnailNotificationSize(mContext);
        return smallSize;
    }

}