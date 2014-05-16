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

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.preference.PreferenceManager;

import com.miz.mizuu.R;

public class TheTVDb {

	private String ratingsProvider;
	private Context c;

	public TheTVDb(Context c) {
		this.c = c;
		ratingsProvider = PreferenceManager.getDefaultSharedPreferences(c).getString("prefsShowsRatingsSource", c.getString(R.string.ratings_option_4));
	}

	public Tvshow searchForShow(DecryptedShowEpisode episode, String language) {
		Tvshow show = new Tvshow();

		checkSearchQuery(show, episode.getDecryptedFileName() + " " + episode.getFileNameYear());
		
		// Try to search again without year
		if (show.getId().equals("invalid")) {
			checkSearchQuery(show, episode.getDecryptedFileName());
		}

		// Try to search again with the parent folder name including year
		if (show.getId().equals("invalid")) {
			checkSearchQuery(show, episode.getDecryptedParentName() + " " + episode.getParentNameYear());
		}

		// Try to search again with the parent folder name without year
		if (show.getId().equals("invalid")) {
			checkSearchQuery(show, episode.getDecryptedParentName());
		}
		
		// Try to search on another site if TheTVDb failed
		if (show.getId().equals("invalid")) {
			try {
				// Connection set-up
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				URL url = new URL("http://services.tvrage.com/feeds/search.php?show=" + URLEncoder.encode(episode.getDecryptedFileName(), "utf-8"));

				URLConnection con = url.openConnection();
				con.setReadTimeout(60000);
				con.setConnectTimeout(60000);

				Document doc = db.parse(con.getInputStream());
				doc.getDocumentElement().normalize();

				// Check if there's an element with the "id" tag
				NodeList nodeList = doc.getElementsByTagName("name");

				if (nodeList.getLength() > 0) {
					if (nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
						show.setTitle(nodeList.item(0).getTextContent());
					}
				} else {
					show.setTitle("");
				}
			} catch (Exception e) {}

			// Attempt search on Wikipedia if previous search was unsuccessful
			if (show.getTitle().isEmpty()) {
				try {
					JSONObject j = MizLib.getJSONObject("http://en.wikipedia.org/w/api.php?format=json&action=query&titles=" + URLEncoder.encode(episode.getDecryptedFileName(), "utf-8") + "&redirects");
					JSONObject jObject = j.getJSONObject("query").getJSONObject("pages");
					show.setTitle(jObject.getJSONObject(jObject.names().get(0).toString()).getString("title"));
				} catch (Exception e) {
					show.setTitle("");
				}
			}

			try {
				// Connection set-up
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				URL url = new URL("http://thetvdb.com/api/GetSeries.php?seriesname=" + URLEncoder.encode(show.getTitle(), "utf-8"));

				URLConnection con = url.openConnection();
				con.setReadTimeout(60000);
				con.setConnectTimeout(60000);

				Document doc = db.parse(con.getInputStream());
				doc.getDocumentElement().normalize();

				// Check if there's an element with the "id" tag
				NodeList nodeList = doc.getElementsByTagName("id");

				if (nodeList.getLength() > 0) {
					if (nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
						show.setId(nodeList.item(0).getTextContent());
					}
				} else {
					show.setId("invalid");
				}
			} catch (Exception e) {}

			// Check if the show still hasn't been found
			if (show.getId().equals("invalid"))
				return show;
		}

		String id = show.getId();
		show = getShow(id, language);

		return show;
	}

	private void checkSearchQuery(Tvshow show, String query) {
		try {
			// Connection set-up
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/GetSeries.php?seriesname=" + URLEncoder.encode(query, "utf-8") + "&language=all");

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			// Check if there's an element with the "id" tag
			NodeList nodeList = doc.getElementsByTagName("id");

			if (nodeList.getLength() > 0) {
				if (nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
					show.setId(nodeList.item(0).getTextContent());
				}
			} else {
				show.setId("invalid");
			}
		} catch (Exception e) {}
	}

