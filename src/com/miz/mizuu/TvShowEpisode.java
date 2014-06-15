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

package com.miz.mizuu;

import static com.miz.functions.PreferenceKeys.TVSHOWS_EPISODE_ORDER;

import android.content.Context;
import android.preference.PreferenceManager;

import java.io.File;

import com.miz.functions.MizLib;

public class TvShowEpisode implements Comparable<TvShowEpisode> {

	private Context CONTEXT;
	private String ROW_ID, FILEPATH, SHOW_ID, TITLE, DESCRIPTION, SEASON, EPISODE, RELEASEDATE, DIRECTOR, WRITER, GUESTSTARS, RATING, HAS_WATCHED, FAVORITE, mSubtitleText;
	private String mGetFilepath;

	public TvShowEpisode(Context context, String rowId, String showId, String filepath, String title, String description,
			String season, String episode, String releasedate, String director, String writer, String gueststars,
			String rating, String hasWatched, String favorite) {

		// Set up episode fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		SHOW_ID = showId;
		TITLE = title;
		DESCRIPTION = description;
		SEASON = season;
		EPISODE = episode;
		RELEASEDATE = releasedate;
		DIRECTOR = director;
		WRITER = writer;
		GUESTSTARS = gueststars;
		RATING = rating;
		HAS_WATCHED = hasWatched;
		FAVORITE = favorite;
		String temp = FILEPATH.contains("<MiZ>") ? FILEPATH.split("<MiZ>")[1] : FILEPATH;
		mGetFilepath = MizLib.transformSmbPath(temp);
		
		// Subtitle text
		StringBuilder sb = new StringBuilder();
		sb.append(CONTEXT.getString(R.string.showEpisode));
		sb.append(" ");
		sb.append(getEpisode());
		if (!hasWatched())
			sb.append(" " + CONTEXT.getString(R.string.unwatched));
		
		mSubtitleText = sb.toString();
	}

	public String getRowId() {
		return ROW_ID;
	}
	
	public String getShowId() {
		return SHOW_ID;
	}

	public String getFilepath() {
		return mGetFilepath;
	}

	public String getFullFilepath() {
		return FILEPATH;
	}
	
	public String getTitle() {
		if (TITLE == null || TITLE.isEmpty()) {
			String temp = FILEPATH.contains("<MiZ>") ? FILEPATH.split("<MiZ>")[0] : FILEPATH;
			File fileName = new File(temp);
			int pointPosition=fileName.getName().lastIndexOf(".");
			return pointPosition == -1 ? fileName.getName() : fileName.getName().substring(0, pointPosition);
		} else {
			return TITLE;
		}
	}

	public String getDescription() {
		if (DESCRIPTION == null || DESCRIPTION.isEmpty()) {
			return CONTEXT.getString(R.string.stringNoPlot);
		} else {
			return DESCRIPTION.trim();
		}
	}

	public String getSeason() {
		return SEASON;
	}

	public String getEpisode() {
		return EPISODE;
	}

	public File getEpisodePhoto() {
		return MizLib.getTvShowEpisode(CONTEXT, SHOW_ID, SEASON, EPISODE);
	}

	public String getReleasedate() {
		return RELEASEDATE;
	}

	public String getDirector() {
		DIRECTOR = DIRECTOR.replace("|", ", ");
		if (DIRECTOR.startsWith(", ")) {
			DIRECTOR = DIRECTOR.substring(2, DIRECTOR.length());
		}
		if (DIRECTOR.endsWith(", ")) {
			DIRECTOR = DIRECTOR.substring(0, DIRECTOR.length() - 2);
		}
		
		return DIRECTOR;
	}

	public String getWriter() {
		WRITER = WRITER.replace("|", ", ");
		if (WRITER.startsWith(", ")) {
			WRITER = WRITER.substring(2, WRITER.length());
		}
		if (WRITER.endsWith(", ")) {
			WRITER = WRITER.substring(0, WRITER.length() - 2);
		}
		
		return WRITER;
	}

	public String getGuestStars() {
		GUESTSTARS = GUESTSTARS.replace("|", ", ");
		if (GUESTSTARS.startsWith(", ")) {
			GUESTSTARS = GUESTSTARS.substring(2, GUESTSTARS.length());
		}
		if (GUESTSTARS.endsWith(", ")) {
			GUESTSTARS = GUESTSTARS.substring(0, GUESTSTARS.length() - 2);
		}
		
		return GUESTSTARS;
	}
	
	public String getRating() {
		if (MizLib.isEmpty(RATING))
			return "0/10";
		return RATING + "/10";
	}
	
	public double getRawRating() {
		try {
			return Double.valueOf(RATING);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}
	
	public boolean hasWatched() {
		return (HAS_WATCHED.isEmpty() || HAS_WATCHED.equals("0")) ? false : true;
	}
	
	public String getHasWatched() {
		return HAS_WATCHED;
	}
	
	public void setHasWatched(boolean hasWatched) {
		if (hasWatched)
			HAS_WATCHED = "1";
		else
			HAS_WATCHED = "0";
	}
	
	public boolean isFavorite() {
		return (FAVORITE.isEmpty() || FAVORITE.equals("0")) ? false : true;
	}
	
	public String getFavorite() {
		return FAVORITE;
	}
	
	public boolean hasOfflineCopy() {
		return getOfflineCopyFile().exists();
	}
	
	public String getOfflineCopyUri() {
		return getOfflineCopyFile().getAbsolutePath();
	}
	
	public File getOfflineCopyFile() {
		return new File(MizLib.getAvailableOfflineFolder(CONTEXT), MizLib.md5(getFullFilepath()) + "." + MizLib.getFileExtension(getFullFilepath()));
	}
	
	/**
	 * Returns the path for the TV show thumbnail
	 * @return
	 */
	public String getThumbnail() {
		return MizLib.getTvShowThumb(CONTEXT, SHOW_ID).getAbsolutePath();
	}
	
	/**
	 * Returns the path for the TV show thumbnail
	 * @return
	 */
	public File getTvShowBackdrop() {
		return MizLib.getTvShowBackdrop(CONTEXT, SHOW_ID);
	}
	
	public String getSubtitleText() {
		return mSubtitleText;
	}

	@Override
	public int compareTo(TvShowEpisode another) {
		String defaultOrder = CONTEXT.getString(R.string.oldestFirst);
		boolean oldestFirst = PreferenceManager.getDefaultSharedPreferences(CONTEXT).getString(TVSHOWS_EPISODE_ORDER, defaultOrder).equals(defaultOrder);
		int multiplier = oldestFirst ? 1 : -1;
		
		return getEpisode().compareToIgnoreCase(another.getEpisode()) * multiplier;
	}
}