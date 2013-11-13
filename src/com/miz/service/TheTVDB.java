package com.miz.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.DecryptedShowEpisode;
import com.miz.functions.Episode;
import com.miz.functions.MizLib;
import com.miz.functions.TheTVDb;
import com.miz.functions.Tvshow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.miz.widgets.ShowCoverWidgetProvider;
import com.miz.widgets.ShowStackWidgetProvider;

public class TheTVDB extends Service {

	private String LOCALE = "", language = "";
	private boolean localizedInfo = false, isEpisodeIdentify = false, isShowIdentify = false, isUpdate = false, isUnidentifiedIdentify = false;
	private File thumbsFolder;
	private long rowId;
	private String[] files, rowsToDrop;
	private TheTVDb tvdb;
	private Tvshow thisShow;
	private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();

	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		final Bundle bundle = intent.getExtras();
		files = bundle.getStringArray("files");
		rowsToDrop = bundle.getStringArray("rowsToDrop");
		isEpisodeIdentify = bundle.getBoolean("isEpisodeIdentify", false);
		isShowIdentify = bundle.getBoolean("isShowIdentify", false);
		isUnidentifiedIdentify = bundle.getBoolean("isUnidentifiedIdentify", false);
		isUpdate = bundle.getBoolean("isUpdate", false);
		rowId = bundle.getLong("rowId", 0);
		language = bundle.getString("language", "");

		for (int i = 0; i < files.length; i++)
			queue.add(files[i]);

		setup();

