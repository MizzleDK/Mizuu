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
		return mName;
	}
	
	public boolean isDirectory() {
		return mIsDirectory;
	}
	
	public long getSize() {
		return mSize;
	}
}
