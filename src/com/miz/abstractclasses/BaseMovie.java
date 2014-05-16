package com.miz.abstractclasses;

import android.content.Context;

import java.io.File;
import java.util.Locale;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

public abstract class BaseMovie implements Comparable<BaseMovie> {

	protected Context CONTEXT;
	protected String ROW_ID, FILEPATH, TITLE, TMDB_ID;
	protected boolean ignorePrefixes, ignoreNfo;

	protected String mGetThumbnail;

	public BaseMovie(Context context, String rowId, String filepath, String title, String tmdbId, boolean ignorePrefixes, boolean ignoreNfo) {
		// Set up movie fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		TITLE = title;
		TMDB_ID = tmdbId;
		this.ignorePrefixes = ignorePrefixes;
		this.ignoreNfo = ignoreNfo;

		// getTitle()
		if (TITLE == null || TITLE.isEmpty()) {
			String temp = FILEPATH.contains("<MiZ>") ? FILEPATH.split("<MiZ>")[0] : FILEPATH;
			File fileName = new File(temp);
			int pointPosition=fileName.getName().lastIndexOf(".");
			TITLE = pointPosition == -1 ? fileName.getName() : fileName.getName().substring(0, pointPosition);
		} else {
			if (ignorePrefixes) {
				String temp = TITLE.toLowerCase(Locale.ENGLISH);
				String[] prefixes = MizLib.getPrefixes(CONTEXT);
				int count = prefixes.length;
				for (int i = 0; i < count; i++) {
					if (temp.startsWith(prefixes[i])) {
						TITLE = TITLE.substring(prefixes[i].length());
						break;
					}
				}
			}
		}
		
		// getThumbnail()
		mGetThumbnail = MizuuApplication.getMovieThumbFolderPath(CONTEXT) + "/" + TMDB_ID + ".jpg";
	}

	public String getRowId() {
		return ROW_ID;
	}

	public String getTitle() {
		return TITLE;
	}

	/**
	 * This is only used for the SectionIndexer in the overview
	 */
	@Override
	public String toString() {
		try {
			return getTitle().substring(0, 1);
		} catch (Exception e) {
			return "";
		}
	}

	public String getThumbnail() {
		if (!ignoreNfo) {
			try {
				// Check if there's a custom cover art image
				String filename = FILEPATH.substring(0, FILEPATH.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
				File parentFolder = new File(FILEPATH).getParentFile();

				if (parentFolder != null) {
					File[] list = parentFolder.listFiles();

					if (list != null) {
						String name, absolutePath;
						int count = list.length;
						for (int i = 0; i < count; i++) {
							name = list[i].getName();
							absolutePath = list[i].getAbsolutePath();
							if (name.equalsIgnoreCase("poster.jpg") ||
									name.equalsIgnoreCase("poster.jpeg") ||
									name.equalsIgnoreCase("poster.tbn") ||
									name.equalsIgnoreCase("folder.jpg") ||
									name.equalsIgnoreCase("folder.jpeg") ||
									name.equalsIgnoreCase("folder.tbn") ||
									name.equalsIgnoreCase("cover.jpg") ||
									name.equalsIgnoreCase("cover.jpeg") ||
									name.equalsIgnoreCase("cover.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.tbn") ||
									absolutePath.equalsIgnoreCase(filename + ".jpg") ||
									absolutePath.equalsIgnoreCase(filename + ".jpeg") ||
									absolutePath.equalsIgnoreCase(filename + ".tbn")) {
								return absolutePath;
							}
						}
					}
				}
			} catch (Exception e) {}
		}

		// New naming style
		return mGetThumbnail;
	}

	public String getBackdrop() {
		if (!ignoreNfo) {
			try {
				// Check if there's a custom cover art image
				String filename = FILEPATH.substring(0, FILEPATH.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
				File parentFolder = new File(FILEPATH).getParentFile();

				if (parentFolder != null) {
					File[] list = parentFolder.listFiles();

					if (list != null) {
						String name, absolutePath;
						int count = list.length;
						for (int i = 0; i < count; i++) {
							name = list[i].getName();
							absolutePath = list[i].getAbsolutePath();
							if (name.equalsIgnoreCase("fanart.jpg") ||
									name.equalsIgnoreCase("fanart.jpeg") ||
									name.equalsIgnoreCase("fanart.tbn") ||
									name.equalsIgnoreCase("banner.jpg") ||
									name.equalsIgnoreCase("banner.jpeg") ||
									name.equalsIgnoreCase("banner.tbn") ||
									name.equalsIgnoreCase("backdrop.jpg") ||
									name.equalsIgnoreCase("backdrop.jpeg") ||
									name.equalsIgnoreCase("backdrop.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.tbn")) {
								return absolutePath;
							}
						}
					}
				}
			} catch (Exception e) {}
		}

		// New naming style
		return new File(MizLib.getMovieBackdropFolder(CONTEXT), TMDB_ID + "_bg.jpg").getAbsolutePath();
	}

	@Override
	public int compareTo(BaseMovie another) {
		return getTitle().compareToIgnoreCase(another.getTitle());
	}

}