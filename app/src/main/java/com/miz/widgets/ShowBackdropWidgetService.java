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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.miz.db.DbAdapterTvShows;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;

import java.util.ArrayList;
import java.util.Collections;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

public class ShowBackdropWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		if (widgetId < 0) {
			return null;
		}
		return new BookmarkFactory(getApplicationContext(), widgetId);
	}

	static class BookmarkFactory implements RemoteViewsService.RemoteViewsFactory {

		private boolean isTablet;
		private Context mContext;
		private ArrayList<TvShow> shows = new ArrayList<TvShow>();
		private boolean ignorePrefixes;

		public BookmarkFactory(Context context, int widgetId) {
			mContext = context.getApplicationContext();
			isTablet = MizLib.isTablet(mContext);
			ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);
		}

		public void onCreate() {
			update();
		}

		public void onDestroy() {
			shows.clear();
		}

		public int getCount() {
			return shows.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public RemoteViews getLoadingView() {
			return new RemoteViews(mContext.getPackageName(), R.layout.movie_backdrop_widget_item);
		}

		@Override
		public RemoteViews getViewAt(int position) {
			RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.movie_backdrop_widget_item);

			// Image
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.RGB_565;
			options.inPreferQualityOverSpeed = true;
			if (isTablet)
				options.inSampleSize = 4;
			else
				options.inSampleSize = 2;

			Bitmap cover = BitmapFactory.decodeFile(shows.get(position).getBackdrop(), options);

			view.setImageViewBitmap(R.id.thumb, cover);

			// Text
			view.setTextViewText(R.id.widgetTitle, shows.get(position).getTitle());
			view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);

			// Intent
			Intent i = new Intent(ShowBackdropWidgetProvider.SHOW_BACKDROP_WIDGET);
			i.putExtra("showId", shows.get(position).getId());
			view.setOnClickFillInIntent(R.id.list_item, i);

			return view;
		}

		private void update() {
			shows.clear();

			// Create and open database
			DbAdapterTvShows dbHelper = MizuuApplication.getTvDbAdapter();

			Cursor cursor = dbHelper.getAllShows();
			ColumnIndexCache cache = new ColumnIndexCache();

			try {
				while (cursor.moveToNext()) {
					shows.add(new TvShow(
							mContext,
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_TITLE)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_PLOT)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RATING)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_GENRES)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ACTORS)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_RUNTIME)),
							ignorePrefixes,
							cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
							MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShows.KEY_SHOW_ID)))
							));
				}
			} catch (NullPointerException e) {} finally {
				cursor.close();
				cache.clear();
			}

			Collections.sort(shows);
		}

		@Override
		public void onDataSetChanged() {
			update();
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}
	}
}