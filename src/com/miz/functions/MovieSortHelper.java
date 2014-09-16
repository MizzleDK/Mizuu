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

import static com.miz.functions.SortingKeys.DATE;
import static com.miz.functions.SortingKeys.DURATION;
import static com.miz.functions.SortingKeys.RATING;
import static com.miz.functions.SortingKeys.RELEASE;
import static com.miz.functions.SortingKeys.TITLE;
import static com.miz.functions.SortingKeys.WEIGHTED_RATING;
import android.text.TextUtils;

public class MovieSortHelper implements Comparable<MovieSortHelper> {

	private MediumMovie mMovie;
	private int mIndex, mSortType;
	private boolean mCollectionsView;

	public MovieSortHelper(MediumMovie movie, int index, int sortType, boolean collectionsView) {
		mMovie = movie;
		mIndex = index;
		mSortType = sortType;
		mCollectionsView = collectionsView;
	}

	public int getIndex() {
		return mIndex;
	}

	@Override
	public int compareTo(MovieSortHelper another) {
		switch (mSortType) {	
		case TITLE:
			if (mMovie != null && another.mMovie != null) {
				if (mCollectionsView)
					return mMovie.getCollection().compareToIgnoreCase(another.mMovie.getCollection());
				return mMovie.getTitle().compareToIgnoreCase(another.mMovie.getTitle());
			}
			return 0;
		case RELEASE:
			// Dates are always presented as YYYY-MM-DD, so removing
			// the hyphens will easily provide a great way of sorting.

			try {
				int firstDate = 0, secondDate = 0;
				String first = "", second = "";

				if (mMovie.getReleasedate() != null)
					first = mMovie.getReleasedate().replace("-", "").replace(".", "").replace("/", ""); // TODO FIX THIS. It shouldn't be possible to have dates with '/' or '.'

				if (!TextUtils.isEmpty(first))
					firstDate = Integer.valueOf(first);

				if (another.mMovie.getReleasedate() != null)
					second = another.mMovie.getReleasedate().replace("-", "").replace("/", "");

				if (!TextUtils.isEmpty(second))
					secondDate = Integer.valueOf(second);

				// This part is reversed to get the highest numbers first
				if (firstDate < secondDate)
					return 1; // First date is lower than second date - put it second
				else if (firstDate > secondDate)
					return -1; // First date is greater than second date - put it first
			} catch (Exception e) {}

			return 0; // They're equal
		case RATING:
			if (mMovie.getRawRating() < another.mMovie.getRawRating())
				return 1;
			else if (mMovie.getRawRating() > another.mMovie.getRawRating())
				return -1;

			return 0;
		case WEIGHTED_RATING:
			if (mMovie.getWeightedRating() < another.mMovie.getWeightedRating())
				return 1;
			else if (mMovie.getWeightedRating() > another.mMovie.getWeightedRating())
				return -1;
			return 0;
		case DATE:
			return mMovie.getDateAdded().compareTo(another.mMovie.getDateAdded()) * -1;
		case DURATION:
			int first = Integer.valueOf(mMovie.getRuntime());
			int second = Integer.valueOf(another.mMovie.getRuntime());
			if (first < second)
				return 1;
			else if (first > second)
				return -1;
			return 0;
		default:
			return 0;
		}
	}
}