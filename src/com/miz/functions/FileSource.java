package com.miz.functions;

import java.io.File;

import com.miz.mizuu.DbAdapterSources;

public class FileSource {
	private long rowId;
	private String filepath, user, password, domain, type;
	private int isSmb;

	public FileSource(long rowId, String filepath, int isSmb, String user, String password, String domain, String type) {
		this.rowId = rowId;
		this.filepath = filepath;
		this.isSmb = isSmb;
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

	public boolean isSmb() {
		return isSmb == 1 ? true : false;
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