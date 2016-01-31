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

package com.miz.abstractclasses;

import android.content.Context;

import com.miz.functions.FileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public abstract class AbstractFileSource<T> {

	protected List<String> mFiles = new ArrayList<String>();
	protected T mFolder;
	protected FileSource mFileSource = null;
	protected Context mContext;
	protected boolean mClearLibrary;
	protected int mFileSizeLimit;

	public void setFolder(T folder) {
		mFolder = folder;
	}

	public T getFolder() {
		return mFolder;
	}

	public List<String> getFiles() {
		return mFiles;
	}

	public FileSource getFileSource() {
		return mFileSource;
	}

	public Context getContext() {
		return mContext;
	}

	public boolean clearLibrary() {
		return mClearLibrary;
	}

	public int getFileSizeLimit() {
		return mFileSizeLimit;
	}

	public abstract void removeUnidentifiedFiles();

	public abstract void removeUnavailableFiles();

	public abstract List<String> searchFolder();

	public abstract void recursiveSearch(T folder, TreeSet<String> results);

	public abstract void addToResults(T folder, TreeSet<String> results);

	public abstract T getRootFolder();

	public abstract String toString();
}
