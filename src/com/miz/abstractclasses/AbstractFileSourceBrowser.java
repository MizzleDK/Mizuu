package com.miz.abstractclasses;

import java.util.ArrayList;
import java.util.List;

import com.miz.functions.BrowserFileObject;

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
		if (mParentFolder != null)
			return browse(mParentFolder);
		return false;
	}

	public boolean browse(int index, boolean fromParent) {
		return browse(fromParent ? getCurrentParentFiles()[index] : getCurrentFiles()[index]);
	}

	public abstract boolean browse(T folder);
	
	public abstract boolean browseParent();

	public abstract String getSubtitle();
}
