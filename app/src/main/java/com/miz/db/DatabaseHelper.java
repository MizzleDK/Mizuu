package com.miz.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

@SuppressLint("NewApi")
public class DatabaseHelper extends SQLiteOpenHelper {

	protected static final String TAG = "Mizuu";

	public static final String DATABASE_NAME = "mizuu_data";
	protected static final int DATABASE_VERSION = 5;

	/**
	 * Create movie table SQL statement
	 */
	private static final String DATABASE_CREATE_MOVIE = "create table movie (tmdbid TEXT PRIMARY KEY, title TEXT, plot TEXT," +
			"imdbid TEXT, rating TEXT, tagline TEXT, release TEXT, certification TEXT, runtime TEXT, trailer TEXT, genres TEXT," +
			"favourite INTEGER, actors TEXT, to_watch TEXT, has_watched TEXT, date_added TEXT, collection_id TEXT);";
	private static final String DATABASE_CREATE_MOVIE_TITLE_INDEX = "create index movie_title_index on " + DbAdapterMovies.DATABASE_TABLE +
			" (" + DbAdapterMovies.KEY_TITLE + ");";
	
	/**
	 * Create movie filepath table SQL statements
	 */
	private static final String DATABASE_CREATE_MOVIE_MAPPING = "create table " + DbAdapterMovieMappings.DATABASE_TABLE + " (" +
			DbAdapterMovieMappings.KEY_FILEPATH + " TEXT, " + DbAdapterMovieMappings.KEY_TMDB_ID + " TEXT, " + DbAdapterMovieMappings.KEY_IGNORED + " INTEGER);";
	private static final String DATABASE_CREATE_TMDB_ID_INDEX = "create index tmdbid_index on " + DbAdapterMovieMappings.DATABASE_TABLE +
			" (" + DbAdapterMovieMappings.KEY_TMDB_ID + ");";

	/**
	 * Create collections table SQL statements
	 */
	private static final String DATABASE_CREATE_MOVIE_COLLECTIONS = "create table " + DbAdapterCollections.DATABASE_TABLE +
			" (" + DbAdapterCollections.KEY_COLLECTION_ID + " TEXT PRIMARY KEY, " + DbAdapterCollections.KEY_COLLECTION + " TEXT);";

	/**
	 * Create TV show table SQL statements
	 */
	private static final String DATABASE_CREATE_TV_SHOWS = "create table tvshows (_id INTEGER PRIMARY KEY AUTOINCREMENT, show_id TEXT," +
			"show_title TEXT, show_description TEXT, show_actors TEXT, show_genres TEXT, show_rating TEXT, show_certification TEXT," +
			"show_runtime TEXT, show_first_airdate TEXT, favourite TEXT);";
	private static final String DATABASE_CREATE_SHOW_ID_INDEX = "create index show_id_index on tvshows (show_id);";

	/**
	 * Create TV show episodes table SQL statements
	 */
	private static final String DATABASE_CREATE_TV_SHOWS_EPISODES = "create table tvshow_episodes (season TEXT, episode TEXT, show_id TEXT," +
			"episode_title TEXT, episode_description TEXT, episode_airdate TEXT, episode_rating TEXT, episode_director TEXT, episode_writer TEXT," +
			"episode_gueststars TEXT, date_added TEXT, to_watch TEXT, has_watched TEXT, favourite TEXT);";
	private static final String DATABASE_CREATE_EPISODE_SHOW_ID_INDEX = "create index episode_show_id_index on tvshow_episodes(show_id);";

	/**
	 * Create TV show episodes filepath table SQL statements
	 */
	private static final String DATABASE_CREATE_TV_SHOWS_EPISODES_MAPPING = "create table " + DbAdapterTvShowEpisodeMappings.DATABASE_TABLE + " (" +
			DbAdapterTvShowEpisodeMappings.KEY_FILEPATH + " TEXT, " + DbAdapterTvShowEpisodeMappings.KEY_SHOW_ID + " TEXT, " +
			DbAdapterTvShowEpisodeMappings.KEY_SEASON + " TEXT, " + DbAdapterTvShowEpisodeMappings.KEY_EPISODE + " TEXT, " +
			DbAdapterTvShowEpisodeMappings.KEY_IGNORED + " INTEGER);";

