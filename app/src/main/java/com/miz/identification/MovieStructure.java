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

package com.miz.identification;

import com.miz.functions.MizLib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieStructure {

	private final String mFilepath;
	private String mFilename, mParentFolder = "", mImdbId, mCustomTags = "";

	public MovieStructure(String filepath) {
		mFilepath = filepath;
		split();
		checkImdbId();
	}
	
	public void split() {		
		Pattern splitPattern = Pattern.compile("/"); // Pre-compiled pattern to speed things up
		// UPnP files don't have an extension, so we add one in those cases in order to make everything work with titles like "G.I. Joe"
		String[] split = splitPattern.split(mFilepath.contains("<MiZ>") ? (mFilepath.split("<MiZ>")[0] + ".mkv") : mFilepath);
		if (split.length >= 2) {
			mFilename = split[split.length - 1];
			mParentFolder = split[split.length - 2].trim();
		} else {
			// We're dealing with two or less parts
			mFilename = split[split.length - 1].trim();
			if (split.length == 2) {
				mParentFolder = split[0].trim();
			}
		}
	}

	public void setCustomTags(String customTags) {
		mCustomTags = customTags;
	}
	
	public void checkImdbId() {
		// Prioritize the filename
		String temp = MizLib.decryptImdbId(getFilename());
		if (null != temp) {
			mImdbId = temp;
			return;
		}
		
		temp = MizLib.decryptImdbId(getParentFolderName());
		if (null != temp) {
			mImdbId = temp;
		}
	}
	
	public String getFilepath() {
		return mFilepath;
	}
	
	public String getFilename() {
		return mFilename;
	}
	
	public String getParentFolderName() {
		return mParentFolder;
	}
	
	public boolean hasImdbId() {
		return null != getImdbId();
	}

	public String getImdbId() {
		return mImdbId;
	}
	
	public String getDecryptedFilename() {
		return MizLib.decryptName(MizLib.getFilenameWithoutExtension(getFilename()), mCustomTags);
	}
	
	public String getDecryptedParentFolderName() {
		return MizLib.decryptName(getParentFolderName(), mCustomTags);
	}
	
	/**
	 * Get release year from file name (priority) or parent folder name.
	 * @return Release year if found, -1 otherwise.
	 */
	public int getReleaseYear() {
		int result = -1;
		Pattern pattern = Pattern.compile(".*?((?:18|19|20)[0-9][0-9]).*?");

		// Attempt to match it against the file name first
		Matcher matcher = pattern.matcher(getFilename());

		while (matcher.find())
			result = MizLib.getInteger(matcher.group(1));
		
		if (result >= 0)
			return result;

		// Check if there's a release year in the parent folder name
		matcher = pattern.matcher(getParentFolderName());

        while (matcher.find())
            result = MizLib.getInteger(matcher.group(1));
		
		if (result >= 0)
			return result;

		return result;
	}

	public boolean hasReleaseYear() {
		return getReleaseYear() >= 0;
	}
}