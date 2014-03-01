package com.miz.abstractclasses;

import java.util.ArrayList;
import java.util.List;

import com.miz.functions.BrowserFileObject;

public abstract class AbstractFileSourceBrowser<T> {

	private T mCurrentFolder, mParentFolder;
	private T[] mCurrentFiles;
	private List<BrowserFileObject> mBrowserFiles;

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

	public T getCurrentFolder() {
		return mCurrentFolder;
	}

	public void setCurrentFiles(T[] files) {
		mCurrentFiles = files;
	}

	public T[] getCurrentFiles() {
		return mCurrentFiles;
	}

	public List<BrowserFileObject> getBrowserFiles() {
		if (mBrowserFiles == null)
			mBrowserFiles = new ArrayList<BrowserFileObject>();
		return mBrowserFiles;
	}

	public void setBrowserFiles(List<BrowserFileObject> list) {
		mBrowserFiles = list;
	}

	public void goUp() {
		if (mParentFolder != null)
			browse(mParentFolder);
	}

	public abstract List<BrowserFileObject> browse(int index);

	public abstract List<BrowserFileObject> browse(T folder);

}
