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

import java.util.HashMap;

import com.miz.functions.ColumnIndexCache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class DbAdapterCollections extends AbstractDbAdapter {

	public static final String KEY_COLLECTION_ID = "collection_id";
	public static final String KEY_COLLECTION = "collection";

	public static final String DATABASE_TABLE = "collections";

	public static final String[] ALL_COLUMNS = new String[]{KEY_COLLECTION_ID, KEY_COLLECTION};

	public DbAdapterCollections(Context context) {
		super(context);
	}

	public long createCollection(String collectionId, String collection) {
		// We don't want to create it again if it already exists
		if (collectionExists(collectionId))
			return -1;
		
		// Add values to a ContentValues object
		ContentValues values = new ContentValues();
		values.put(KEY_COLLECTION_ID, collectionId);
		values.put(KEY_COLLECTION, collection);

		// Insert into database
		return mDatabase.insert(DATABASE_TABLE, null, values);
	}

	public boolean collectionExists(String collectionId) {
		Cursor mCursor = mDatabase.query(true, DATABASE_TABLE, ALL_COLUMNS, KEY_COLLECTION_ID + "='" + collectionId + "'", null, null, null, null, null);
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
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_COLLECTION_ID + "='" + collectionId + "'", null, null, null, null);
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
		return mDatabase.delete(DATABASE_TABLE, KEY_COLLECTION_ID + "='" + collectionId + "'", null) > 0;
	}
	
	public boolean deleteAllCollections() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}
}