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

/**
 * Simple class to hold keys for sorting movies and TV shows. This should
 * always be used when sorting. Scattered keys across the app? No thank you.
 * @author Michell
 *
 */
public class SortingKeys {
	
	private SortingKeys() {} // No instantiation
	
	public static final int ALL_MOVIES = 0;
	public static final int ALL_SHOWS = 0;
	public static final int FAVORITES = 1;
	public static final int AVAILABLE_FILES = 2;
	public static final int UNWATCHED_SHOWS = 2;
	public static final int COLLECTIONS = 3;
	public static final int WATCHED_MOVIES = 4;
	public static final int UNWATCHED_MOVIES = 5;
	public static final int UNIDENTIFIED_MOVIES = 6;
	public static final int OFFLINE_COPIES = 7;
	
	public static final int TITLE = 10;
	public static final int RELEASE = 11;
	public static final int RATING = 12;
	public static final int DATE = 13;
	public static final int NEWEST_EPISODE = 13;
	public static final int WEIGHTED_RATING = 14;
	public static final int DURATION = 15;
	
	public static final int GENRES = 20;
	public static final int CERTIFICATION = 21;
	public static final int FILE_SOURCES = 22;
	public static final int FOLDERS = 23;
	public static final int RELEASE_YEAR = 24;
}