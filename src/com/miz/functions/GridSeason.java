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