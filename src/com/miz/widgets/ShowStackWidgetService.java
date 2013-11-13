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

import com.miz.db.DbAdapterTvShow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.R;

public class ShowStackWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactoryTv(this.getApplicationContext(), intent);
	}
}

class StackRemoteViewsFactoryTv implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private ArrayList<TvShow> shows = new ArrayList<TvShow>();
	private Cursor cursor;
	private DbAdapterTvShow dbHelper;
	private boolean ignorePrefixes;

	public StackRemoteViewsFactoryTv(Context context, Intent intent) {
		mContext = context;
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

	public RemoteViews getViewAt(int position) {
		RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

		// Image
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inPreferQualityOverSpeed = true;

		Bitmap cover = BitmapFactory.decodeFile(shows.get(position).getThumbnail(), options);

		if (cover != null)
			view.setImageViewBitmap(R.id.widget_item, cover);
		else {
			view.setImageViewResource(R.id.widget_item, R.drawable.loading_image);

			// Text
			view.setTextViewText(R.id.widgetTitle, shows.get(position).getTitle());
			view.setViewVisibility(R.id.widgetTitle, View.VISIBLE);
		}

		Intent fillInIntent = new Intent(ShowStackWidgetProvider.SHOW_STACK_WIDGET);
		fillInIntent.putExtra("showId", shows.get(position).getId());
		view.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

		return view;
	}

	private void update() {
		shows.clear();

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("prefsIgnorePrefixesInTitles", false);

		// Create and open database
		dbHelper = MizuuApplication.getTvDbAdapter();

		cursor = dbHelper.getAllShows();

		while (cursor.moveToNext()) {
			try {
				shows.add(new TvShow(
						mContext,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
						ignorePrefixes,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1))
						));
			} catch (NullPointerException e) {}
		}

		cursor.close();
		
		Collections.sort(shows);
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
		return true;
	}

	public void onDataSetChanged() {
		update();
	}
}