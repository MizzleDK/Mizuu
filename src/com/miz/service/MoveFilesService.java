package com.miz.service;

import java.io.File;
import java.io.IOException;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.mizuu.SplashScreen;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

public class MoveFilesService extends IntentService {

	public MoveFilesService() {
		super("MoveFilesService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Setup up notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.refresh);
		builder.setTicker(getString(R.string.movingDataFiles));
		builder.setContentTitle(getString(R.string.movingDataFiles));
		builder.setContentText(getString(R.string.movingDataFilesDescription));
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.refresh));

		// Build notification
		Notification updateNotification = builder.build();

		// Show the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(5, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(5, updateNotification);

		File[] listFiles;
		String name = "";

		File oldDataFolder = MizLib.getOldDataFolder();
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

		File oldMovieThumbs = new File(MizLib.getOldDataFolder() + "/movie-thumbs");
		if (oldMovieThumbs.exists() && oldMovieThumbs.listFiles() != null) {
			listFiles = oldMovieThumbs.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				movieThumb(listFiles[i]);
				listFiles[i].delete();
			}
		}

		File oldTvShowThumbs = new File(MizLib.getOldDataFolder() + "/tvshows-thumbs");
		if (oldTvShowThumbs.exists() && oldTvShowThumbs.listFiles() != null) {
			listFiles = oldTvShowThumbs.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				tvShowThumb(listFiles[i]);
				listFiles[i].delete();
			}
		}

		// Delete the old data folder
		MizLib.deleteRecursive(oldDataFolder);

		Intent i = new Intent(this, SplashScreen.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

	private void movieBackdrop(File backdrop) {
		try {
			MizLib.copyFile(backdrop, new File(MizLib.getMovieBackdropFolder(this), backdrop.getName()));
		} catch (IOException e) {}
	}

	private void tvShowBackdrop(File backdrop) {
		try {
			MizLib.copyFile(backdrop, new File(MizLib.getTvShowBackdropFolder(this), backdrop.getName()));
		} catch (IOException e) {}
	}

	private void episodePhoto(File episode) {
		try {
			MizLib.copyFile(episode, new File(MizLib.getTvShowEpisodeFolder(this), episode.getName()));
		} catch (IOException e) {}
	}

	private void movieThumb(File thumb) {
		try {
			MizLib.copyFile(thumb, new File(MizLib.getMovieThumbFolder(this), thumb.getName()));
		} catch (IOException e) {}
	}

	private void tvShowThumb(File thumb) {
		try {
			MizLib.copyFile(thumb, new File(MizLib.getTvShowThumbFolder(this), thumb.getName()));
		} catch (IOException e) {}
	}
}