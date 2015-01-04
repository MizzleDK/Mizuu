/*
 * Copyright (C) 2013 Square, Inc.
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
package com.miz.functions;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.mizuu.MizuuApplication;
import com.miz.utils.StringUtils;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.Downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import jcifs.smb.SmbFile;

import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;

/** A {@link Downloader} which uses OkHttp to download images. */
public class OkHttpDownloader implements Downloader {

	private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

	static final String RESPONSE_SOURCE_ANDROID = "X-Android-Response-Source";
	static final String RESPONSE_SOURCE_OKHTTP = "OkHttp-Response-Source";

	private final Context mContext;
	private final File mCacheDir;
	private final OkUrlFactory mUrlFactory;

	/**
	 * Create new downloader that uses OkHttp. This will install an image cache into your application
	 * cache directory.
	 */
	public OkHttpDownloader(Context context) {

		// Create cache directory
		mCacheDir = new File(context.getApplicationContext().getCacheDir(), "picasso-cache");
		if (!mCacheDir.exists()) {
			mCacheDir.mkdirs();
		}

		mUrlFactory = new OkUrlFactory(MizuuApplication.getOkHttpClient());

		try {
			mUrlFactory.client().setCache(new Cache(mCacheDir, calculateDiskCacheSize(mCacheDir)));
		} catch (IOException e) {}

		// Set context
		mContext = context;
	}

	static long calculateDiskCacheSize(File dir) {
		long size = MIN_DISK_CACHE_SIZE;

		try {
			// Target 2% of the total space.
			size = MizLib.getFreeMemory() / 50;
		} catch (IllegalArgumentException ignored) {}

		// Bound inside min/max size for disk cache.
		return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
	}

	protected HttpURLConnection openConnection(Uri uri) throws IOException {
		HttpURLConnection connection = mUrlFactory.open(new URL(uri.toString()));
		connection.setConnectTimeout(15 * 1000); // 15s
		connection.setReadTimeout(20 * 1000); // 20s		
		return connection;
	}

	protected OkHttpClient getClient() {
		return mUrlFactory.client();
	}

