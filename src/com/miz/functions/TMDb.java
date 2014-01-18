package com.miz.functions;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.miz.mizuu.R;

import android.content.Context;
import android.preference.PreferenceManager;


public class TMDb {

	private boolean hasTriedFileNameOnce = false, hasTriedParent = false, includeAdult = false;
	private Context c;
	private String ratingsProvider;

	public TMDb(Context c) {
		this.c = c;
		ratingsProvider = PreferenceManager.getDefaultSharedPreferences(c).getString("prefsMovieRatingsSource", c.getString(R.string.ratings_option_1));
		includeAdult = PreferenceManager.getDefaultSharedPreferences(c).getBoolean("prefsIncludeAdultContent", false);
	}

	public TMDbMovie searchForMovie(String query, String year, String filepath, String language) {
		TMDbMovie movie = new TMDbMovie();

		try {
			JSONObject jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&api_key=" + MizLib.TMDB_API + (!MizLib.isEmpty(year) ? "&year=" + year : "") + (includeAdult ? "&include_adult=true" : ""));

			if (jObject.getJSONArray("results").length() > 0)
				movie.setId(jObject.getJSONArray("results").getJSONObject(0).getString("id"));
			else
				movie.setId("invalid");

		} catch (Exception e) {
			movie.setId("invalid");
		}

		if (!movie.getId().equals("invalid")) {
			movie = getMovie(movie.getId(), language);
		} else {
			if (!hasTriedFileNameOnce) {
				hasTriedFileNameOnce = true;
				movie = searchForMovie(query, "", filepath, language); // Remove the year parameter to perform a more generic search
			} else {
				if (!hasTriedParent) {
					hasTriedParent = true;
					DecryptedMovie dm = MizLib.decryptMovie(filepath, PreferenceManager.getDefaultSharedPreferences(c).getString("ignoredTags", ""));
					if (dm.hasParentName()) {
						movie = searchForMovie(dm.getDecryptedParentName(), dm.getParentNameYear(), filepath, language);
					}
				}
			}
		}

		return movie;
	}

	public ArrayList<TMDbMovie> searchForMovies(String query, String year, String language) {

		ArrayList<TMDbMovie> results = new ArrayList<TMDbMovie>();

		try {
			String baseImgUrl = PreferenceManager.getDefaultSharedPreferences(c).getString("tmdbBaseUrl", MizLib.TMDB_BASE_URL);
			JSONObject jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&api_key=" + MizLib.TMDB_API + (!MizLib.isEmpty(year) ? "&year=" + year : "") + (includeAdult ? "&include_adult=true" : ""));

			for (int i = 0; i < jObject.getJSONArray("results").length(); i++) {
				TMDbMovie movie = new TMDbMovie();
				movie.setId(jObject.getJSONArray("results").getJSONObject(i).getString("id"));
				movie.setCover(baseImgUrl + MizLib.getImageUrlSize(c) + jObject.getJSONArray("results").getJSONObject(i).getString("poster_path"));
				movie.setBackdrop(baseImgUrl + MizLib.getBackdropUrlSize(c) + jObject.getJSONArray("results").getJSONObject(i).getString("backdrop_path"));
				movie.setTitle(jObject.getJSONArray("results").getJSONObject(i).getString("title"));
				movie.setOriginalTitle(jObject.getJSONArray("results").getJSONObject(i).getString("original_title"));
				movie.setReleasedate(jObject.getJSONArray("results").getJSONObject(i).getString("release_date"));
				movie.setRating(jObject.getJSONArray("results").getJSONObject(i).getString("vote_average"));
				results.add(movie);
			}

		} catch (Exception e) {}

		return results;
	}

	public TMDbMovie getMovie(String id, String language) {
		return getMovie(id, null, language);
	}

