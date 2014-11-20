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