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

public enum TvShowLibraryType {

        ALL_SHOWS(TvShowLoader.ALL_SHOWS),
        FAVORITES(TvShowLoader.FAVORITES),
        RECENTLY_AIRED(TvShowLoader.RECENTLY_AIRED),
        WATCHED(TvShowLoader.WATCHED),
        UNWATCHED(TvShowLoader.UNWATCHED);

        private final int mType;

        TvShowLibraryType(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }

        public static TvShowLibraryType fromInt(int type) {
            switch (type) {
                case TvShowLoader.ALL_SHOWS:
                    return ALL_SHOWS;
                case TvShowLoader.FAVORITES:
                    return FAVORITES;
                case TvShowLoader.RECENTLY_AIRED:
                    return RECENTLY_AIRED;
                case TvShowLoader.WATCHED:
                    return WATCHED;
                case TvShowLoader.UNWATCHED:
                    return UNWATCHED;
                default:
                    return ALL_SHOWS;
            }
        }
    }