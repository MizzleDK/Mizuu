package com.miz.test;/*
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
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.TvShowDatabaseUtils;

/**
 * Tests various TV show database queries.
 * Do not use this on a non-debug build, as
 * it will remove all data.
 */
public class DatabaseTvShowTests extends InstrumentationTestCase {

    private Context mContext;

    public void testCreateTvShow() {
        DbAdapterTvShows db = getAndResetDatabase();

        // There shouldn't be any TV shows to begin with
        assertEquals(0, db.count());

        // Create a test TV show
        createTestShow(db);

        // We should now have one TV show
        assertEquals(1, db.count());
    }

    public void testTvShowDuplication() {
        DbAdapterTvShows db = getAndResetDatabase();

        // There shouldn't be any TV shows to begin with
        assertEquals(0, db.count());

        // Create a test TV show
        createTestShow(db);

        // We should now have one TV show
        assertEquals(1, db.count());

        // Create a test TV show
        createTestShow(db);

        // We should still only have one TV show
        assertEquals(1, db.count());
    }

    public void testInvalidTvShowCreation() {
        DbAdapterTvShows db = getAndResetDatabase();

        // There shouldn't be any TV shows to begin with
        assertEquals(0, db.count());

        // Create a test TV show
        db.createShow(DbAdapterTvShows.UNIDENTIFIED_ID, "title", "plot", "actors", "genres", "8.5", "PG", "100", "2014-12-04", "1");

        // We should still have zero TV shows
        assertEquals(0, db.count());
    }

    public void testShowExists() {
        DbAdapterTvShows db = getAndResetDatabase();

        // We haven't added a show yet, so it shouldn't exist
        assertEquals(false, db.showExists("", "title"));
        assertEquals(false, db.showExists("1234", ""));

        // Create a test TV show
        createTestShow(db);

        // Should all be good now
        assertEquals(true, db.showExists("", "title"));
        assertEquals(true, db.showExists("1234", ""));
    }

    public void testGetShowIdByTitle() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Nothing since we haven't created the TV show yet
        assertEquals("", db.getShowId("title"));

        // Create a test TV show
        createTestShow(db);

