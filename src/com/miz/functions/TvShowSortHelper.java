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

import static com.miz.functions.SortingKeys.NEWEST_EPISODE;
import static com.miz.functions.SortingKeys.DURATION;
import static com.miz.functions.SortingKeys.RATING;
import static com.miz.functions.SortingKeys.RELEASE;
import static com.miz.functions.SortingKeys.TITLE;
import static com.miz.functions.SortingKeys.WEIGHTED_RATING;

import com.miz.mizuu.TvShow;

public class TvShowSortHelper implements Comparable<TvShowSortHelper> {

	private TvShow mTvShow;
	private int mIndex, mSortType;

	public TvShowSortHelper(TvShow tvShow, int index, int sortType) {
		mTvShow = tvShow;
		mIndex = index;
		mSortType = sortType;
	}

	public int getIndex() {
		return mIndex;
	}

	@Override
	public int compareTo(TvShowSortHelper another) {
		switch (mSortType) {	
		case TITLE:
			if (mTvShow != null && another.mTvShow != null) {
				String s1 = mTvShow.getTitle(), s2 = another.mTvShow.getTitle();
				if (s1 != null && s2 != null)
					return s1.compareToIgnoreCase(s2);
				return 0;
			}
			return 0;

		case RELEASE:
			try {
				return mTvShow.getFirstAirdate().compareTo(another.mTvShow.getFirstAirdate()) * -1;
			} catch (Exception e) {
				return 0;
			}

		case RATING:
			try {
				if (mTvShow.getRawRating() < another.mTvShow.getRawRating())
					return 1;
				else if (mTvShow.getRawRating() > another.mTvShow.getRawRating())
					return -1;
			} catch (Exception e) {}
			return 0;

		case WEIGHTED_RATING:
			try {
				if (mTvShow.getWeightedRating() < another.mTvShow.getWeightedRating()) {
					return 1;
				} else if (mTvShow.getWeightedRating() > another.mTvShow.getWeightedRating())
					return -1;
			} catch (Exception e) {}
			return 0;

		case NEWEST_EPISODE:
			return mTvShow.getLatestEpisodeAirdate().compareToIgnoreCase(another.mTvShow.getLatestEpisodeAirdate()) * -1;
		case DURATION:
			int show1 = Integer.valueOf(mTvShow.getRuntime());
			int show2 = Integer.valueOf(another.mTvShow.getRuntime());

			if (show1 < show2)
				return 1;
			else if (show1 > show2)
				return -1;

			return 0;

		default:
			return 0;
		}
	}
}