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

import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.miz.db.DbAdapter;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.SmallMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class MovieCoverWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		if (widgetId < 0) {
			return null;
		}
		return new BookmarkFactory(getApplicationContext(), widgetId);
	}

	static class BookmarkFactory implements RemoteViewsService.RemoteViewsFactory {

		private Context mContext;
		private ArrayList<SmallMovie> movies = new ArrayList<SmallMovie>();
		private boolean ignorePrefixes, ignoreNfo;
		private Picasso mPicasso;
		private Bitmap.Config mConfig;
		private RemoteViews mLoadingView;

		public BookmarkFactory(Context context, int widgetId) {
			mContext = context.getApplicationContext();
			ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);
			ignoreNfo = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_NFO_FILES, true);
			
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

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public RemoteViews getLoadingView() {
			return mLoadingView;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.movie_cover_widget_item);

			
			try {
				Bitmap cover = mPicasso.load(movies.get(position).getThumbnail()).config(mConfig).get();
				view.setImageViewBitmap(R.id.thumb, cover);
			} catch (IOException e) {
				view.setImageViewResource(R.id.thumb, R.drawable.loading_image);

				// Text
				view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
				view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
			}
			
			// Image
			/*BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.RGB_565;
			options.inPreferQualityOverSpeed = true;
			
			Bitmap cover = BitmapFactory.decodeFile(movies.get(position).getThumbnail().getAbsolutePath(), options);

			if (cover != null)
				view.setImageViewBitmap(R.id.thumb, cover);
			else {
				view.setImageViewResource(R.id.thumb, R.drawable.loading_image);

				// Text
				view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
				view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
			}*/

			// Intent
			Intent i = new Intent(MovieCoverWidgetProvider.MOVIE_COVER_WIDGET);
			i.putExtra("tmdbId", movies.get(position).getTmdbId());
			view.setOnClickFillInIntent(R.id.list_item, i);

			return view;
		}

		private void update() {
			movies.clear();

			// Create and open database
			DbAdapter dbHelper = MizuuApplication.getMovieAdapter();

			Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false);
			ColumnIndexCache cache = new ColumnIndexCache();

			try {
				while (cursor.moveToNext()) {
					movies.add(new SmallMovie(mContext,
							MizuuApplication.getMovieMappingAdapter().getFirstFilepathForMovie(cursor.getString(cache.getColumnIndex(cursor, DbAdapter.KEY_TMDB_ID))),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapter.KEY_TITLE)),
							cursor.getString(cache.getColumnIndex(cursor, DbAdapter.KEY_TMDB_ID)),
							ignorePrefixes,
							ignoreNfo
							));
				}
			} catch (NullPointerException e) {} finally {
				cursor.close();
				cache.clear();
			}

			Collections.sort(movies);
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