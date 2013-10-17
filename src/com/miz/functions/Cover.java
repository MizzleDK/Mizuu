package com.miz.functions;

import java.util.Locale;


public class Cover {
	private String url, language;

	public Cover(String url, String language) {
		this.url = url;
		this.language = language;
	}

	public String getUrl() {
		return url;
	}

	public String getLanguage() {
		return MizLib.toCapitalFirstChar(new Locale(language).getDisplayLanguage());
	}
}