package com.miz.mizuu;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jcifs.smb.SmbFile;

import org.json.JSONObject;

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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.functions.FileSource;
import com.miz.functions.MizFile;
import com.miz.functions.MizLib;
import com.miz.functions.TraktMoviesSyncService;
import com.miz.widgets.MovieBackdropWidgetProvider;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;

public class UpdateMovieService extends Service implements OnSharedPreferenceChangeListener {

	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder builder;
	private PendingIntent contentIntent;
	private SharedPreferences settings;
	private HashMap<String, String> existingMovies = new HashMap<String, String>();
	private ArrayList<FileSource> storageDirectories;
	private LinkedList<MizFile> queue = new LinkedList<MizFile>();
	private TreeMap<String, MizFile> unique = new TreeMap<String, MizFile>();
	private boolean clearLibrary = false, subfolderSearch = true, clearUnavailable, ignoreNfoFiles, prefsDisableEthernetWiFiCheck, ignoreRemovedFiles, syncLibraries;
	private int count, total;
	private final int NOTIFICATION_ID = 200;
	private Editor editor;

	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelUpdateDialog.class);
		notificationIntent.putExtra("type", CancelUpdateDialog.MOVIE);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Setup up notification
		builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.refresh);
		builder.setTicker(getString(R.string.updateLookingForFiles));
		builder.setContentTitle(getString(R.string.updatingMovies) + " (0%)");
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
		ignoreNfoFiles = settings.getBoolean("prefsIgnoreNfoFiles", true);
		prefsDisableEthernetWiFiCheck = settings.getBoolean("prefsDisableEthernetWiFiCheck", false);
		ignoreRemovedFiles = settings.getBoolean("prefsIgnoredFilesEnabled", false);
		syncLibraries = settings.getBoolean("syncLibrariesWithTrakt", true);

		editor = settings.edit();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-object"));

		new MovieUpdateSetup().start();

		return Service.START_NOT_STICKY;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Set up the notification with cover art image, title and backdrop image
			builder.setLargeIcon(MizLib.getNotificationImageThumbnail(context, intent.getExtras().getString("thumbFile")));
			builder.setContentTitle(getString(R.string.updatingMovies) + " (" + (int) ((100.0 / (double) total) * (double) count) + "%)");
			builder.setContentText(getString(R.string.stringJustAdded) + ": " + intent.getExtras().getString("movieName"));
			builder.setStyle(new NotificationCompat.BigPictureStyle()
			.setSummaryText(getString(R.string.stringJustAdded) + ": " + intent.getExtras().getString("movieName"))
			.bigPicture(MizLib.decodeSampledBitmapFromFile(intent.getExtras().getString("backdrop"), MizLib.getLargeNotificationWidth(context), MizLib.getLargeNotificationWidth(context) / 2)));

			// Show the updated notification
			mNotificationManager.notify(NOTIFICATION_ID, builder.build());

			updateNextMovie();
		}
	};

	private class MovieUpdateSetup extends Thread {
		@Override
		public void run() {
			setup();
			
			// Remove unavailable movies, so we can try to identify them again
			removeUnidentifiedMovies();

			if (clearLibrary) {
				editor.putBoolean("prefsClearLibrary", false);
				editor.commit();

				// Remove all previous entries from the database
				removeMoviesFromDatabase();
			} else if (!clearLibrary && clearUnavailable) {
				// Remove all unavailable files from the database
				removeUnavailableFiles();
			}

			// Start the update by getting refreshed configuration URLs
			getConfiguration();
		}
	}

	private void setup() {
		storageDirectories = new ArrayList<FileSource>();
		DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();
		Cursor c = dbHelperSources.fetchAllMovieSources();
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

	private void updateNextMovie() {
		if (queue.peek() != null) {
			final MizFile file = queue.pop();
			count++;

			if (ignoreNfoFiles) {
				if (MizLib.isOnline(getApplicationContext())) {
					Intent tmdbIntent = new Intent(this, TheMovieDB.class);
					Bundle b = new Bundle();
					b.putString("filepath", file.getAbsolutePath());
					tmdbIntent.putExtras(b);

					startService(tmdbIntent);
				} else {
					stopSelf();
				}
			} else {
				new Thread() {
					@Override
					public void run() {

						MizFile nfoFile = null;

						String filename = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")).replace("cd1", "").replace("cd2", "").replace("part1", "").replace("part2", "").trim();
						MizFile parent = null;

						if (file.isSmb()) {
							try {
								parent = new MizFile(new SmbFile(file.getParent()));
							} catch (MalformedURLException e) {}
						} else {
							parent = new MizFile(new File(file.getParent()));
						}


						if (parent != null) {
							MizFile[] allFiles = parent.listFiles();

							for (int i = 0; i < allFiles.length; i++) {
								String name = allFiles[i].getName();
								String absolutePath = allFiles[i].getAbsolutePath();
								if (name.equalsIgnoreCase("movie.nfo") ||
										absolutePath.equalsIgnoreCase(filename + ".nfo")) {
									nfoFile = allFiles[i];
									continue;
								}	
							}
						}

						// If there's a .nfo file, process it
						if (nfoFile != null) {
							new NfoMovie(file, nfoFile, getApplicationContext());
						} else {	
							if (MizLib.isOnline(getApplicationContext())) {
								Intent tmdbIntent = new Intent(getApplicationContext(), TheMovieDB.class);
								Bundle b = new Bundle();
								b.putString("filepath", file.getAbsolutePath());
								tmdbIntent.putExtras(b);

								startService(tmdbIntent);
							} else {
								stopSelf();
							}
						}
					}
				}.start();
			}
		} else {
			// There are no more videos, stop the update service
			stopSelf();
		}
	}

	private static MizFile tempMizFile;
	private static SmbFile tempSmbFile;

	private void checkAllDirsAndFiles(MizFile dir) {
		try {
			if (subfolderSearch) {
				if (dir.isDirectory()) {
					// Check if this is a DVD folder
					if (dir.getName().equalsIgnoreCase("video_ts/") || dir.getName().equalsIgnoreCase("video_ts")) { // slash in the end == smb
						MizFile[] children = dir.listFiles();
						for (int i = 0; i < children.length; i++) {
							if (children[i].getName().equalsIgnoreCase("video_ts.ifo"))
								addToResults(children[i]);
						}
					} // Check if this is a Blu-ray folder
					else if (dir.getName().equalsIgnoreCase("bdmv/") || dir.getName().equalsIgnoreCase("bdmv")) { // slash in the end == smb
						MizFile[] children = dir.listFiles();
						for (int i = 0; i < children.length; i++) {
							if (children[i].getName().equalsIgnoreCase("stream/") || children[i].getName().equalsIgnoreCase("stream")) {
								MizFile[] m2tsVideoFiles = children[i].listFiles();

								if (m2tsVideoFiles.length > 0) {
									MizFile largestFile = m2tsVideoFiles[0];

									for (int j = 0; j < m2tsVideoFiles.length; j++)
										if (largestFile.length() < m2tsVideoFiles[j].length())
											largestFile = m2tsVideoFiles[j];

									addToResults(largestFile);
								}
							}
						}
					} else {
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
			if (f.length() < MizLib.getFileSizeLimit(getApplicationContext())
					&& !f.getName().equalsIgnoreCase("video_ts.ifo"))
				return;

			if (!clearLibrary)
				if (existingMovies.get(f.getAbsolutePath()) != null) return;

			String tempFileName = f.getName().substring(0, f.getName().lastIndexOf("."));
			if (tempFileName.toLowerCase(Locale.ENGLISH).matches(".*cd2") || tempFileName.toLowerCase(Locale.ENGLISH).matches(".*part2")) return;

			//Add the file if it reaches this point
			unique.put(f.getName(), f);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		ignoreNfoFiles = settings.getBoolean("prefsIgnoreNfoFiles", true);
	}

	private void getConfiguration() {
		String baseUrl = "";
		try {
			if (MizLib.isOnline(getApplicationContext())) {
				JSONObject jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/configuration?api_key=" + MizLib.TMDB_API);
				try {
					baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) {
					baseUrl = "http://d3gtl9l2a4fn1j.cloudfront.net/t/p/";
				}
			}
		} catch (Exception e) {
			baseUrl = "http://d3gtl9l2a4fn1j.cloudfront.net/t/p/";
		} finally {
			editor = settings.edit();
			editor.putString("tmdbBaseUrl", baseUrl);
			editor.commit();

			goThroughFiles();
		}
	}

	private void goThroughFiles() {
		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();
		Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", ignoreRemovedFiles); // Query database to return all movies to a cursor

		while (cursor.moveToNext()) {// Add all movies in cursor to ArrayList of all existing movies
			existingMovies.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)), "");
		}

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

		for (Map.Entry<String, MizFile> entry : unique.entrySet())
			queue.add(entry.getValue());

		total = queue.size();

		unique.clear(); // Clear up resources
		existingMovies.clear(); // Clear up resources

		if (queue.size() > 0) {
			updateNextMovie();
		} else {
			mNotificationManager.cancel(NOTIFICATION_ID);
			stopSelf();
		}
	}

	private void removeUnidentifiedMovies() {
		ArrayList<DbMovie> dbMovies = new ArrayList<DbMovie>();

		// Fetch all the movies from the database
		DbAdapter db = MizuuApplication.getMovieAdapter();

		Cursor tempCursor = db.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", ignoreRemovedFiles);
		while (tempCursor.moveToNext()) {
			try {
				dbMovies.add(new DbMovie(tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
						tempCursor.getLong(tempCursor.getColumnIndex(DbAdapter.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_GENRES)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_TITLE))));
			} catch (NullPointerException e) {}
		}
		
		tempCursor.close();
		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);
		
		for (int i = 0; i < dbMovies.size(); i++) {
			if (dbMovies.get(i).isNetworkFile()) {
				if (MizLib.isWifiConnected(getApplicationContext(), prefsDisableEthernetWiFiCheck)) {
					FileSource source = null;

					for (int j = 0; j < filesources.size(); j++)
						if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
							source = filesources.get(j);
							continue;
						}

					if (source == null) {
						if (dbMovies.get(i).isUnidentified()) {
							db.deleteMovie(dbMovies.get(i).getRowId());
						}
						continue;
					}

					try {
						final SmbFile file = new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										dbMovies.get(i).getFilepath(),
										false
										));

						if (file.exists()) {
							if (dbMovies.get(i).isUnidentified()) {
								db.deleteMovie(dbMovies.get(i).getRowId());
							}
						}
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			} else {
				if (new File(dbMovies.get(i).getFilepath()).exists()) {
					if (dbMovies.get(i).isUnidentified()) {
						db.deleteMovie(dbMovies.get(i).getRowId());
					}
				}
			}
		}
		
		dbMovies.clear();
		filesources.clear();
	}

	private void removeMoviesFromDatabase() {
		// Delete all movies from the database
		DbAdapter db = MizuuApplication.getMovieAdapter();
		db.deleteAllMovies();

		// Delete all downloaded images files from the device
		MizLib.deleteRecursive(MizLib.getMovieThumbFolder(this));
		MizLib.deleteRecursive(MizLib.getMovieBackdropFolder(this));
	}

	private void removeUnavailableFiles() {
		ArrayList<DbMovie> dbMovies = new ArrayList<DbMovie>(),
				deletedMovies = new ArrayList<DbMovie>();

		// Fetch all the movies from the database
		DbAdapter db = MizuuApplication.getMovieAdapter();

		Cursor tempCursor = db.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", ignoreRemovedFiles);
		while (tempCursor.moveToNext()) {
			try {
				dbMovies.add(new DbMovie(tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
						tempCursor.getLong(tempCursor.getColumnIndex(DbAdapter.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapter.KEY_TMDBID))));
			} catch (NullPointerException e) {}
		}

		tempCursor.close();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

		for (int i = 0; i < dbMovies.size(); i++) {
			if (dbMovies.get(i).isNetworkFile()) {
				if (MizLib.isWifiConnected(getApplicationContext(), prefsDisableEthernetWiFiCheck)) {
					FileSource source = null;

					for (int j = 0; j < filesources.size(); j++)
						if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
							source = filesources.get(j);
							continue;
						}

					if (source == null) {
						boolean deleted = db.deleteMovie(dbMovies.get(i).getRowId());
						if (deleted)
							deletedMovies.add(dbMovies.get(i));
						continue;
					}

					try {
						final SmbFile file = new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										dbMovies.get(i).getFilepath(),
										false
										));

						if (!file.exists()) {
							boolean deleted = db.deleteMovie(dbMovies.get(i).getRowId());
							if (deleted)
								deletedMovies.add(dbMovies.get(i));
						}
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			} else {
				if (!new File(dbMovies.get(i).getFilepath()).exists()) {
					boolean deleted = db.deleteMovie(dbMovies.get(i).getRowId());
					if (deleted)
						deletedMovies.add(dbMovies.get(i));
				}
			}
		}

		int count = deletedMovies.size();
		for (int i = 0; i < count; i++) {
			MizLib.deleteFile(new File(deletedMovies.get(i).getThumbnail()));
			MizLib.deleteFile(new File(deletedMovies.get(i).getBackdrop()));
		}

		// Clean up
		dbMovies.clear();
		deletedMovies.clear();
		filesources.clear();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget

		MizLib.scheduleMovieUpdate(this);

		if (MizLib.hasTraktAccount(this) && syncLibraries) {
			Intent movies = new Intent(getApplicationContext(), TraktMoviesSyncService.class);
			getApplicationContext().startService(movies);
		}
	}

	private class DbMovie {
		private String filepath, tmdbId, runtime, year, genres, title;
		private long rowId;

		public DbMovie(String filepath, long rowId, String tmdbId) {
			this.filepath = filepath;
			this.rowId = rowId;
			this.tmdbId = tmdbId;
		}
		
		public DbMovie(String filepath, long rowId, String tmdbId, String runtime, String year, String genres, String title) {
			this.filepath = filepath;
			this.rowId = rowId;
			this.tmdbId = tmdbId;
			this.runtime = runtime;
			this.year = year;
			this.genres = genres;
			this.title = title;
		}

		public String getFilepath() {
			if (filepath.contains("smb") && filepath.contains("@"))
				return "smb://" + filepath.substring(filepath.indexOf("@") + 1);
			return filepath.replace("/smb:/", "smb://");
		}

		public long getRowId() {
			return rowId;
		}

		public String getThumbnail() {
			return new File(MizLib.getMovieThumbFolder(getApplicationContext()), tmdbId + ".jpg").getAbsolutePath();
		}

		public String getBackdrop() {
			return new File(MizLib.getMovieBackdropFolder(getApplicationContext()), tmdbId + "_bg.jpg").getAbsolutePath();
		}
		
		public String getRuntime() {
			return runtime;
		}
		
		public String getReleaseYear() {
			return year;
		}
		
		public String getGenres() {
			return genres;
		}
		
		public String getTitle() {
			return title;
		}
		
		public boolean isUnidentified() {
			if (getRuntime().equals("0")
					&& MizLib.isEmpty(getReleaseYear())
					&& MizLib.isEmpty(getGenres())
					&& MizLib.isEmpty(getTitle()))
				return true;
			
			return false;
		}

		public boolean isNetworkFile() {
			return getFilepath().contains("smb:/");
		}
	}
}