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
	
	public String getUpnpFolderId() {
		return domain;
	}
	
	public String getUpnpSerialNumber() {
		return password;
	}
	
	public String getUpnpName() {
		return user;
	}

	public boolean isMovie() {
		if (type.equals(DbAdapterSources.KEY_TYPE_MOVIE))
			return true;
		return false;
	}
}