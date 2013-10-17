package com.miz.widgets;

import com.miz.functions.MizLib;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

@SuppressWarnings("deprecation")
public class MovieCoverWidgetProvider extends AppWidgetProvider {

	public static final String MOVIE_COVER_WIDGET = "movieCoverWidget";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (MOVIE_COVER_WIDGET.equals(action)) {
			Intent openMovie = new Intent();
			openMovie.setClass(context, MovieDetails.class);
			openMovie.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			openMovie.putExtra("rowId", intent.getIntExtra("rowId", -1));
			openMovie.putExtra("isFromWidget", true);
			context.startActivity(openMovie);
		} else {
			super.onReceive(context, intent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int i = 0; i < appWidgetIds.length; ++i) {
			Intent intent = new Intent(context, MovieCoverWidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.movie_cover_widget);

			if (MizLib.hasICS())
				rv.setRemoteAdapter(R.id.widget_grid, intent);
			else
				rv.setRemoteAdapter(appWidgetIds[i], R.id.widget_grid, intent);

			Intent toastIntent = new Intent(context, MovieCoverWidgetProvider.class);
			toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			rv.setPendingIntentTemplate(R.id.widget_grid, PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT));

			appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		}
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}