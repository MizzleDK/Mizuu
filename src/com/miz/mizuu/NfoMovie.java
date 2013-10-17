package com.miz.mizuu;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.miz.functions.MizFile;
import com.miz.functions.Movie;
import com.miz.functions.TMDbMovie;

public class NfoMovie {

	private TMDbMovie movie;
	private MizFile file, nfoFile;
	private Context c;

	public NfoMovie(MizFile file, MizFile nfoFile, Context c) {
		this.file = file;
		this.nfoFile = nfoFile;
		this.c = c;

		readFile();
	}

	private void readFile() {
		movie = new TMDbMovie();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document doc = db.parse(nfoFile.getInputStream()); // This can be null, but the exception will be caught
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
						movie.setRating(((Node) tag.item(0)).getNodeValue().trim());
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
						movie.setId("");
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
						movie.setGenres(((Node) tag.item(0)).getNodeValue().trim());
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
		} catch (Exception e) {}

		addToDatabase();
	}

	private void addToDatabase() {
		// Create and open database
		DbAdapter dbHelper = MizuuApplication.getMovieAdapter();
		long rowId = dbHelper.createMovie(file.getAbsolutePath(), movie.getCover(), movie.getTitle(), movie.getPlot(), movie.getId(), movie.getImdbId(), movie.getRating(), movie.getTagline(), movie.getReleasedate(), movie.getCertification(), movie.getRuntime(), movie.getTrailer(), movie.getGenres(), "0", movie.getCast(), movie.getCollectionTitle(), movie.getCollectionId(), "0", "0", String.valueOf(System.currentTimeMillis()));

		Movie temp = new Movie(c,
				String.valueOf(rowId),
				file.getAbsolutePath(),
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

		Intent intent = new Intent("mizuu-movies-object");
		intent.putExtra("movieName", movie.getTitle());
		intent.putExtra("cover", temp.getPoster());
		intent.putExtra("thumbFile", temp.getThumbnail());
		intent.putExtra("backdrop", temp.getBackdrop());

		LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
		LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent("mizuu-movies-update"));
	}
}