	public TMDbMovie getMovie(String id, String json, String language) {
		TMDbMovie movie = new TMDbMovie();
		movie.setId(id);

		if (id.equals("invalid"))
			return movie;

		try {
			// Get the base URL from the preferences
			String baseUrl = PreferenceManager.getDefaultSharedPreferences(c).getString("tmdbBaseUrl", MizLib.TMDB_BASE_URL);

			JSONObject jObject = null;
			if (json != null)
				jObject = new JSONObject(json);
			else
				jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/movie/" + id + "?api_key=" + MizLib.TMDB_API + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,casts,images");
				
			movie.setTitle(MizLib.getStringFromJSONObject(jObject, "title", ""));

			movie.setPlot(MizLib.getStringFromJSONObject(jObject, "overview", ""));

			movie.setImdbId(MizLib.getStringFromJSONObject(jObject, "imdb_id", ""));

			movie.setRating(MizLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

			movie.setTagline(MizLib.getStringFromJSONObject(jObject, "tagline", ""));

			movie.setReleasedate(MizLib.getStringFromJSONObject(jObject, "release_date", ""));

			movie.setRuntime(MizLib.getStringFromJSONObject(jObject, "runtime", "0"));

			if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
				JSONObject englishResults = MizLib.getJSONObject("https://api.themoviedb.org/3/movie/" + id + "?api_key=" + MizLib.TMDB_API + "&language=en&append_to_response=releases");

				if (movie.getTitle().isEmpty())
					movie.setTitle(MizLib.getStringFromJSONObject(englishResults, "title", ""));

				if (movie.getPlot().isEmpty())
					movie.setPlot(MizLib.getStringFromJSONObject(englishResults, "overview", ""));

				if (movie.getTagline().isEmpty())
					movie.setTagline(MizLib.getStringFromJSONObject(englishResults, "tagline", ""));

				if (movie.getRating().equals("0.0"))
					movie.setRating(MizLib.getStringFromJSONObject(englishResults, "vote_average", "0.0"));

				if (movie.getReleasedate().isEmpty())
					movie.setReleasedate(MizLib.getStringFromJSONObject(englishResults, "release_date", ""));

				if (movie.getRuntime().equals("0"))
					movie.setRuntime(MizLib.getStringFromJSONObject(englishResults, "runtime", "0"));
			}

			try {
				movie.setCover(baseUrl + MizLib.getImageUrlSize(c) + jObject.getString("poster_path"));
			} catch (Exception e) {}

			try {
				movie.setCollectionTitle(jObject.getJSONObject("belongs_to_collection").getString("name"));
				movie.setCollectionId(jObject.getJSONObject("belongs_to_collection").getString("id"));
			} catch (Exception e) {}

			if (!movie.getCollectionId().isEmpty() && json == null) {
				JSONObject collection = MizLib.getJSONObject("https://api.themoviedb.org/3/collection/" + movie.getCollectionId() + "/images?api_key=" + MizLib.TMDB_API);
				JSONArray array = collection.getJSONArray("posters");
				if (array.length() > 0)
					movie.setCollectionImage(baseUrl + MizLib.getImageUrlSize(c) + array.getJSONObject(0).getString("file_path"));
			}

			try {
				String genres = "";
				for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
					genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
				movie.setGenres(genres.substring(0, genres.length() - 2));
			} catch (Exception e) {}

			try {
				if (jObject.getJSONObject("trailers").getJSONArray("youtube").length() > 0)
					movie.setTrailer("http://www.youtube.com/watch?v=" + jObject.getJSONObject("releases").getJSONArray("youtube").getJSONObject(0).getString("source"));
			} catch (Exception e) {}

			try {
				for (int i = 0; i < jObject.getJSONObject("releases").getJSONArray("countries").length(); i++) {
					JSONObject jo = jObject.getJSONObject("releases").getJSONArray("countries").getJSONObject(i);
					if (jo.getString("iso_3166_1").equalsIgnoreCase("us") || jo.getString("iso_3166_1").equalsIgnoreCase(language))
						movie.setCertification(jo.getString("certification"));
				}
			} catch (Exception e) {}

			try {
				String cast = "";

				JSONArray array = jObject.getJSONObject("casts").getJSONArray("cast");
				for (int i = 0; i < array.length(); i++) {
					cast += array.getJSONObject(i).getString("name") + "|";
				}

				if (cast.endsWith("|"))
					cast = cast.substring(0, cast.length() - 1);

				movie.setCast(cast);
			} catch (Exception e) {}

			try {
				JSONArray array = jObject.getJSONObject("images").getJSONArray("backdrops");
				
				if (array.length() > 0) {
					movie.setBackdrop(baseUrl + MizLib.getBackdropUrlSize(c) + array.getJSONObject(0).getString("file_path"));
				} else { // Try with English set as the language, if no results are returned (usually caused by a server-side cache error)
					if (json == null)
						try {
							jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/movie/" + id + "/images?api_key=" + MizLib.TMDB_API + "&language=en");

							JSONArray array2 = jObject.getJSONArray("backdrops");
							if (array2.length() > 0) {
								movie.setBackdrop(baseUrl + MizLib.getBackdropUrlSize(c) + array2.getJSONObject(0).getString("file_path"));
							}
						} catch (Exception e) {}
				}
			} catch (Exception e) {}

			// Trakt.tv
			if (ratingsProvider.equals(c.getString(R.string.ratings_option_2)) && json == null) {
				try {
					jObject = MizLib.getJSONObject("http://api.trakt.tv/movie/summary.json/" + MizLib.TRAKT_API + "/" + id);
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject.getJSONObject("ratings"), "percentage", "0")) / 10;

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

			// OMDb API / IMDb
			if (ratingsProvider.equals(c.getString(R.string.ratings_option_3)) && json == null) {
				try {
					jObject = MizLib.getJSONObject("http://www.omdbapi.com/?i=" + movie.getImdbId());
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

		} catch (Exception e) {}

		return movie;
	}
}