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
		return new File(MizLib.getMovieThumbFolder(context), tmdbId + ".jpg").getAbsolutePath();
	}

	public String getBackdrop() {
		return new File(MizLib.getMovieBackdropFolder(context), tmdbId + "_bg.jpg").getAbsolutePath();
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
		return new File(MizLib.getAvailableOfflineFolder(CONTEXT), MizLib.md5(filepath) + "." + MizLib.getFileExtension(filepath));
	}
}