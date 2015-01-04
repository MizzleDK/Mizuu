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

package com.miz.utils;

import android.content.Context;
import android.os.Environment;

import com.miz.db.DatabaseHelper;
import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

	private FileUtils() {} // No instantiation!
	
	public static File getDatabaseFile(Context context) {
		return context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
	}
	
	public static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public static void moveFile(File src, File dst) throws IOException {
		copyFile(src, dst);
		src.delete();
	}

	public static void deleteRecursive(File fileOrDirectory, boolean deleteTopFolder) {
		if (fileOrDirectory.isDirectory()) {
			File[] listFiles = fileOrDirectory.listFiles();
			if (listFiles != null) {
				int count = listFiles.length;
				for (int i = 0; i < count; i++)
					deleteRecursive(listFiles[i], true);
			}
		}

		if (deleteTopFolder)
			fileOrDirectory.delete();
	}
	
	public static File getOldDataFolder() {
		return new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu");
	}

	public static boolean oldDataFolderExists() {
		return getOldDataFolder().exists() && getOldDataFolder().list() != null;
	}

	public static File getMovieThumb(Context c, String movieId) {
		return new File(MizuuApplication.getMovieThumbFolder(c), movieId + ".jpg");
	}

	public static File getMovieBackdrop(Context c, String movieId) {
		return new File(MizuuApplication.getMovieBackdropFolder(c), movieId + "_bg.jpg");
	}

	public static File getTvShowThumb(Context c, String showId) {
		return new File(MizuuApplication.getTvShowThumbFolder(c), showId + ".jpg");
	}

	public static File getTvShowBackdrop(Context c, String showId) {
		return new File(MizuuApplication.getTvShowBackdropFolder(c), showId + "_tvbg.jpg");
	}

	public static File getTvShowEpisode(Context c, String showId, String season, String episode) {
		return new File(MizuuApplication.getTvShowEpisodeFolder(c), showId + "_S" + MizLib.addIndexZero(season) + "E" + MizLib.addIndexZero(episode) + ".jpg");
	}

	public static File getTvShowEpisode(Context c, String showId, int season, int episode) {
		return getTvShowEpisode(c, showId, String.valueOf(season), String.valueOf(episode));
	}

	public static File getTvShowSeason(Context c, String showId, String season) {
		return new File(MizuuApplication.getTvShowSeasonFolder(c), showId + "_S" + MizLib.addIndexZero(season) + ".jpg");
	}

	public static File getTvShowSeason(Context c, String showId, int season) {
		return getTvShowSeason(c, showId, String.valueOf(season));
	}

	public static File getOfflineFile(Context c, String filepath) {
		return new File(MizuuApplication.getAvailableOfflineFolder(c), MizLib.md5(filepath) + "." + StringUtils.getExtension(filepath));
	}

    public static boolean hasOfflineCopy(Context c, Filepath path) {
        return getOfflineCopyFile(c, path).exists();
    }

    private static File getOfflineCopyFile(Context c, Filepath path) {
        return getOfflineFile(c, path.getFilepath());
    }

    public static String copyDatabase(Context context) {
        try {
            File newPath = new File(MizuuApplication.getAppFolder(context), "database.db");
            newPath.createNewFile();
            newPath.setReadable(true);
            FileUtils.copyFile(FileUtils.getDatabaseFile(context), newPath);

            return newPath.exists() ? newPath.getAbsolutePath() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
