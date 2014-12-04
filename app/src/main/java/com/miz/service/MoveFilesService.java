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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.SplashScreen;
import com.miz.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class MoveFilesService extends IntentService {

	public MoveFilesService() {
		super("MoveFilesService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Setup up notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setColor(getResources().getColor(R.color.color_primary));
		builder.setSmallIcon(R.drawable.ic_sync_white_24dp);
		builder.setTicker(getString(R.string.movingDataFiles));
		builder.setContentTitle(getString(R.string.movingDataFiles));
		builder.setContentText(getString(R.string.movingDataFilesDescription));
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);

		// Build notification
		Notification updateNotification = builder.build();

		// Show the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(5, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(5, updateNotification);

		File[] listFiles;
		String name = "";

		File oldDataFolder = FileUtils.getOldDataFolder();
		if (oldDataFolder.listFiles() != null) {
			listFiles = oldDataFolder.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				name = listFiles[i].getName();
				if (!listFiles[i].isDirectory()) {
					if (name.endsWith("_bg.jpg")) { // Movie backdrop
						movieBackdrop(listFiles[i]);
					} else if (name.endsWith("_tvbg.jpg")) { // TV show backdrop
						tvShowBackdrop(listFiles[i]);
					} else if (name.contains("_S") && name.contains("E") && name.endsWith(".jpg")) { // Episode photo
						episodePhoto(listFiles[i]);
					} else { // Collection cover art
						movieThumb(listFiles[i]);
					}
					listFiles[i].delete();
				}
			}
		}

		File oldMovieThumbs = new File(FileUtils.getOldDataFolder() + "/movie-thumbs");
		if (oldMovieThumbs.exists() && oldMovieThumbs.listFiles() != null) {
			listFiles = oldMovieThumbs.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				movieThumb(listFiles[i]);
				listFiles[i].delete();
			}
		}

		File oldTvShowThumbs = new File(FileUtils.getOldDataFolder() + "/tvshows-thumbs");
		if (oldTvShowThumbs.exists() && oldTvShowThumbs.listFiles() != null) {
			listFiles = oldTvShowThumbs.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				tvShowThumb(listFiles[i]);
				listFiles[i].delete();
			}
		}

		// Delete the old data folder
		FileUtils.deleteRecursive(oldDataFolder, true);

		Intent i = new Intent(this, SplashScreen.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

	private void movieBackdrop(File backdrop) {
		try {
			FileUtils.copyFile(backdrop, new File(MizuuApplication.getMovieBackdropFolder(this), backdrop.getName()));
		} catch (IOException e) {}
	}

	private void tvShowBackdrop(File backdrop) {
		try {
			FileUtils.copyFile(backdrop, new File(MizuuApplication.getTvShowBackdropFolder(this), backdrop.getName()));
		} catch (IOException e) {}
	}

	private void episodePhoto(File episode) {
		try {
			FileUtils.copyFile(episode, new File(MizuuApplication.getTvShowEpisodeFolder(this), episode.getName()));
		} catch (IOException e) {}
	}

	private void movieThumb(File thumb) {
		try {
			FileUtils.copyFile(thumb, new File(MizuuApplication.getMovieThumbFolder(this), thumb.getName()));
		} catch (IOException e) {}
	}

	private void tvShowThumb(File thumb) {
		try {
			FileUtils.copyFile(thumb, new File(MizuuApplication.getTvShowThumbFolder(this), thumb.getName()));
		} catch (IOException e) {}
	}
}