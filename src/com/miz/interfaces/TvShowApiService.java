package com.miz.interfaces;

import java.util.List;

import com.miz.apis.thetvdb.TvShow;

public interface TvShowApiService extends ApiService<TvShow> {

	/**
	 * Get a {@link List} of URL's to cover images.
	 * @param id TV show ID.
	 * @return {@link List} containing cover image paths
	 */
	public List<String> getCovers(String id);
	
	/**
	 * Get a {@link List} of URL's to backdrop images.
	 * @param id TV show ID.
	 * @return {@link List} containing backdrop image paths
	 */
	public List<String> getBackdrops(String id);
	
}
