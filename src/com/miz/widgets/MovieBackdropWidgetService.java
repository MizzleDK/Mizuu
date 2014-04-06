package com.miz.widgets;

import java.util.ArrayList;
import java.util.Collections;

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

import com.miz.db.DbAdapter;
import com.miz.functions.MizLib;
import com.miz.functions.SmallMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class MovieBackdropWidgetService extends RemoteViewsService {

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
		private ArrayList<SmallMovie> movies = new ArrayList<SmallMovie>();
		private boolean ignorePrefixes, ignoreNfo;

		public BookmarkFactory(Context context, int widgetId) {
			mContext = context.getApplicationContext();
			isTablet = MizLib.isTablet(mContext);
			ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("prefsIgnorePrefixesInTitles", false);
			ignoreNfo = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("prefsIgnoreNfoFiles", true);
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
			
			Bitmap cover = BitmapFactory.decodeFile(movies.get(position).getBackdrop(), options);
			
			view.setImageViewBitmap(R.id.thumb, cover);
			
			// Text
			view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
			view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);

			// Intent
			Intent i = new Intent(MovieBackdropWidgetProvider.MOVIE_BACKDROP_WIDGET);
			i.putExtra("rowId", Integer.parseInt(movies.get(position).getRowId()));
			view.setOnClickFillInIntent(R.id.list_item, i);

			return view;
		}

		private void update() {
			movies.clear();

			// Create and open database
			DbAdapter dbHelper = MizuuApplication.getMovieAdapter();

			Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false, false);

			while (cursor.moveToNext()) {
				try {
					movies.add(new SmallMovie(mContext,
							cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
							cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
							cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
							cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
							ignorePrefixes,
							ignoreNfo
							));
				} catch (NullPointerException e) {}
			}
			
			cursor.close();
			
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