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

public class DecryptedEpisode {
	private String name, season, episode;
	
	public DecryptedEpisode(String name, String season, String episode) {
		setName(name);
		setSeason(season);
		setEpisode(episode);
	}
	
	public void setName(String name) {
		if (!MizLib.isEmpty(name)) {
			String[] split;
			String year = "";

			if (name.contains(" "))
				split = name.split(" ");
			else
				split = name.split("\\+");
			for (int i = split.length - 1; i > 0; i--) {
				if (split[i].matches("(19|20)[0-9][0-9]")) {
					year = split[i];
					continue;
				}
			}

			name = name.replace(year, "").trim();
		}
		
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setSeason(String season) {
		this.season = season;
	}
	
	public String getSeason() {
		return season;
	}
	
	public void setEpisode(String episode) {
		this.episode = episode;
	}
	
	public String getEpisode() {
		return episode;
	}
}