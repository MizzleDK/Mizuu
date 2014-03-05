package com.miz.functions;

public class BrowserFileObject {

	private String mName;
	private boolean mIsDirectory;
	private long mSize;
	
	public BrowserFileObject(String name, boolean isDirectory, long size) {
		mName = name;
		mIsDirectory = isDirectory;
		mSize = size;
	}
	
	public String getName() {
		if (mName.endsWith("/"))
			return mName.substring(0, mName.length() - 1);
		return mName;
	}
	
	public boolean isDirectory() {
		return mIsDirectory;
	}
	
	public long getSize() {
		return mSize;
	}
}
