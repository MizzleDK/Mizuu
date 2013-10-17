package com.miz.mizuu;

import android.content.Context;

import java.io.File;
import java.util.Locale;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;

public class TvShow implements Comparable<TvShow> {

	private Context CONTEXT;
	private String ID, TITLE, DESCRIPTION, RATING, GENRES, ACTORS, CERTIFICATION, FIRST_AIR_DATE, RUNTIME;
	private boolean ignorePrefixes, isFavorite;

	public TvShow(Context context, String id, String title, String description, String rating, String genres, String actors, String certification, String firstAirdate, String runtime, boolean ignorePrefixes, String isFavorite) {

		// Set up episode fields based on constructor
		CONTEXT = context;
		ID = id;
		TITLE = title;
		DESCRIPTION = description;
		RATING = rating;
		GENRES = genres;
		ACTORS = actors;
		CERTIFICATION = certification;
		FIRST_AIR_DATE = firstAirdate;
		RUNTIME = runtime;
		this.ignorePrefixes = ignorePrefixes;
		this.isFavorite = !(isFavorite.equals("0") || isFavorite.isEmpty());
	}

	public String getTitle() {
		if (MizLib.isEmpty(TITLE)) {
			return "";
		} else {
			if (ignorePrefixes) {
				String temp = TITLE.toLowerCase(Locale.ENGLISH);
				String[] prefixes = MizLib.getPrefixes(CONTEXT);
				int count = prefixes.length;
				for (int i = 0; i < count; i++) {
					if (temp.startsWith(prefixes[i]))
						return TITLE.substring(prefixes[i].length());
				}
			}
			return TITLE;
		}
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

	public String getId() {
		if (ID == null || ID.isEmpty()) {
			return "NOSHOW";
		} else {
			return ID;
		}
	}

	public String getDescription() {
		if (DESCRIPTION == null || DESCRIPTION.isEmpty()) {
			return CONTEXT.getString(R.string.stringNoPlot);
		} else {
			return DESCRIPTION;
		}
	}

	public String getCoverPhoto() {
		return getThumbnail();
	}

	public String getThumbnail() {
		return new File(MizLib.getTvShowThumbFolder(CONTEXT), ID + ".jpg").getAbsolutePath();
	}

	public String getBackdrop() {
		return new File(MizLib.getTvShowBackdropFolder(CONTEXT), ID + "_tvbg.jpg").getAbsolutePath();
	}

	public String getRating() {
		return RATING + "/10";
	}

	public double getRawRating() {
		try {
			return Double.valueOf(RATING);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	public double getWeightedRating() {
		if (isFavorite())
			return (10 + getRawRating()) / 2;
		return (getRawRating() + 5) / 2;
	}

	public String getGenres() {
		return convertToCommas(GENRES);
	}

	public String getActors() {
		return convertToCommas(ACTORS);
	}

	private String convertToCommas(String input) {
		input = input.replace("|", ", ");
		if (input.startsWith(","))
			input = input.substring(1);
		input = input.trim();
		if (input.endsWith(","))
			input = input.substring(0, input.length() - 1);
		return input;
	}

	public String getCertification() {
		return CERTIFICATION;
	}

	public String getFirstAirdate() {
		return FIRST_AIR_DATE;
	}

	public String getFirstAirdateYear() {
		try {
			return FIRST_AIR_DATE.substring(0, 4);
		} catch (Exception e) {
			return "N/A";
		}
	}

	public String getRuntime() {
		return RUNTIME;
	}

	public boolean isFavorite() {
		return isFavorite;
	}

	public void setFavorite(boolean fav) {
		isFavorite = fav;
	}

	public String getFavorite() {
		return isFavorite ? "1" : "0";
	}

	@Override
	public int compareTo(TvShow another) {
		return getTitle().compareToIgnoreCase(another.getTitle());
	}
}