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

public class DecryptedMovie {
	
	private String mFilepath = "", mFileName = "", mParentName = "", mFileNameYear = "", mParentNameYear = "", mDecryptedFileName = "", mDecryptedParentName = "", mImdbId;

	public String getFilepath() {
		return mFilepath;
	}

	public void setFilepath(String filepath) {
		mFilepath = filepath;
		
		if (mFilepath.contains("/") && mFilepath.contains(".") && (mFilepath.lastIndexOf('/') < mFilepath.lastIndexOf('.')) && MizLib.checkFileTypes(mFilepath)) {
			setFileName(mFilepath.substring(mFilepath.lastIndexOf('/') + 1, mFilepath.lastIndexOf('.')).trim());
		} else if (mFilepath.contains("/")) {
			setFileName(mFilepath.substring(mFilepath.lastIndexOf('/') + 1, mFilepath.length()).trim());
		} else {
			setFileName(mFilepath.trim());
		}
		
		// Check if the file has a parent folder by checking if the filepath contains more than one forward slash.
		if (MizLib.countOccurrences(mFilepath, '/') > 1)  {
			mParentName = mFilepath.substring(0, mFilepath.lastIndexOf('/'));
			setParentName(mParentName.substring(mParentName.lastIndexOf('/') + 1, mParentName.length()).trim());
		}
	}

	public String getFileName() {
		return mFileName;
	}

	public void setFileName(String fileName) {
		mFileName = fileName;
	}

	public String getParentName() {
		return mParentName;
	}

	public void setParentName(String parentName) {
		mParentName = parentName;
	}

	public String getFileNameYear() {
		return mFileNameYear;
	}

	public void setFileNameYear(String fileNameYear) {
		mFileNameYear = fileNameYear;
	}

	public String getParentNameYear() {
		return mParentNameYear;
	}

	public void setParentNameYear(String parentNameYear) {
		mParentNameYear = parentNameYear;
	}
	
	public void setDecryptedFileName(String decryptedFileName) {
		mDecryptedFileName = decryptedFileName;
		if (hasImdbId())
			mDecryptedFileName = mDecryptedFileName.replace(getImdbId(), "").trim();
	}
	
	public String getDecryptedFileName() {
		return mDecryptedFileName;
	}
	
	public void setDecryptedParentName(String decryptedParentName) {
		mDecryptedParentName = decryptedParentName;
	}
	
	public String getDecryptedParentName() {
		return mDecryptedParentName;
	}
	
	public boolean hasFileNameYear() {
		return !getFileNameYear().isEmpty();
	}
	
	public boolean hasParentName() {
		return !getParentName().isEmpty();
	}
	
	public boolean hasParentNameYear() {
		return !getParentNameYear().isEmpty();
	}

	public void setImdbId(String id) {
		mImdbId = id;
	}
	
	public String getImdbId() {
		return mImdbId;
	}
	
	public boolean hasImdbId() {
		return getImdbId() != null;
	}
}