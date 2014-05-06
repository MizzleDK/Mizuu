package com.miz.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.mizuu.CancelOfflineDownload;
import com.miz.mizuu.R;
import com.squareup.okhttp.OkHttpClient;

public class MakeAvailableOffline extends IntentService {

	private static final int NOTIFICATION_ID = 20;

	public static final String FILEPATH = "filepath";
	public static final String TYPE = "type";
	private String file;
	private int type;
	private NtlmPasswordAuthentication auth;
	private SmbFile smb;
	private Context mContext;
	private long mSize;
	private NotificationCompat.Builder builder;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private PendingIntent contentIntent;
	private Handler mHandler;
	private boolean mStopDownload = false;
	private long lastTime, currentTime, lastLength, totalLength, currentLength;
	private DecimalFormat oneDecimal = new DecimalFormat("#.#");

	public MakeAvailableOffline() {
		super("MakeAvailableOffline");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mHandler = new Handler();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStopDownload = true;
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		reset();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-stop-offline-download"));

		mContext = getApplicationContext();

		file = intent.getExtras().getString(FILEPATH);
		type = intent.getExtras().getInt(TYPE);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelOfflineDownload.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		builder = new NotificationCompat.Builder(mContext);
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setContentIntent(contentIntent);

		String message = getString(R.string.downloadingMovie);
		if (type == MizLib.TYPE_SHOWS)
			message = getString(R.string.downloadingEpisode);
		builder.setTicker(message);
		builder.setContentTitle(message);

		builder.setContentText(getContentText());
		builder.setLargeIcon(MizLib.getNotificationImageThumbnail(mContext, intent.getExtras().getString("thumb")));

		builder.addAction(R.drawable.remove, getString(android.R.string.cancel), contentIntent);

		boolean exists = checkIfNetworkFileExists();

		if (!exists) {
			mHandler.post(new Runnable() {            
				@Override
				public void run() {
					Toast.makeText(mContext, R.string.unavailable_file, Toast.LENGTH_LONG).show();              
				}
			});
			stopSelf();
			return;
		}

		boolean ignoreSizeCheck = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("prefsIgnoreFilesizeCheck", false);
		if (!ignoreSizeCheck) {
			boolean sizeOK = checkFilesize();

			if (!sizeOK) {
				mHandler.post(new Runnable() {            
					@Override
					public void run() {
						Toast.makeText(mContext, R.string.not_enough_space, Toast.LENGTH_LONG).show();              
					}
				});
				stopSelf();
				return;
			}
		}

		exists = checkIfLocalCopyExists();
		if (exists) { // There's already an exact local copy - don't download again
			stopSelf();
			return;
		}

		mHandler.post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(mContext, R.string.starting_download, Toast.LENGTH_SHORT).show();              
			}
		});

		updateNotification();
		startForeground(NOTIFICATION_ID, mNotification);

		boolean success = beginTransfer();

		if (!success) { // Delete the offline file if the transfer wasn't successful
			if (file.startsWith("http"))
				new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(file) + "." + MizLib.getFileExtension(file)).delete();
			else
				new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(smb.getCanonicalPath()) + "." + MizLib.getFileExtension(smb.getName())).delete();
		}
	}

	private boolean checkIfNetworkFileExists() {
		if (file.startsWith("http")) {
			return MizLib.exists(file);
		} else {
			auth = MizLib.getAuthFromFilepath(type, file);
			try {
				smb = new SmbFile(
						MizLib.createSmbLoginString(
								URLEncoder.encode(auth.getDomain(), "utf-8"),
								URLEncoder.encode(auth.getUsername(), "utf-8"),
								URLEncoder.encode(auth.getPassword(), "utf-8"),
								file,
								false
								));
				return smb.exists() && smb.length() > 1000;
			} catch (Exception e) {
				return false;
			}
		}
	}

	private boolean checkIfLocalCopyExists() {
		File f = null;
		if (file.startsWith("http"))
			f = new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(file) + "." + MizLib.getFileExtension(file));
		else
			f = new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(smb.getCanonicalPath()) + "." + MizLib.getFileExtension(smb.getName()));
		return f.exists() && mSize == f.length();
	}

	private boolean checkFilesize() {
		try {			
			long freeMemory = (long) (MizLib.getFreeMemory() * 0.975);
			if (file.startsWith("http"))
				mSize = MizLib.getFileSize(new URL(file));
			else
				mSize = smb.length();
			return freeMemory > mSize;
		} catch (SmbException e) {
			return false;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private boolean beginTransfer() {
		if (file.startsWith("http")) {
			// the size of my buffer in bits
			int bufferSize = 16384;
			byte[] retVal = null;
			InputStream in = null;
			OutputStream fileos = null;
			OkHttpClient client = new OkHttpClient();
			HttpURLConnection urlConnection = null;

			try {
				totalLength = MizLib.getFileSize(new URL(file));

				urlConnection = client.open(new URL(file));
				File offline = new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(file) + "." + MizLib.getFileExtension(file));
				offline.createNewFile();

				fileos = new BufferedOutputStream(new FileOutputStream(offline));

				in = new BufferedInputStream(urlConnection.getInputStream(), bufferSize);

				retVal = new byte[bufferSize];
				int length = 0;
				while((length = in.read(retVal)) > -1) {

					if (mStopDownload) {
						fileos.close();
						in.close();
						return false;
					}

					fileos.write(retVal, 0, length);

					currentLength += length;
					update();
				}

				return true;
			} catch(IOException e) {
				return false;
			} finally {
				if(fileos != null) {
					try {
						fileos.flush();
						fileos.close();
					} catch (IOException e) {}
				}
				if(in != null) {
					try {
						in.close();
					} catch (IOException e) {}
				}
				if (urlConnection != null)
					urlConnection.disconnect();
			}
		} else {
			try {
				File offline = new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(smb.getCanonicalPath()) + "." + MizLib.getFileExtension(smb.getName()));
				offline.createNewFile();

				totalLength = smb.length();

				OutputStream os = new BufferedOutputStream(new FileOutputStream(offline), 16384);
				BufferedInputStream in = new BufferedInputStream(new SmbFileInputStream(smb), 16384);
				byte[] b = new byte[16384];
				int n;
				while((n = in.read(b)) != -1) {
					if (mStopDownload) {
						os.close();
						in.close();
						return false;
					}
					os.write(b, 0, n);
					currentLength += n;
					update();
				}
				os.close();
				in.close();

				return true;
			} catch (Exception ignored) {
				return false;
			}
		}
	}

	private void update() {
		currentTime = System.currentTimeMillis();
		if (currentTime - 1000 > lastTime) {
			updateNotification();

			lastTime = currentTime;
			lastLength = currentLength;
		}
	}

	private String getContentText() {
		try {
			long elapsed = (currentTime - lastTime);
			long data_sent = (currentLength - lastLength);
			double percentage = (100.0 / (double) totalLength) * (double) currentLength;
			long data_per_sec = (data_sent / elapsed) * 1000;

			if (mSize == -1)
				return getString(R.string.unknown_size) + " (" + (data_per_sec / 1000) + " KB/s)";
			return Double.valueOf(oneDecimal.format(percentage)) + "% - " + (currentLength / 1024 / 1024) + " of " + (totalLength / 1024 / 1024) + " MB (" + (data_per_sec / 1000) + " KB/s)";
		} catch (Exception e) {
			return getString(R.string.starting_download);
		}
	}

	private void updateNotification() {
		builder.setContentText(getContentText());
		mNotification = builder.build();
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	private void reset() {
		file = "";
		type = -1;
		auth = null;
		smb = null;
		mContext = null;
		mSize = -1;
		builder = null;
		mNotificationManager = null;
		mNotification = null;
		contentIntent = null;
		mStopDownload = false;
		lastTime = 0;
		currentTime = 0;
		lastLength = 0;
		totalLength = 0;
		currentLength = 0;
	}
}
