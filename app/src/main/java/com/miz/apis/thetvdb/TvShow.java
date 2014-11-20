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

package com.miz.apis.thetvdb;

import com.miz.db.DbAdapterTvShows;

import java.util.ArrayList;

public class TvShow {

	private String mShowId = DbAdapterTvShows.UNIDENTIFIED_ID,
			mTitle = "",
			mOriginalTitle = "",
			mDescription = "",
			mActors = "",
			mGenres = "",
			mRating = "",
			mCoverUrl = "",
			mBackdropUrl = "",
			mCertification = "",
			mRuntime = "",
			mFirstAired = "",
			mImdbId = "";
	private ArrayList<Episode> mEpisodes = new ArrayList<Episode>();
	private ArrayList<Season> mSeasons = new ArrayList<Season>();

	public TvShow() {}

	public String getId() {
		return mShowId;
	}

	public void setId(String id) {
		mShowId = id;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}
	
	public String getOriginalTitle() {
		return mOriginalTitle;
	}
	
	public void setOriginalTitle(String originalTitle) {
		mOriginalTitle = originalTitle;
	}

	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	public String getActors() {
		return mActors;
	}

	public void setActors(String actors) {
		mActors = actors;
	}

	public String getGenres() {
		return mGenres;
	}

	public void setGenres(String genres) {
		mGenres = genres;
	}

	public String getRating() {
		return mRating;
	}

	public void setRating(String rating) {
		mRating = rating;
	}

	public String getCoverUrl() {
		return mCoverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		mCoverUrl = coverUrl;
	}

	public String getBackdropUrl() {
		return mBackdropUrl;
	}

	public void setBackdropUrl(String backdropUrl) {
		mBackdropUrl = backdropUrl;
	}

	public String getCertification() {
		return mCertification;
	}

	public void setCertification(String certification) {
		mCertification = certification;
	}

	public String getRuntime() {
		return mRuntime;
	}

	public void setRuntime(String runtime) {
		mRuntime = runtime;
	}

	public String getFirstAired() {
		return mFirstAired;
	}

	public void setFirstAired(String firstAired) {
		mFirstAired = firstAired;
	}
	
	public void addEpisode(Episode ep) {
		mEpisodes.add(ep);
	}
	
	public ArrayList<Episode> getEpisodes() {
		return mEpisodes;
	}
	
	public void setIMDbId(String id) {
		mImdbId = id;
	}
	
	public String getImdbId() {
		return mImdbId;
	}
	
	public void addSeason(Season s) {
		mSeasons.add(s);
	}
	
	public ArrayList<Season> getSeasons() {
		return mSeasons;
	}
	
	public boolean hasSeason(int season) {
		for (Season s : mSeasons)
			if (s.getSeason() == season)
				return true;
		return false;
	}
	
	public Season getSeason(int season) {
		for (Season s : mSeasons)
			if (s.getSeason() == season)
				return s;
		return new Season();
	}
}