package com.miz.service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import jcifs.smb.SmbFile;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.AsyncTask;
import com.miz.functions.FileSource;
import com.miz.functions.MizFile;
import com.miz.functions.MizLib;
import com.miz.mizuu.CancelUpdateDialog;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.miz.widgets.ShowCoverWidgetProvider;
import com.miz.widgets.ShowStackWidgetProvider;

public class UpdateShowsService extends Service {

	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder builder;
	private PendingIntent contentIntent;
	private SharedPreferences settings;
	private HashMap<String, String> existingEpisodes = new HashMap<String, String>();
	private ArrayList<FileSource> storageDirectories = new ArrayList<FileSource>();
	private LinkedList<String> queue = new LinkedList<String>();
	private TreeSet<MizFile> uniqueVideos = new TreeSet<MizFile>();
	private boolean clearLibrary = false, subfolderSearch = true, clearUnavailable, prefsDisableEthernetWiFiCheck, ignoreRemovedFiles, syncLibraries;
	private int count, total, mapCount;
	private final int NOTIFICATION_ID = 220;
	private Editor editor;

	private TreeMap<String, ArrayList<MizFile>> map = new TreeMap<String, ArrayList<MizFile>>();

	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (!MizLib.isOnline(this))
			stopSelf();

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelUpdateDialog.class);
		notificationIntent.putExtra("type", CancelUpdateDialog.TVSHOWS);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Setup up notification
		builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.refresh);
		builder.setTicker(getString(R.string.updateLookingForFiles));
		builder.setContentTitle(getString(R.string.updatingTvShows) + " (0%)");
		builder.setContentText(getString(R.string.updateLookingForFiles));
		builder.setContentIntent(contentIntent);
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.refresh));
		builder.addAction(R.drawable.remove, getString(R.string.stringCancelUpdate), contentIntent);

		// Build notification
		Notification updateNotification = builder.build();

		// Show the notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, updateNotification);

		// Tell the system that this is an ongoing notification, so it shouldn't be killed
		startForeground(NOTIFICATION_ID, updateNotification);

		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		clearLibrary = settings.getBoolean("prefsClearLibrary", false);
		subfolderSearch = settings.getBoolean("prefsEnableSubFolderSearch", true);
		clearUnavailable = settings.getBoolean("prefsRemoveUnavailable", false);
		prefsDisableEthernetWiFiCheck = settings.getBoolean("prefsDisableEthernetWiFiCheck", false);
		ignoreRemovedFiles = settings.getBoolean("prefsIgnoredFilesEnabled", false);
		syncLibraries = settings.getBoolean("syncLibrariesWithTrakt", true);

		editor = settings.edit();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-tvshows-object"));

		new ShowUpdateSetup().execute();

		return Service.START_NOT_STICKY;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String id = intent.getExtras().getString("id", ""), contentText = "";
			if (id.isEmpty() || id.equalsIgnoreCase("invalid"))
				contentText = getString(R.string.unidentified) + ": " + intent.getExtras().getString("movieName");
			else
				contentText = getString(R.string.stringJustAdded) + ": " + intent.getExtras().getString("movieName");

			// Set up the notification with cover art image, title and backdrop image
			builder.setLargeIcon(MizLib.getNotificationImageThumbnail(context, intent.getExtras().getString("thumbFile")));
			builder.setContentTitle(getString(R.string.updatingTvShows) + " (" + (int) ((100.0 / (double) total) * (double) count) + "%)");
			builder.setContentText(contentText);
			builder.setStyle(new NotificationCompat.BigPictureStyle()
			.setSummaryText(contentText)
			.bigPicture(MizLib.decodeSampledBitmapFromFile(intent.getExtras().getString("backdrop"), MizLib.getLargeNotificationWidth(context), MizLib.getLargeNotificationWidth(context) / 2)));

			// Show the updated notification
			mNotificationManager.notify(NOTIFICATION_ID, builder.build());

			count++;

			if (count == mapCount) {
				updateNextShow(); // There are no more files in this map, continue to the next map
			}
		}
	};

	private class ShowUpdateSetup extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {

			setup();

			if (clearLibrary) {
				editor.putBoolean("prefsClearLibrary", false);
				editor.commit();

				// Remove all previous entries from the database
				removeShowsFromDatabase();
			} else if (!clearLibrary && clearUnavailable) {
				// Remove all unavailable files from the database
				removeUnavailableFiles();
			}

			goThroughFiles();

			return null;
		}
	}

	private void setup() {		
		DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();
		Cursor c = dbHelperSources.fetchAllShowSources();
		while (c.moveToNext()) {
			storageDirectories.add(new FileSource(
					c.getLong(c.getColumnIndex(DbAdapterSources.KEY_ROWID)),
					c.getString(c.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
					c.getInt(c.getColumnIndex(DbAdapterSources.KEY_IS_SMB)),
					c.getString(c.getColumnIndex(DbAdapterSources.KEY_USER)),
					c.getString(c.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
					c.getString(c.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
					c.getString(c.getColumnIndex(DbAdapterSources.KEY_TYPE))
					));
		}
		c.close();
	}

	private void updateNextShow() {
		sendUpdateBroadcast();

		if (queue.peek() != null) {			
			String show = queue.pop();
			ArrayList<MizFile> mizfiles = map.get(show);
			String[] files = new String[mizfiles.size()];

			for (int i = 0; i < mizfiles.size(); i++)
				files[i] = mizfiles.get(i).getAbsolutePath();

			mapCount += mizfiles.size();

			if (MizLib.isOnline(getApplicationContext())) {				
				Intent tvdbIntent = new Intent(this, TheTVDB.class);
				tvdbIntent.putExtra("showName", show);
				tvdbIntent.putExtra("files", files);
				tvdbIntent.putExtra("isUpdate", true);

				startService(tvdbIntent);
			} else {
				stopSelf();
			}
		} else {
			// There are no more videos, clean up and stop the update service
			stopSelf();
		}
	}

	private static MizFile tempMizFile;
	private static SmbFile tempSmbFile;

	private void checkAllDirsAndFiles(MizFile dir) {
		try {
			if (subfolderSearch) {
				if (dir.isDirectory()) {
					String[] childs = dir.list();				
					for (int i = 0; i < childs.length; i++) {
						if (dir.getAbsolutePath().startsWith("smb://")) {
							tempSmbFile = new SmbFile(dir.getAbsolutePath() + childs[i] + "/");
							if (tempSmbFile.isDirectory()) {
								tempMizFile = new MizFile(tempSmbFile);
								checkAllDirsAndFiles(tempMizFile);
							} else {
								tempSmbFile = new SmbFile(dir.getAbsolutePath() + childs[i]);
								tempMizFile = new MizFile(tempSmbFile);
								addToResults(tempMizFile);
							}
						} else {
							tempMizFile = new MizFile(new File(dir.getAbsolutePath() + "/" + childs[i]));
							checkAllDirsAndFiles(tempMizFile);
						}
					}
				} else {
					addToResults(dir);
				}
			} else {
				MizFile[] children = dir.listFiles();
				for (int i = 0; i < children.length; i++)
					addToResults(children[i]);
			}
		} catch (Exception e) {}
	}

	private void addToResults(MizFile f) {
		if (MizLib.checkFileTypes(f.getAbsolutePath())) {
			if (f.length() < MizLib.getFileSizeLimit(getApplicationContext()))
				return;

			if (!clearLibrary)
				if (existingEpisodes.get(f.getAbsolutePath()) != null) return;

			// Add the file if it reaches this point
			synchronized (uniqueVideos) {
				uniqueVideos.add(f);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

		MizLib.scheduleShowsUpdate(this);

		if (MizLib.hasTraktAccount(this) && syncLibraries) {
			Intent shows = new Intent(getApplicationContext(), TraktTvShowsSyncService.class);
			getApplicationContext().startService(shows);
		}
	}

	private void goThroughFiles() {

		DbAdapterTvShowEpisode dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
		Cursor cursor = dbHelper.getAllEpisodesInDatabase(ignoreRemovedFiles); // Query database to return all show episodes to a cursor
		while (cursor.moveToNext()) // Add all show episodes in cursor to ArrayList of all existing episodes
			existingEpisodes.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)), "");
		cursor.close(); // Close cursor

		int count = storageDirectories.size();
		for (int i = 0; i < count; i++) {
			if (storageDirectories.get(i).isSmb()) {
				if (MizLib.isOnline(getApplicationContext())) {
					try {
						MizFile storageDirectory = new MizFile(new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(storageDirectories.get(i).getDomain(), "utf-8"),
										URLEncoder.encode(storageDirectories.get(i).getUser(), "utf-8"),
										URLEncoder.encode(storageDirectories.get(i).getPassword(), "utf-8"),
										storageDirectories.get(i).getFilepath(),
										true
										)));
						if (storageDirectory.exists())
							checkAllDirsAndFiles(storageDirectory);
					}
					catch (MalformedURLException e) {}
					catch (UnsupportedEncodingException e) {}
				}
			} else {
				MizFile storageDirectory = new MizFile(new File(storageDirectories.get(i).getFilepath()));
				if (storageDirectory.exists())
					checkAllDirsAndFiles(storageDirectory);
			}
		}

		synchronized (uniqueVideos) {
			for (MizFile f : uniqueVideos) {
				String decryptedShowname = MizLib.decryptEpisode(f.getAbsolutePath(), settings.getString("ignoredTags", "")).getDecryptedFileName();

				if (map.get(decryptedShowname) == null)
					map.put(decryptedShowname, new ArrayList<MizFile>());

				map.get(decryptedShowname).add(f);
			}
		}

		queue.addAll(map.keySet());

		// Set the total size of unique videos
		synchronized (uniqueVideos) {
			total = uniqueVideos.size();
		}

		// Clear up resources
		synchronized (uniqueVideos) {
			uniqueVideos.clear();
		}
		existingEpisodes.clear();

		if (queue.size() > 0) {
			updateNextShow();
		} else {
			mNotificationManager.cancel(NOTIFICATION_ID);
			sendUpdateBroadcast();
			stopSelf();
		}
	}

	private void removeShowsFromDatabase() {
		// Delete all shows from the database
		DbAdapterTvShow db = MizuuApplication.getTvDbAdapter();
		db.deleteAllShowsInDatabase();

		DbAdapterTvShowEpisode dbEpisodes = MizuuApplication.getTvEpisodeDbAdapter();
		dbEpisodes.deleteAllEpisodesInDatabase();

		// Delete all downloaded images files from the device
		MizLib.deleteRecursive(MizLib.getTvShowThumbFolder(this));
		MizLib.deleteRecursive(MizLib.getTvShowEpisodeFolder(this));
		MizLib.deleteRecursive(MizLib.getTvShowBackdropFolder(this));
	}

	private void removeUnavailableFiles() {
		ArrayList<DbEpisode> dbEpisodes = new ArrayList<DbEpisode>(),
				removedEpisodes = new ArrayList<DbEpisode>();

		// Fetch all the movies from the database
		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor tempCursor = db.getAllEpisodesInDatabase(ignoreRemovedFiles);
		while (tempCursor.moveToNext()) {
			try {
				dbEpisodes.add(new DbEpisode(tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)) + "_S" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)) + "E" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)) + ".jpg"));
			} catch (NullPointerException e) {}
		}
		tempCursor.close();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_SHOWS, true);

		for (int i = 0; i < dbEpisodes.size(); i++) {
			if (dbEpisodes.get(i).isNetworkFile()) {
				if (MizLib.isWifiConnected(getApplicationContext(), prefsDisableEthernetWiFiCheck)) {
					FileSource source = null;

					for (int j = 0; j < filesources.size(); j++)
						if (dbEpisodes.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
							source = filesources.get(j);
							continue;
						}

					if (source == null) {
						boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
						if (deleted)
							removedEpisodes.add(dbEpisodes.get(i));
						continue;
					}

					try {
						final SmbFile file = new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										dbEpisodes.get(i).getFilepath(),
										false
										));

						if (!file.exists()) {
							boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
							if (deleted)
								removedEpisodes.add(dbEpisodes.get(i));
						}
					} catch (Exception e) {
						boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
						if (deleted)
							removedEpisodes.add(dbEpisodes.get(i));
					}
				}
			} else {
				if (!new File(dbEpisodes.get(i).getFilepath()).exists()) {
					boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
					if (deleted)
						removedEpisodes.add(dbEpisodes.get(i));
				}
			}
		}

		int count = removedEpisodes.size();
		for (int i = 0; i < count; i++) {
			if (db.getEpisodeCount(removedEpisodes.get(i).getShowId()) == 0) { // No more episodes for this show
				DbAdapterTvShow dbShow = MizuuApplication.getTvDbAdapter();
				boolean deleted = dbShow.deleteShow(removedEpisodes.get(i).getShowId());

				if (deleted) {
					MizLib.deleteFile(new File(removedEpisodes.get(i).getThumbnail()));
					MizLib.deleteFile(new File(removedEpisodes.get(i).getBackdrop()));
				}
			}

			MizLib.deleteFile(new File(removedEpisodes.get(i).getEpisodeCoverPath()));
		}

		// Clean up
		dbEpisodes.clear();
		removedEpisodes.clear();
	}

	private void sendUpdateBroadcast() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("mizuu-shows-update"));
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private class DbEpisode {

		private String filepath, rowId, showId, episodeCoverPath;

		public DbEpisode(String filepath, String rowId, String showId, String episodeCoverPath) {
			this.filepath = filepath;
			this.rowId = rowId;
			this.showId = showId;
			this.episodeCoverPath = episodeCoverPath;
		}

		public String getFilepath() {
			if (filepath.contains("smb") && filepath.contains("@"))
				return "smb://" + filepath.substring(filepath.indexOf("@") + 1);
			return filepath.replace("/smb:/", "smb://");
		}

		public String getRowId() {
			return rowId;
		}

		public String getShowId() {
			return showId;
		}

		public String getEpisodeCoverPath() {
			return new File(MizLib.getTvShowEpisodeFolder(getApplicationContext()), episodeCoverPath).getAbsolutePath();
		}

		public String getThumbnail() {
			return new File(MizLib.getTvShowThumbFolder(getApplicationContext()), getShowId() + ".jpg").getAbsolutePath();
		}

		public String getBackdrop() {
			return new File(MizLib.getTvShowBackdropFolder(getApplicationContext()), getShowId() + "_tvbg.jpg").getAbsolutePath();
		}

		public boolean isNetworkFile() {
			return getFilepath().contains("smb:/");
		}
	}
}