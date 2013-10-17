package com.miz.functions;

public class DecryptedMovie {
	
	private String mFilepath = "", mFileName = "", mParentName = "", mFileNameYear = "", mParentNameYear = "", mDecryptedFileName = "", mDecryptedParentName = "";

	public String getFilepath() {
		return mFilepath;
	}

	public void setFilepath(String filepath) {
		mFilepath = filepath;
		
		if (mFilepath.contains("/")) {
			setFileName(mFilepath.substring(mFilepath.lastIndexOf('/') + 1, mFilepath.lastIndexOf('.')).trim());
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

}