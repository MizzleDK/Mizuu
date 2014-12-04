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

package com.miz.apis.thetvdb;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.abstractclasses.TvShowApiService;
import com.miz.apis.trakt.Show;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.Actor;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static com.miz.functions.PreferenceKeys.DVD_ORDERING;
import static com.miz.functions.PreferenceKeys.TVSHOWS_RATINGS_SOURCE;

public class TheTVDbService extends TvShowApiService {

	private static TheTVDbService mService;

	private final String mTvdbApiKey;
	private final Context mContext;

	public static TheTVDbService getInstance(Context context) {
		if (mService == null)
			mService = new TheTVDbService(context);
		return mService;
	}
	
	private TheTVDbService(Context context) {
		mContext = context;
		mTvdbApiKey = MizLib.getTvdbApiKey(mContext);
	}

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     * @return
     */
    private String getRatingsProvider() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_4));
    }

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     * @return
     */
    private boolean useDvdOrder() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(DVD_ORDERING, false);
    }

	@Override
	public List<TvShow> search(String query, String language) {
		language = getLanguage(language);
		String serviceUrl = "";

		try {
			serviceUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + URLEncoder.encode(query, "utf-8") + "&language=" + language;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<TvShow> search(String query, String year, String language) {
		language = getLanguage(language);
		String serviceUrl = "";

		try {
			serviceUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + URLEncoder.encode(query + " " + year, "utf-8") + "&language=" + language;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<TvShow> searchByImdbId(String imdbId, String language) {
		language = getLanguage(language);
		String serviceUrl = "";

		try {
			serviceUrl = "http://thetvdb.com/api/GetSeriesByRemoteID.php?imdbid=" + URLEncoder.encode(imdbId, "utf-8") + "&language=" + language;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public TvShow get(String id, String language) {
		TvShow show = new TvShow();
		show.setId(id);

		// Show details
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/" + language + ".xml");

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
						show.setTitle((tag.item(0)).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setDescription((tag.item(0)).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Actors");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setActors((tag.item(0)).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Genre");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setGenres((tag.item(0)).getNodeValue());
					} catch (Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setRating((tag.item(0)).getNodeValue());
					} catch(Exception e) {
						show.setRating("0");
					}

					try {
						list = firstElement.getElementsByTagName("poster");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCoverUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("fanart");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setBackdropUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("ContentRating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCertification(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Runtime");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setRuntime(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setFirstAired(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("IMDB_ID");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setIMDbId(tag.item(0).getNodeValue());
					} catch(Exception e) {}
				}
			}

			// Trakt.tv
			if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2))) {
				try {
					Show showSummary = Trakt.getShowSummary(mContext, id);
					double rating = Double.valueOf(showSummary.getRating() / 10);

					if (rating > 0 || show.getRating().equals("0.0"))
						show.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

			// OMDb API / IMDb
			if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3))) {
				try {
					JSONObject jObject = MizLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + show.getImdbId());
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

					if (rating > 0 || show.getRating().equals("0.0"))
						show.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}
		} catch (Exception e) {}

		// Episode details
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/all/" + language + ".xml");

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

                    if (useDvdOrder()) {
                        try {
                            list = firstElement.getElementsByTagName("DVD_episodenumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setEpisode(MizLib.getInteger(Double.valueOf(tag.item(0).getNodeValue())));
                        } catch (Exception e) {}
                    }

                    if (episode.getEpisode() == -1) {
                        try {
                            list = firstElement.getElementsByTagName("EpisodeNumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setEpisode(MizLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {
                            episode.setEpisode(0);
                        }
                    }

                    if (useDvdOrder()) {
                        try {
                            list = firstElement.getElementsByTagName("DVD_season");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setSeason(MizLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {}
                    }

                    if (episode.getSeason() == -1) {
                        try {
                            list = firstElement.getElementsByTagName("SeasonNumber");
                            element = (Element) list.item(0);
                            tag = element.getChildNodes();
                            episode.setSeason(MizLib.getInteger(tag.item(0).getNodeValue()));
                        } catch (Exception e) {
                            episode.setSeason(0);
                        }
                    }

					try {
						list = firstElement.getElementsByTagName("EpisodeName");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setTitle(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setAirdate(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setDescription(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("filename");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setScreenshotUrl("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
					} catch (Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setRating(tag.item(0).getNodeValue());
					} catch(Exception e) {
						episode.setRating("0");
					}

					try {
						list = firstElement.getElementsByTagName("Director");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setDirector(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("Writer");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setWriter(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					try {
						list = firstElement.getElementsByTagName("GuestStars");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						episode.setGueststars(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					show.addEpisode(episode);
				}
			}
		} catch (Exception e) {}

		// Season covers
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + show.getId() + "/banners.xml");

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
						bannerType = tag.item(0).getNodeValue();
					} catch(Exception e) {}

					if (!bannerType.equals("season"))
						continue;

					try {
						list = firstElement.getElementsByTagName("Season");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						seasonNumber = Integer.valueOf(tag.item(0).getNodeValue());
					} catch(Exception e) {}

					if (seasonNumber >= 0 && !show.hasSeason(seasonNumber)) {
						season.setSeason(seasonNumber);

						try {
							list = firstElement.getElementsByTagName("BannerPath");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							season.setCoverPath("http://thetvdb.com/banners/" + tag.item(0).getNodeValue());
						} catch (Exception e) {
							season.setCoverPath("");
						}

						show.addSeason(season);
					}
				}
			}			
		} catch (Exception e) {}

		return show;
	}

	private ArrayList<TvShow> getListFromUrl(String serviceUrl) {
		ArrayList<TvShow> results = new ArrayList<TvShow>();

		// Fail early
		if (TextUtils.isEmpty(serviceUrl))
			return results;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new URL(serviceUrl);

			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			con.setConnectTimeout(60000);

			Document doc = db.parse(con.getInputStream());
			doc.getDocumentElement().normalize();

			// Check if there's an element with the "id" tag
			NodeList nodeList = doc.getElementsByTagName("Series");

			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					TvShow show = new TvShow();

					Element firstElement = (Element) nodeList.item(i);
					NodeList list;
					Element element;
					NodeList tag;

					try {
						list = firstElement.getElementsByTagName("SeriesName");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setTitle(tag.item(0).getNodeValue());
					} catch(Exception e) {
						show.setTitle(mContext.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("Overview");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setDescription(tag.item(0).getNodeValue());
					} catch(Exception e) {
						show.setDescription(mContext.getString(R.string.stringNA));
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setId(tag.item(0).getNodeValue());
					} catch(Exception e) {
						show.setId(DbAdapterTvShows.UNIDENTIFIED_ID);
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setCoverUrl("http://thetvdb.com/banners/posters/" + tag.item(0).getNodeValue() + "-1.jpg");
					} catch(Exception e) {
						show.setCoverUrl("");
					}

					try {
						list = firstElement.getElementsByTagName("FirstAired");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						show.setFirstAired(tag.item(0).getNodeValue());
					} catch(Exception e) {
						show.setFirstAired(mContext.getString(R.string.stringNA));
					}

					results.add(show);
				}
			}
		} catch (Exception e) {}

		return results;
	}

	@Override
	public List<String> getCovers(String id) {
		ArrayList<String> covers = new ArrayList<String>();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + id + "/banners.xml");
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("Banners");
			if (nodeList.getLength() > 0) {
				Node firstNode = nodeList.item(0);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element firstElement = (Element) firstNode;
					NodeList list, list2;
					Element element;

					list = firstElement.getChildNodes();
					list2 = firstElement.getChildNodes();
					nodeList = doc.getElementsByTagName("Banner");
					if (nodeList.getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("BannerType");
							list2 = firstElement.getElementsByTagName("BannerPath");

							for (int i = 0; i < list.getLength(); i++) {
								element = (Element) list.item(i);
								if (element.getTextContent().equals("poster"))
									covers.add("http://thetvdb.com/banners/_cache/" + list2.item(i).getTextContent());
							}
						} catch (Exception e) {}
					}
				}
			}
		} catch (Exception e) {}

		return covers;
	}

	@Override
	public List<String> getBackdrops(String id) {
		ArrayList<String> backdrops = new ArrayList<String>();
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse("http://thetvdb.com/api/" + mTvdbApiKey + "/series/" + id + "/banners.xml");
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("Banners");
			if (nodeList.getLength() > 0) {

				Node firstNode = nodeList.item(0);

				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element firstElement = (Element) firstNode;
					NodeList list, list2;
					Element element;

					list = firstElement.getChildNodes();
					list2 = firstElement.getChildNodes();
					nodeList = doc.getElementsByTagName("Banner");
					if (nodeList.getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("BannerType");
							list2 = firstElement.getElementsByTagName("BannerPath");
							for (int i = 0; i < list.getLength(); i++) {
								element = (Element) list.item(i);
								if (element.getTextContent().equals("fanart"))
									backdrops.add("http://thetvdb.com/banners/_cache/" + list2.item(i).getTextContent());
							}
						} catch (Exception e) {} // No such tag
					}
				}
			}
		} catch (Exception e) {}
		
		return backdrops;
	}

	@Override
	public List<TvShow> searchNgram(String query, String language) {
		throw new UnsupportedOperationException(); // Not supported for TheTVDb
	}

	@Override
	public List<Actor> getActors(String id) {
		throw new UnsupportedOperationException(); // Not supported for TheTVDb
	}
}