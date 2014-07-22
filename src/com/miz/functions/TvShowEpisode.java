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