		new Thread() {
			@Override
			public void run() {
				tvdb = new TheTVDb(getApplicationContext());
				if (isUpdate) {
					thisShow = tvdb.searchForShow(MizLib.decryptEpisode(files[0], PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ignoredTags", "")), LOCALE);
					createShow("", "");
				} else if (isUnidentifiedIdentify) {
					thisShow = tvdb.getShow(bundle.getString("tvdbId"), LOCALE);

					if (rowsToDrop != null) {
						DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
						for (int i = 0; i < rowsToDrop.length; i++)
							db.deleteEpisode(rowsToDrop[i]);

						createShow("", "");
					} else {
						createShow(bundle.getString("season"), bundle.getString("episode"));
					}
				} else if (isEpisodeIdentify) {
					thisShow = tvdb.getShow(bundle.getString("tvdbId"), LOCALE);
					loadNextEpisode(bundle.getString("season"), bundle.getString("episode"));
				} else if (isShowIdentify) {
					// Delete the old show based on its ID
					TvShow temp = new TvShow(getApplicationContext(),
							bundle.getString("oldShowId"),
							"", "", "", "", "", "", "", "",
							false, "0");

					MizLib.deleteShow(getApplicationContext(), temp, false);

					thisShow = tvdb.getShow(bundle.getString("tvdbId"), LOCALE);
					createShow("", "");
				}
			}
		}.start();

		return Service.START_NOT_STICKY;
	}

	private void loadNextEpisode() {
		loadNextEpisode("", ""); // This will be ignored
	}

	private synchronized void loadNextEpisode(final String season, final String episode) {
		synchronized (queue) {
			if (queue.peek() != null && !queue.isEmpty()) {
				new Thread() {
					@Override
					public void run() {
						String file = queue.poll();
						if (season.isEmpty() && episode.isEmpty()) {
							DecryptedShowEpisode decrypted = MizLib.decryptEpisode(file, PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ignoredTags", ""));
							downloadEpisode(MizLib.addIndexZero(decrypted.getSeason()), MizLib.addIndexZero(decrypted.getEpisode()), file);
						} else {
							downloadEpisode(season, episode, file);
						}
					}
				}.start();
			} else {			
				updateWidgets();
				stopSelf(); // Let's stop this service, so a new one can be started...
			}
		}
	}

	private void setup() {
		localizedInfo = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefsUseLocalData", false);

		if (language.isEmpty())
			if (localizedInfo) {
				LOCALE = Locale.getDefault().toString();
				if (LOCALE.contains("_"))
					LOCALE = LOCALE.substring(0, LOCALE.indexOf("_"));

				if (!MizLib.tvdbLanguages.contains(LOCALE)) // Check if system language is supported by TheTVDb
					LOCALE = "en";
			} else LOCALE = "en";
		else
			LOCALE = language;

		thumbsFolder = MizLib.getTvShowThumbFolder(this);
	}

	private void createShow(String season, String episode) {

		boolean downloadCovers = true;

		// Check if the show already exists before downloading the show info
		if (!thisShow.getId().equals("invalid") && !thisShow.getId().isEmpty()) {
			DbAdapterTvShow db = MizuuApplication.getTvDbAdapter();
			Cursor cursor = db.getShow(thisShow.getId());
			if (cursor.getCount() > 0) {
				downloadCovers = false;
			}
		}

		if (downloadCovers) {
			if (!thisShow.getId().equals("invalid") && !thisShow.getId().isEmpty()) {
				String thumb_filepath = new File(thumbsFolder, thisShow.getId() + ".jpg").getAbsolutePath();
				String backdrop_filepath = new File(MizLib.getTvShowBackdropFolder(this), thisShow.getId() + "_tvbg.jpg").getAbsolutePath();

				// Download the cover file and try again if it fails
				if (!thisShow.getCover_url().isEmpty())
					if (!MizLib.downloadFile(thisShow.getCover_url(), thumb_filepath))
						MizLib.downloadFile(thisShow.getCover_url(), thumb_filepath);

				MizLib.resizeBitmapFileToCoverSize(getApplicationContext(), thumb_filepath);

				// Download the backdrop image file and try again if it fails
				if (!thisShow.getBackdrop_url().isEmpty())
					if (!MizLib.downloadFile(thisShow.getBackdrop_url(), backdrop_filepath))
						MizLib.downloadFile(thisShow.getBackdrop_url(), backdrop_filepath);

				DbAdapterTvShow dbHelper = MizuuApplication.getTvDbAdapter();
				dbHelper.createShow(thisShow.getId(), thisShow.getTitle(), thisShow.getDescription(), thisShow.getActors(), thisShow.getGenre(),
						thisShow.getRating(), thisShow.getCertification(), thisShow.getRuntime(), thisShow.getFirst_aired(), "0");
			}
		}

		loadNextEpisode(season, episode);
	}

	private void downloadEpisode(String season, String episode, String filepath) {		
		Episode thisEpisode = new Episode();

		ArrayList<Episode> episodes = thisShow.getEpisodes();
		int count = episodes.size();
		for (int i = 0; i < count; i++) {
			if (MizLib.addIndexZero(episodes.get(i).getSeason()).equals(season) && MizLib.addIndexZero(episodes.get(i).getEpisode()).equals(episode)) {
				thisEpisode = episodes.get(i);
				continue;
			}
		}

		if (thisEpisode.getEpisode().isEmpty()) {
			thisEpisode.setEpisode(episode);
			thisEpisode.setSeason(season);
		}

		// Download the episode screenshot file and try again if it fails
		if (!thisEpisode.getScreenshot_url().isEmpty()) {
			String screenshotFile = new File(MizLib.getTvShowEpisodeFolder(this), thisShow.getId() + "_S" + season + "E" + episode + ".jpg").getAbsolutePath();
			if (!MizLib.downloadFile(thisEpisode.getScreenshot_url(), screenshotFile))
				MizLib.downloadFile(thisEpisode.getScreenshot_url(), screenshotFile);
		}

		addToDatabase(thisEpisode, filepath);
	}

	private void addToDatabase(Episode ep, String filepath) {
		DbAdapterTvShowEpisode dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
		if (isUpdate || isShowIdentify || rowsToDrop != null) {
			dbHelper.createEpisode(filepath, MizLib.addIndexZero(ep.getSeason()), MizLib.addIndexZero(ep.getEpisode()), thisShow.getId(), ep.getTitle(), ep.getDescription(),
					ep.getAirdate(), ep.getRating(), ep.getDirector(), ep.getWriter(), ep.getGueststars(), "0", "0");
		} else {
			dbHelper.updateEpisode(rowId, filepath, MizLib.addIndexZero(ep.getSeason()), MizLib.addIndexZero(ep.getEpisode()), thisShow.getId(), ep.getTitle(), ep.getDescription(),
					ep.getAirdate(), ep.getRating(), ep.getDirector(), ep.getWriter(), ep.getGueststars(), "0", "0"); 
		}

		updateNotification(ep);
	}

	private void updateNotification(Episode ep) {
		Intent intent = null;
		if (isUpdate) {
			intent = new Intent("mizuu-tvshows-object");
			intent.putExtra("id", thisShow.getId());
			intent.putExtra("movieName", thisShow.getTitle() + " S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()) +
					(!ep.getTitle().isEmpty() ? " (" + ep.getTitle() + ")" : ""));
			intent.putExtra("thumbFile", new File(thumbsFolder, thisShow.getId() + ".jpg").getAbsolutePath());

			File backdropFile = new File(MizLib.getTvShowEpisodeFolder(this), thisShow.getId() + "_S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()) + ".jpg");
			if (!backdropFile.exists())
				backdropFile = new File(thumbsFolder, thisShow.getId() + ".jpg");

			intent.putExtra("backdrop", backdropFile.getAbsolutePath());

			sendUpdateBroadcast(intent);
		} else {
			if (queue.peek() == null) {
				intent = new Intent("mizuu-tvshows-identification");
				sendUpdateBroadcast(intent);
			}
		}

		loadNextEpisode();
	}

	private void sendUpdateBroadcast(Intent i) {
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
	}

	private void updateWidgets() {
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}