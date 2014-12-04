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

import com.miz.functions.BrowserFileObject;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFileSourceBrowser<T> {

	private T mCurrentFolder, mParentFolder;
	private T[] mCurrentFiles, mCurrentParentFiles;
	private List<BrowserFileObject> mBrowserFiles, mBrowserParentFiles;

	public AbstractFileSourceBrowser(T folder) {
		mCurrentFolder = folder;

		browse(getCurrentFolder());
	}

	public void setCurrentFolder(T folder) {
		mCurrentFolder = folder;
	}

	public void setParentFolder(T folder) {
		mParentFolder = folder;
	}
	
	public T getParentFolder() {
		return mParentFolder;
	}

	public T getCurrentFolder() {
		return mCurrentFolder;
	}

	public void setCurrentFiles(T[] files) {
		mCurrentFiles = files;
	}
	
	public void setCurrentParentFiles(T[] files) {
		mCurrentParentFiles = files;
	}

	public T[] getCurrentFiles() {
		return mCurrentFiles;
	}
	
	public T[] getCurrentParentFiles() {
		return mCurrentParentFiles;
	}

	public List<BrowserFileObject> getBrowserFiles() {
		if (mBrowserFiles == null)
			mBrowserFiles = new ArrayList<BrowserFileObject>();
		return mBrowserFiles;
	}
	
	public List<BrowserFileObject> getBrowserParentFiles() {
		if (mBrowserParentFiles == null)
			mBrowserParentFiles = new ArrayList<BrowserFileObject>();
		return mBrowserParentFiles;
	}

	public void setBrowserFiles(List<BrowserFileObject> list) {
		mBrowserFiles = list;
	}
	
	public void setBrowserParentFiles(List<BrowserFileObject> list) {
		mBrowserParentFiles = list;
	}

	public boolean goUp() {
        return mParentFolder != null && browse(mParentFolder);
    }

	public boolean browse(int index, boolean fromParent) {
		return browse(fromParent ? getCurrentParentFiles()[index] : getCurrentFiles()[index]);
	}

	public abstract boolean browse(T folder);
	
	public abstract boolean browseParent();

	public abstract String getSubtitle();
}
