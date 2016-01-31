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

import android.content.Context;
import android.database.Cursor;

import com.miz.abstractclasses.MovieFileSource;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.DbMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.utils.MovieDatabaseUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class FileMovie extends MovieFileSource<File> {

	private HashMap<String, String> existingMovies = new HashMap<String, String>();
	private File tempFile;

	public FileMovie(Context context, FileSource fileSource, boolean clearLibrary) {
		super(context, fileSource, clearLibrary);
	}

	@Override
	public void removeUnidentifiedFiles() {
		List<DbMovie> dbMovies = getDbMovies();

		File temp;
		int count = dbMovies.size();
		for (int i = 0; i < count; i++) {
			if (!dbMovies.get(i).isNetworkFile() && !dbMovies.get(i).isUpnpFile()) {
				temp = new File(dbMovies.get(i).getFilepath());
				if (temp.exists() && dbMovies.get(i).isUnidentified())
					MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
			}
		}
	}

	@Override
	public void removeUnavailableFiles() {
		List<DbMovie> dbMovies = getDbMovies();

		File temp;
		int count = dbMovies.size();
		for (int i = 0; i < count; i++) {
			if (!dbMovies.get(i).isNetworkFile()) {
				temp = new File(dbMovies.get(i).getFilepath());
				if (!temp.exists()) {
					MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
				}
			}
		}
	}

	@Override
	public List<String> searchFolder() {

		DbAdapterMovieMappings dbHelper = MizuuApplication.getMovieMappingAdapter();
		Cursor cursor = dbHelper.getAllFilepaths(false); // Query database to return all filepaths in a cursor
		ColumnIndexCache cache = new ColumnIndexCache();

		try {
			while (cursor.moveToNext()) {// Add all movies in cursor to ArrayList of all existing movies
				existingMovies.put(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovieMappings.KEY_FILEPATH)), "");
			}
		} catch (Exception e) {
		} finally {
			cursor.close(); // Close cursor
			cache.clear();
		}

		TreeSet<String> results = new TreeSet<String>();

		// Do a recursive search in the file source folder
		recursiveSearch(getFolder(), results);

		List<String> list = new ArrayList<String>();

		Iterator<String> it = results.iterator();
		while (it.hasNext())
			list.add(it.next());

		return list;
	}

	@Override
	public void recursiveSearch(File folder, TreeSet<String> results) {
		try {
			if (folder.isDirectory()) {
				// Check if this is a DVD folder
				if (folder.getName().equalsIgnoreCase("video_ts")) {
					File[] children = folder.listFiles();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getName().equalsIgnoreCase("video_ts.ifo"))
							addToResults(children[i], results);
					}
				} // Check if this is a Blu-ray folder
				else if (folder.getName().equalsIgnoreCase("bdmv")) {
					File[] children = folder.listFiles();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getName().equalsIgnoreCase("stream")) {
							File[] m2tsVideoFiles = children[i].listFiles();

							if (m2tsVideoFiles.length > 0) {
								File largestFile = m2tsVideoFiles[0];

								for (int j = 0; j < m2tsVideoFiles.length; j++)
									if (largestFile.length() < m2tsVideoFiles[j].length())
										largestFile = m2tsVideoFiles[j];

								addToResults(largestFile, results);
							}
						}
					}
				} else {
					String[] childs = folder.list();
					for (int i = 0; i < childs.length; i++) {
						tempFile = new File(folder.getAbsolutePath(), childs[i]);
						recursiveSearch(tempFile, results);
					}
				}
			} else {
				addToResults(folder, results);
			}
		} catch (Exception e) {}
	}

	@Override
	public void addToResults(File file, TreeSet<String> results) {
		if (MizLib.checkFileTypes(file.getAbsolutePath())) {
			if (file.length() < getFileSizeLimit() && !file.getName().equalsIgnoreCase("video_ts.ifo"))
				return;

			if (!clearLibrary())
				if (existingMovies.get(file.getAbsolutePath()) != null) return;

			String tempFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
			if (tempFileName.toLowerCase(Locale.ENGLISH).matches(".*part[2-9]|cd[2-9]")) return;

			//Add the file if it reaches this point
			results.add(file.getAbsolutePath());
		}
	}

	@Override
	public File getRootFolder() {
		return new File(getFileSource().getFilepath());
	}

	@Override
	public String toString() {
		return getRootFolder().getAbsolutePath();
	}
}