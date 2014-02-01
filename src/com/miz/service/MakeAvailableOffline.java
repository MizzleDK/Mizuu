package com.miz.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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

public class MakeAvailableOffline extends IntentService {

	private static final int NOTIFICATION_ID = 20;

	public static final String FILEPATH = "filepath";
	public static final String TYPE = "type";
	private String file;
	private int type;
	private NtlmPasswordAuthentication auth;
	private SmbFile smb;
	private Context mContext;
	private long smbSize;
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		return START_NOT_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		reset();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-stop-offline-download"));

		mContext = getApplicationContext();

		file = intent.getExtras().getString(FILEPATH);
		type = intent.getExtras().getInt(TYPE); // MizLib.TYPE_MOVIE

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
		builder.setTicker("Downloading movie");
		builder.setContentTitle("Downloading movie");
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

		if (!success) // Delete the offline file if the transfer wasn't successful
			new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(smb.getCanonicalPath()) + "." + MizLib.getFileExtension(smb.getName())).delete();
	}

	private boolean checkIfNetworkFileExists() {
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
			return smb.exists();
		} catch (Exception e) {
			return false;
		}
	}

	private boolean checkIfLocalCopyExists() {
		File f = new File(MizLib.getAvailableOfflineFolder(mContext), MizLib.md5(smb.getCanonicalPath()) + "." + MizLib.getFileExtension(smb.getName()));
		return f.exists() && smbSize == f.length();
	}

	private boolean checkFilesize() {
		try {
			long freeMemory = (long) (MizLib.getFreeMemory() * 0.975);
			smbSize = smb.length();

			return freeMemory > smbSize;
		} catch (SmbException e) {
			return false;
		}
	}

	private boolean beginTransfer() {
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

			return offline.length() == smbSize;
		} catch (Exception ignored) {
			return false;
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

			return Double.valueOf(oneDecimal.format(percentage)) + "% - " + (currentLength / 1024 / 1024) + " of " + (totalLength / 1024 / 1024) + " MB (" + (data_per_sec / 1000) + " KB/s)";
		} catch (Exception e) {
			return "Starting...";
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
		smbSize = -1;
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
