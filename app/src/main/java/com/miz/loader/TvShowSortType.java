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

package com.miz.loader;

import android.text.TextUtils;

import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.mizuu.TvShow;

import java.util.Comparator;

public enum TvShowSortType {

        TITLE(TvShowLoader.TITLE, true),
        FIRST_AIR_DATE(TvShowLoader.FIRST_AIR_DATE, false),
        NEWEST_EPISODE(TvShowLoader.NEWEST_EPISODE, false),
        DURATION(TvShowLoader.DURATION, false),
        RATING(TvShowLoader.RATING, false),
        WEIGHTED_RATING(TvShowLoader.WEIGHTED_RATING, false);

        private final int mType;
        private boolean mAscendingSort;

        TvShowSortType(int type, boolean ascendingSort) {
            mType = type;
            mAscendingSort = ascendingSort;
        }

        public void toggleSortOrder() {
            mAscendingSort = !mAscendingSort;
        }

        private boolean isAscendingSort() {
            return mAscendingSort;
        }

        public Comparator<TvShow> getComparator() {
            return new Comparator<TvShow>() {
                @Override
                public int compare(TvShow lhs, TvShow rhs) {

                    // Let's assume that they're equal to begin with
                    int result = 0;

                    switch (mType) {
                        case TvShowLoader.TITLE:
                            if (lhs != null && rhs != null)
                                result = lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                            break;

                        case TvShowLoader.FIRST_AIR_DATE:
                            if (lhs != null && rhs != null)
                                result = lhs.getFirstAirdate().compareToIgnoreCase(rhs.getFirstAirdate());
                            break;

                        case TvShowLoader.NEWEST_EPISODE:
                            if (lhs != null && rhs != null)
                                result = lhs.getLatestEpisodeAirdate().compareToIgnoreCase(rhs.getLatestEpisodeAirdate());
                            break;

                        case TvShowLoader.RATING:
                            if (lhs.getRawRating() < rhs.getRawRating())
                                result = -1;
                            else if (lhs.getRawRating() > rhs.getRawRating())
                                result = 1;
                            break;

                        case TvShowLoader.WEIGHTED_RATING:
                            if (lhs.getWeightedRating() < rhs.getWeightedRating())
                                result = -1;
                            else if (lhs.getWeightedRating() > rhs.getWeightedRating())
                                result = 1;
                            break;

                        case TvShowLoader.DURATION:
                            int first = MizLib.getInteger(lhs.getRuntime());
                            int second = MizLib.getInteger(rhs.getRuntime());
                            if (first < second)
                                result = -1;
                            else if (first > second)
                                result = 1;
                            break;
                    }

                    return isAscendingSort() ? result : result * -1;
                }
            };
        }
    }