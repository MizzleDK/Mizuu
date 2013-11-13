package com.miz.contentprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.miz.db.DbAdapter;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public class MovieContentProvider extends SearchRecentSuggestionsProvider {

	static final String TAG = MovieContentProvider.class.getSimpleName();
	public static final String AUTHORITY = MovieContentProvider.class.getName();
	public static final int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;
	private static final String[] COLUMNS = {
		BaseColumns._ID, // must include this column
		SearchManager.SUGGEST_COLUMN_TEXT_1, // First line (title)
		SearchManager.SUGGEST_COLUMN_TEXT_2, // Second line (smaller text)
		SearchManager.SUGGEST_COLUMN_INTENT_DATA, // Icon
		SearchManager.SUGGEST_COLUMN_ICON_1, // Icon
		SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, // Movie ID
		SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
		SearchManager.SUGGEST_COLUMN_SHORTCUT_ID };

	public MovieContentProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		String query = selectionArgs[0];
		if (query == null || query.length() == 0) {
			return null;
		}

		MatrixCursor cursor = new MatrixCursor(COLUMNS);

		try {
			List<MediumMovie> list = getSearchResults(query);
			int n = 0;
			for (MediumMovie movie : list) {
				cursor.addRow(createRow(Integer.valueOf(n), movie.getTitle(), movie.getReleaseYear().replace("(", "").replace(")", ""), movie.getThumbnail(), movie.getRowId()));
				n++;
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to lookup " + query, e);
		}
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	private Object[] createRow(Integer id, String text1, String text2, String icon, String rowId) {
		return new Object[] {
				id, // _id
				text1, // text1
				text2, // text2
				icon,
				icon,
				rowId,
				"android.intent.action.SEARCH", // action
				SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT };
	}

	private List<MediumMovie> getSearchResults(String query) {
		List<MediumMovie> movies = new ArrayList<MediumMovie>();
		if (!MizLib.isEmpty(query)) {

			DbAdapter db = MizuuApplication.getMovieAdapter();

			query = query.toLowerCase(Locale.ENGLISH);

			Cursor c = db.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false);
			String title = "";
			while (c.moveToNext()) {
				title = c.getString(c.getColumnIndex(DbAdapter.KEY_TITLE));

				if (title.toLowerCase(Locale.ENGLISH).startsWith(query)) {
					movies.add(new MediumMovie(getContext(),
							c.getString(c.getColumnIndex(DbAdapter.KEY_ROWID)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_FILEPATH)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_TITLE)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_TMDBID)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_RATING)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_GENRES)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_CAST)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_COLLECTION)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
							c.getString(c.getColumnIndex(DbAdapter.KEY_RUNTIME)),
							false,
							true
							));
				}
			}
		}
		return movies;
	}
}