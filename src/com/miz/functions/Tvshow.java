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

import java.util.ArrayList;

public class Tvshow {

	private String id = "invalid", title = "", description = "", actors = "", genre = "", rating = "",
	cover_url = "", backdrop_url = "", certification = "", runtime = "", first_aired = "", imdb_id = "";
	private ArrayList<Episode> episodes = new ArrayList<Episode>();
	private ArrayList<Season> mSeasons = new ArrayList<Season>();

	public Tvshow() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActors() {
		return actors;
	}

	public void setActors(String actors) {
		this.actors = actors;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public String getCover_url() {
		return cover_url;
	}

	public void setCover_url(String cover_url) {
		this.cover_url = cover_url;
	}

	public String getBackdrop_url() {
		return backdrop_url;
	}

	public void setBackdrop_url(String backdrop_url) {
		this.backdrop_url = backdrop_url;
	}

	public String getCertification() {
		return certification;
	}

	public void setCertification(String certification) {
		this.certification = certification;
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	public String getFirst_aired() {
		return first_aired;
	}

	public void setFirst_aired(String first_aired) {
		this.first_aired = first_aired;
	}
	
	public void addEpisode(Episode ep) {
		episodes.add(ep);
	}
	
	public ArrayList<Episode> getEpisodes() {
		return episodes;
	}
	
	public void setIMDbId(String id) {
		imdb_id = id;
	}
	
	public String getImdbId() {
		return imdb_id;
	}
	
	public void addSeason(Season s) {
		mSeasons.add(s);
	}
	
	public ArrayList<Season> getSeasons() {
		return mSeasons;
	}
	
	public boolean hasSeason(int season) {
		for (Season s : mSeasons)
			if (s.getSeason() == season)
				return true;
		return false;
	}
	
	public Season getSeason(int season) {
		for (Season s : mSeasons)
			if (s.getSeason() == season)
				return s;
		return new Season();
	}
}