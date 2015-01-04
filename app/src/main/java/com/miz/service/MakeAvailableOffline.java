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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.functions.SmbLogin;
import com.miz.mizuu.CancelOfflineDownload;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import static com.miz.functions.PreferenceKeys.BUFFER_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORE_FILESIZE_CHECK;

public class MakeAvailableOffline extends IntentService {

	private static final int NOTIFICATION_ID = 20;

	public static final String FILEPATH = "filepath";
	public static final String TYPE = "type";
	private String mFileUrl;
	private int mType, mBufferSize;
	private SmbLogin mAuth;
	private SmbFile mSmb;
	private Context mContext;
	private long mSize;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private PendingIntent mContentIntent;
	private Handler mHandler;
	private boolean mStopDownload = false;
	private long mLastTime, mCurrentTime, mLastLength, mTotalLength, mCurrentLength;
	private DecimalFormat mOneDecimal = new DecimalFormat("#.#");

	public MakeAvailableOffline() {
		super("MakeAvailableOffline");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mHandler = new Handler(Looper.getMainLooper());
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
		
		String defaultSize = mContext.getString(R.string._16kb);
		String bufferSize = PreferenceManager.getDefaultSharedPreferences(mContext).getString(BUFFER_SIZE, defaultSize);
		if (bufferSize.equals(defaultSize))
			mBufferSize = 8192 * 2; // This appears to be the limit for most video players
		else mBufferSize = 8192;

		mFileUrl = intent.getExtras().getString(FILEPATH);
		mType = intent.getExtras().getInt(TYPE);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Set up cancel dialog intent
		Intent notificationIntent = new Intent(this, CancelOfflineDownload.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		mContentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setColor(getResources().getColor(R.color.color_primary));
		mBuilder.setOngoing(true);
		mBuilder.setOnlyAlertOnce(true);
		mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
		mBuilder.setContentIntent(mContentIntent);

		String message = getString(R.string.downloadingMovie);
		if (mType == MizLib.TYPE_SHOWS)
			message = getString(R.string.downloadingEpisode);
		mBuilder.setTicker(message);
		mBuilder.setContentTitle(message);
		
		mBuilder.setContentText(getContentText());
		try {
			int size = MizLib.getThumbnailNotificationSize(mContext);
			mBuilder.setLargeIcon(MizuuApplication.getPicasso(mContext).load(intent.getExtras().getString("thumb")).resize(size, (int) (size * 1.5)).get());
		} catch (IOException ignored) {}
		try {
			int width = MizLib.getLargeNotificationWidth(mContext);
			int height = (int) (width / 1.78);
			mBuilder.setStyle(
					new NotificationCompat.BigPictureStyle()
					.setSummaryText(message)
					.bigPicture(MizuuApplication.getPicasso(getApplicationContext()).load(intent.getExtras().getString("backdrop")).resize(width, height).get())
					);
		} catch (IOException e) {}

		mBuilder.addAction(R.drawable.ic_close_white_24dp, getString(android.R.string.cancel), mContentIntent);

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

		boolean ignoreSizeCheck = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORE_FILESIZE_CHECK, false);
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
			if (mFileUrl.startsWith("http"))
				FileUtils.getOfflineFile(mContext, mFileUrl).delete();
			else
				FileUtils.getOfflineFile(mContext, mSmb.getCanonicalPath()).delete();
		}
	}

	private boolean checkIfNetworkFileExists() {
		if (mFileUrl.startsWith("http")) {
			return MizLib.exists(mFileUrl);
		} else {
			mAuth = MizLib.getLoginFromFilepath(mType, mFileUrl);
			try {
				mSmb = new SmbFile(
						MizLib.createSmbLoginString(
								mAuth.getDomain(),
								mAuth.getUsername(),
								mAuth.getPassword(),
								mFileUrl,
								false
								));
				return mSmb.exists() && mSmb.length() > 1000;
			} catch (Exception e) {
				return false;
			}
		}
	}

	private boolean checkIfLocalCopyExists() {
		File f = null;
		if (mFileUrl.startsWith("http"))
			f = FileUtils.getOfflineFile(mContext, mFileUrl);
		else
			f = FileUtils.getOfflineFile(mContext, mSmb.getCanonicalPath());
		return f.exists() && mSize == f.length();
	}

	private boolean checkFilesize() {
		try {			
			long freeMemory = (long) (MizLib.getFreeMemory() * 0.975);
			if (mFileUrl.startsWith("http"))
				mSize = MizLib.getFileSize(new URL(mFileUrl));
			else
				mSize = mSmb.length();
			return freeMemory > mSize;
		} catch (SmbException e) {
			return false;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private boolean beginTransfer() {
		if (mFileUrl.startsWith("http")) {
			
			InputStream in = null;
			OutputStream fileos = null;
			
			try {
				int bufferSize = mBufferSize;
				byte[] retVal = null;

				Request request = new Request.Builder()
				.url(mFileUrl)
				.build();

				Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
				if (!response.isSuccessful())
					return false;

				File offline = FileUtils.getOfflineFile(mContext, mFileUrl);
				offline.createNewFile();
				
				fileos = new BufferedOutputStream(new FileOutputStream(offline));
				in = new BufferedInputStream(response.body().byteStream(), bufferSize);

				retVal = new byte[bufferSize];
				int length = 0;
				while((length = in.read(retVal)) > -1) {
					if (mStopDownload) {
						fileos.close();
						in.close();
						return false;
					}
					fileos.write(retVal, 0, length);
					mCurrentLength += length;
					update();
				}
				return true;
			} catch(Exception e) {
				return false;
			} finally {
				if (fileos != null) {
					try {
						fileos.flush();
						fileos.close();
					} catch (IOException e) {}
				}

				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {}
				}
			}
		} else {
			try {
				File offline = FileUtils.getOfflineFile(mContext, mSmb.getCanonicalPath());
				offline.createNewFile();

				mTotalLength = mSmb.length();

				OutputStream os = new BufferedOutputStream(new FileOutputStream(offline), mBufferSize);
				BufferedInputStream in = new BufferedInputStream(new SmbFileInputStream(mSmb), mBufferSize);
				byte[] b = new byte[mBufferSize];
				int n;
				while((n = in.read(b)) != -1) {
					if (mStopDownload) {
						os.close();
						in.close();
						return false;
					}
					os.write(b, 0, n);
					mCurrentLength += n;
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
		mCurrentTime = System.currentTimeMillis();
		if (mCurrentTime - 1000 > mLastTime) {
			updateNotification();

			mLastTime = mCurrentTime;
			mLastLength = mCurrentLength;
		}
	}

	private String getContentText() {
		try {
			long elapsed = (mCurrentTime - mLastTime);
			long data_sent = (mCurrentLength - mLastLength);
			double percentage = (100.0 / (double) mTotalLength) * (double) mCurrentLength;
			long data_per_sec = (data_sent / elapsed) * 1000;

			if (mSize == -1)
				return getString(R.string.unknown_size) + " (" + (data_per_sec / 1000) + " KB/s)";
			return Double.valueOf(mOneDecimal.format(percentage)) + "% - " + (mCurrentLength / 1024 / 1024) + " of " + (mTotalLength / 1024 / 1024) + " MB (" + (data_per_sec / 1000) + " KB/s)";
		} catch (Exception e) {
			return getString(R.string.starting_download);
		}
	}

	private void updateNotification() {
		mBuilder.setContentText(getContentText());
		mNotification = mBuilder.build();
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	private void reset() {
		mFileUrl = "";
		mType = -1;
		mAuth = null;
		mSmb = null;
		mContext = null;
		mSize = -1;
		mBuilder = null;
		mNotificationManager = null;
		mNotification = null;
		mContentIntent = null;
		mStopDownload = false;
		mLastTime = 0;
		mCurrentTime = 0;
		mLastLength = 0;
		mTotalLength = 0;
		mCurrentLength = 0;
	}
}