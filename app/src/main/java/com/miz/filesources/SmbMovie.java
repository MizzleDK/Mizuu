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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SmbMovie extends MovieFileSource<SmbFile> {

	private HashMap<String, String> existingMovies = new HashMap<String, String>();
	private SmbFile tempSmbFile;

	public SmbMovie(Context context, FileSource fileSource, boolean clearLibrary) {
		super(context, fileSource, clearLibrary);
	}

	@Override
	public void removeUnidentifiedFiles() {
		List<DbMovie> dbMovies = getDbMovies();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

		FileSource source;
		SmbFile temp;
		int count = dbMovies.size();
		if (MizLib.isWifiConnected(getContext())) {
			for (int i = 0; i < count; i++) {
				if (dbMovies.get(i).isNetworkFile()) {
					try {
						source = null;

						for (int j = 0; j < filesources.size(); j++)
							if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
								source = filesources.get(j);
								break;
							}

						if (source == null) {
							if (dbMovies.get(i).isUnidentified())
								MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
							continue;
						}

						temp = new SmbFile(
								MizLib.createSmbLoginString(
										source.getDomain(),
										source.getUser(),
										source.getPassword(),
										dbMovies.get(i).getFilepath(),
										false
								));

						if (temp.exists() && dbMovies.get(i).isUnidentified())
							MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			}
		}
	}

	@Override
	public void removeUnavailableFiles() {
		List<DbMovie> dbMovies = getDbMovies();
		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

		FileSource source;
		SmbFile temp;
		int count = dbMovies.size();
		if (MizLib.isWifiConnected(getContext())) {
			for (int i = 0; i < count; i++) {
				if (dbMovies.get(i).isNetworkFile() && !dbMovies.get(i).hasOfflineCopy()) {
					try {
						source = null;

						for (int j = 0; j < filesources.size(); j++)
							if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
								source = filesources.get(j);
								break;
							}

						if (source == null) {
							MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
							continue;
						}

						temp = new SmbFile(
								MizLib.createSmbLoginString(
										source.getDomain(),
										source.getUser(),
										source.getPassword(),
										dbMovies.get(i).getFilepath(),
										false
								));

						if (!temp.exists()) {
							MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
						}
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			}
		}

		// Clean up
		filesources.clear();
	}

	@Override
	public List<String> searchFolder() {
		if (getFolder() == null)
			return new ArrayList<String>(); // Return empty List

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
	public void recursiveSearch(SmbFile folder, TreeSet<String> results) {
		try {
			if (folder.isDirectory()) {
				// Check if this is a DVD folder
				if (folder.getName().equalsIgnoreCase("video_ts/")) {
					SmbFile[] children = folder.listFiles();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getName().equalsIgnoreCase("video_ts.ifo"))
							addToResults(children[i], results);
					}
				} // Check if this is a Blu-ray folder
				else if (folder.getName().equalsIgnoreCase("bdmv/")) {
					SmbFile[] children = folder.listFiles();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getName().equalsIgnoreCase("stream/")) {
							SmbFile[] m2tsVideoFiles = children[i].listFiles();

							if (m2tsVideoFiles.length > 0) {
								SmbFile largestFile = m2tsVideoFiles[0];

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
						tempSmbFile = new SmbFile(folder.getCanonicalPath() + childs[i] + "/");
						if (tempSmbFile.isDirectory()) {
							recursiveSearch(tempSmbFile, results);
						} else {
							tempSmbFile = new SmbFile(folder.getCanonicalPath() + childs[i]);
							addToResults(tempSmbFile, results);
						}
					}
				}
			} else {
				addToResults(folder, results);
			}
		} catch (Exception e) {}
	}

	@Override
	public void addToResults(SmbFile file, TreeSet<String> results) {
		if (MizLib.checkFileTypes(file.getCanonicalPath())) {
			try {
				if (file.length() < getFileSizeLimit() && !file.getName().equalsIgnoreCase("video_ts.ifo"))
					return;
			} catch (SmbException e) {
				return;
			}

			if (!clearLibrary())
				if (existingMovies.get(file.getCanonicalPath()) != null) return;

			String tempFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
			if (tempFileName.toLowerCase(Locale.ENGLISH).matches(".*part[2-9]|cd[2-9]")) return;

			//Add the file if it reaches this point
			results.add(file.getCanonicalPath());
		}
	}

	@Override
	public SmbFile getRootFolder() {
		try {
			FileSource fs = getFileSource();
			return new SmbFile(
					MizLib.createSmbLoginString(
							fs.getDomain(),
							fs.getUser(),
							fs.getPassword(),
							fs.getFilepath(),
							true
					));
		} catch (Exception e) {}
		return null;
	}

	@Override
	public String toString() {
		return MizLib.transformSmbPath(getRootFolder().getCanonicalPath());
	}
}