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

import android.content.Context;
import android.text.TextUtils;

import com.miz.apis.tmdb.Movie;
import com.miz.db.DbAdapterMovies;
import com.miz.mizuu.MizuuApplication;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class NfoMovie {

	private final Picasso mPicasso;
	private final MovieLibraryUpdateCallback mCallback;
	private final String mFilepath;
	private final Context mContext;
	private final InputStream mInputStream;
	private final int mCount;

	private Movie mMovie;

	public NfoMovie(String file, InputStream is, Context c, MovieLibraryUpdateCallback callback, int count) {
		mFilepath = file;
		mInputStream = is;
		mContext = c;
		mCallback = callback;
		mPicasso = Picasso.with(mContext);
		mCount = count;

		readFile();
	}

	private void readFile() {
		mMovie = new Movie();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document doc;

			doc = db.parse(mInputStream);
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
						mMovie.setTitle(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("originaltitle");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setOriginalTitle(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setOriginalTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("set");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setCollectionTitle(tag.item(0).getNodeValue().trim());
						mMovie.setCollectionId(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setCollectionTitle("");
					}

					try {
						list = firstElement.getElementsByTagName("rating");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setRating(tag.item(0).getNodeValue().trim().replace(",", "."));
					} catch(Exception e) {
						mMovie.setRating("0.0");
					}

					if (firstElement.getElementsByTagName("premiered").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("premiered");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setReleasedate(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setReleasedate("");
						}
					} else if (firstElement.getElementsByTagName("releasedate").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("releasedate");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setReleasedate(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setReleasedate("");
						}
					} else {
						try {
							list = firstElement.getElementsByTagName("year");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setReleasedate(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setReleasedate("");
						}
					}

					if (TextUtils.isEmpty(mMovie.getReleasedate()))
						try {
							list = firstElement.getElementsByTagName("year");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setReleasedate(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setReleasedate("");
						}

					if (firstElement.getElementsByTagName("outline").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("outline");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setTagline(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setTagline("");
						}
					} else {
						try {
							list = firstElement.getElementsByTagName("tagline");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setTagline(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setTagline("");
						}
					}

					try {
						list = firstElement.getElementsByTagName("plot");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setPlot(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setPlot("");
					}

					try {
						list = firstElement.getElementsByTagName("runtime");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setRuntime(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setRuntime("0");
					}

					try {
						list = firstElement.getElementsByTagName("thumb");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setCover(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setCover("");
					}

					if (firstElement.getElementsByTagName("mpaa").getLength() > 0) {
						try {
							list = firstElement.getElementsByTagName("mpaa");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setCertification(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setCertification("");
						}
					} else {					
						try {
							list = firstElement.getElementsByTagName("certification");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							mMovie.setCertification(tag.item(0).getNodeValue().trim());
						} catch(Exception e) {
							mMovie.setCertification("");
						}
					}

					try {
						list = firstElement.getElementsByTagName("id");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setImdbId(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setImdbId("");
					}

					try {
						list = firstElement.getElementsByTagName("tmdbid");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setId(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setId(DbAdapterMovies.UNIDENTIFIED_ID);
					}

					try {
						list = firstElement.getElementsByTagName("trailer");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						mMovie.setTrailer(tag.item(0).getNodeValue().trim());
					} catch(Exception e) {
						mMovie.setTrailer("");
					}

					try {
						list = firstElement.getElementsByTagName("genre");
						element = (Element) list.item(0);
						tag = element.getChildNodes();
						try {
							String genres = tag.item(0).getNodeValue().trim();
							String[] genresArray = genres.split(",");
							StringBuilder sb = new StringBuilder();
							for (int k = 0; k < genresArray.length; k++) {
								sb.append(MizLib.toCapitalWords(genresArray[k].toLowerCase(Locale.getDefault()))).append(", ");
							}
							genres = sb.substring(0, sb.length() - 2);
							mMovie.setGenres(genres);
						} catch (Exception e) {
							mMovie.setGenres(tag.item(0).getNodeValue().trim());
						}
					} catch(Exception e) {
						mMovie.setGenres("");
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
									sbActor.append(actorTag.item(0).getNodeValue().trim());
									sbActor.append("|");
								} catch(Exception e) {}

							}
						}

						String tempCast = sbActor.toString();
						mMovie.setCast(tempCast.substring(0, tempCast.length() - 1));
					}
				}
			}
		} catch (Exception ignored) {
		} finally {
			try {
				if (mInputStream != null)
					mInputStream.close();
			} catch (IOException ignored) {}
		}

		addToDatabase();
	}

	private void addToDatabase() {
		// Create and open database
		DbAdapterMovies dbHelper = MizuuApplication.getMovieAdapter();

		// Create the movie entry
		dbHelper.createMovie(TextUtils.isEmpty(mMovie.getId()) ? mFilepath : mMovie.getId(), mMovie.getTitle(), mMovie.getPlot(), mMovie.getImdbId(),
				mMovie.getRating(), mMovie.getTagline(), mMovie.getReleasedate(), mMovie.getCertification(), mMovie.getRuntime(), mMovie.getTrailer(),
				mMovie.getGenres(), "0", mMovie.getCast(), mMovie.getCollectionTitle(), mMovie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

		// Create the filepath mapping
		MizuuApplication.getMovieMappingAdapter().createFilepathMapping(mFilepath, TextUtils.isEmpty(mMovie.getId()) ? mFilepath : mMovie.getId());

		com.miz.functions.Movie temp = new com.miz.functions.Movie(mContext, mMovie.getTitle(), mMovie.getPlot(), mMovie.getTagline(), mMovie.getId(), mMovie.getImdbId(), mMovie.getRating(),
				mMovie.getReleasedate(), mMovie.getCertification(), mMovie.getRuntime(), mMovie.getTrailer(), mMovie.getGenres(), "0", mMovie.getCast(),
				mMovie.getCollectionTitle(), mMovie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()), false);

		if (mCallback != null)
			try {
				mCallback.onMovieAdded(mMovie.getTitle(), mPicasso.load(temp.getThumbnail()).get(), mPicasso.load(temp.getBackdrop()).get(), mCount);
			} catch (Exception e) {
				mCallback.onMovieAdded(mMovie.getTitle(), null, null, mCount);
			}
	}
}