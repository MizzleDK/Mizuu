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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BrowserFile extends AbstractFileSourceBrowser<File> {

	public BrowserFile(File folder) {
		super(folder);
	}

	@Override
	public boolean browse(File folder) {
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		List<File> orderedArray = new ArrayList<File>();
		List<File> temp = new ArrayList<File>();

		try {
			File[] listFiles = folder.listFiles();
			if (listFiles == null)
				return false;

            Collections.addAll(orderedArray, listFiles);

			Collections.sort(orderedArray, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
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

			for (File f : orderedArray)
				if (MizLib.isValidFilename(f.getName())) {
					list.add(new BrowserFileObject(f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length()));
					temp.add(f);
				}

			listFiles = temp.toArray(new File[temp.size()]);
			temp.clear();
			orderedArray.clear();

			if (folder.getAbsolutePath().equals("/"))
				setParentFolder(null);
			else
				setParentFolder(new File(folder.getParent()));
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
		return getCurrentFolder().getAbsolutePath();
	}

	@Override
	public boolean browseParent() {
		File folder = getParentFolder();
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		List<File> orderedArray = new ArrayList<File>();
		List<File> temp = new ArrayList<File>();

		if (folder == null) {
			setBrowserParentFiles(list);
			return true;
		}

		try {
			File[] listFiles = folder.listFiles();
			if (listFiles == null)
				return false;

			for (File f : listFiles)
				if (f.isDirectory())
					orderedArray.add(f);

			Collections.sort(orderedArray, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					return f1.getName().toLowerCase(Locale.getDefault()).compareTo(f2.getName().toLowerCase(Locale.getDefault()));  
				}
			});

			for (File f : orderedArray) {
				list.add(new BrowserFileObject(f.getName(), true, 0));
				temp.add(f);
			}

			listFiles = temp.toArray(new File[temp.size()]);
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
