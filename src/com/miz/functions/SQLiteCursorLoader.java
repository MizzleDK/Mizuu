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
import android.database.sqlite.SQLiteDatabase;

public class SQLiteCursorLoader extends AbstractCursorLoader {

	SQLiteDatabase db = null;
	String table, selection, groupBy, having, orderBy;
	String[] columns, selectionArgs;

	public SQLiteCursorLoader(Context context, SQLiteDatabase db, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
		super(context);
		this.db = db;
		this.table = table;
		this.selection = selection;
		this.groupBy = groupBy;
		this.having = having;
		this.orderBy = orderBy;
		this.columns = columns;
		this.selectionArgs = selectionArgs;
	}

	@Override
	protected Cursor buildCursor() {
		return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
	}
}