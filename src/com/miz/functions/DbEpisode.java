package com.miz.functions;

import java.io.File;

import android.content.Context;

public class DbEpisode {

	private Context context;
	private String filepath, rowId, showId, episodeCoverPath;

	public DbEpisode(Context context, String filepath, String rowId, String showId, String episodeCoverPath) {
		this.context = context;
		this.filepath = filepath;
		this.rowId = rowId;
		this.showId = showId;
		this.episodeCoverPath = episodeCoverPath;
	}

	public String getFilepath() {
		if (filepath.contains("smb") && filepath.contains("@"))
			return "smb://" + filepath.substring(filepath.indexOf("@") + 1);
		return filepath.replace("/smb:/", "smb://");
	}

	public String getRowId() {
		return rowId;
	}

	public String getShowId() {
		return showId;
	}

	public String getEpisodeCoverPath() {
		return new File(MizLib.getTvShowEpisodeFolder(context), episodeCoverPath).getAbsolutePath();
	}

	public String getThumbnail() {
		return new File(MizLib.getTvShowThumbFolder(context), getShowId() + ".jpg").getAbsolutePath();
	}

	public String getBackdrop() {
		return new File(MizLib.getTvShowBackdropFolder(context), getShowId() + "_tvbg.jpg").getAbsolutePath();
	}

	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public boolean isUnidentified() {
		return getShowId().equals("invalid");
	}
}