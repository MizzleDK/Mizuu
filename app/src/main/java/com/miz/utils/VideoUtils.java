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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.SmbLogin;
import com.miz.functions.TmdbTrailerSearch;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisode;
import com.miz.smbstreamer.Streamer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;

import jcifs.smb.SmbFile;

import static com.miz.functions.PreferenceKeys.BUFFER_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORE_VIDEO_FILE_TYPE;

public class VideoUtils {

	// TODO Move all related methods from MizLib, etc. into this class

	private VideoUtils() {} // No instantiation

	public static boolean playVideo(Activity activity, String filepath, int filetype, Object videoObject) {
		boolean videoWildcard = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(IGNORE_VIDEO_FILE_TYPE, false);
		boolean playbackStarted = true;

		if (filetype == FileSource.SMB) {
			playbackStarted = playNetworkFile(activity, filepath, videoObject);
		} else {
			try { // Attempt to launch intent based on the MIME type
				activity.startActivity(getVideoIntent(filepath, videoWildcard, videoObject));
			} catch (Exception e) {
				try { // Attempt to launch intent based on wildcard MIME type
					activity.startActivity(getVideoIntent(filepath, "video/*", videoObject));
				} catch (Exception e2) {
					playbackStarted = false;
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
		final SmbLogin auth = MizLib.getLoginFromFilepath(contentType, filepath);

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									auth.getDomain(),
									auth.getUsername(),
									auth.getPassword(),
									filepath,
									false
							));

					s.setStreamSrc(file, MizLib.getSubtitleFiles(filepath, auth)); //the second argument can be a list of subtitle files
					activity.runOnUiThread(new Runnable(){
						public void run(){
							try{
								Uri uri = Uri.parse(s.getUrl() + Uri.fromFile(new File(Uri.parse(filepath).getPath())).getEncodedPath());
								activity.startActivity(getVideoIntent(uri, videoWildcard, videoObject));
							} catch (Exception e) {
								try { // Attempt to launch intent based on wildcard MIME type
									Uri uri = Uri.parse(s.getUrl() + Uri.fromFile(new File(Uri.parse(filepath).getPath())).getEncodedPath());
									activity.startActivity(getVideoIntent(uri, "video/*", videoObject));
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

	public static String startSmbServer(final Activity activity, final String filepath, final Object videoObject) {
		if (!MizLib.isWifiConnected(activity)) {
			Toast.makeText(activity, activity.getString(R.string.noConnection), Toast.LENGTH_LONG).show();
			return "";
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
			return "";
		}

		int contentType = (videoObject instanceof Movie) ? MizLib.TYPE_MOVIE : MizLib.TYPE_SHOWS;
		final SmbLogin auth = MizLib.getLoginFromFilepath(contentType, filepath);

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									auth.getDomain(),
									auth.getUsername(),
									auth.getPassword(),
									filepath,
									false
							));

					s.setStreamSrc(file, MizLib.getSubtitleFiles(filepath, auth)); //the second argument can be a list of subtitle files
				}
				catch (MalformedURLException e) {}
				catch (UnsupportedEncodingException e1) {}
			}
		}.start();

		return Uri.parse(s.getUrl() + Uri.fromFile(new File(Uri.parse(filepath).getPath())).getEncodedPath()).toString();
	}

	public static void playTrailer(final Activity activity, final Movie movie) {
		String localTrailer = "";
		for (Filepath path : movie.getFilepaths()) {
			if (path.getType() == FileSource.FILE) {
				localTrailer = path.getFullFilepath();
				break;
			}
		}

		localTrailer = movie.getLocalTrailer(localTrailer);

		if (!TextUtils.isEmpty(localTrailer)) {
			try { // Attempt to launch intent based on the MIME type
				activity.startActivity(getVideoIntent(localTrailer, false, movie.getTitle() + " " + activity.getString(R.string.detailsTrailer)));
			} catch (Exception e) {
				try { // Attempt to launch intent based on wildcard MIME type
					activity.startActivity(getVideoIntent(localTrailer, "video/*", movie.getTitle() + " " + activity.getString(R.string.detailsTrailer)));
				} catch (Exception e2) {
					Toast.makeText(activity, activity.getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
				}
			}
		} else {
			if (!TextUtils.isEmpty(movie.getTrailer())) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(movie.getTrailer()));
				activity.startActivity(intent);
			} else {
				new TmdbTrailerSearch(activity, movie.getTmdbId()).execute();
			}
		}
	}

	public static Intent getVideoIntent(String fileUrl, boolean useWildcard, Object videoObject) {
		if (fileUrl.startsWith("http"))
			return getVideoIntent(Uri.parse(fileUrl), useWildcard, videoObject);

		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(Uri.fromFile(new File(fileUrl)), getMimeType(fileUrl, useWildcard));
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(Uri file, boolean useWildcard, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(file, getMimeType(file.getPath(), useWildcard));
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(String fileUrl, String mimeType, Object videoObject) {
		if (fileUrl.startsWith("http"))
			return getVideoIntent(Uri.parse(fileUrl), mimeType, videoObject);

		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(Uri.fromFile(new File(fileUrl)), mimeType);
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(Uri file, String mimeType, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(file, mimeType);
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	private static Bundle getVideoIntentBundle(Object videoObject) {
		Bundle b = new Bundle();
		String title = "";
		if (videoObject instanceof Movie) {
			title = ((Movie) videoObject).getTitle();
			b.putString("plot", ((Movie) videoObject).getPlot());
			b.putString("date", ((Movie) videoObject).getReleasedate());
			b.putDouble("rating", ((Movie) videoObject).getRawRating());
			b.putString("cover", ((Movie) videoObject).getThumbnail().getAbsolutePath());
			b.putString("genres", ((Movie) videoObject).getGenres());
		} else if (videoObject instanceof TvShowEpisode) {
			title = ((TvShowEpisode) videoObject).getTitle();
			b.putString("plot", ((TvShowEpisode) videoObject).getDescription());
			b.putString("date", ((TvShowEpisode) videoObject).getReleasedate());
			b.putDouble("rating", ((TvShowEpisode) videoObject).getRawRating());
			b.putString("cover", ((TvShowEpisode) videoObject).getEpisodePhoto().getAbsolutePath());
			b.putString("episode", ((TvShowEpisode) videoObject).getEpisode());
			b.putString("season", ((TvShowEpisode) videoObject).getSeason());
		} else {
			title = (String) videoObject;
		}
		b.putString("title", title);
		b.putString("forcename", title);
		b.putBoolean("forcedirect", true);
		return b;
	}

	public static String getMimeType(String filepath, boolean useWildcard) {
		if (useWildcard)
			return "video/*";

		HashMap<String, String> mimeTypes = new HashMap<String, String>();
		mimeTypes.put("3gp",	"video/3gpp");
		mimeTypes.put("aaf",	"application/octet-stream");
		mimeTypes.put("mp4",	"video/mp4");
		mimeTypes.put("ts",		"video/mp2t");
		mimeTypes.put("webm",	"video/webm");
		mimeTypes.put("m4v",	"video/x-m4v");
		mimeTypes.put("mkv",	"video/x-matroska");
		mimeTypes.put("divx",	"video/x-divx");
		mimeTypes.put("xvid",	"video/x-xvid");
		mimeTypes.put("rec",	"application/octet-stream");
		mimeTypes.put("avi",	"video/avi");
		mimeTypes.put("flv",	"video/x-flv");
		mimeTypes.put("f4v",	"video/x-f4v");
		mimeTypes.put("moi",	"application/octet-stream");
		mimeTypes.put("mpeg",	"video/mpeg");
		mimeTypes.put("mpg",	"video/mpeg");
		mimeTypes.put("mts",	"video/mts");
		mimeTypes.put("m2ts",	"video/mp2t");
		mimeTypes.put("ogv",	"video/ogg");
		mimeTypes.put("rm",		"application/vnd.rn-realmedia");
		mimeTypes.put("rmvb",	"application/vnd.rn-realmedia-vbr");
		mimeTypes.put("mov",	"video/quicktime");
		mimeTypes.put("wmv",	"video/x-ms-wmv");
		mimeTypes.put("iso",	"application/octet-stream");
		mimeTypes.put("vob",	"video/dvd");
		mimeTypes.put("ifo",	"application/octet-stream");
		mimeTypes.put("wtv",	"video/wtv");
		mimeTypes.put("pyv",	"video/vnd.ms-playready.media.pyv");
		mimeTypes.put("ogm",	"video/ogg");
		mimeTypes.put("img",	"application/octet-stream");

		String mime = mimeTypes.get(StringUtils.getExtension(filepath));
		if (mime == null)
			return "video/*";
		return mime;
	}
}
