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

import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.miz.functions.Utils.parseResponseSourceHeader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import jcifs.smb.SmbFile;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.mizuu.MizuuApplication;
import com.miz.utils.StringUtils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.Downloader;

/** A {@link Downloader} which uses OkHttp to download images. */
public class OkHttpDownloader implements Downloader {

	private Context mContext;
	static final String RESPONSE_SOURCE_ANDROID = "X-Android-Response-Source";
	static final String RESPONSE_SOURCE_OKHTTP = "OkHttp-Response-Source";

	private final OkUrlFactory urlFactory;

	/**
	 * Create new downloader that uses OkHttp. This will install an image cache into your application
	 * cache directory.
	 */
	public OkHttpDownloader(final Context context) {
		this(Utils.createDefaultCacheDir(context));
		mContext = context;
	}

	/**
	 * Create new downloader that uses OkHttp. This will install an image cache into your application
	 * cache directory.
	 *
	 * @param cacheDir The directory in which the cache should be stored
	 */
	public OkHttpDownloader(final File cacheDir) {
		this(cacheDir, Utils.calculateDiskCacheSize(cacheDir));
	}

	/**
	 * Create new downloader that uses OkHttp. This will install an image cache into your application
	 * cache directory.
	 *
	 * @param maxSize The size limit for the cache.
	 */
	public OkHttpDownloader(final Context context, final long maxSize) {
		this(Utils.createDefaultCacheDir(context), maxSize);
		mContext = context;
	}

	/**
	 * Create new downloader that uses OkHttp. This will install an image cache into your application
	 * cache directory.
	 *
	 * @param cacheDir The directory in which the cache should be stored
	 * @param maxSize The size limit for the cache.
	 */
	public OkHttpDownloader(final File cacheDir, final long maxSize) {
		this(new OkHttpClient());
		try {
			urlFactory.client().setCache(new com.squareup.okhttp.Cache(cacheDir, maxSize));
		} catch (IOException ignored) {
		}
	}

	/**
	 * Create a new downloader that uses the specified OkHttp instance. A response cache will not be
	 * automatically configured.
	 */
	public OkHttpDownloader(OkHttpClient client) {
		this.urlFactory = new OkUrlFactory(client);
	}

	protected HttpURLConnection openConnection(Uri uri) throws IOException {
		HttpURLConnection connection = urlFactory.open(new URL(uri.toString()));
		connection.setConnectTimeout(Utils.DEFAULT_CONNECT_TIMEOUT);
		connection.setReadTimeout(Utils.DEFAULT_READ_TIMEOUT);
		return connection;
	}

	protected OkHttpClient getClient() {
		return urlFactory.client();
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
					
					if (!TextUtils.isEmpty(filename)) {
						SmbFile s = null;
						
						// Determine if this is a movie path or TV show path
						if (imageUri.contains("movie")) {
							if (imageUri.contains("movie-thumbs")) {
								// Movie thumb
								s = MizLib.getCustomCoverArt(filename, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.COVER);
							} else {
								// Movie backdrop
								s = MizLib.getCustomCoverArt(filename, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.BACKDROP);
							}
							
							if (s != null)
								return new Response(s.getInputStream(), localCacheOnly, s.getContentLength());
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