	/**
	 * Create file sources table SQL statement
	 */
	private static final String DATABASE_CREATE_FILESOURCES = "create table sources (_id INTEGER PRIMARY KEY AUTOINCREMENT, filepath TEXT," +
			"type TEXT, is_smb INTEGER, user TEXT, password TEXT, domain TEXT);";

	private static DatabaseHelper mInstance;

	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized DatabaseHelper getHelper(Context context) {
		if (mInstance == null)
			mInstance = new DatabaseHelper(context);
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		// Movie table and index
		database.execSQL(DATABASE_CREATE_MOVIE);
		database.execSQL(DATABASE_CREATE_MOVIE_TITLE_INDEX);

		// Movie filepath mapping table and index
		database.execSQL(DATABASE_CREATE_MOVIE_MAPPING);
		database.execSQL(DATABASE_CREATE_TMDB_ID_INDEX);

		// Movie collections
		database.execSQL(DATABASE_CREATE_MOVIE_COLLECTIONS);

		// TV show table and index
		database.execSQL(DATABASE_CREATE_TV_SHOWS);
		database.execSQL(DATABASE_CREATE_SHOW_ID_INDEX);

		// TV show episode table and index
		database.execSQL(DATABASE_CREATE_TV_SHOWS_EPISODES);
		database.execSQL(DATABASE_CREATE_EPISODE_SHOW_ID_INDEX);

		// TV show episode filepath table
		database.execSQL(DATABASE_CREATE_TV_SHOWS_EPISODES_MAPPING);

		// File source table
		database.execSQL(DATABASE_CREATE_FILESOURCES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.d("Mizuu", "Upgrading database...");

		if (oldVersion == 4) { // Upgrade to the new database structure

			// Upgrade the movie table first
			upgradeMovieTable(database);

			// Move TV show database and TV show episodes database to mizuu_data database
			moveTvShowDb(MizuuApplication.getContext(), database);

			// Move file sources database to mizuu_data database
			moveFileSourcesDb(MizuuApplication.getContext(), database);
		} else {
			database.execSQL("DROP TABLE IF EXISTS movie");
			onCreate(database);
		}
	}

	private void upgradeMovieTable(SQLiteDatabase database) {
		// ArrayList to store the old database values
		ArrayList<ContentValues> mOldValues = new ArrayList<ContentValues>();

		// Old database table columns
		String[] oldColumns = new String[] {"filepath", "coverpath", "title", "plot", "tmdbid", "imdbid", "rating", "tagline", "release", "certification",
				"runtime", "trailer", "genres", "favourite", "watched" /* cast / actors */, "collection", "to_watch", "has_watched",
				"extra1" /* date added */, "extra2" /* collection ID */};

		Cursor cursor = database.query(DbAdapterMovies.DATABASE_TABLE, oldColumns, null, null, null, null, null);

		// Let's load all the old data
		try {
			while (cursor.moveToNext()) {
				// Fetch it
				String filepath = cursor.getString(cursor.getColumnIndex("filepath"));
				String title = cursor.getString(cursor.getColumnIndex("title"));
				String plot = cursor.getString(cursor.getColumnIndex("plot"));
				String tmdbId = cursor.getString(cursor.getColumnIndex("tmdbid"));
				String imdbId = cursor.getString(cursor.getColumnIndex("imdbid"));
				String rating = cursor.getString(cursor.getColumnIndex("rating"));
				String tagline = cursor.getString(cursor.getColumnIndex("tagline"));
				String releaseDate = cursor.getString(cursor.getColumnIndex("release"));
				String certification = cursor.getString(cursor.getColumnIndex("certification"));
				String runtime = cursor.getString(cursor.getColumnIndex("runtime"));
				String trailer = cursor.getString(cursor.getColumnIndex("trailer"));
				String genres = cursor.getString(cursor.getColumnIndex("genres"));
				String favourite = cursor.getString(cursor.getColumnIndex("favourite"));
				String cast = cursor.getString(cursor.getColumnIndex("watched")); // a mistake in previous version of the database structure
				String collection = cursor.getString(cursor.getColumnIndex("collection"));
				String collectionId = cursor.getString(cursor.getColumnIndex("extra2"));
				String toWatch = cursor.getString(cursor.getColumnIndex("to_watch"));
				String hasWatched = cursor.getString(cursor.getColumnIndex("has_watched"));
				String dateAdded = cursor.getString(cursor.getColumnIndex("extra1"));

				// Add it to a ContentValues object
				ContentValues values = new ContentValues();
				values.put(DbAdapterMovieMappings.KEY_FILEPATH, filepath);
				values.put(DbAdapterMovies.KEY_TMDB_ID, tmdbId);
				values.put(DbAdapterMovies.KEY_TITLE, title);
				values.put(DbAdapterMovies.KEY_PLOT, plot);
				values.put(DbAdapterMovies.KEY_IMDB_ID, imdbId);
				values.put(DbAdapterMovies.KEY_RATING, rating);
				values.put(DbAdapterMovies.KEY_TAGLINE, tagline);
				values.put(DbAdapterMovies.KEY_RELEASEDATE, releaseDate);
				values.put(DbAdapterMovies.KEY_CERTIFICATION, certification);
				values.put(DbAdapterMovies.KEY_RUNTIME, runtime);
				values.put(DbAdapterMovies.KEY_TRAILER, trailer);
				values.put(DbAdapterMovies.KEY_GENRES, genres);
				values.put(DbAdapterMovies.KEY_FAVOURITE, favourite);
				values.put(DbAdapterMovies.KEY_ACTORS, cast);
				values.put(DbAdapterCollections.KEY_COLLECTION, collection);
				values.put(DbAdapterMovies.KEY_TO_WATCH, toWatch);
				values.put(DbAdapterMovies.KEY_HAS_WATCHED, hasWatched);
				values.put(DbAdapterMovies.KEY_DATE_ADDED, dateAdded);
				values.put(DbAdapterMovies.KEY_COLLECTION_ID, collectionId);

				// Add it to the array
				mOldValues.add(values);
			}
		} catch (Exception e) {
		} finally {
			// Close the cursor
			if (cursor != null)
				cursor.close();
		}

		// We have a copy of all the old data - now let's drop the old table...
		database.execSQL("DROP TABLE IF EXISTS movie");

		// ... and create the new one and index
		database.execSQL(DATABASE_CREATE_MOVIE);
		database.execSQL(DATABASE_CREATE_MOVIE_TITLE_INDEX);

		// Also create movie filepath mapping table and index
		database.execSQL(DATABASE_CREATE_MOVIE_MAPPING);
		database.execSQL(DATABASE_CREATE_TMDB_ID_INDEX);

		// And also create collections table
		database.execSQL(DATABASE_CREATE_MOVIE_COLLECTIONS);

		// Create a Set of Strings to keep track of which
		// collections have been added - without querying the DB
		HashSet<String> collectionsAdded = new HashSet<String>();

		// ... and add data to it and the filepath mapping database
		for (ContentValues values : mOldValues) {

			// Check if movie exists - if not, create it
			if (!movieExists(database, values.getAsString(DbAdapterMovies.KEY_TMDB_ID))) {

				ContentValues contentValues = new ContentValues();
				contentValues.put(DbAdapterMovies.KEY_TMDB_ID, values.getAsString(DbAdapterMovies.KEY_TMDB_ID).isEmpty() ? values.getAsString(DbAdapterMovieMappings.KEY_FILEPATH) : values.getAsString(DbAdapterMovies.KEY_TMDB_ID));
				contentValues.put(DbAdapterMovies.KEY_TITLE, values.getAsString(DbAdapterMovies.KEY_TITLE));
				contentValues.put(DbAdapterMovies.KEY_PLOT, values.getAsString(DbAdapterMovies.KEY_PLOT));
				contentValues.put(DbAdapterMovies.KEY_IMDB_ID, values.getAsString(DbAdapterMovies.KEY_IMDB_ID));
				contentValues.put(DbAdapterMovies.KEY_RATING, values.getAsString(DbAdapterMovies.KEY_RATING));
				contentValues.put(DbAdapterMovies.KEY_TAGLINE, values.getAsString(DbAdapterMovies.KEY_TAGLINE));
				contentValues.put(DbAdapterMovies.KEY_RELEASEDATE, values.getAsString(DbAdapterMovies.KEY_RELEASEDATE));
				contentValues.put(DbAdapterMovies.KEY_CERTIFICATION, values.getAsString(DbAdapterMovies.KEY_CERTIFICATION));
				contentValues.put(DbAdapterMovies.KEY_RUNTIME, values.getAsString(DbAdapterMovies.KEY_RUNTIME));
				contentValues.put(DbAdapterMovies.KEY_TRAILER, values.getAsString(DbAdapterMovies.KEY_TRAILER));
				contentValues.put(DbAdapterMovies.KEY_GENRES, values.getAsString(DbAdapterMovies.KEY_GENRES));
				contentValues.put(DbAdapterMovies.KEY_FAVOURITE, values.getAsString(DbAdapterMovies.KEY_FAVOURITE));
				contentValues.put(DbAdapterMovies.KEY_ACTORS, values.getAsString(DbAdapterMovies.KEY_ACTORS));
				contentValues.put(DbAdapterMovies.KEY_TO_WATCH, values.getAsString(DbAdapterMovies.KEY_TO_WATCH));
				contentValues.put(DbAdapterMovies.KEY_HAS_WATCHED, values.getAsString(DbAdapterMovies.KEY_HAS_WATCHED));
				contentValues.put(DbAdapterMovies.KEY_DATE_ADDED, values.getAsString(DbAdapterMovies.KEY_DATE_ADDED));
				contentValues.put(DbAdapterMovies.KEY_COLLECTION_ID, values.getAsString(DbAdapterMovies.KEY_COLLECTION_ID));

				database.insert(DbAdapterMovies.DATABASE_TABLE, null, contentValues);
			}

			if (!collectionsAdded.contains(values.getAsString(DbAdapterMovies.KEY_COLLECTION_ID))) {
				// Add values to a ContentValues object
				ContentValues cv = new ContentValues();
				cv.put(DbAdapterCollections.KEY_COLLECTION_ID, values.getAsString(DbAdapterMovies.KEY_COLLECTION_ID));
				cv.put(DbAdapterCollections.KEY_COLLECTION, values.getAsString(DbAdapterCollections.KEY_COLLECTION));

				// Create collection
				database.insert(DbAdapterCollections.DATABASE_TABLE, null, cv);
				collectionsAdded.add(values.getAsString(DbAdapterMovies.KEY_COLLECTION_ID));
			}

			// Add values to a ContentValues object
			ContentValues fileMap = new ContentValues();
			fileMap.put(DbAdapterMovieMappings.KEY_FILEPATH, values.getAsString(DbAdapterMovieMappings.KEY_FILEPATH));
			fileMap.put(DbAdapterMovieMappings.KEY_TMDB_ID, values.getAsString(DbAdapterMovies.KEY_TMDB_ID).isEmpty() ? values.getAsString(DbAdapterMovieMappings.KEY_FILEPATH) : values.getAsString(DbAdapterMovies.KEY_TMDB_ID));
			fileMap.put(DbAdapterMovieMappings.KEY_IGNORED, 0);

			// Create filepath mapping to movie
			database.insert(DbAdapterMovieMappings.DATABASE_TABLE, null, fileMap);
		}
	}

	private static boolean movieExists(SQLiteDatabase db, String movieId) {
		boolean result = false;

		Cursor mCursor = db.query(true, DbAdapterMovies.DATABASE_TABLE, new String[]{DbAdapterMovies.KEY_TMDB_ID}, DbAdapterMovies.KEY_TMDB_ID + "='" + movieId + "'", null, DbAdapterMovies.KEY_TMDB_ID, null, null, null);
		if (mCursor == null)
			return false;

		try {
			result = mCursor.getCount() > 0;
		} catch (Exception e) {
		} finally {
			mCursor.close();
		}

		return result;
	}

	private void moveTvShowDb(Context context, SQLiteDatabase database) {
		File dbFile = context.getDatabasePath("mizuu_tv_show_data");
		File dbFile2 = context.getDatabasePath("mizuu_tv_show_episode_data");

		if (dbFile.exists() && dbFile2.exists()) {
			// The old database file exists! Let's create
			// a table for it in the new combined database
			database.execSQL(DATABASE_CREATE_TV_SHOWS);

			// ... and the index
			database.execSQL(DATABASE_CREATE_SHOW_ID_INDEX);

			String KEY_SHOW_ID = "show_id";
			String KEY_SHOW_TITLE = "show_title";
			String KEY_SHOW_PLOT = "show_description";
			String KEY_SHOW_ACTORS = "show_actors";
			String KEY_SHOW_GENRES = "show_genres";
			String KEY_SHOW_RATING = "show_rating";
			String KEY_SHOW_CERTIFICATION = "show_certification";
			String KEY_SHOW_RUNTIME = "show_runtime";
			String KEY_SHOW_FIRST_AIRDATE = "show_first_airdate";
			String KEY_SHOW_EXTRA1 = "extra1"; // Favorite

			String[] SELECT_ALL = new String[]{KEY_SHOW_ID, KEY_SHOW_TITLE, KEY_SHOW_PLOT, KEY_SHOW_ACTORS,
					KEY_SHOW_GENRES, KEY_SHOW_RATING, KEY_SHOW_RATING, KEY_SHOW_CERTIFICATION, KEY_SHOW_RUNTIME,
					KEY_SHOW_FIRST_AIRDATE, KEY_SHOW_EXTRA1};

			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			Cursor c = db.query("tvshows", SELECT_ALL, null, null, null, null, KEY_SHOW_TITLE + " ASC");

			try {
				while (c.moveToNext()) {
					ContentValues cv = new ContentValues();
					cv.put(KEY_SHOW_ID, c.getString(c.getColumnIndex(KEY_SHOW_ID)));
					cv.put(KEY_SHOW_TITLE, c.getString(c.getColumnIndex(KEY_SHOW_TITLE)));
					cv.put(KEY_SHOW_PLOT, c.getString(c.getColumnIndex(KEY_SHOW_PLOT)));
					cv.put(KEY_SHOW_ACTORS, c.getString(c.getColumnIndex(KEY_SHOW_ACTORS)));
					cv.put(KEY_SHOW_GENRES, c.getString(c.getColumnIndex(KEY_SHOW_GENRES)));
					cv.put(KEY_SHOW_RATING, c.getString(c.getColumnIndex(KEY_SHOW_RATING)));
					cv.put(KEY_SHOW_CERTIFICATION, c.getString(c.getColumnIndex(KEY_SHOW_CERTIFICATION)));
					cv.put(KEY_SHOW_RUNTIME, c.getString(c.getColumnIndex(KEY_SHOW_RUNTIME)));
					cv.put(KEY_SHOW_FIRST_AIRDATE, c.getString(c.getColumnIndex(KEY_SHOW_FIRST_AIRDATE)));
					cv.put(DbAdapterTvShows.KEY_SHOW_FAVOURITE, c.getString(c.getColumnIndex(KEY_SHOW_EXTRA1)));
					
					database.insert("tvshows", null, cv);
				}
			} catch (Exception e) {} finally {
				c.close();
				db.close();

				if (MizLib.hasJellyBean())
					SQLiteDatabase.deleteDatabase(dbFile);
				else					
					dbFile.delete();
			}


			// Move TV show episodes
			// The old database file exists! Let's create
			// a table for it in the new combined database
			database.execSQL(DATABASE_CREATE_TV_SHOWS_EPISODES);

			// ... and the index
			database.execSQL(DATABASE_CREATE_EPISODE_SHOW_ID_INDEX);
			
			// ... and the file mapping table
			database.execSQL(DATABASE_CREATE_TV_SHOWS_EPISODES_MAPPING);

			String KEY_FILEPATH = "filepath";
			String KEY_SEASON = "season";
			String KEY_EPISODE = "episode";
			String KEY_DATE_ADDED = "date_added";
			String KEY_TO_WATCH = "to_watch";
			String KEY_HAS_WATCHED = "has_watched";
			String KEY_EXTRA_1 = "extra1"; // Favorite
			KEY_SHOW_ID = "show_id";
			String KEY_EPISODE_TITLE = "episode_title";
			String KEY_EPISODE_PLOT = "episode_description";
			String KEY_EPISODE_AIRDATE = "episode_airdate";
			String KEY_EPISODE_RATING = "episode_rating";
			String KEY_EPISODE_DIRECTOR = "episode_director";
			String KEY_EPISODE_WRITER = "episode_writer";
			String KEY_EPISODE_GUESTSTARS = "episode_gueststars";

			SELECT_ALL = new String[]{KEY_FILEPATH, KEY_SEASON, KEY_EPISODE, KEY_DATE_ADDED, KEY_TO_WATCH, KEY_HAS_WATCHED,
					KEY_EXTRA_1, KEY_SHOW_ID, KEY_EPISODE_TITLE, KEY_EPISODE_PLOT, KEY_EPISODE_AIRDATE, KEY_EPISODE_RATING, KEY_EPISODE_DIRECTOR,
					KEY_EPISODE_WRITER, KEY_EPISODE_GUESTSTARS};

			db = SQLiteDatabase.openOrCreateDatabase(dbFile2, null);
			c = db.query("tvshow_episodes", SELECT_ALL, null, null, null, null, null);

			HashSet<String> fileSet = new HashSet<String>();

			try {
				while (c.moveToNext()) {
					ContentValues cv = new ContentValues();
					cv.put(KEY_SEASON, c.getString(c.getColumnIndex(KEY_SEASON)));
					cv.put(KEY_EPISODE, c.getString(c.getColumnIndex(KEY_EPISODE)));
					cv.put(KEY_DATE_ADDED, c.getString(c.getColumnIndex(KEY_DATE_ADDED)));
					cv.put(KEY_TO_WATCH, c.getString(c.getColumnIndex(KEY_TO_WATCH)));
					cv.put(KEY_HAS_WATCHED, c.getString(c.getColumnIndex(KEY_HAS_WATCHED)));
					cv.put(DbAdapterTvShowEpisodes.KEY_FAVOURITE, c.getString(c.getColumnIndex(KEY_EXTRA_1)));
					cv.put(KEY_SHOW_ID, c.getString(c.getColumnIndex(KEY_SHOW_ID)));
					cv.put(KEY_EPISODE_TITLE, c.getString(c.getColumnIndex(KEY_EPISODE_TITLE)));
					cv.put(KEY_EPISODE_PLOT, c.getString(c.getColumnIndex(KEY_EPISODE_PLOT)));
					cv.put(KEY_EPISODE_AIRDATE, c.getString(c.getColumnIndex(KEY_EPISODE_AIRDATE)));
					cv.put(KEY_EPISODE_RATING, c.getString(c.getColumnIndex(KEY_EPISODE_RATING)));
					cv.put(KEY_EPISODE_DIRECTOR, c.getString(c.getColumnIndex(KEY_EPISODE_DIRECTOR)));
					cv.put(KEY_EPISODE_WRITER, c.getString(c.getColumnIndex(KEY_EPISODE_WRITER)));
					cv.put(KEY_EPISODE_GUESTSTARS, c.getString(c.getColumnIndex(KEY_EPISODE_GUESTSTARS)));

					database.insert(DbAdapterTvShowEpisodes.DATABASE_TABLE, null, cv);

					// We don't want duplicates :-)
					if (!fileSet.contains(c.getString(c.getColumnIndex(KEY_FILEPATH)))) {
						cv.clear();
						cv.put(DbAdapterTvShowEpisodeMappings.KEY_SHOW_ID, c.getString(c.getColumnIndex(KEY_SHOW_ID)));
						cv.put(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH, c.getString(c.getColumnIndex(KEY_FILEPATH)));
						cv.put(DbAdapterTvShowEpisodeMappings.KEY_SEASON, c.getString(c.getColumnIndex(KEY_SEASON)));
						cv.put(DbAdapterTvShowEpisodeMappings.KEY_EPISODE, c.getString(c.getColumnIndex(KEY_EPISODE)));
						cv.put(DbAdapterTvShowEpisodeMappings.KEY_IGNORED, c.getString(c.getColumnIndex(KEY_EPISODE_TITLE)).equals("KEY_EPISODE_TITLE") ? 1 : 0);
						
						database.insert(DbAdapterTvShowEpisodeMappings.DATABASE_TABLE, null, cv);
						fileSet.add(c.getString(c.getColumnIndex(KEY_FILEPATH)));
					}
				}
			} catch (Exception e) {} finally {
				c.close();
				db.close();

				if (MizLib.hasJellyBean())
					SQLiteDatabase.deleteDatabase(dbFile2);
				else					
					dbFile2.delete();
			}

		}
	}

	private void moveFileSourcesDb(Context context, SQLiteDatabase database) {
		File dbFile = context.getDatabasePath("mizuu_sources");
		if (dbFile.exists()) {
			// The old database file exists! Let's create
			// a table for it in the new combined database
			database.execSQL(DATABASE_CREATE_FILESOURCES);

			String KEY_FILEPATH = "filepath";
			String KEY_TYPE = "type"; // movie / TV show
			String KEY_FILESOURCE_TYPE = "is_smb"; // local file / NAS / other sources... Used to be 0 = local file, 1 = smb.
			String KEY_USER = "user";
			String KEY_PASSWORD = "password";
			String KEY_DOMAIN = "domain";

			String[] SELECT_ALL = new String[]{KEY_FILEPATH, KEY_TYPE, KEY_FILESOURCE_TYPE, KEY_USER, KEY_PASSWORD, KEY_DOMAIN};

			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			Cursor c = db.query("sources", SELECT_ALL, null, null, null, null, null);

			try {
				while (c.moveToNext()) {
					ContentValues cv = new ContentValues();
					cv.put(KEY_FILEPATH, c.getString(c.getColumnIndex(KEY_FILEPATH)));
					cv.put(KEY_TYPE, c.getString(c.getColumnIndex(KEY_TYPE)));
					cv.put(KEY_FILESOURCE_TYPE, c.getInt(c.getColumnIndex(KEY_FILESOURCE_TYPE)));
					cv.put(KEY_USER, c.getString(c.getColumnIndex(KEY_USER)));
					cv.put(KEY_PASSWORD, c.getString(c.getColumnIndex(KEY_PASSWORD)));
					cv.put(KEY_DOMAIN, c.getString(c.getColumnIndex(KEY_DOMAIN)));

					database.insert("sources", null, cv);
				}
			} catch (Exception e) {} finally {
				c.close();
				db.close();

				if (MizLib.hasJellyBean())
					SQLiteDatabase.deleteDatabase(dbFile);
				else					
					dbFile.delete();
			}
		}
	}
}