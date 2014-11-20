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

public class Episode {
	
	private int mSeason = -1, mEpisode = -1;
	private String mTitle = "",
			mAirdate = "",
			mDescription = "",
			mScreenshotUrl = "",
			mRating = "",
			mDirector = "",
			mWriter = "",
			mGuestStars = "";
	
	public Episode() {}

	public int getSeason() {
		return mSeason;
	}

	public void setSeason(int season) {
		mSeason = season;
	}

	public int getEpisode() {
		return mEpisode;
	}

	public void setEpisode(int episode) {
		mEpisode = episode;
	}
	
	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}

	public String getAirdate() {
		return mAirdate;
	}

	public void setAirdate(String airdate) {
		mAirdate = airdate;
	}

	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	public String getScreenshotUrl() {
		return mScreenshotUrl;
	}

	public void setScreenshotUrl(String screenshotUrl) {
		mScreenshotUrl = screenshotUrl;
	}

	public String getRating() {
		return mRating;
	}

	public void setRating(String rating) {
		mRating = rating;
	}

	public String getDirector() {
		return mDirector;
	}

	public void setDirector(String director) {
		mDirector = director;
	}

	public String getWriter() {
		return mWriter;
	}

	public void setWriter(String writer) {
		mWriter = writer;
	}

	public String getGueststars() {
		return mGuestStars;
	}

	public void setGueststars(String gueststars) {
		mGuestStars = gueststars;
	}
}