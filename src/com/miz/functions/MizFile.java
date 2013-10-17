package com.miz.functions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MizFile implements Comparable<MizFile> {

	private SmbFile smb;
	private File file;
	private boolean isDirectory, isSmb, exists;
	private long length;
	private String absolutePath, name, parent;
	private String[] list;
	private MizFile[] listFiles;

	public MizFile(File f) {
		file = f;
		isSmb = false;
		length = file.length();
		absolutePath = file.getAbsolutePath();
		name = file.getName();
		exists = file.exists();
		list = file.list();
		
		parent = f.getParent();
	}

	public MizFile(SmbFile s) {
		smb = s;
		isSmb = true;

		try {
			length = smb.length();
		} catch (SmbException e) {
			length = 0;
		}
		try {
			exists = smb.exists();
		} catch (SmbException e) {
			exists = false;
		}
		try {
			list = smb.list();
		} catch (SmbException e) {
			list = new String[]{};
		}
		absolutePath = smb.getCanonicalPath();
		try {
			name = smb.getName();
		} catch (Exception e) {
			name = "smb://";
		}
		
		parent = s.getParent();
	}

	public boolean isDirectory() {
		
		if (isSmb) {
			try {
				isDirectory = smb.isDirectory();
			} catch (SmbException e) {
				isDirectory = false;
			}
		} else {
			isDirectory = file.isDirectory();
		}
		
		return isDirectory;
	}

	public boolean isSmb() {
		return isSmb;
	}

	public boolean exists() {
		return exists;
	}

	public long length() {
		return length;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getName() {
		return name;
	}

	public String[] list() {
		return list;
	}

	public MizFile[] listFiles() {
		
		if (isSmb) {
			try {
				SmbFile[] fileList = smb.listFiles();
				int size = fileList.length;
				listFiles = new MizFile[size];
				for (int i = 0; i < size; i++)
					if (fileList[i] != null)
						listFiles[i] = new MizFile(fileList[i]);
			} catch (SmbException e) {
				listFiles = new MizFile[]{};
			}
		} else {
			File[] fileList = file.listFiles();
			if (fileList != null) {
				int size = fileList.length;
				listFiles = new MizFile[size];
				for (int i = 0; i < size; i++)
					listFiles[i] = new MizFile(fileList[i]);
			} else {
				listFiles = new MizFile[]{};
			}
		}
		
		return listFiles;
	}
	
	public InputStream getInputStream() {
		if (isSmb) {
			try {
				return smb.getInputStream();
			} catch (IOException e1) {}
		} else {
			try {
				return new FileInputStream(absolutePath);
			} catch (FileNotFoundException e) {}
		}
		
		return null;
	}
	
	public String getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return getAbsolutePath();
	}

	@Override
	public int compareTo(MizFile another) {
		return getAbsolutePath().compareToIgnoreCase(another.getAbsolutePath());
	}
}