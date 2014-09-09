package com.miz.apis.tmdb;

import static com.miz.functions.PreferenceKeys.MOVIE_RATINGS_SOURCE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.abstractclasses.MovieApiService;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

public class TMDbMovieService extends MovieApiService {

	private final String mRatingsProvider, mTmdbApiKey;
	private final Context mContext;

	public TMDbMovieService(Context context) {
		mContext = context;
		mRatingsProvider = PreferenceManager.getDefaultSharedPreferences(mContext).getString(MOVIE_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_4));
		mTmdbApiKey = MizLib.getTmdbApiKey(mContext);
	}

	@Override
	public List<Movie> search(String query, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<Movie> search(String query, String year, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&year=" + year + "&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<Movie> searchByImdbId(String imdbId, String language) {
		language = getLanguage(language);

		ArrayList<Movie> results = new ArrayList<Movie>();

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + imdbId + "?language=" + language + "&external_source=imdb_id&api_key=" + mTmdbApiKey);

			JSONArray array = jObject.getJSONArray("movie_results");

			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
			String imageSizeUrl = MizLib.getImageUrlSize(mContext);

			for (int i = 0; i < array.length(); i++) {
				Movie movie = new Movie();
				movie.setTitle(array.getJSONObject(i).getString("title"));
				movie.setOriginalTitle(array.getJSONObject(i).getString("original_title"));
				movie.setReleasedate(array.getJSONObject(i).getString("release_date"));
				movie.setPlot(""); // TMDb doesn't support descriptions in search results
				movie.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
				movie.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
				movie.setCover(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
				results.add(movie);
			}
		} catch (JSONException e) {}

		return results;
	}

	@Override
	public Movie get(String id, String json, String language) {
		Movie movie = new Movie();
		movie.setId(id);

		if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
			return movie;

		try {
			// Get the base URL from the preferences
			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

			JSONObject jObject = null;
			if (json == null)
				jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + mTmdbApiKey + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,casts,images");
			else
				jObject = new JSONObject(json);

			movie.setTitle(MizLib.getStringFromJSONObject(jObject, "title", ""));

			movie.setPlot(MizLib.getStringFromJSONObject(jObject, "overview", ""));

			movie.setImdbId(MizLib.getStringFromJSONObject(jObject, "imdb_id", ""));

			movie.setRating(MizLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

			movie.setTagline(MizLib.getStringFromJSONObject(jObject, "tagline", ""));

			movie.setReleasedate(MizLib.getStringFromJSONObject(jObject, "release_date", ""));

			movie.setRuntime(MizLib.getStringFromJSONObject(jObject, "runtime", "0"));

			if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
				JSONObject englishResults = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases");

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
				movie.setCover(baseUrl + MizLib.getImageUrlSize(mContext) + jObject.getString("poster_path"));
			} catch (Exception e) {}

			try {
				movie.setCollectionTitle(jObject.getJSONObject("belongs_to_collection").getString("name"));
				movie.setCollectionId(jObject.getJSONObject("belongs_to_collection").getString("id"));
			} catch (Exception e) {}

			if (!movie.getCollectionId().isEmpty() && json == null) {
				JSONObject collection = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/collection/" + movie.getCollectionId() + "/images?api_key=" + mTmdbApiKey);
				JSONArray array = collection.getJSONArray("posters");
				if (array.length() > 0)
					movie.setCollectionImage(baseUrl + MizLib.getImageUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
			}

			try {
				String genres = "";
				for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
					genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
				movie.setGenres(genres.substring(0, genres.length() - 2));
			} catch (Exception e) {}

			try {
				if (jObject.getJSONObject("trailers").getJSONArray("youtube").length() > 0) {

					// Go through all YouTube links and looks for trailers
					JSONArray youtube = jObject.getJSONObject("trailers").getJSONArray("youtube");
					for (int i = 0; i < youtube.length(); i++) {
						if (youtube.getJSONObject(i).getString("type").equals("Trailer")) {
							movie.setTrailer("http://www.youtube.com/watch?v=" + youtube.getJSONObject(i).getString("source"));
							break;
						}
					}

					// If no trailer was set, use whatever YouTube link is available (featurette, interviews, etc.)
					if (TextUtils.isEmpty(movie.getTrailer())) {
						movie.setTrailer("http://www.youtube.com/watch?v=" + jObject.getJSONObject("trailers").getJSONArray("youtube").getJSONObject(0).getString("source"));
					}
				}
			} catch (Exception e) {}

			try {
				for (int i = 0; i < jObject.getJSONObject("releases").getJSONArray("countries").length(); i++) {
					JSONObject jo = jObject.getJSONObject("releases").getJSONArray("countries").getJSONObject(i);
					if (jo.getString("iso_3166_1").equalsIgnoreCase("us") || jo.getString("iso_3166_1").equalsIgnoreCase(language))
						movie.setCertification(jo.getString("certification"));
				}
			} catch (Exception e) {}

			try {
				StringBuilder cast = new StringBuilder();

				JSONArray array = jObject.getJSONObject("casts").getJSONArray("cast");
				for (int i = 0; i < array.length(); i++) {
					cast.append(array.getJSONObject(i).getString("name"));
					cast.append("|");
				}

				movie.setCast(cast.toString());
			} catch (Exception e) {}

			try {
				JSONArray array = jObject.getJSONObject("images").getJSONArray("backdrops");

				if (array.length() > 0) {
					movie.setBackdrop(baseUrl + MizLib.getBackdropUrlSize(mContext) + array.getJSONObject(0).getString("file_path"));
				} else { // Try with English set as the language, if no results are returned (usually caused by a server-side cache error)
					try {
						jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "/images?api_key=" + mTmdbApiKey);

						JSONArray array2 = jObject.getJSONArray("backdrops");
						if (array2.length() > 0) {
							movie.setBackdrop(baseUrl + MizLib.getBackdropUrlSize(mContext) + array2.getJSONObject(0).getString("file_path"));
						}
					} catch (Exception e) {}
				}
			} catch (Exception e) {}

			// Trakt.tv
			if (mRatingsProvider.equals(mContext.getString(R.string.ratings_option_2)) && json == null) {
				try {
					com.miz.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
					double rating = Double.valueOf(movieSummary.getRating()) / 10;

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

			// OMDb API / IMDb
			if (mRatingsProvider.equals(mContext.getString(R.string.ratings_option_3)) && json == null) {
				try {
					jObject = MizLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + movie.getImdbId());
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

		} catch (Exception e) {}

		return movie;
	}

	@Override
	public Movie get(String id, String language) {
		return get(id, null, language);
	}

	@Override
	public List<String> getCovers(String id) {
		ArrayList<String> covers = new ArrayList<String>();
		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "/images" + "?api_key=" + mTmdbApiKey);
			JSONArray jArray = jObject.getJSONArray("posters");
			for (int i = 0; i < jArray.length(); i++) {
				covers.add(baseUrl + MizLib.getImageUrlSize(mContext) + MizLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
			}
		} catch (JSONException e) {}

		return covers;
	}

	@Override
	public List<String> getBackdrops(String id) {
		ArrayList<String> covers = new ArrayList<String>();
		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "/images" + "?api_key=" + mTmdbApiKey);
			JSONArray jArray = jObject.getJSONArray("backdrops");
			for (int i = 0; i < jArray.length(); i++) {
				covers.add(baseUrl + MizLib.getBackdropThumbUrlSize(mContext) + MizLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
			}
		} catch (JSONException e) {}

		return covers;
	}

	private ArrayList<Movie> getListFromUrl(String serviceUrl) {
		ArrayList<Movie> results = new ArrayList<Movie>();

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, serviceUrl);
			JSONArray array = jObject.getJSONArray("results");

			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
			String imageSizeUrl = MizLib.getImageUrlSize(mContext);

			for (int i = 0; i < array.length(); i++) {
				Movie movie = new Movie();
				movie.setTitle(array.getJSONObject(i).getString("title"));
				movie.setOriginalTitle(array.getJSONObject(i).getString("original_title"));
				movie.setReleasedate(array.getJSONObject(i).getString("release_date"));
				movie.setPlot(""); // TMDb doesn't support descriptions in search results
				movie.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
				movie.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
				movie.setCover(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
				results.add(movie);
			}
		} catch (JSONException e) {}

		return results;
	}

	@Override
	public List<Movie> searchNgram(String query, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/movie?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&search_type=ngram&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}
}