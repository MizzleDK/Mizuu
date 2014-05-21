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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.db.DbAdapter;
import com.miz.mizuu.MizuuApplication;

public class NfoMovie {

	private MovieLibraryUpdateCallback callback;
	private TMDbMovie movie;
	private String filepath;
	private Context c;
	private InputStream is;

	public NfoMovie(String file, InputStream is, Context c, MovieLibraryUpdateCallback callback) {
		this.filepath = file;
		this.is = is;
		this.c = c;
		this.callback = callback;

		readFile();
	}

	private void readFile() {
		movie = new TMDbMovie();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document doc;

			doc = db.parse(is);
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("movie");
			if (nodeList.getLength() > 0) {
				if (nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {

					Element firstElement = (Element) nodeList.item(0);
					NodeList list;
					Element element;
					NodeList tag;

					try {
						list = firstElement.getElementsByTagName("title");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setTitle(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("originaltitle");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setOriginalTitle(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setOriginalTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("set");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setCollectionTitle(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setCollectionTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setRating(((Node) tag.item(0)).getNodeValue().trim().replace(",", "."));
					} catch(Exception e) {
						movie.setRating("0.0");
					}

					if (firstElement.getElementsByTagName("premiered").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("premiered");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setReleasedate(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setReleasedate("");
						}
					} else if (firstElement.getElementsByTagName("releasedate").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("releasedate");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setReleasedate(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setReleasedate("");
						}
					} else {
						try {
							list = firstElement.getElementsByTagName("year");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setReleasedate(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setReleasedate("");
						}
					}

					if (movie.getReleasedate().isEmpty())
						try {
							list = firstElement.getElementsByTagName("year");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setReleasedate(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setReleasedate("");
						}

					if (firstElement.getElementsByTagName("outline").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("outline");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setTagline(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setTagline("");
						}
					} else {
						try {
							list = firstElement.getElementsByTagName("tagline");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setTagline(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setTagline("");
						}
					}

					try {
						list = firstElement.getElementsByTagName("plot");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setPlot(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setPlot("");
					}

					try {
						list = firstElement.getElementsByTagName("runtime");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setRuntime(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setRuntime("0");
					}

					try {
						list = firstElement.getElementsByTagName("thumb");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setCover(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setCover("");
					}

					if (firstElement.getElementsByTagName("mpaa").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("mpaa");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setCertification(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setCertification("");
						}
					} else {					
						try {
							list = firstElement.getElementsByTagName("certification");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							movie.setCertification(((Node) tag.item(0)).getNodeValue().trim());
						} catch(Exception e) {
							movie.setCertification("");
						}
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setImdbId(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setImdbId("");
					}

					try {
						list = firstElement.getElementsByTagName("tmdbid");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setId(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setId("invalid");
					}

					try {
						list = firstElement.getElementsByTagName("trailer");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						movie.setTrailer(((Node) tag.item(0)).getNodeValue().trim());
					} catch(Exception e) {
						movie.setTrailer("");
					}

					try {
						list = firstElement.getElementsByTagName("genre");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						try {
							String genres = ((Node) tag.item(0)).getNodeValue().trim();
							String[] genresArray = genres.split(",");
							StringBuilder sb = new StringBuilder();
							for (int k = 0; k < genresArray.length; k++) {
								sb.append(MizLib.toCapitalWords(genresArray[k].toLowerCase(Locale.getDefault())) + ", ");
							}
							genres = sb.substring(0, sb.length() - 2).toString();
							movie.setGenres(genres);
						} catch (Exception e) {
							movie.setGenres(((Node) tag.item(0)).getNodeValue().trim());
						}
					} catch(Exception e) {
						movie.setGenres("");
					}

					NodeList actorsList = firstElement.getElementsByTagName("actor");
					if (actorsList.getLength() > 0) {
						StringBuilder sbActor = new StringBuilder();

						for (int i = 0; i < actorsList.getLength(); i++) {
							if (actorsList.item(i).getNodeType() == Node.ELEMENT_NODE) {
								Element firstActorElement = (Element) actorsList.item(i);
								NodeList actorList;
								Element actorElement;
								NodeList actorTag;

								try {
									actorList = firstActorElement.getElementsByTagName("name");
									actorElement = (Element) actorList.item(0);
									actorTag = actorElement.getChildNodes();
									sbActor.append(((Node) actorTag.item(0)).getNodeValue().trim() + "|");
								} catch(Exception e) {}

							}
						}

						String tempCast = sbActor.toString();
						movie.setCast(tempCast.substring(0, tempCast.length() - 1));
					}
				}
			}
		} catch (Exception ignored) {
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException ignored) {}
		}

		addToDatabase();
	}

	private void addToDatabase() {
		// Create and open database
		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();
		long rowId = dbHelper.createMovie(filepath, movie.getCover(), movie.getTitle(), movie.getPlot(), movie.getId(), movie.getImdbId(), movie.getRating(), movie.getTagline(), movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0", movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

		Movie temp = new Movie(c,
				String.valueOf(rowId),
				filepath,
				movie.getTitle(),
				movie.getPlot(),
				movie.getTagline(),
				movie.getId(),
				movie.getImdbId(),
				movie.getRating(),
				movie.getReleasedate(),
				movie.getCertification(),
				movie.getRuntime(),
				movie.getTrailer(),
				movie.getGenres(),
				"0",
				movie.getCast(),
				movie.getCollectionTitle(),
				movie.getCollectionId(),
				"0",
				"0",
				movie.getCover(),
				String.valueOf(System.currentTimeMillis()),
				false,
				false
				);

		if (callback != null)
			callback.onMovieAdded(movie.getTitle(), temp.getThumbnail(), temp.getBackdrop());

		Intent intent = new Intent("mizuu-movies-object");
		intent.putExtra("movieName", movie.getTitle());
		intent.putExtra("cover", temp.getPoster());
		intent.putExtra("thumbFile", temp.getThumbnail());
		intent.putExtra("backdrop", temp.getBackdrop());

		LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
		LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent("mizuu-movies-update"));
	}
}