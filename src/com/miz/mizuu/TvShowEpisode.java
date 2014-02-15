package com.miz.mizuu;

import android.content.Context;

import java.io.File;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;

public class TvShowEpisode {

	private Context CONTEXT;
	private String ROW_ID, FILEPATH, SHOW_ID, TITLE, DESCRIPTION, SEASON, EPISODE, RELEASEDATE, DIRECTOR, WRITER, GUESTSTARS, RATING, HAS_WATCHED, FAVORITE;

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
	}

	public String getRowId() {
		return ROW_ID;
	}
	
	public String getShowId() {
		return SHOW_ID;
	}

	public String getFilepath() {
		return MizLib.transformSmbPath(FILEPATH);
	}

	public String getFullFilepath() {
		return FILEPATH;
	}
	
	public String getTitle() {
		if (TITLE == null || TITLE.isEmpty()) {
			File fileName = new File(FILEPATH);
			return fileName.getName().substring(0, fileName.getName().lastIndexOf("."));
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

	public String getEpisodePhoto() {
		return new File(MizLib.getTvShowEpisodeFolder(CONTEXT), SHOW_ID +  "_S" + SEASON + "E" + EPISODE + ".jpg").getAbsolutePath();
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
		return new File(MizLib.getTvShowThumbFolder(CONTEXT), SHOW_ID + ".jpg").getAbsolutePath();
	}
}