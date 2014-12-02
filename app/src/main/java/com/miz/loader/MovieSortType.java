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

import java.util.Comparator;

public enum MovieSortType {

        TITLE(MovieLoader.TITLE, true),
        RELEASE(MovieLoader.RELEASE, false),
        DURATION(MovieLoader.DURATION, false),
        RATING(MovieLoader.RATING, false),
        WEIGHTED_RATING(MovieLoader.WEIGHTED_RATING, false),
        DATE_ADDED(MovieLoader.DATE_ADDED, false),
        COLLECTION_TITLE(MovieLoader.COLLECTION_TITLE, true);

        private final int mType;
        private boolean mAscendingSort;

        MovieSortType(int type, boolean ascendingSort) {
            mType = type;
            mAscendingSort = ascendingSort;
        }

        public void toggleSortOrder() {
            mAscendingSort = !mAscendingSort;
        }

        private boolean isAscendingSort() {
            return mAscendingSort;
        }

        public Comparator<MediumMovie> getComparator() {
            return new Comparator<MediumMovie>() {
                @Override
                public int compare(MediumMovie lhs, MediumMovie rhs) {

                    // Let's assume that they're equal to begin with
                    int result = 0;

                    switch (mType) {
                        case MovieLoader.TITLE:
                            if (lhs != null && rhs != null)
                                result = lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                            break;

                        case MovieLoader.COLLECTION_TITLE:
                            if (lhs != null && rhs != null)
                                result = lhs.getCollection().compareToIgnoreCase(rhs.getCollection());
                            break;

                        case MovieLoader.RELEASE:
                            try {
                                int firstDate = 0, secondDate = 0;
                                String first = "", second = "";

                                if (lhs.getReleasedate() != null)
                                    first = lhs.getReleasedate().replace("-", "").replace(".", "").replace("/", ""); // TODO FIX THIS. It shouldn't be possible to have dates with '/' or '.'

                                if (!TextUtils.isEmpty(first))
                                    firstDate = Integer.valueOf(first);

                                if (rhs.getReleasedate() != null)
                                    second = rhs.getReleasedate().replace("-", "").replace("/", "");

                                if (!TextUtils.isEmpty(second))
                                    secondDate = Integer.valueOf(second);

                                if (firstDate < secondDate)
                                    result = -1;
                                else if (firstDate > secondDate)
                                    result = 1;
                            } catch (Exception e) {}
                            break;

                        case MovieLoader.RATING:
                            if (lhs.getRawRating() < rhs.getRawRating())
                                result = -1;
                            else if (lhs.getRawRating() > rhs.getRawRating())
                                result = 1;
                            break;

                        case MovieLoader.WEIGHTED_RATING:
                            if (lhs.getWeightedRating() < rhs.getWeightedRating())
                                result = -1;
                            else if (lhs.getWeightedRating() > rhs.getWeightedRating())
                                result = 1;
                            break;

                        case MovieLoader.DATE_ADDED:
                            result = lhs.getDateAdded().compareTo(rhs.getDateAdded());
                            break;

                        case MovieLoader.DURATION:
                            int first = Integer.valueOf(lhs.getRuntime());
                            int second = Integer.valueOf(rhs.getRuntime());
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