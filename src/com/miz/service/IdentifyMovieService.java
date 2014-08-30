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

import java.util.ArrayList;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.functions.MovieLibraryUpdateCallback;
import com.miz.identification.MovieIdentification;
import com.miz.identification.MovieStructure;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.WidgetUtils;

public class IdentifyMovieService extends IntentService implements MovieLibraryUpdateCallback {

	private boolean mDebugging = true;
	private String mMovieId, mOldMovieId, mLanguage, mFilepath;
	private ArrayList<MovieStructure> mFiles = new ArrayList<MovieStructure>();
	private final int NOTIFICATION_ID = 4500;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;

	public IdentifyMovieService() {
		super("IdentifyMovieService");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		log("onDestroy()");

		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);
		
		LocalBroadcastUtils.updateMovieLibrary(this);

		WidgetUtils.updateMovieWidgets(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (MizLib.isMovieLibraryBeingUpdated(this)) {
			Handler mHandler = new Handler(Looper.getMainLooper());
			mHandler.post(new Runnable() {            
		        @Override
		        public void run() {
		            Toast.makeText(IdentifyMovieService.this, R.string.cant_identify_while_updating, Toast.LENGTH_LONG).show();                
		        }
		    });
			
			return;
		}
		
		log("clear()");
		clear();

		log("setup()");
		setup();

		log("Intent extras");
		Bundle b = intent.getExtras();
		mMovieId = b.getString("movieId");
		mLanguage = b.getString("language", "en");
		mFilepath = b.getString("filepath");
		
		log("setupList()");
		setupList();

		log("removeOldData()");
		removeOldData();

		log("start()");
		start();
	}

	private void setupList() {
		mFiles.add(new MovieStructure(mFilepath));
	}

	private void removeOldData() {
		// Get the movie ID of the old mapping
		mOldMovieId = MizuuApplication.getMovieMappingAdapter().getIdForFilepath(mFilepath);
		
		// Check if there's just one filepath mapping for this particular movie
		if (MizuuApplication.getMovieMappingAdapter().getMovieFilepaths(mOldMovieId).size() == 1) {			
			// Delete database entries
			MizuuApplication.getMovieAdapter().deleteMovie(mOldMovieId);
			
			// Delete the old movie cover art and backdrop images
			MizLib.getMovieThumb(this, mOldMovieId).delete();
			MizLib.getMovieBackdrop(this, mOldMovieId).delete();
			
			LocalBroadcastUtils.updateMovieLibrary(this);
		}
	}

	private void start() {
		MovieIdentification identification = new MovieIdentification(this, this, mFiles, null);
		identification.setMovieId(mMovieId);
		identification.setLanguage(mLanguage);
		identification.start();
	}

	/**
	 * Clear all instance variables
	 * since this is an {@link IntentService}.
	 */
	private void clear() {
		mMovieId = "";
		mLanguage = "";
		mFiles = new ArrayList<MovieStructure>();
	}

	private void setup() {
		// Setup up notification
		mBuilder = new NotificationCompat.Builder(getApplicationContext());
		mBuilder.setSmallIcon(R.drawable.refresh);
		mBuilder.setTicker(getString(R.string.identifying_movie));
		mBuilder.setContentTitle(getString(R.string.identifying_movie));
		mBuilder.setContentText(getString(R.string.gettingReady));
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.refresh));

		// Build notification
		Notification updateNotification = mBuilder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);
	}
	
	@Override
	public void onMovieAdded(String title, Bitmap cover, Bitmap backdrop, int count) {
		// We're done!
	}

	private void log(String msg) {
		if (mDebugging)
			Log.d("IdentifyMovieService", msg);
	}
}