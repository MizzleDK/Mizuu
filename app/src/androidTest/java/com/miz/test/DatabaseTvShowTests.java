package com.miz.test;/*
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

import android.content.Context;
import android.test.InstrumentationTestCase;

/**
 * Tests various TV show database queries.
 * Do not use this on a non-debug build, as
 * it will remove all data.
 */
public class DatabaseTvShowTests extends InstrumentationTestCase {

    private Context mContext;



    /*
    TV shows:

- create show
- show exists
  - TVDb ID
  - TMDb ID
  - Title
- get show ID by show title
- get show by show ID
- get all shows
- delete show by ID
- delete all shows
- get certifications
- edit show


TV show episodes:

- create episode
- update episode
- get episode
- get all episodes by show ID
- get all episodes
- delete episode
- delete all episodes by show ID
- remove season
- delete all episodes
- get episode count by show ID
- get episode count for season
- get season count
- get seasons
- get episodes in season
- get latest episode airdate by show ID
- has unwatched episodes
- set season watched status
- set episode watched status


TV show episode mapping:

- create filepath mapping
- filepath exists
- get first filepath
- get filepaths for episode
- get all unidentified filepaths
- get all filepaths
- get all info for filepath
- get all filepaths by show ID
- get all ignored filepaths
- delete filepath by filepath
- ignore filepath
- delete all filepaths by show ID
- delete all filepaths
- delete all unidentified filepaths
- has multiple filepaths
- remove season
- ignore season
     */

}