        // Should be there now
        assertEquals("1234", db.getShowId("title"));
    }

    public void testGetShowTitleById() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Nothing since we haven't created the TV show yet
        assertEquals("", db.getShowTitle("1234"));

        // Create a test TV show
        createTestShow(db);

        // Should be there now
        assertEquals("title", db.getShowTitle("1234"));
    }

    public void testGetShowById() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Nothing since we haven't created the TV show yet
        assertEquals(0, db.getShow("1234").getCount());

        // Create a test TV show
        createTestShow(db);

        // Should be there now
        assertEquals(1, db.getShow("1234").getCount());
    }

    public void testGetAllShows() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Nothing since we haven't created the TV show yet
        assertEquals(0, db.getAllShows().getCount());

        // Create a test TV show
        createTestShow(db);

        // Should be one now
        assertEquals(1, db.getAllShows().getCount());

        // Create another test TV show
        createTestShow2(db);

        // Should be two now
        assertEquals(2, db.getAllShows().getCount());
    }

    public void testDeleteShowById() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Create a few test TV shows
        createTestShow(db);
        createTestShow2(db);

        // Should be two now
        assertEquals(2, db.getAllShows().getCount());

        db.deleteShow("1234");

        // Should be one now
        assertEquals(1, db.getAllShows().getCount());

        db.deleteShow("12345");

        // Should be zero now
        assertEquals(0, db.getAllShows().getCount());
    }

    public void testDeleteAllShows() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Create a few test TV shows
        createTestShow(db);
        createTestShow2(db);

        // Should be two now
        assertEquals(2, db.getAllShows().getCount());

        db.deleteAllShowsInDatabase();

        // Should be zero now
        assertEquals(0, db.getAllShows().getCount());
    }

    public void testGetCertifications() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Create a few test TV shows
        createTestShow(db);
        createTestShow2(db);

        // Each had a unique certification, so
        // there should be two now
        assertEquals(2, db.getCertifications().size());

        assertEquals("PG", db.getCertifications().get(0));
        assertEquals("PG-13", db.getCertifications().get(1));
    }

    public void testEditTvShow() {
        DbAdapterTvShows db = getAndResetDatabase();

        // Create a test TV show
        createTestShow(db);

        db.editShow("1234", "new title", "new plot", "genres...", "200", "9.7", "1970-01-02", "PG-17");

        // Make sure we're still only dealing with one TV show
        assertEquals(1, db.count());

        // Title
        assertEquals("new title", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_TITLE));

        // Plot
        assertEquals("new plot", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_PLOT));

        // Genres
        assertEquals("genres...", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_GENRES));

        // Runtime
        assertEquals("200", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_RUNTIME));

        // Rating
        assertEquals("9.7", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_RATING));

        // Date
        assertEquals("1970-01-02", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE));

        // Certification
        assertEquals("PG-17", db.getSingleItem("1234", DbAdapterTvShows.KEY_SHOW_CERTIFICATION));
    }

    public void testCreateEpisode() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(1, dbEpisodes.getEpisodeCount("1234"));
    }

    public void testCreateEpisodeDuplication() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(1, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lul2z.mkv", "05",  "15", "1234", "episode title 2", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        // Still just one
        assertEquals(1, dbEpisodes.getEpisodeCount("1234"));
    }

    public void testGetEpisode() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisode("1234", 5, 15).getCount());

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(1, dbEpisodes.getEpisode("1234", 5, 15).getCount());
    }

    public void testGetEpisodesByShowId() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodes("1234").getCount());

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(1, dbEpisodes.getEpisodes("1234").getCount());

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodes("1234").getCount());
    }

    public void testGetAllEpisodes() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getAllEpisodes().getCount());

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(1, dbEpisodes.getAllEpisodes().getCount());

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getAllEpisodes().getCount());

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "12345", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(3, dbEpisodes.getAllEpisodes().getCount());
    }

    public void testDeleteEpisode() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodeCount("1234"));

        // Let's delete one of them
        TvShowDatabaseUtils.deleteEpisode(mContext, "1234", 5, 15);

        assertEquals(1, dbEpisodes.getEpisodeCount("1234"));

        // We've deleted the first, but should still have the second one
        assertEquals(1, dbEpisodes.getEpisode("1234", 6, 15).getCount());

        // Let's delete the last one
        TvShowDatabaseUtils.deleteEpisode(mContext, "1234", 6, 15);

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        // No more!
        assertEquals(0, dbEpisodes.getEpisode("1234", 6, 15).getCount());
    }

    public void testDeleteEpisodeAlternate() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodeCount("1234"));

        // Let's delete one of them
        dbEpisodes.deleteEpisode("1234", 5, 15);

        assertEquals(1, dbEpisodes.getEpisodeCount("1234"));

        // We've deleted the first, but should still have the second one
        assertEquals(1, dbEpisodes.getEpisode("1234", 6, 15).getCount());

        // Let's delete the last one
        dbEpisodes.deleteEpisode("1234", 6, 15);

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        // No more!
        assertEquals(0, dbEpisodes.getEpisode("1234", 6, 15).getCount());
    }

    public void testDeleteAllEpisodesByShowId() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.deleteAllEpisodes("1234");

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));
    }

    public void testDeleteSeason() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        // 5th season
        assertEquals(2, dbEpisodes.getEpisodesInSeason(mContext, "1234", 5).size());

        // 6th season
        assertEquals(1, dbEpisodes.getEpisodesInSeason(mContext, "1234", 6).size());

        dbEpisodes.deleteSeason("1234", 5);

        assertEquals(0, dbEpisodes.getEpisodesInSeason(mContext, "1234", 5).size());

        dbEpisodes.deleteSeason("1234", 6);

        assertEquals(0, dbEpisodes.getEpisodesInSeason(mContext, "1234", 6).size());
    }

    public void testDeleteAllEpisodes() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(3, dbEpisodes.getAllEpisodes().getCount());

        dbEpisodes.deleteAllEpisodes();

        assertEquals(0, dbEpisodes.getAllEpisodes().getCount());
    }

    public void testGetEpisodeCountByShowId() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodeCount("1234"));
    }

    public void testGetEpisodeCountForSeason() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodeCountForSeason("1234", "05"));
        assertEquals(0, dbEpisodes.getEpisodeCountForSeason("1234", "06"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05", "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodeCountForSeason("1234", "05"));
        assertEquals(1, dbEpisodes.getEpisodeCountForSeason("1234", "06"));
    }

    public void testGetSeasonCount() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getSeasonCount("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getSeasonCount("1234"));
    }

    public void testGetSeasons() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getSeasons("1234").size());

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getSeasons("1234").size());
    }

    public void testGetEpisodesInSeason() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals(0, dbEpisodes.getEpisodesInSeason(mContext, "1234", 5).size());
        assertEquals(0, dbEpisodes.getEpisodesInSeason(mContext, "1234", 6).size());

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulaz.mkv", "06",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(2, dbEpisodes.getEpisodesInSeason(mContext, "1234", 5).size());
        assertEquals(1, dbEpisodes.getEpisodesInSeason(mContext, "1234", 6).size());
    }

    public void testGetLatestEpisodeAirdateByShow() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        assertEquals("", dbEpisodes.getLatestEpisodeAirdate("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-07-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals("1980-07-07", dbEpisodes.getLatestEpisodeAirdate("1234"));
    }

    public void testHasUnwatchedEpisodes() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "1", "1");

        assertEquals(false, dbEpisodes.hasUnwatchedEpisodes("1234"));

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        assertEquals(true, dbEpisodes.hasUnwatchedEpisodes("1234"));
    }

    public void testSetSeasonWatchedStatus() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        dbEpisodes.createEpisode("/test/lulz.mkv", "05", "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        assertEquals("0", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
        assertEquals("0", dbEpisodes.getSingleItem("1234", "05", "16", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));

        dbEpisodes.setSeasonWatchStatus("1234", "05", true);

        assertEquals("1", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
        assertEquals("1", dbEpisodes.getSingleItem("1234", "05", "16", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
    }

    public void testSetEpisodeWatchedStatus() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "16", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        assertEquals("0", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
        assertEquals("0", dbEpisodes.getSingleItem("1234", "05", "16", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));

        dbEpisodes.setEpisodeWatchStatus("1234", "05", "15", true);
        dbEpisodes.setEpisodeWatchStatus("1234", "05", "16", true);

        assertEquals("1", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
        assertEquals("1", dbEpisodes.getSingleItem("1234", "05", "16", DbAdapterTvShowEpisodes.KEY_HAS_WATCHED));
    }

    public void testUpdateEpisode() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();

        dbEpisodes.createEpisode("/test/lulz.mkv", "05",  "15", "1234", "episode title", "episode plot",
                "1980-06-07", "7.6", "director", "writer", "guest stars", "0", "1");

        dbEpisodes.editEpisode("1234", 5, 15, "new episode title", "new episode plot", "new director", "new writer", "new guest stars", "6.7", "2000-06-07");

        assertEquals("new episode title", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE));
        assertEquals("new episode plot", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT));
        assertEquals("new director", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR));
        assertEquals("new writer", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER));
        assertEquals("new guest stars", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS));
        assertEquals("6.7", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_RATING));
        assertEquals("2000-06-07", dbEpisodes.getSingleItem("1234", "05", "15", DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE));
    }

    public void testCreateFilepathMapping() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(false, dbMappings.filepathExists("1234", "05", "15", "/test/lulz.mkv"));

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        assertEquals(true, dbMappings.filepathExists("1234", "05", "15", "/test/lulz.mkv"));
    }

    public void testCreateFilepathMappingDuplication() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.count());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        assertEquals(1, dbMappings.count());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        assertEquals(1, dbMappings.count());
    }

    public void testGetFirstFilepath() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals("", dbMappings.getFirstFilepath("1234", "05", "15"));

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "15");

        assertEquals("/test/lulz.mkv", dbMappings.getFirstFilepath("1234", "05", "15"));
    }

    public void testGetFilepathsForEpisode() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getFilepathsForEpisode("1234", "05", "15").size());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "15");

        assertEquals(2, dbMappings.getFilepathsForEpisode("1234", "05", "15").size());
    }

    public void testGetUnidentifiedFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllUnidentifiedFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", DbAdapterTvShows.UNIDENTIFIED_ID, "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", DbAdapterTvShows.UNIDENTIFIED_ID, "05", "16");

        assertEquals(2, dbMappings.getAllUnidentifiedFilepaths().getCount());
    }

    public void testGetAllFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");

        assertEquals(2, dbMappings.getAllFilepaths().getCount());
    }

    public void testGetInfoForFilepath() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepathInfo("/test/lulz.mkv").getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        assertEquals(1, dbMappings.getAllFilepathInfo("/test/lulz.mkv").getCount());

        Cursor c = dbMappings.getAllFilepathInfo("/test/lulz.mkv");
        c.moveToFirst();
        assertEquals("/test/lulz.mkv", c.getString(c.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));
    }

    public void testGetAllFilepathsForShow() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths("1234").getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");

        assertEquals(2, dbMappings.getAllFilepaths("1234").getCount());
        assertEquals(1, dbMappings.getAllFilepaths("12345").getCount());
    }

    public void testGetAllIgnoredFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllIgnoredFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");

        dbMappings.ignoreFilepath("/test/lulz.mkv");

        assertEquals(1, dbMappings.getAllIgnoredFilepaths().getCount());
    }

    public void testDeleteFilepathByFilepath() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");

        assertEquals(3, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteFilepath("/test/lulz.mkv");

        assertEquals(2, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteFilepath("/test/lulzfs.mkv");

        assertEquals(0, dbMappings.getAllFilepaths().getCount());
    }

    public void testIgnoreFilepath() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        dbMappings.ignoreFilepath("/test/lulz.mkv");

        Cursor c = dbMappings.getAllFilepathInfo("/test/lulz.mkv");
        c.moveToFirst();

        assertEquals("1", c.getString(c.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_IGNORED)));
    }

    public void testDeleteAllFilepathsByShowId() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");

        assertEquals(3, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteAllFilepaths("1234");

        assertEquals(1, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteAllFilepaths("12345");

        assertEquals(0, dbMappings.getAllFilepaths().getCount());
    }

    public void testDeleteAllFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");

        assertEquals(3, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteAllFilepaths();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());
    }

    public void testDeleteAllUnidentifiedFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(0, dbMappings.getAllFilepaths().getCount());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "12345", "05", "16");
        dbMappings.createFilepathMapping("/test/lulz.mkv", DbAdapterTvShows.UNIDENTIFIED_ID, "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", DbAdapterTvShows.UNIDENTIFIED_ID, "05", "16");

        assertEquals(5, dbMappings.getAllFilepaths().getCount());

        dbMappings.deleteAllUnidentifiedFilepaths();

        assertEquals(3, dbMappings.getAllFilepaths().getCount());
    }

    public void testHasMultipleFilepaths() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        assertEquals(false, dbMappings.hasMultipleFilepaths("1234", "05", "15"));

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");

        assertEquals(false, dbMappings.hasMultipleFilepaths("1234", "05", "15"));

        dbMappings.createFilepathMapping("/test/lulsz.mkv", "1234", "05", "15");

        assertEquals(true, dbMappings.hasMultipleFilepaths("1234", "05", "15"));
    }

    public void testRemoveSeason() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "06", "16");

        assertEquals(3, dbMappings.getAllFilepaths("1234").getCount());

        dbMappings.removeSeason("1234", 5);

        assertEquals(1, dbMappings.getAllFilepaths("1234").getCount());

        dbMappings.removeSeason("1234", 6);

        assertEquals(0, dbMappings.getAllFilepaths("1234").getCount());
    }

    public void testIgnoreSeason() {
        getAndResetDatabase();

        DbAdapterTvShowEpisodeMappings dbMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234", "05", "15");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "05", "16");
        dbMappings.createFilepathMapping("/test/lulzfs.mkv", "1234", "06", "16");

        assertEquals(0, dbMappings.getAllIgnoredFilepaths().getCount());

        dbMappings.ignoreSeason("1234", 5);

        assertEquals(2, dbMappings.getAllIgnoredFilepaths().getCount());

        dbMappings.ignoreSeason("1234", 6);

        assertEquals(3, dbMappings.getAllIgnoredFilepaths().getCount());
    }

    /**
     * Creates a test TV show in the database.
     * @param db
     */
    private void createTestShow(DbAdapterTvShows db) {
        db.createShow("1234", "title", "plot", "actors", "genres", "8.5", "PG", "100", "2014-12-04", "1");
    }

    /**
     * Creates a test TV show in the database.
     * @param db
     */
    private void createTestShow2(DbAdapterTvShows db) {
        db.createShow("12345", "title", "plot", "actors", "genres", "8.5", "PG-13", "100", "2014-12-04", "1");
    }

    /**
     * Get a database instance and reset it before the test begins.
     * @return
     */
    private DbAdapterTvShows getAndResetDatabase() {
        // Ensures that we've got an application context, which
        // is required in order to use the MizuuApplication methods.
        getInstrumentation().waitForIdleSync();

        mContext = getInstrumentation().getTargetContext().getApplicationContext();

        DbAdapterTvShows db = MizuuApplication.getTvDbAdapter();
        resetDatabase(db);
        return db;
    }

    /**
     * Resets the TV shows database by deleting all shows,
     * episodes and filepath mappings.
     * @param db
     */
    private void resetDatabase(DbAdapterTvShows db) {
        TvShowDatabaseUtils.deleteAllTvShows(mContext);

        // Test TV show count
        assertEquals(0, db.count());

        // Test episode count
        DbAdapterTvShowEpisodes dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();
        assertEquals(0, dbEpisodes.count());

        // Test filepath mapping count
        DbAdapterTvShowEpisodeMappings dbEpisodeMappings = MizuuApplication.getTvShowEpisodeMappingsDbAdapter();
        assertEquals(0, dbEpisodeMappings.count());
    }
}