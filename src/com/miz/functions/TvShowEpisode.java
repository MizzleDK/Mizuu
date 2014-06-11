package com.miz.functions;

public class TvShowEpisode {

	private String mShowId;
	private int mEpisode, mSeason;
	
	public TvShowEpisode(String showId, int episode, int season) {
		mShowId = showId;
		mEpisode = episode;
		mSeason = season;
	}
	
	public String getShowId() {
		return mShowId;
	}
	
	public int getEpisode() {
		return mEpisode;
	}
	
	public int getSeason() {
		return mSeason;
	}
}
