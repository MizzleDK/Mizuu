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

package com.miz.service;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.miz.widgets.ShowCoverWidgetProvider;
import com.miz.widgets.ShowStackWidgetProvider;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class DownloadImageService extends IntentService {

	public static final String IMAGE_URL = "imageUrl";
	public static final String CONTENT_ID = "contentId";
	public static final String IMAGE_TYPE = "imageType";

	public static final int IMAGE_TYPE_TVSHOW_COVER = 1;
	public static final int IMAGE_TYPE_TVSHOW_BACKDROP = 2;
	public static final int IMAGE_TYPE_MOVIE_COVER = 3;
	public static final int IMAGE_TYPE_MOVIE_BACKDROP = 4;

	private String mImageUrl = "", mContentId = "", mDownloadPath = "";
	private int mImageType = 0;
	private boolean mNeedsResizing = false;

	private final int NOTIFICATION_ID = 54321;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;

	public DownloadImageService() {
		super("DownloadImageService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		boolean isOnline = MizLib.isOnline(this);
		final int stringId = isOnline ? R.string.addingCover : R.string.noInternet;
		
		new Handler(Looper.getMainLooper()).post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(MizuuApplication.getContext(), stringId, Toast.LENGTH_LONG).show();                
			}
		});

		if (!isOnline) {
			// End the download service
			return;
		}

		// Clear /reset all instance variables
		clear();

		// Get the current setup
		Bundle bundle = intent.getExtras();
		mImageUrl = bundle.getString(IMAGE_URL, "");
		mContentId = bundle.getString(CONTENT_ID, "");
		mImageType = bundle.getInt(IMAGE_TYPE, 0);

		// Setup...
		setup();

		// Show Notification
		showNotification();

		// Download!
		download();

		// Resize the image
		resize();

		// Broadcast and update widgets
		update();

		// Dismiss the Notification
		dismiss();
	}

	private void setup() {

		switch (mImageType) {
		case IMAGE_TYPE_TVSHOW_COVER:

			if (!mContentId.startsWith("tmdb_")) {
				// TheTVDb URL's need to have part of it replaced
				mImageUrl = mImageUrl.replace("/_cache/", "/");
				mNeedsResizing = true;
			}

			mDownloadPath = MizLib.getTvShowThumb(this, mContentId).getAbsolutePath();

			break;

		case IMAGE_TYPE_TVSHOW_BACKDROP:

			if (mContentId.startsWith("tmdb_")) {
				// TMDb URL's need to have part of it replaced with something else
				mImageUrl = mImageUrl.replace(MizLib.getBackdropThumbUrlSize(this), MizLib.getBackdropUrlSize(this));
			} else {
				// TheTVDb URL's need to have part of it removed
				mImageUrl = mImageUrl.replace("", "").replace("/_cache/", "/");
			}

			mDownloadPath = MizLib.getTvShowBackdrop(this, mContentId).getAbsolutePath();

			break;

		case IMAGE_TYPE_MOVIE_COVER:

			mDownloadPath = MizLib.getMovieThumb(this, mContentId).getAbsolutePath();

			break;

		case IMAGE_TYPE_MOVIE_BACKDROP:

			// TMDb URL's need to have part of it replaced with something else
			mImageUrl = mImageUrl.replace(MizLib.getBackdropThumbUrlSize(this), MizLib.getBackdropUrlSize(this));

			mDownloadPath = MizLib.getMovieBackdrop(this, mContentId).getAbsolutePath();

			break;
		}
	}

	private void showNotification() {
		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
		mBuilder.setTicker(getString(R.string.addingCover));
		mBuilder.setContentTitle(getString(R.string.addingCover));
		mBuilder.setContentText(getString(R.string.few_moments));
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_download));

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);
	}

	private void download() {
		MizLib.downloadFile(mImageUrl, mDownloadPath);
	}

	private void resize() {
		if (!mNeedsResizing)
			return;

		MizLib.resizeBitmapFileToCoverSize(this, mDownloadPath);
	}

	private void update() {

		AppWidgetManager awm = AppWidgetManager.getInstance(this);

		switch (mImageType) {
		case IMAGE_TYPE_TVSHOW_COVER:

			// Broadcast
			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-show-cover-change"));

			// Update widgets
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

			break;

		case IMAGE_TYPE_TVSHOW_BACKDROP:

			// Broadcast
			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-show-backdrop-change"));

			// Update widget
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

			break;

		case IMAGE_TYPE_MOVIE_COVER:

			// Broadcast
			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-movie-cover-change"));

			// Update widgets
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

			break;

		case IMAGE_TYPE_MOVIE_BACKDROP:

			// Broadcast
			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-movie-backdrop-change"));

			// Update widget
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

			break;
		}
	}

	private void dismiss() {
		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);
	}

	private void clear() {
		mImageUrl = "";
		mContentId = "";
		mDownloadPath = "";
		mImageType = 0;
		mNeedsResizing = false;
	}
}