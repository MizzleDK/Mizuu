package com.miz.functions;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteCursorLoader extends AbstractCursorLoader {

	SQLiteOpenHelper db = null;
	String table, selection, groupBy, having, orderBy;
	String[] columns, selectionArgs;

	public SQLiteCursorLoader(Context context, SQLiteOpenHelper db, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
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
		return db.getWritableDatabase().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
	}
}