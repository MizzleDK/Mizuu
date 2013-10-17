package com.miz.functions;

public class WebMovie {
	
	private String title, id, url, date;

	public WebMovie(String title, String id, String url) {
		this.title = title;
		this.id = id;
		this.url = url;
	}
	
	public WebMovie(String title, String id, String url, String date) {
		this.title = title;
		this.id = id;
		this.url = url;
		this.date = date;
	}

	public String getTitle() { return title; }
	public String getId() { return id; }
	public String getUrl() { return url; }
	public String getDate() { return date; }

}
