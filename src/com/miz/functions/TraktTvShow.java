package com.miz.functions;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class TraktTvShow {

	private String mId, mTitle;
	private Multimap<String, String> mSeasonEpisodesMapping = LinkedListMultimap.create();
	
	public TraktTvShow(String id, String title) {
		mId = id;
		mTitle = title;
	}
	
	public void addEpisode(String season, String episode) {
		mSeasonEpisodesMapping.put(season, episode);
	}
	
	public Multimap<String, String> getSeasons() {
		return mSeasonEpisodesMapping;
	}
	
	public boolean contains(String season, String episode) {
		return getSeasons().get(season).contains(episode);
	}
	
	public String getId() {
		return mId;
	}
	
	public String getTitle() {
		return mTitle;
	}
}