	public ArrayList<Tvshow> searchForShows(String query, String language) {
		ArrayList<Tvshow> results = new ArrayList<Tvshow>();

		try {
			// Connection set-up
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/GetSeries.php?seriesname=" + URLEncoder.encode(query, "utf-8") + "&language=" + language);

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			// Check if there's an element with the "id" tag
			NodeList nodeList = doc.getElementsByTagName("Series");

			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Tvshow show = new Tvshow();

					Element firstElement = (Element) nodeList.item(i);
					NodeList list;
					Element element;
					NodeList tag;

					try {
						list = firstElement.getElementsByTagName("SeriesName");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setTitle(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setTitle(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setDescription(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setDescription(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setId(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setId("invalid");
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCover_url("http://thetvdb.com/banners/posters/" + ((Node) tag.item(0)).getNodeValue() + "-1.jpg");
					} catch(Exception e) {
						show.setCover_url("");
					}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setFirst_aired(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setFirst_aired(c.getString(R.string.stringNA));
					}

					results.add(show);
				}
			}
		} catch (Exception e) {}

		return results;
	}

	public Tvshow getShow(String showId, String language) {
		Tvshow show = new Tvshow();
		show.setId(showId);

		try {
			// Connection set-up
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + MizLib.TVDBAPI + "/series/" + show.getId() + "/" + language + ".xml");

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("Series");
			if (nodeList.getLength() > 0) {
				Node firstNode = nodeList.item(0);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element firstElement = (Element) firstNode;
					NodeList list;
					Element element;
					NodeList tag;

					try {
						list = firstElement.getElementsByTagName("SeriesName");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setTitle(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setTitle(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setDescription(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setDescription(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Actors");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setActors(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setActors(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Genre");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setGenre(((Node) tag.item(0)).getNodeValue());
					} catch (Exception e) {
						show.setGenre(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setRating(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setRating("0");
					}

					try {
						list = firstElement.getElementsByTagName("poster");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCover_url("http://thetvdb.com/banners/" + ((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setCover_url("");
					}

					try {
						list = firstElement.getElementsByTagName("fanart");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setBackdrop_url("http://thetvdb.com/banners/" + ((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setBackdrop_url("");
					}

					try {
						list = firstElement.getElementsByTagName("ContentRating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCertification(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setCertification(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Runtime");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setRuntime(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setRuntime(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setFirst_aired(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setFirst_aired(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("IMDB_ID");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setIMDbId(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setIMDbId("");
					}
				}
			}

			// Trakt.tv
			if (ratingsProvider.equals(c.getString(R.string.ratings_option_2))) {
				try {
					JSONObject jObject = MizLib.getJSONObject("http://api.trakt.tv/show/summary.json/" + MizLib.TRAKT_API + "/" + showId);
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject.getJSONObject("ratings"), "percentage", "0")) / 10;

					if (rating > 0 || show.getRating().equals("0.0"))
						show.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

			// OMDb API / IMDb
			if (ratingsProvider.equals(c.getString(R.string.ratings_option_3))) {
				try {
					JSONObject jObject = MizLib.getJSONObject("http://www.omdbapi.com/?i=" + show.getImdbId());
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

					if (rating > 0 || show.getRating().equals("0.0"))
						show.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}
		} catch (Exception e) {}

		try {
			// Connection set-up
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + MizLib.TVDBAPI + "/series/" + show.getId() + "/all/" + language + ".xml");

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("Episode");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node firstNode = nodeList.item(i);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element firstElement = (Element) firstNode;
					NodeList list;
					Element element;
					NodeList tag;

					Episode episode = new Episode();

					try {
						list = firstElement.getElementsByTagName("EpisodeNumber");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setEpisode(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setEpisode("0");
					}

					try {
						list = firstElement.getElementsByTagName("SeasonNumber");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setSeason(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setSeason("0");
					}

					try {
						list = firstElement.getElementsByTagName("EpisodeName");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setTitle(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setTitle(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setAirdate(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setAirdate(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setDescription(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setDescription(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("filename");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setScreenshot_url("http://thetvdb.com/banners/" + ((Node) tag.item(0)).getNodeValue());
					} catch (Exception e) {
						episode.setScreenshot_url("");
					}

					try {
						list = firstElement.getElementsByTagName("Rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setRating(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setRating("0");
					}

					try {
						list = firstElement.getElementsByTagName("Director");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setDirector(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setDirector(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Writer");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setWriter(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setWriter(c.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("GuestStars");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setGueststars(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {
						episode.setGueststars(c.getString(R.string.stringNA));
					}

					show.addEpisode(episode);
				}
			}
		} catch (Exception e) {}
		
		try {
			// Connection set-up
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + MizLib.TVDBAPI + "/series/" + show.getId() + "/banners.xml");

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getElementsByTagName("Banner");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node firstNode = nodeList.item(i);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element firstElement = (Element) firstNode;
					NodeList list;
					Element element;
					NodeList tag;

					Season season = new Season();

					String bannerType = "";
					int seasonNumber = -1;
					
					try {
						list = firstElement.getElementsByTagName("BannerType");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						bannerType = ((Node) tag.item(0)).getNodeValue();
					} catch(Exception e) {}
					
					if (!bannerType.equals("season"))
						continue;
					
					try {
						list = firstElement.getElementsByTagName("Season");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						seasonNumber = Integer.valueOf(((Node) tag.item(0)).getNodeValue());
					} catch(Exception e) {}
					
					if (seasonNumber >= 0 && !show.hasSeason(seasonNumber)) {
						season.setSeason(seasonNumber);
						
						try {
							list = firstElement.getElementsByTagName("BannerPath");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							season.setCoverPath("http://thetvdb.com/banners/" + ((Node) tag.item(0)).getNodeValue());
						} catch (Exception e) {
							season.setCoverPath("");
						}
						
						show.addSeason(season);
					}
				}
			}
			
			for (Season s : show.getSeasons()) {
				System.out.println("SEASON " + s.getSeason() + ": " + s.getCoverPath());
			}
			
		} catch (Exception e) {}

		return show;
	}
}