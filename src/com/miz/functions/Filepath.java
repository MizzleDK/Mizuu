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

import com.miz.utils.StringUtils;

public class Filepath {

	private final String mFilepath;

	public Filepath(String path) {
		mFilepath = path;
	}

	public String getFilepath() {
		String temp = mFilepath.contains("<MiZ>") ? mFilepath.split("<MiZ>")[1] : mFilepath;

		if (getType() == FileSource.SMB)
			return MizLib.transformSmbPath(temp);

		return temp;
	}

	public String getFullFilepath() {
		return mFilepath;
	}

	public int getType() {
		if (mFilepath.contains("smb://")) {
			return FileSource.SMB;
		} else if (mFilepath.contains("http://")) {
			return FileSource.UPNP;
		} else {
			return FileSource.FILE;
		}
	}

	public String getFilepathName() {
		try {
			String path = mFilepath;

			// Check if this is a UPnP filepath
			if (path.contains("<MiZ>"))
				path = path.split("<MiZ>")[0];

			if (path.contains("/"))
				path = path.substring(path.lastIndexOf("/") + 1, path.length());

			return path;
		} catch (Exception e) {
			return getFilepath();
		}
	}

	public boolean isNetworkFile() {
		int type = getType();
		return type == FileSource.SMB || type == FileSource.UPNP;
	}
}
