package com.miz.functions;
public class GridSeason implements Comparable<GridSeason> {
	
	private String mPoster;
	private int mSeason, mEpisodeCount;
	
	public GridSeason(int season, int episodeCount, String poster) {
		mSeason = season;
		mEpisodeCount = episodeCount;
		mPoster = poster;
	}

	public int getSeason() {
		return mSeason;
	}
	
	public String getSeasonZeroIndex() {
		return MizLib.addIndexZero(mSeason);
	}
	
	public int getEpisodeCount() {
		return mEpisodeCount;
	}

	public String getPoster() {
		return mPoster;
	}

	@Override
	public int compareTo(GridSeason another) {
		if (getSeason() < another.getSeason())
			return -1;
		if (getSeason() > another.getSeason())
			return 1;
		return 0;
	}
}