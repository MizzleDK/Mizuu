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

package com.miz.filesources;

import android.util.Log;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.functions.BrowserFileObject;
import com.miz.functions.MizLib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jcifs.smb.SmbFile;

public class BrowserSmb extends AbstractFileSourceBrowser<SmbFile> {

	public BrowserSmb(SmbFile folder) {
		super(folder);
	}

	@Override
	public boolean browse(SmbFile folder) {
		if (folder.getName().equals("smb://"))
			return false;

		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		List<SmbFile> orderedArray = new ArrayList<SmbFile>();
		List<SmbFile> temp = new ArrayList<SmbFile>();

		try {
			SmbFile[] listFiles = folder.listFiles();
			if (listFiles == null)
				return false;

            Collections.addAll(orderedArray, listFiles);

			Collections.sort(orderedArray, new Comparator<SmbFile>() {
				@Override
				public int compare(SmbFile f1, SmbFile f2) {
					try {
						if (f1.isDirectory() && !f2.isDirectory()) {  
							// Directory before non-directory  
							return -1;  
						} else if (!f1.isDirectory() && f2.isDirectory()) {  
							// Non-directory after directory  
							return 1;  
						}
					} catch (Exception ignored) {}

					return f1.getName().toLowerCase(Locale.getDefault()).compareTo(f2.getName().toLowerCase(Locale.getDefault()));  
				}
			});

			for (SmbFile f : orderedArray)
				if (MizLib.isValidFilename(f.getName())) {
					list.add(new BrowserFileObject(f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length()));
					temp.add(f);
				}

			listFiles = temp.toArray(new SmbFile[temp.size()]);
			temp.clear();
			orderedArray.clear();

			setParentFolder(new SmbFile(folder.getParent()));
			setCurrentFolder(folder);
			setCurrentFiles(listFiles);

			setBrowserFiles(list);
		} catch (Exception e) {
			Log.d("Mizuu", e.toString());
			return false;
		}

		return browseParent();
	}

	@Override
	public String getSubtitle() {
		return MizLib.transformSmbPath(getCurrentFolder().getCanonicalPath());
	}

	@Override
	public boolean browseParent() {
		SmbFile folder = getParentFolder();
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		List<SmbFile> orderedArray = new ArrayList<SmbFile>();
		List<SmbFile> temp = new ArrayList<SmbFile>();

		if (folder.getName().equals("smb://")) {
			setBrowserParentFiles(list);
			return true;
		}

		try {
			SmbFile[] listFiles = folder.listFiles();
			if (listFiles == null)
				return false;
			
			for (SmbFile f : listFiles)
				if (f.isDirectory())
					orderedArray.add(f);

			Collections.sort(orderedArray, new Comparator<SmbFile>() {
				@Override
				public int compare(SmbFile f1, SmbFile f2) {
					return f1.getName().toLowerCase(Locale.getDefault()).compareTo(f2.getName().toLowerCase(Locale.getDefault()));  
				}
			});

			for (SmbFile f : orderedArray) {
				list.add(new BrowserFileObject(f.getName(), true, 0));
				temp.add(f);
			}

			listFiles = temp.toArray(new SmbFile[temp.size()]);
			temp.clear();
			orderedArray.clear();
			
			setCurrentParentFiles(listFiles);
			setBrowserParentFiles(list);
		} catch (Exception e) {
			Log.d("Mizuu", e.toString());
			return false;
		}

		return true;
	}
}
