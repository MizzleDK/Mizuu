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

package com.miz.identification;

public class Episode {

	private String mBefore, mAfter;
	private int mSeason, mEpisode;

	public Episode(int season, int episode, String before, String after) {
		mSeason = season;
		mEpisode = episode;
		mBefore = before;
		mAfter = after;
	}

	public void setSeason(int season) {
		mSeason = season;
	}

	public int getSeason() {
		return mSeason;
	}

	public int getEpisode() {
		return mEpisode;
	}

	/**
	 * Used primarily to look for show name and release year
	 * @return
	 */
	public String getBefore() {
		return mBefore;
	}

	/**
	 * Used primarily to look for release year
	 * @return
	 */
	public String getAfter() {
		return mAfter;
	}
}