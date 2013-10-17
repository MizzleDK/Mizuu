package com.miz.functions;

public class Actor {
	String name, character, id, url;

	public Actor(String name, String character, String id, String url) {
		this.name = name;
		this.character = character;
		this.id = id;
		this.url = url;
	}

	public String getName() { return name; }
	public String getId() { return id; }
	public String getUrl() { return url; }
	public String getCharacter() {
		String characters = character.replace("|", ", ");
		if (characters.endsWith(", "))
			return characters.substring(0, characters.length() - 2);
		return characters;
	}
}