package com.miz.functions;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class NetworkFile {

		private boolean isDirectory;
		private String path, name, parent;
		private long length;

		public NetworkFile(SmbFile smb) {
			path = smb.getCanonicalPath();
			try {
				name = smb.getName();
			} catch (Exception e) {
				name = "smb://";
			}
			try {
				isDirectory = smb.isDirectory();
			} catch (SmbException e) {
				isDirectory = false;
			}
			try {
				length = smb.length();
			} catch (SmbException e) {
				length = 0;
			}
			try {
				parent = smb.getParent();
			} catch (Exception e) {
				parent = "smb://";
			}
		}

		public String getCanonicalPath() {
			return path;
		}

		public String getName() {
			if (name.endsWith("/"))
				return name.substring(0, name.length() - 1);
			return name;
		}

		public boolean isDirectory() {
			return isDirectory;
		}

		public long length() {
			return length;
		}

		public String getParent() {
			return parent;
		}
	}