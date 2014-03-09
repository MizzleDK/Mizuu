package com.miz.functions;

import java.io.File;

import com.miz.db.DbAdapterSources;

public class FileSource {
	
	public static final int FILE = 0, SMB = 1, UPNP = 2;
	private long rowId;
	private String filepath, user, password, domain, type;
	private int fileSourceType;

	public FileSource(long rowId, String filepath, int fileSourceType, String user, String password, String domain, String type) {
		this.rowId = rowId;
		this.filepath = filepath;
		this.fileSourceType = fileSourceType;
		this.user = user;
		this.password = password;
		this.domain = domain;
		this.type = type;
	}

	public long getRowId() {
		return rowId;
	}

	public String getFilepath() {
		return filepath;
	}

	public String getTitle() {
		try {
			return new File(filepath).getName();
		} catch (Exception e) {
			return filepath;
		}
	}

	public int getFileSourceType() {
		return fileSourceType;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getDomain() {
		return domain;
	}

	public boolean isMovie() {
		if (type.equals(DbAdapterSources.KEY_TYPE_MOVIE))
			return true;
		return false;
	}
}