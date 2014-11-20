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
import android.test.InstrumentationTestCase;

import com.miz.db.DbAdapterCollections;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.MovieDatabaseUtils;

/**
 * Tests various movie database queries.
 * Do not use this on a non-debug build, as
 * it will remove all data.
 */
public class DatabaseMovieTests extends InstrumentationTestCase {

    private Context mContext;

    /**
     * Tests if it's possible to create a movie.
     */
    public void testMovieCreation() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
       assertEquals(1, db.count());
    }

    /**
     * Tests if it's possible to delete a movie.
     */
    public void testMovieDeletion() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // Delete the test movie
        MovieDatabaseUtils.deleteMovie(mContext, "1234");

        // We've just deleted the movie, so there should be zero now
        assertEquals(0, db.count());
    }

    /**
     * Tests if it's possible to delete all movies in the database.
     */
    public void testDeleteAllMovies() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create two test movies
        createTwoTestMovies(db);

        // We've just created two movie, so there should be two now
        assertEquals(2, db.count());

        // Delete all movies
        MovieDatabaseUtils.deleteAllMovies(mContext);

        // We've just deleted all movies, so there should be zero now
        assertEquals(0, db.count());

        // Test collection count
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();
        assertEquals(0, dbCollections.count());

        // Test filepath mapping count
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();
        assertEquals(0, dbMovieMappings.count());

        // There should be no files left to list in the movie thumbs folder
        assertEquals(0, MizuuApplication.getMovieThumbFolder(mContext).listFiles().length);

        // There should be no files left to list in the movie backdrops folder
        assertEquals(0, MizuuApplication.getMovieBackdropFolder(mContext).listFiles().length);
    }

    /**
     * Tests if it's possible to create a movie twice. Hint: It shouldn't be.
     */
    public void testMovieDuplication() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // Create the test movie again
        createTestMovie(db);

        // There should still just be one movie in the database, since it
        // already existed when we tried to create the movie the second time.
        assertEquals(1, db.count());
    }

    /**
     * Tests if it's possible to create a movie with an invalid movie ID.
     */
    public void testInvalidMovieCreation() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create a movie with an unidentified ID
        db.createMovie(DbAdapterMovies.UNIDENTIFIED_ID, "title", "plot", "tt1234567", "7.9", "tagline", "1970-01-01", "PG-13", "90", "http://youtube.com", "genres", "1", "actors", "collection", "collectionId", "1", "1", "123456789");

        // There should still be zero movies, since we tried to add
        // a movie with an unidentified ID
        assertEquals(0, db.count());
    }

    /**
     * Tests if it's possible to create a movie collection.
     */
    public void testMovieCollectionCreation() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // We've just created a movie with a collection ID, so there should be one now
        assertEquals(true, MizuuApplication.getCollectionsAdapter().collectionExists("collectionId"));
    }

    /**
     * Tests if it's possible to ignore a movie.
     */
    public void testIgnoreMovie() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        DbAdapterMovieMappings dbMappings = MizuuApplication.getMovieMappingAdapter();
        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // We've just created a filepath mapping for the movie, so there should be one now
        assertEquals(1, dbMappings.getMovieFilepaths("1234").size());

        // Ignore the movie based on its movie ID
        MovieDatabaseUtils.ignoreMovie("1234");

        // We've just ignored the movie, hence removing it from the movie database
        assertEquals(0, db.count());

        // We've just ignored all filepath mappings for the movie, so there should be zero now
        assertEquals(0, dbMappings.getMovieFilepaths("1234").size());
    }

    /**
     * Tests if it's possible to update the contents of an existing movie
     * while keeping favourite, watchlist and watched status data intact.
     */
    public void testUpdateMovie() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        db.createOrUpdateMovie("1234", "new title", "new plot", "tt12345678", "9.7", "new tagline", "1970-01-02", "PG-17", "900", "youtube.com", "genres...", "0", "new actors", "collectionName", "collID", "0", "0", "123456");

        // Make sure that there's still just one movie
        assertEquals(1, db.count());

        // Make sure that the title has been updated
        assertEquals("new title", db.getSingleItem("1234", DbAdapterMovies.KEY_TITLE));

        // ... plot
        assertEquals("new plot", db.getSingleItem("1234", DbAdapterMovies.KEY_PLOT));

        // ... IMDB ID
        assertEquals("tt12345678", db.getSingleItem("1234", DbAdapterMovies.KEY_IMDB_ID));

        // ... Rating
        assertEquals("9.7", db.getSingleItem("1234", DbAdapterMovies.KEY_RATING));

        // ... Tagline
        assertEquals("new tagline", db.getSingleItem("1234", DbAdapterMovies.KEY_TAGLINE));

        // ... Release date
        assertEquals("1970-01-02", db.getSingleItem("1234", DbAdapterMovies.KEY_RELEASEDATE));

        // ... Certification
        assertEquals("PG-17", db.getSingleItem("1234", DbAdapterMovies.KEY_CERTIFICATION));

        // ... Runtime
        assertEquals("900", db.getSingleItem("1234", DbAdapterMovies.KEY_RUNTIME));

        // ... Trailer
        assertEquals("youtube.com", db.getSingleItem("1234", DbAdapterMovies.KEY_TRAILER));

        // ... Genres
        assertEquals("genres...", db.getSingleItem("1234", DbAdapterMovies.KEY_GENRES));

        // ... Favorite - this should NOT be changed
        assertEquals("1", db.getSingleItem("1234", DbAdapterMovies.KEY_FAVOURITE));

        // ... Actors
        assertEquals("new actors", db.getSingleItem("1234", DbAdapterMovies.KEY_ACTORS));

        // ... Collection ID
        assertEquals("collID", db.getSingleItem("1234", DbAdapterMovies.KEY_COLLECTION_ID));

        // ... To watch - this should NOT be changed
        assertEquals("0", db.getSingleItem("1234", DbAdapterMovies.KEY_TO_WATCH));

        // ... Has watched - this should NOT be changed
        assertEquals("1", db.getSingleItem("1234", DbAdapterMovies.KEY_HAS_WATCHED));

        // ... Date added
        assertEquals("123456", db.getSingleItem("1234", DbAdapterMovies.KEY_DATE_ADDED));

        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();
        assertEquals("collectionName", dbCollections.getCollection("collID"));
    }

    /**
     * Tests if it's possible to edit the contents of an existing movie
     * while keeping favourite, watchlist and watched status data intact.
     */
    public void testEditMovie() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        db.editMovie("1234", "new title", "new tagline", "new plot", "genres...", "900", "9.7", "1970-01-02", "PG-17");

        // Make sure that there's still just one movie
        assertEquals(1, db.count());

        // Make sure that the title has been updated
        assertEquals("new title", db.getSingleItem("1234", DbAdapterMovies.KEY_TITLE));

        // ... plot
        assertEquals("new plot", db.getSingleItem("1234", DbAdapterMovies.KEY_PLOT));

        // ... Rating
        assertEquals("9.7", db.getSingleItem("1234", DbAdapterMovies.KEY_RATING));

        // ... Tagline
        assertEquals("new tagline", db.getSingleItem("1234", DbAdapterMovies.KEY_TAGLINE));

        // ... Release date
        assertEquals("1970-01-02", db.getSingleItem("1234", DbAdapterMovies.KEY_RELEASEDATE));

        // ... Certification
        assertEquals("PG-17", db.getSingleItem("1234", DbAdapterMovies.KEY_CERTIFICATION));

        // ... Runtime
        assertEquals("900", db.getSingleItem("1234", DbAdapterMovies.KEY_RUNTIME));

        // ... Genres
        assertEquals("genres...", db.getSingleItem("1234", DbAdapterMovies.KEY_GENRES));
    }

    /**
     * Tests if it's possible to create a movie
     * and map multiple filepaths to it.
     */
    public void testMultipleFilepathMappings() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        DbAdapterMovieMappings dbMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added any filepaths yet, so there shouldn't be any
        assertEquals(0, dbMappings.getMovieFilepaths("1234").size());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // We have just added a filepath, so there should be one now
        assertEquals(1, dbMappings.getMovieFilepaths("1234").size());

        dbMappings.createFilepathMapping("/test/lulz_2.mkv", "1234");

        // We have just added another filepath, so there should be two now
        assertEquals(2, dbMappings.getMovieFilepaths("1234").size());
    }

    /**
     * Tests if it's possible to create a movie
     * and map the same filepath to it multiple times.
     * Hint: It shouldn't be possible.
     */
    public void testMultipleIdenticalFilepathMappings() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        DbAdapterMovieMappings dbMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added any filepaths yet, so there shouldn't be any
        assertEquals(0, dbMappings.getMovieFilepaths("1234").size());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // We have just added a filepath, so there should be one now
        assertEquals(1, dbMappings.getMovieFilepaths("1234").size());

        dbMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // We have just added the exact same filepath, which should be
        // ignored, so there should still just be one filepath
        assertEquals(1, dbMappings.getMovieFilepaths("1234").size());
    }

    /**
     * Tests if it's possible to query a collection ID
     * based on a movie ID.
     */
    public void testGetCollectionId() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // Check that it's the correct collection ID
        assertEquals("collectionId", db.getCollectionId("1234"));
    }

    public void testMovieExists() {
        DbAdapterMovies db = getAndResetDatabase();

        // We don't expect the movie to exist
        assertEquals(false, db.movieExists("1234"));

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so it should exist
        assertEquals(true, db.movieExists("1234"));
    }

    public void testCountWatchlist() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.countWatchlist());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie that's not on
        // the watchlist, so we still expect zero movies
        assertEquals(0, db.countWatchlist());

        // Create two test movies, where one
        // is on the watchlist
        createTwoTestMovies(db);

        // There should now be one movie on the watchlist
        assertEquals(1, db.countWatchlist());

        // ... and two movies in total
        assertEquals(2, db.count());

        // Put the first movie on the watchlist
        db.updateMovieSingleItem("1234", DbAdapterMovies.KEY_TO_WATCH, "1");

        // There should now be two movies on the watchlist
        assertEquals(2, db.countWatchlist());
    }

    public void testGetCertifications() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Each movie had a unique certification,
        // so there should be a total of 2 certifications.
        assertEquals(2, db.getCertifications().size());

        // The first... PG-13
        assertEquals("PG-13", db.getCertifications().get(0));

        // The second... PG-17
        assertEquals("PG-17", db.getCertifications().get(1));
    }

    /**
     * Tests if it's possible to delete
     * all movie collections.
     */
    public void testDeleteAllCollections() {
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // ... and zero collections
        assertEquals(0, dbCollections.count());

        // Create test movies
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // ... and two collections
        assertEquals(2, dbCollections.count());

        // Delete all collections
        dbCollections.deleteAllCollections();

        // Back to zero
        assertEquals(0, dbCollections.count());
    }

    /**
     * Tests if it's possible to create a movie
     * collection twice. Hint: It shouldn't be.
     */
    public void testCollectionDuplication() {
        DbAdapterMovies db = getAndResetDatabase();
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // ... and zero collections
        assertEquals(0, dbCollections.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // ... and one collection
        assertEquals(1, dbCollections.count());

        // Create test movie again
        createTestMovie(db);

        // Still one of each
        assertEquals(1, db.count());
        assertEquals(1, dbCollections.count());

        // Try to manually create a collection
        // using an existing collection ID
        dbCollections.createCollection("1234", "collectionId", "something, something");

        // Nope, still just one
        assertEquals(1, dbCollections.count());
    }

    public void testDeleteCollection() {
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // ... and zero collections
        assertEquals(0, dbCollections.count());

        // Create test movies
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // ... and two collections
        assertEquals(2, dbCollections.count());

        // Delete one of the collections
        dbCollections.deleteCollection("collectionId");

        // Back to one
        assertEquals(1, dbCollections.count());

        // Delete the remaining collection
        dbCollections.deleteCollection("collectionId2");

        // Back to zero
        assertEquals(0, dbCollections.count());
    }

    /**
     * Tests if a collection exists.
     */
    public void testCollectionExists() {
        DbAdapterMovies db = getAndResetDatabase();
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // ... and zero collections
        assertEquals(0, dbCollections.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // ... and one collection
        assertEquals(1, dbCollections.count());

        // Check if it exists
        assertEquals(true, dbCollections.collectionExists("collectionId"));

        // Make sure we're not getting a false positive
        assertEquals(false, dbCollections.collectionExists("lulz"));
    }

    /**
     * Tests if it's possible to get the number of
     * movies mapped to a given collection.
     */
    public void testMovieCountForCollection() {
        DbAdapterMovies db = getAndResetDatabase();
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();

        // We expect zero movies to begin with
        assertEquals(0, db.count());
        assertEquals(0, dbCollections.getMovieCount("collectionId"));

        // Create test movie
        createTestMovie(db);

        // There should be one now
        assertEquals(1, db.count());
        assertEquals(1, dbCollections.getMovieCount("collectionId"));
    }

    /**
     * Tests if it's possible to delete all unidentified movies in the database.
     */
    public void testDeleteAllUnidentified() {
        getAndResetDatabase();

        // Test filepath mapping count
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();
        assertEquals(0, dbMovieMappings.count());

        // Create an unidentified mapping
        dbMovieMappings.createFilepathMapping("/yo/lulz.mkv", DbAdapterMovies.UNIDENTIFIED_ID);

        // Make sure it's created
        assertEquals(1, dbMovieMappings.count());

        // Create another unidentified mapping
        dbMovieMappings.createFilepathMapping("/yo/lulzz.mkv", DbAdapterMovies.UNIDENTIFIED_ID);

        // Make sure it's created
        assertEquals(2, dbMovieMappings.count());

        // Create a third unidentified mapping
        dbMovieMappings.createFilepathMapping("/yo/lulzzz.mkv", DbAdapterMovies.UNIDENTIFIED_ID);

        // Make sure it's created
        assertEquals(3, dbMovieMappings.count());

        // Finally, let's create a mapping for an actual movie ID
        dbMovieMappings.createFilepathMapping("/yo/lulzzzz.mkv", "1234");

        // Make sure it's created
        assertEquals(4, dbMovieMappings.count());

        // Delete all unidentified filepaths
        dbMovieMappings.deleteAllUnidentifiedFilepaths();

        // Since we have added three unidentified mappings and one
        // correct mapping we should be back at one now.
        assertEquals(1, dbMovieMappings.count());
    }

    /**
     * Test if it's possible to update a filepath movie mapping.
     */
    public void testUpdateTmdbId() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // Make sure that there aren't any filepaths linked to begin with
        assertEquals(0, dbMovieMappings.getMovieFilepaths("1234").size());

        // Create a filepath
        dbMovieMappings.createFilepathMapping("/test/yo.mkv", "1234");

        // We should have one now
        assertEquals(1, dbMovieMappings.getMovieFilepaths("1234").size());

        // We're going to update the filepath mapping to another movie.
        // Let's make sure that other movie doesn't have any mappings to begin with.
        assertEquals(0, dbMovieMappings.getMovieFilepaths("12345").size());

        // Let's update
        dbMovieMappings.updateTmdbId("/test/yo.mkv", "1234", "12345");

        // We should have one now
        assertEquals(1, dbMovieMappings.getMovieFilepaths("12345").size());

        // ... and zero for the old one
        // We should have one now
        assertEquals(0, dbMovieMappings.getMovieFilepaths("1234").size());
    }

    /**
     * Tests if it's possible to check if a
     * movie has a filepath mapping.
     */
    public void testMovieMappingExists() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added a filepath mapping yet, so we don't expect it to exist
        assertEquals(false, dbMovieMappings.exists("1234"));

        // Add the filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // It should exist now
        assertEquals(true, dbMovieMappings.exists("1234"));
    }

    /**
     * Test if it's possible to determine if a a filepath
     * has been mapped to a movie by its movie ID.
     */
    public void testFilepathExists() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added a filepath mapping yet, so we don't expect it to exist
        assertEquals(false, dbMovieMappings.filepathExists("1234", "/test/lulz.mkv"));

        // Add the filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // It should exist now
        assertEquals(true, dbMovieMappings.filepathExists("1234", "/test/lulz.mkv"));
    }

    /**
     * Test if it's possible to delete all filepaths
     * mapped to a movie, including
     */
    public void testDeleteAllFilepathsMappedToMovie() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create two test movies
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added any filepath mappings for the movies yet
        assertEquals(0, dbMovieMappings.count());

        // Add a filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // Add another filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulzz.mkv", "1234");

        // Add a filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz_1.mkv", "12345");

        // Add another filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulzz_1.mkv", "12345");

        // We have added a total of four filepath mappings
        assertEquals(4, dbMovieMappings.count());

        // Delete all filepaths for a movie
        dbMovieMappings.deleteMovie("1234");

        // Back at 2
        assertEquals(2, dbMovieMappings.count());

        // Delete all filepaths for the second movie
        dbMovieMappings.deleteMovie("12345");

        // Back at zero
        assertEquals(0, dbMovieMappings.count());

        // Let's make sure it works with unidentified ID as well
        dbMovieMappings.createFilepathMapping("/test/wat.mkv", DbAdapterMovies.UNIDENTIFIED_ID);

        // At one
        assertEquals(1, dbMovieMappings.count());

        // Delete all unidentified filepaths
        dbMovieMappings.deleteAllUnidentifiedFilepaths();

        // Back at zero
        assertEquals(0, dbMovieMappings.count());
    }

    /**
     * Tests if it's possible to delete all filepaths, including those
     * marked with an unidentified ID.
     */
    public void testDeleteAllMovieFilepaths() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create two test movies
        createTwoTestMovies(db);

        // We've just created two movies, so there should be two now
        assertEquals(2, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added any filepath mappings for the movies yet
        assertEquals(0, dbMovieMappings.count());

        // Add a filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // Add another filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulzz.mkv", "1234");

        // Add a filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz_1.mkv", "12345");

        // Add another filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulzz_1.mkv", "12345");

        // We have added a total of four filepath mappings
        assertEquals(4, dbMovieMappings.count());

        // Delete them all
        dbMovieMappings.deleteAllMovies();

        // Zero, plz!
        assertEquals(0, dbMovieMappings.count());

        // Let's make sure it works with unidentified ID as well
        dbMovieMappings.createFilepathMapping("/test/wat.mkv", DbAdapterMovies.UNIDENTIFIED_ID);

        // There should be one now
        assertEquals(1, dbMovieMappings.count());

        // Delete them all (well, hopefully just one)
        dbMovieMappings.deleteAllMovies();

        // Again... Zero, plz!
        assertEquals(0, dbMovieMappings.count());
    }

    /**
     * Test if it's possible to get all filepaths
     * mapped to a movie ID.
     */
    public void testMovieFilepathsByMovieId() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // We haven't added any filepath mappings for the movie yet
        assertEquals(0, dbMovieMappings.getMovieFilepaths("1234").size());

        // Add a filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // Add another filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulzz.mkv", "1234");

        // We've added two filepath mappings
        assertEquals(2, dbMovieMappings.getMovieFilepaths("1234").size());

        // Check the first one...
        assertEquals("/test/lulz.mkv", dbMovieMappings.getMovieFilepaths("1234").get(0));

        // And the second one...
        assertEquals("/test/lulzz.mkv", dbMovieMappings.getMovieFilepaths("1234").get(1));
    }

    /**
     * Test if it's possible to get a movie ID based on a filepath.
     */
    public void testMovieIdByFilepath() {
        DbAdapterMovies db = getAndResetDatabase();

        // We expect zero movies to begin with
        assertEquals(0, db.count());

        // Create test movie
        createTestMovie(db);

        // We've just created a movie, so there should be one now
        assertEquals(1, db.count());

        // Get the DB adapter for movie mappings
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();

        // There shouldn't be any
        assertEquals("", dbMovieMappings.getIdForFilepath("/test/lulz.mkv"));

        // Add the filepath mapping
        dbMovieMappings.createFilepathMapping("/test/lulz.mkv", "1234");

        // It should exist now
        assertEquals("1234", dbMovieMappings.getIdForFilepath("/test/lulz.mkv"));
    }

    /**
     * Get a database instance and reset it before the test begins.
     * @return
     */
    private DbAdapterMovies getAndResetDatabase() {
        // Ensures that we've got an application context, which
        // is required in order to use the MizuuApplication methods.
        getInstrumentation().waitForIdleSync();

        mContext = getInstrumentation().getTargetContext().getApplicationContext();

        DbAdapterMovies db = MizuuApplication.getMovieAdapter();
        resetDatabase(db);
        return db;
    }

    /**
     * Resets the movie database by deleting all movies,
     * collections and filepath mappings.
     * @param db
     */
    private void resetDatabase(DbAdapterMovies db) {
        MovieDatabaseUtils.deleteAllMovies(mContext);

        // Test movie count
        assertEquals(0, db.count());

        // Test collection count
        DbAdapterCollections dbCollections = MizuuApplication.getCollectionsAdapter();
        assertEquals(0, dbCollections.count());

        // Test filepath mapping count
        DbAdapterMovieMappings dbMovieMappings = MizuuApplication.getMovieMappingAdapter();
        assertEquals(0, dbMovieMappings.count());
    }

    /**
     * Creates a test movie in the database.
     * @param db
     */
    private void createTestMovie(DbAdapterMovies db) {
        // Create test movie
        db.createMovie("1234", "title", "plot", "tt1234567", "7.9", "tagline", "1970-01-01", "PG-13", "90", "http://youtube.com", "genres", "1", "actors", "collection", "collectionId", "0", "1", "123456789");
    }

    /**
     * Creates two test movies in the database.
     * @param db
     */
    private void createTwoTestMovies(DbAdapterMovies db) {
        // Create test movie
        db.createMovie("1234", "title", "plot", "tt1234567", "7.9", "tagline", "1970-01-01", "PG-13", "90", "http://youtube.com", "genres", "1", "actors", "collection", "collectionId", "0", "1", "123456789");
        db.createMovie("12345", "title", "plot", "tt1234567", "7.9", "tagline", "1970-01-01", "PG-17", "90", "http://youtube.com", "genres", "1", "actors", "collection2", "collectionId2", "1", "1", "123456789");
    }
}