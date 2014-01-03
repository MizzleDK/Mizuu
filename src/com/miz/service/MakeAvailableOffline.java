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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import com.miz.functions.MizLib;
import com.miz.mizuu.CancelUpdateDialog;
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

	public MakeAvailableOffline() {
		super("MakeAvailableOffline");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		reset();

		mContext = getApplicationContext();
		file = intent.getExtras().getString(FILEPATH);
		type = intent.getExtras().getInt(TYPE); // MizLib.TYPE_MOVIE
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelUpdateDialog.class);
		notificationIntent.putExtra("type", CancelUpdateDialog.MOVIE);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		builder = new NotificationCompat.Builder(mContext);
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setSmallIcon(R.drawable.refresh);
		builder.setContentIntent(contentIntent);
		builder.setTicker("Downloading movie");
		builder.setContentTitle("Downloading movie");
		builder.setContentText(getContentText());
		builder.setLargeIcon(MizLib.getNotificationImageThumbnail(mContext, intent.getExtras().getString("thumb")));

		/*
		 * builder.setTicker(getString(R.string.updateLookingForFiles));
		builder.setContentTitle(getString(R.string.updatingMovies) + " (0%)");
		builder.setContentText(getString(R.string.updateLookingForFiles));
		builder.setContentIntent(contentIntent);
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		 */

		boolean exists = checkIfFileExists();

		if (!exists) {
			// TODO EXIT AND ALERT THE USER!
			stopSelf();
		}

		boolean sizeOK = checkFilesize();

		if (!sizeOK) {
			// TODO EXIT AND ALERT THE USER!
			stopSelf();
		}

		long time = System.currentTimeMillis();

		updateNotification();
		startForeground(NOTIFICATION_ID, mNotification);

		boolean success = beginTransfer();

		System.out.println("SUCCESS: " + success + ". Time: " + (System.currentTimeMillis() - time));
	}

	private boolean checkIfFileExists() {
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

	private boolean checkFilesize() {
		try {
			long freeMemory = (long) (MizLib.getFreeMemory() * 0.975);
			smbSize = smb.length();

			return freeMemory > smbSize;
		} catch (SmbException e) {
			return false;
		}
	}

	private long totalLength, currentLength;

	private boolean beginTransfer() {
		try {
			File offline = new File(MizLib.getAvailableOfflineFolder(mContext), smb.getName());
			offline.createNewFile();

			totalLength = smb.length();

			OutputStream os = new BufferedOutputStream(new FileOutputStream(offline), 16384);
			BufferedInputStream in = new BufferedInputStream(new SmbFileInputStream(smb), 16384);
			byte[] b = new byte[16384];
			int n;
			while((n = in.read(b)) != -1) {
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


	private long lastTime, currentTime, lastLength;
	private DecimalFormat oneDecimal = new DecimalFormat("#.#");

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
	}
}
