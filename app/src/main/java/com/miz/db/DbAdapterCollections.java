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

package com.miz.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.miz.functions.ColumnIndexCache;
import com.miz.mizuu.MizuuApplication;

import java.util.HashMap;

public class DbAdapterCollections extends AbstractDbAdapter {

	public static final String KEY_COLLECTION_ID = "collection_id";
	public static final String KEY_COLLECTION = "collection";

	public static final String DATABASE_TABLE = "collections";

	public static final String[] ALL_COLUMNS = new String[]{KEY_COLLECTION_ID, KEY_COLLECTION};

	public DbAdapterCollections(Context context) {
		super(context);
	}

    /**
     * Creates a collection mapping. This method also gets the collection ID
     * of the supplied movie ID, and checks if that collection has more than
     * one movie mapped to it. If it doesn't, it'll remove the old mapping,
     * since it also re-maps to a new collection.
     * @param tmdbId
     * @param collectionId
     * @param collection
     * @return
     */
	public long createCollection(String tmdbId, String collectionId, String collection) {
        DbAdapterMovies db = MizuuApplication.getMovieAdapter();

		// We don't want to create it again if it already exists
        // or if the movie doesn't exist
		if (collectionExists(collectionId) || !db.movieExists(tmdbId))
			return -1;

        String currentCollectionId = db.getSingleItem(tmdbId, DbAdapterMovies.KEY_COLLECTION_ID);

        // If there's only a single movie mapped to the collection ID,
        // we can remove the mapping, since we're about to re-map it.
        if (getMovieCount(currentCollectionId) == 1) {
            deleteCollection(currentCollectionId);
        }
		
		// Add values to a ContentValues object
		ContentValues values = new ContentValues();
		values.put(KEY_COLLECTION_ID, collectionId);
		values.put(KEY_COLLECTION, collection);

		// Insert into database
		return mDatabase.insert(DATABASE_TABLE, null, values);
	}

	public boolean collectionExists(String collectionId) {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, ALL_COLUMNS, KEY_COLLECTION_ID + " = ?", new String[]{collectionId}, null, null, null, null);
		if (mCursor == null)
			return false;
		try {
			if (mCursor.getCount() == 0) {
				mCursor.close();
				return false;
			}
		} catch (Exception e) {
			mCursor.close();
			return false;
		}

		mCursor.close();
		return true;
	}
	
	public String getCollection(String collectionId) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_COLLECTION_ID + " = ?", new String[]{collectionId}, null, null, null);
		String collection = "";

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					collection = cursor.getString(cursor.getColumnIndex(KEY_COLLECTION));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return collection;
	}

    /**
     * Returns the number of movies mapped to the given collection ID.
     * @param collectionId
     * @return
     */
    public int getMovieCount(String collectionId) {
        int count = 0;

        Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_COLLECTION_ID + " = ?", new String[]{collectionId}, null, null, null);

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } catch (Exception e) {
            } finally {
                cursor.close();
            }
        }

        return count;
    }
	
	public HashMap<String, String> getCollectionsMap() {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, null, null, null, null, null);

		ColumnIndexCache cache = new ColumnIndexCache();
		HashMap<String, String> map = new HashMap<String, String>();

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					map.put(cursor.getString(cache.getColumnIndex(cursor, KEY_COLLECTION_ID)), cursor.getString(cache.getColumnIndex(cursor, KEY_COLLECTION)));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return map;
	}
	
	public boolean deleteCollection(String collectionId) {
		return mDatabase.delete(DATABASE_TABLE, KEY_COLLECTION_ID + " = ?", new String[]{collectionId}) > 0;
	}
	
	public boolean deleteAllCollections() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}

    /**
     * Used for unit testing.
     * @return
     */
    public int count() {
        Cursor c = mDatabase.query(DATABASE_TABLE, new String[]{KEY_COLLECTION_ID}, null, null, null, null, null);
        int count = c.getCount();
        c.close();
        return count;
    }
}