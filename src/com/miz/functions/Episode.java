package com.miz.functions;

public class Episode {
	
	private String season = "", episode = "", title = "", airdate = "", description = "",
	screenshot_url = "", rating = "", director = "", writer = "", gueststars = "";
	
	public Episode() {}

	public String getSeason() {
		return season;
	}

	public void setSeason(String season) {
		this.season = season;
	}

	public String getEpisode() {
		return episode;
	}

	public void setEpisode(String episode) {
		this.episode = episode;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAirdate() {
		return airdate;
	}

	public void setAirdate(String airdate) {
		this.airdate = airdate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getScreenshot_url() {
		return screenshot_url;
	}

	public void setScreenshot_url(String screenshot_url) {
		this.screenshot_url = screenshot_url;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public String getDirector() {
		return director;
	}

	public void setDirector(String director) {
		this.director = director;
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String writer) {
		this.writer = writer;
	}

	public String getGueststars() {
		return gueststars;
	}

	public void setGueststars(String gueststars) {
		this.gueststars = gueststars;
	}
}