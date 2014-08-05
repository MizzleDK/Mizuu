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

public class MovieFilepath {
	
	private int mRowId;
	private String mFilepath;
	private boolean mIgnored;

	public MovieFilepath(int rowId, String filepath, boolean ignored) {
		mRowId = rowId;
		mFilepath = filepath;
		mIgnored = ignored;
	}

	public int getRowId() {
		return mRowId;
	}

	public String getFilepath() {
		String temp = mFilepath.contains("<MiZ>") ? mFilepath.split("<MiZ>")[1] : mFilepath;
		mFilepath = MizLib.transformSmbPath(temp);
		return mFilepath;
	}
	
	public boolean isIgnored() {
		return mIgnored;
	}
}