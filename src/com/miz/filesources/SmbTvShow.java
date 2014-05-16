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

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import android.content.Context;
import android.database.Cursor;

import com.miz.abstractclasses.TvShowFileSource;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.DbEpisode;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public class SmbTvShow extends TvShowFileSource<SmbFile> {

	private HashMap<String, String> existingEpisodes = new HashMap<String, String>();
	private SmbFile tempSmbFile;

	public SmbTvShow(Context context, FileSource fileSource, boolean ignoreRemovedFiles, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		super(context, fileSource, ignoreRemovedFiles, subFolderSearch, clearLibrary, disableEthernetWiFiCheck);
	}

	@Override
	public void removeUnidentifiedFiles() {
		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
		List<DbEpisode> dbEpisodes = getDbEpisodes();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_SHOWS, true);

		FileSource source;
		SmbFile tempFile;
		int count = dbEpisodes.size();
		if (MizLib.isWifiConnected(getContext(), disableEthernetWiFiCheck())) {
			for (int i = 0; i < count; i++) {
				if (dbEpisodes.get(i).isUnidentified()) {
					source = null;

					for (int j = 0; j < filesources.size(); j++) {
						if (dbEpisodes.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
							source = filesources.get(j);
							continue;
						}
					}

					if (source != null) {
						try {
							tempFile = new SmbFile(
									MizLib.createSmbLoginString(
											URLEncoder.encode(source.getDomain(), "utf-8"),
											URLEncoder.encode(source.getUser(), "utf-8"),
											URLEncoder.encode(source.getPassword(), "utf-8"),
											dbEpisodes.get(i).getFilepath(),
											false
											));

							if (tempFile.exists())
								db.deleteEpisode(dbEpisodes.get(i).getRowId());

						} catch (Exception e) {}
					}
				}
			}
		}
	}

	@Override
	public void removeUnavailableFiles() {
		ArrayList<DbEpisode> dbEpisodes = new ArrayList<DbEpisode>(), removedEpisodes = new ArrayList<DbEpisode>();

		// Fetch all the episodes from the database
		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor tempCursor = db.getAllEpisodesInDatabase(ignoreRemovedFiles());
		while (tempCursor.moveToNext()) {
			try {
				dbEpisodes.add(new DbEpisode(getContext(),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)) + "_S" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)) + "E" + tempCursor.getString(tempCursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)) + ".jpg"));
			} catch (NullPointerException e) {}
		}
		tempCursor.close();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_SHOWS, true);

		SmbFile tempFile;
		int count = dbEpisodes.size();
		if (MizLib.isWifiConnected(getContext(), disableEthernetWiFiCheck())) {
			for (int i = 0; i < dbEpisodes.size(); i++) {
				FileSource source = null;

				for (int j = 0; j < filesources.size(); j++)
					if (dbEpisodes.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
						source = filesources.get(j);
						continue;
					}

				if (source == null) {
					boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
					if (deleted)
						removedEpisodes.add(dbEpisodes.get(i));
					continue;
				}

				try {
					tempFile = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(source.getDomain(), "utf-8"),
									URLEncoder.encode(source.getUser(), "utf-8"),
									URLEncoder.encode(source.getPassword(), "utf-8"),
									dbEpisodes.get(i).getFilepath(),
									false
									));

					if (!tempFile.exists()) {
						boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
						if (deleted)
							removedEpisodes.add(dbEpisodes.get(i));
					}
				} catch (Exception e) {
					boolean deleted = db.deleteEpisode(dbEpisodes.get(i).getRowId());
					if (deleted)
						removedEpisodes.add(dbEpisodes.get(i));
				}
			}
		}

		count = removedEpisodes.size();
		for (int i = 0; i < count; i++) {
			if (db.getEpisodeCount(removedEpisodes.get(i).getShowId()) == 0) { // No more episodes for this show
				DbAdapterTvShow dbShow = MizuuApplication.getTvDbAdapter();
				boolean deleted = dbShow.deleteShow(removedEpisodes.get(i).getShowId());

				if (deleted) {
					MizLib.deleteFile(new File(removedEpisodes.get(i).getThumbnail()));
					MizLib.deleteFile(new File(removedEpisodes.get(i).getBackdrop()));
				}
			}

			MizLib.deleteFile(new File(removedEpisodes.get(i).getEpisodeCoverPath()));
		}

		// Clean up
		dbEpisodes.clear();
		removedEpisodes.clear();
	}

	@Override
	public List<String> searchFolder() {
		DbAdapterTvShowEpisode dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
		Cursor cursor = dbHelper.getAllEpisodesInDatabase(ignoreRemovedFiles()); // Query database to return all show episodes to a cursor
		try {
			while (cursor.moveToNext()) // Add all show episodes in cursor to ArrayList of all existing episodes
				existingEpisodes.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)), "");
		} catch (Exception e) {
		} finally {
			cursor.close(); // Close cursor
		}

		LinkedHashSet<String> results = new LinkedHashSet<String>();

		// Do a recursive search in the file source folder
		recursiveSearch(getFolder(), results);

		List<String> list = new ArrayList<String>();

		Iterator<String> it = results.iterator();
		while (it.hasNext())
			list.add(it.next());

		return list;
	}

	@Override
	public void recursiveSearch(SmbFile folder, LinkedHashSet<String> results) {
		try {
			if (searchSubFolders()) {
				if (folder.isDirectory()) {
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
				} else {
					addToResults(folder, results);
				}
			} else {
				SmbFile[] children = folder.listFiles();
				for (int i = 0; i < children.length; i++)
					addToResults(children[i], results);
			}
		} catch (Exception e) {}
	}

	@Override
	public void addToResults(SmbFile file, LinkedHashSet<String> results) {
		if (MizLib.checkFileTypes(file.getCanonicalPath())) {
			try {
				if (file.length() < getFileSizeLimit())
					return;
			} catch (SmbException e) {
				return;
			}

			if (!clearLibrary())
				if (existingEpisodes.get(file.getCanonicalPath()) != null) return;

			//Add the file if it reaches this point
			results.add(file.getCanonicalPath());
		}
	}

	@Override
	public SmbFile getRootFolder() {
		try {
			FileSource fs = getFileSource();
			SmbFile root = new SmbFile(
					MizLib.createSmbLoginString(
							URLEncoder.encode(fs.getDomain(), "utf-8"),
							URLEncoder.encode(fs.getUser(), "utf-8"),
							URLEncoder.encode(fs.getPassword(), "utf-8"),
							fs.getFilepath(),
							true
							));
			return root;
		} catch (Exception e) {}
		return null;
	}

	@Override
	public String toString() {
		return MizLib.transformSmbPath(getRootFolder().getCanonicalPath());
	}
}