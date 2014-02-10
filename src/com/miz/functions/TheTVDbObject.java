package com.miz.functions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.miz.widgets.ShowCoverWidgetProvider;
import com.miz.widgets.ShowStackWidgetProvider;

public class TheTVDbObject {

	private TvShowLibraryUpdateCallback callback;
	private Context context;
	private String LOCALE = "", language = "", ignoredTags = "";
	private boolean localizedInfo = false;
	private File thumbsFolder;
	private TheTVDb tvdb;
	private Tvshow thisShow;
	private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
	private Bitmap cover, backdrop;

	public TheTVDbObject(Context context, Collection<String> files, String language, TvShowLibraryUpdateCallback callback) {
		this.context = context;
		this.language = language;
		this.callback = callback;
		this.ignoredTags = PreferenceManager.getDefaultSharedPreferences(context).getString("ignoredTags", "");

		for (String file : files)
			queue.add(file);

		setup();

		tvdb = new TheTVDb(context);
		thisShow = tvdb.searchForShow(MizLib.decryptEpisode(queue.peek(), ignoredTags), LOCALE);
		createShow();
	}

	private void loadEpisodes() {
		int count = 0;
		for (String file : queue) {
			if (file == null)
				continue;

			DecryptedShowEpisode decrypted = MizLib.decryptEpisode(file, ignoredTags);
			downloadEpisode(MizLib.addIndexZero(decrypted.getSeason()), MizLib.addIndexZero(decrypted.getEpisode()), file);

			count++;
		}

		updateWidgets();

		File coverFile = new File(thumbsFolder, thisShow.getId() + ".jpg");
		File backdropFile = new File(MizLib.getTvShowBackdropFolder(context), thisShow.getId() + "_tvbg.jpg");
		if (!backdropFile.exists())
			backdropFile = coverFile;
		
		cover = MizLib.getNotificationImageThumbnail(context, coverFile.getAbsolutePath());
		backdrop = MizLib.decodeSampledBitmapFromFile(backdropFile.getAbsolutePath(), getNotificationImageWidth(), getNotificationImageHeight());
		
		callback.onTvShowAdded(thisShow.getId(), thisShow.getTitle(), cover, backdrop, count);
	}

	private void setup() {
		localizedInfo = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefsUseLocalData", false);

		if (language == null || language.isEmpty())
			if (localizedInfo) {
				LOCALE = Locale.getDefault().toString();
				if (LOCALE.contains("_"))
					LOCALE = LOCALE.substring(0, LOCALE.indexOf("_"));

				if (!MizLib.tvdbLanguages.contains(LOCALE)) // Check if system language is supported by TheTVDb
					LOCALE = "en";
			} else LOCALE = "en";
		else
			LOCALE = language;

		thumbsFolder = MizLib.getTvShowThumbFolder(context);
	}

	private void createShow() {

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
				String backdrop_filepath = new File(MizLib.getTvShowBackdropFolder(context), thisShow.getId() + "_tvbg.jpg").getAbsolutePath();

				// Download the cover file and try again if it fails
				if (!thisShow.getCover_url().isEmpty())
					if (!MizLib.downloadFile(thisShow.getCover_url(), thumb_filepath))
						MizLib.downloadFile(thisShow.getCover_url(), thumb_filepath);

				MizLib.resizeBitmapFileToCoverSize(context, thumb_filepath);

				// Download the backdrop image file and try again if it fails
				if (!thisShow.getBackdrop_url().isEmpty())
					if (!MizLib.downloadFile(thisShow.getBackdrop_url(), backdrop_filepath))
						MizLib.downloadFile(thisShow.getBackdrop_url(), backdrop_filepath);

				DbAdapterTvShow dbHelper = MizuuApplication.getTvDbAdapter();
				dbHelper.createShow(thisShow.getId(), thisShow.getTitle(), thisShow.getDescription(), thisShow.getActors(), thisShow.getGenre(),
						thisShow.getRating(), thisShow.getCertification(), thisShow.getRuntime(), thisShow.getFirst_aired(), "0");
			}
		}

		loadEpisodes();
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
			String screenshotFile = new File(MizLib.getTvShowEpisodeFolder(context), thisShow.getId() + "_S" + season + "E" + episode + ".jpg").getAbsolutePath();
			if (!MizLib.downloadFile(thisEpisode.getScreenshot_url(), screenshotFile))
				MizLib.downloadFile(thisEpisode.getScreenshot_url(), screenshotFile);
		}

		addToDatabase(thisEpisode, filepath);
	}

	private void addToDatabase(Episode ep, String filepath) {
		DbAdapterTvShowEpisode dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
		dbHelper.createEpisode(filepath, MizLib.addIndexZero(ep.getSeason()), MizLib.addIndexZero(ep.getEpisode()), thisShow.getId(), ep.getTitle(), ep.getDescription(),
				ep.getAirdate(), ep.getRating(), ep.getDirector(), ep.getWriter(), ep.getGueststars(), "0", "0");

		updateNotification(ep, filepath);
	}

	private void updateNotification(Episode ep, String filepath) {
		File coverFile = new File(thumbsFolder, thisShow.getId() + ".jpg");
		File backdropFile = new File(MizLib.getTvShowEpisodeFolder(context), thisShow.getId() + "_S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()) + ".jpg");
		if (!backdropFile.exists())
			backdropFile = coverFile;

		cover = MizLib.getNotificationImageThumbnail(context, coverFile.getAbsolutePath());
		backdrop = MizLib.decodeSampledBitmapFromFile(backdropFile.getAbsolutePath(), getNotificationImageWidth(), getNotificationImageHeight());
		
		callback.onEpisodeAdded(thisShow.getId(), thisShow.getId().equals("invalid") ? filepath : thisShow.getTitle() + " S" + MizLib.addIndexZero(ep.getSeason()) + "E" + MizLib.addIndexZero(ep.getEpisode()), cover, backdrop);
	}

	private void updateWidgets() {
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(context, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}

	// These variables don't need to be re-initialized
	private int widgetWidth = 0, widgetHeight = 0;

	private int getNotificationImageWidth() {
		if (widgetWidth == 0)
			widgetWidth = MizLib.getLargeNotificationWidth(context);
		return widgetWidth;
	}

	private int getNotificationImageHeight() {
		if (widgetHeight == 0)
			widgetHeight = MizLib.getLargeNotificationWidth(context) / 2;
		return widgetHeight;
	}
}