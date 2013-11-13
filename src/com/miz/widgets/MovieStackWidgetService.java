package com.miz.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Collections;

import com.miz.db.DbAdapter;
import com.miz.functions.SmallMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class MovieStackWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private ArrayList<SmallMovie> movies = new ArrayList<SmallMovie>();
	private DbAdapter dbHelper;
	private boolean ignorePrefixes, ignoreNfo;

	public StackRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
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

	public RemoteViews getViewAt(int position) {
		RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

		// Image
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inPreferQualityOverSpeed = true;

		Bitmap cover = BitmapFactory.decodeFile(movies.get(position).getThumbnail(), options);

		if (cover != null)
			view.setImageViewBitmap(R.id.widget_item, cover);
		else {
			view.setImageViewResource(R.id.widget_item, R.drawable.loading_image);

			// Text
			view.setTextViewText(R.id.widgetTitle, movies.get(position).getTitle());
			view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
		}

		Intent fillInIntent = new Intent(MovieStackWidgetProvider.MOVIE_STACK_WIDGET);
		fillInIntent.putExtra("rowId", Integer.parseInt(movies.get(position).getRowId()));
		view.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

		return view;
	}

	private void update() {
		movies.clear();

		// Create and open database
		dbHelper = MizuuApplication.getMovieAdapter();

		Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false);

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

	public RemoteViews getLoadingView() {
		return null;
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