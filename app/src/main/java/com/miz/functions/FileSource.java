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

import com.miz.db.DbAdapterSources;

import java.io.File;

public class FileSource {
	
	public static final int FILE = 0, SMB = 1, UPNP = 2;
	private long mRowId;
	private String mFilepath, mUser, mPassword, mDomain, mType;
	private int mFileSourceType;

	public FileSource(long rowId, String filepath, int fileSourceType, String user, String password, String domain, String type) {
		mRowId = rowId;
		mFilepath = filepath;
		mFileSourceType = fileSourceType;
		mUser = user;
		mPassword = password;
		mDomain = domain;
		mType = type;
	}

	public long getRowId() {
		return mRowId;
	}

	public String getFilepath() {
		return mFilepath;
	}

	public String getTitle() {
		try {
			return new File(mFilepath).getName();
		} catch (Exception e) {
			return mFilepath;
		}
	}

	public int getFileSourceType() {
		return mFileSourceType;
	}

	public String getUser() {
		return mUser;
	}

	public String getPassword() {
		return mPassword;
	}

	public String getDomain() {
		return mDomain;
	}
	
	public String getUpnpFolderId() {
		return mDomain;
	}
	
	public String getUpnpSerialNumber() {
		return mPassword;
	}
	
	public String getUpnpName() {
		return mUser;
	}

	public boolean isMovie() {
        return mType.equals(DbAdapterSources.KEY_TYPE_MOVIE);
    }
}