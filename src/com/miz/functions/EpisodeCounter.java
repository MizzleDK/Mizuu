package com.miz.functions;

public class EpisodeCounter {

	private int mEpisodeCount, mWatchedCount;
	
	public EpisodeCounter() {}
	
	public void incrementEpisodeCount() {
		mEpisodeCount++;
	}
	
	public int getEpisodeCount() {
		return mEpisodeCount;
	}
	
	public void incrementWatchedCount() {
		mWatchedCount++;
	}
	
	public int getWatchedCount() {
		return mWatchedCount;
	}
}
