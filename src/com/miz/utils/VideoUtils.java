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

package com.miz.utils;

import static com.miz.functions.PreferenceKeys.BUFFER_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORE_VIDEO_FILE_TYPE;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.mizuu.R;
import com.miz.smbstreamer.Streamer;

import android.app.Activity;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class VideoUtils {
	
	// TODO Move all related methods from MizLib, etc. into this class

	private VideoUtils() {} // No instantiation
	
	public static boolean playVideo(Activity activity, String filepath, boolean isNetworkFile, Object videoObject) {
		
		boolean videoWildcard = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(IGNORE_VIDEO_FILE_TYPE, false);
		boolean playbackStarted = false;
		
		if (isNetworkFile) {
			playbackStarted = playNetworkFile(activity, filepath, videoObject);
		} else {
			try { // Attempt to launch intent based on the MIME type
				activity.startActivity(MizLib.getVideoIntent(filepath, videoWildcard, videoObject));
			} catch (Exception e) {
				try { // Attempt to launch intent based on wildcard MIME type
					activity.startActivity(MizLib.getVideoIntent(filepath, "video/*", videoObject));
				} catch (Exception e2) {
					Toast.makeText(activity, activity.getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
				}
			}
		}
		
		return playbackStarted;
	}
	
	private static boolean playNetworkFile(final Activity activity, final String filepath, final Object videoObject) {
		
		final boolean videoWildcard = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(IGNORE_VIDEO_FILE_TYPE, false);
		
		if (!MizLib.isWifiConnected(activity)) {
			Toast.makeText(activity, activity.getString(R.string.noConnection), Toast.LENGTH_LONG).show();
			return false;
		}

		int bufferSize;
		String buff = PreferenceManager.getDefaultSharedPreferences(activity).getString(BUFFER_SIZE, activity.getString(R.string._16kb));
		if (buff.equals(activity.getString(R.string._16kb)))
			bufferSize = 8192 * 2; // This appears to be the limit for most video players
		else bufferSize = 8192;

		final Streamer s = Streamer.getInstance();
		if (s != null)
			s.setBufferSize(bufferSize);
		else {
			Toast.makeText(activity, activity.getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		int contentType = (videoObject instanceof Movie) ? MizLib.TYPE_MOVIE : MizLib.TYPE_SHOWS;
		final NtlmPasswordAuthentication auth = MizLib.getAuthFromFilepath(contentType, filepath);

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(auth.getDomain(), "utf-8"),
									URLEncoder.encode(auth.getUsername(), "utf-8"),
									URLEncoder.encode(auth.getPassword(), "utf-8"),
									filepath,
									false
									));

					s.setStreamSrc(file, MizLib.getSubtitleFiles(filepath, auth)); //the second argument can be a list of subtitle files
					activity.runOnUiThread(new Runnable(){
						public void run(){
							try{
								Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(filepath).getPath())).getEncodedPath());	
								activity.startActivity(MizLib.getVideoIntent(uri, videoWildcard, videoObject));
							} catch (Exception e) {
								try { // Attempt to launch intent based on wildcard MIME type
									Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(filepath).getPath())).getEncodedPath());	
									activity.startActivity(MizLib.getVideoIntent(uri, "video/*", videoObject));
								} catch (Exception e2) {
									Toast.makeText(activity, activity.getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
								}
							}
						}
					});
				}
				catch (MalformedURLException e) {}
				catch (UnsupportedEncodingException e1) {}
			}
		}.start();
		
		return true;
	}
}
