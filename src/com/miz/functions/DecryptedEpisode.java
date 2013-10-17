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