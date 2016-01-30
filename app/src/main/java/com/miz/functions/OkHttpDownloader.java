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

import com.miz.mizuu.MizuuApplication;
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

		// Return a Response based on the standard image path
		File f = new File(imageUri);
		return new Response(new BufferedInputStream(new FileInputStream(f), 4096), localCacheOnly, f.length());
	}
}