	/** Returns {@code true} if header indicates the response body was loaded from the disk cache. */
	private boolean parseResponseSourceHeader(String header) {
		if (header == null)
			return false;
			
		String[] parts = header.split(" ", 2);
		if ("CACHE".equals(parts[0]))
			return true;
		
		if (parts.length == 1)
			return false;

		try {
			return "CONDITIONAL_CACHE".equals(parts[0]) && Integer.parseInt(parts[1]) == 304;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public Response load(Uri uri, boolean localCacheOnly) throws IOException {		
		String imageUri = uri.toString();

		// Check if this is a HTTP request
		if (imageUri.startsWith("http")) {
			HttpURLConnection connection = openConnection(uri);
			connection.setUseCaches(true);
			if (localCacheOnly) {
				connection.setRequestProperty("Cache-Control", "only-if-cached,max-age=" + Integer.MAX_VALUE);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode >= 300) {
				connection.disconnect();
				throw new ResponseException(responseCode + " " + connection.getResponseMessage());
			}

			String responseSource = connection.getHeaderField(RESPONSE_SOURCE_OKHTTP);
			if (responseSource == null) {
				responseSource = connection.getHeaderField(RESPONSE_SOURCE_ANDROID);
			}

			long contentLength = connection.getHeaderFieldInt("Content-Length", -1);
			boolean fromCache = parseResponseSourceHeader(responseSource);

			return new Response(connection.getInputStream(), fromCache, contentLength);
		}

		// Custom data!
		ArrayList<Filepath> filepaths = new ArrayList<Filepath>();
		boolean ignoreNfo = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_NFO_FILES, true);

		// We only want to look for custom images if the user has asked for it
		if (!ignoreNfo) {

			// Determine if this is a movie path or TV show path
			if (imageUri.contains("movie")) {
				// Let's get the movie ID
				String id = StringUtils.getMovieIdFromImagePath(imageUri);
				if (!TextUtils.isEmpty(id)) {
					ArrayList<String> strings = MizuuApplication.getMovieMappingAdapter().getMovieFilepaths(id);
					filepaths = new ArrayList<Filepath>();
					for (int i = 0; i < strings.size(); i++)
						filepaths.add(new Filepath(strings.get(i)));
					strings.clear();
				}
			} else if (imageUri.contains("tvshows")) {
				// Handle TV shows... soon(TM)
			}

			for (Filepath path : filepaths) {	
				String filename = path.getFilepath();

				if (path.getType() == FileSource.SMB) {
                    if (MizLib.isOnline(mContext)) {
                        if (!TextUtils.isEmpty(filename)) {
                            SmbFile s;

                            // Determine if this is a movie path or TV show path
                            if (imageUri.contains("movie")) {
                                s = MizLib.getCustomCoverArt(filename, MizLib.getLoginFromFilepath(MizLib.TYPE_MOVIE, filename),
                                        imageUri.contains("movie-thumbs") ? MizLib.COVER : MizLib.BACKDROP);

                                if (s != null)
                                    return new Response(s.getInputStream(), localCacheOnly, s.getContentLength());
                            }
                        }
                    }

				} else if (path.getType() == FileSource.FILE) {

					if (!TextUtils.isEmpty(filename)) {
						// There's a valid filepath, let's look for custom movie data
						filename = filename.substring(0, filename.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
						File parentFolder = new File(filename).getParentFile();
						if (parentFolder != null) {
							File[] list = parentFolder.listFiles();
							if (list != null) {
								String name, absolutePath;
								int count = list.length;

								// Determine if this is a movie path or TV show path
								if (imageUri.contains("movie")) {
									if (imageUri.contains("movie-thumbs")) {
										// We're looking for movie thumbnails
										for (int i = 0; i < count; i++) {
											name = list[i].getName();
											absolutePath = list[i].getAbsolutePath();

											if (name.equalsIgnoreCase("poster.jpg") ||
													name.equalsIgnoreCase("poster.jpeg") ||
													name.equalsIgnoreCase("poster.tbn") ||
													name.equalsIgnoreCase("folder.jpg") ||
													name.equalsIgnoreCase("folder.jpeg") ||
													name.equalsIgnoreCase("folder.tbn") ||
													name.equalsIgnoreCase("cover.jpg") ||
													name.equalsIgnoreCase("cover.jpeg") ||
													name.equalsIgnoreCase("cover.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-poster.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-poster.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-poster.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-folder.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-folder.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-folder.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-cover.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-cover.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-cover.tbn") ||
													absolutePath.equalsIgnoreCase(filename + ".jpg") ||
													absolutePath.equalsIgnoreCase(filename + ".jpeg") ||
													absolutePath.equalsIgnoreCase(filename + ".tbn")) {

												return new Response(new BufferedInputStream(new FileInputStream(list[i]), 4096), localCacheOnly, list[i].length());
											}
										}
									} else {
										// We're looking for movie backdrops
										for (int i = 0; i < count; i++) {
											name = list[i].getName();
											absolutePath = list[i].getAbsolutePath();
											if (name.equalsIgnoreCase("fanart.jpg") ||
													name.equalsIgnoreCase("fanart.jpeg") ||
													name.equalsIgnoreCase("fanart.tbn") ||
													name.equalsIgnoreCase("banner.jpg") ||
													name.equalsIgnoreCase("banner.jpeg") ||
													name.equalsIgnoreCase("banner.tbn") ||
													name.equalsIgnoreCase("backdrop.jpg") ||
													name.equalsIgnoreCase("backdrop.jpeg") ||
													name.equalsIgnoreCase("backdrop.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-fanart.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-fanart.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-fanart.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-banner.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-banner.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-banner.tbn") ||
													absolutePath.equalsIgnoreCase(filename + "-backdrop.jpg") ||
													absolutePath.equalsIgnoreCase(filename + "-backdrop.jpeg") ||
													absolutePath.equalsIgnoreCase(filename + "-backdrop.tbn")) {

												return new Response(new BufferedInputStream(new FileInputStream(list[i]), 4096), localCacheOnly, list[i].length());
											}
										}
									}
								} else {
									// We're looking for movie backdrops
								}
							}
						}
					}
				}
			}	
		}

		// Return a Response based on the standard image path
		File f = new File(imageUri);
		return new Response(new BufferedInputStream(new FileInputStream(f), 4096), localCacheOnly, f.length());
	}
}