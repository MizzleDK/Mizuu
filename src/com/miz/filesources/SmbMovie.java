package com.miz.filesources;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import android.content.Context;
import android.database.Cursor;

import com.miz.abstractclasses.MovieFileSource;
import com.miz.db.DbAdapter;
import com.miz.functions.DbMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public class SmbMovie extends MovieFileSource<SmbFile> {

	private HashMap<String, String> existingMovies = new HashMap<String, String>();
	private SmbFile tempSmbFile;

	public SmbMovie(Context context, FileSource fileSource, boolean ignoreRemovedFiles, boolean subFolderSearch, boolean clearLibrary, boolean disableEthernetWiFiCheck) {
		super(context, fileSource, ignoreRemovedFiles, subFolderSearch, clearLibrary, disableEthernetWiFiCheck);
	}

	@Override
	public void removeUnidentifiedFiles() {
		DbAdapter db = MizuuApplication.getMovieAdapter();
		List<DbMovie> dbMovies = getDbMovies();

		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

		FileSource source;
		SmbFile temp;
		int count = dbMovies.size();
		if (MizLib.isWifiConnected(getContext(), disableEthernetWiFiCheck())) {
			for (int i = 0; i < count; i++) {
				if (dbMovies.get(i).isNetworkFile()) {
					try {
						source = null;

						for (int j = 0; j < filesources.size(); j++)
							if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
								source = filesources.get(j);
								continue;
							}

						if (source == null) {
							if (dbMovies.get(i).isUnidentified())
								db.deleteMovie(dbMovies.get(i).getRowId());
							continue;
						}

						temp = new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										dbMovies.get(i).getFilepath(),
										false
										));

						if (temp.exists() && dbMovies.get(i).isUnidentified())
							db.deleteMovie(dbMovies.get(i).getRowId());
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			}
		}
	}

	@Override
	public void removeUnavailableFiles() {
		DbAdapter db = MizuuApplication.getMovieAdapter();
		List<DbMovie> dbMovies = getDbMovies(), deletedMovies = new ArrayList<DbMovie>();
		ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

		FileSource source;
		boolean deleted;
		SmbFile temp;
		int count = dbMovies.size();
		if (MizLib.isWifiConnected(getContext(), disableEthernetWiFiCheck())) {
			for (int i = 0; i < count; i++) {
				if (dbMovies.get(i).isNetworkFile() && !dbMovies.get(i).hasOfflineCopy(getContext())) {
					try {
						source = null;

						for (int j = 0; j < filesources.size(); j++)
							if (dbMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
								source = filesources.get(j);
								continue;
							}

						if (source == null) {
							deleted = db.deleteMovie(dbMovies.get(i).getRowId());
							if (deleted)
								deletedMovies.add(dbMovies.get(i));
							continue;
						}

						temp = new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										dbMovies.get(i).getFilepath(),
										false
										));

						if (!temp.exists()) {
							deleted = db.deleteMovie(dbMovies.get(i).getRowId());
							if (deleted)
								deletedMovies.add(dbMovies.get(i));
						}
					} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
				}
			}
		}

		count = deletedMovies.size();
		for (int i = 0; i < count; i++) {
			if (!db.movieExists(deletedMovies.get(i).getTmdbId())) {
				MizLib.deleteFile(new File(deletedMovies.get(i).getThumbnail()));
				MizLib.deleteFile(new File(deletedMovies.get(i).getBackdrop()));
			}
		}

		// Clean up
		deletedMovies.clear();
		filesources.clear();
	}

	@Override
	public List<String> searchFolder() {
		if (getFolder() == null)
			return new ArrayList<String>(); // Return empty List

		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();
		Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", ignoreRemovedFiles(), true); // Query database to return all movies to a cursor

		try {
			while (cursor.moveToNext()) {// Add all movies in cursor to ArrayList of all existing movies
				existingMovies.put(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)), "");
			}
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
			} else {
				SmbFile[] children = folder.listFiles();
				for (int i = 0; i < children.length; i++)
					addToResults(children[i], results);
			}
		} catch (Exception e) {}
	}

	@Override
	public void addToResults(SmbFile file, LinkedHashSet<String> results) {
		if (supportsNfo() && MizLib.isNfoFile(file.getCanonicalPath())) {
			try {
				addNfoFile(MizLib.removeExtension(file.getCanonicalPath()), file.getInputStream());
			} catch (IOException ignored) {}
		} else if (MizLib.checkFileTypes(file.getCanonicalPath())) {
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