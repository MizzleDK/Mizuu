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

package com.miz.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.miz.db.DbAdapterMovies;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.SmallMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

public class MovieStackWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private ArrayList<SmallMovie> movies = new ArrayList<SmallMovie>();
	private DbAdapterMovies dbHelper;
	private boolean ignorePrefixes;
	private Picasso mPicasso;
	private Bitmap.Config mConfig;
	private RemoteViews mLoadingView;

	public StackRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);

		mPicasso = MizuuApplication.getPicasso(mContext);
		mConfig = MizuuApplication.getBitmapConfig();
		
		mLoadingView = new RemoteViews(mContext.getPackageName(), R.layout.movie_cover_widget_item);
	}

	public void onCreate() {
		update();
	}

	public void onDestroy() {
		movies.clear();
	}

	public int getCount() {
		return movies.size();
	}

	public RemoteViews getViewAt(int position) {
		RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

		try {
			Bitmap cover = mPicasso.load(movies.get(position).getThumbnail()).config(mConfig).get();
			view.setImageViewBitmap(R.id.widget_item, cover);
		} catch (IOException e) {
			view.setImageViewResource(R.id.widget_item, R.drawable.loading_image);

			// Text
			view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
			view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
		}

		Intent fillInIntent = new Intent(MovieStackWidgetProvider.MOVIE_STACK_WIDGET);
		fillInIntent.putExtra("tmdbId", movies.get(position).getTmdbId());
		view.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

		return view;
	}

	private void update() {
		movies.clear();

		// Create and open database
		dbHelper = MizuuApplication.getMovieAdapter();

		Cursor cursor = dbHelper.fetchAllMovies(DbAdapterMovies.KEY_TITLE + " ASC");
		ColumnIndexCache cache = new ColumnIndexCache();

		try {
			while (cursor.moveToNext()) {
				movies.add(new SmallMovie(mContext,
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TITLE)),
						cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
						ignorePrefixes
						));
			}
		} catch (NullPointerException e) {} finally {
			cursor.close();
			cache.clear();
		}

		Collections.sort(movies);
	}

	public RemoteViews getLoadingView() {
		return mLoadingView;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return false;
	}

	public void onDataSetChanged() {
		update();
	}
}