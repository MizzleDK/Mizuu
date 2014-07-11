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

package com.miz.functions;

import java.io.File;

import android.content.Context;

public class DbMovie {

	private Context context;
	private String filepath, tmdbId, runtime, year, genres, title;
	private long rowId;

	public DbMovie(Context context, String filepath, long rowId, String tmdbId) {
		this.context = context;
		this.filepath = filepath;
		this.rowId = rowId;
		this.tmdbId = tmdbId;
	}

	public DbMovie(Context context, String filepath, long rowId, String tmdbId, String runtime, String year, String genres, String title) {
		this.context = context;
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
		return MizLib.getMovieThumb(context, tmdbId).getAbsolutePath();
	}

	public String getBackdrop() {
		return MizLib.getMovieBackdrop(context, tmdbId).getAbsolutePath();
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
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}

	public String getTmdbId() {
		return tmdbId;
	}

	public boolean hasOfflineCopy(Context ctx) {
		return getOfflineCopyFile(ctx).exists();
	}

	public File getOfflineCopyFile(Context CONTEXT) {
		return MizLib.getOfflineFile(CONTEXT, filepath);
	}
}