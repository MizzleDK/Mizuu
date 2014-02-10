package com.miz.abstractclasses;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.content.Context;

import com.miz.functions.FileSource;

public abstract class AbstractFileSource<T> {

	protected List<String> mFiles = new ArrayList<String>();
	protected T mFolder;
	protected FileSource mFileSource = null;
	protected Context mContext;
	protected boolean mIgnoreRemovedFiles, mSubFolderSearch, mClearLibrary, mDisableEthernetWiFiCheck;
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

	public boolean ignoreRemovedFiles() {
		return mIgnoreRemovedFiles;
	}

	public boolean searchSubFolders() {
		return mSubFolderSearch;
	}

	public boolean clearLibrary() {
		return mClearLibrary;
	}

	public boolean disableEthernetWiFiCheck() {
		return mDisableEthernetWiFiCheck;
	}

	public int getFileSizeLimit() {
		return mFileSizeLimit;
	}

	public abstract void removeUnidentifiedFiles();

	public abstract void removeUnavailableFiles();

	public abstract List<String> searchFolder();

	public abstract void recursiveSearch(T folder, LinkedHashSet<String> results);

	public abstract void addToResults(T folder, LinkedHashSet<String> results);

	public abstract T getRootFolder();

	public abstract String toString();
}
