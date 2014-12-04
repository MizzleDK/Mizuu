package com.miz.apis.tmdb;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.abstractclasses.MovieApiService;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.Actor;
import com.miz.functions.CompleteActor;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.miz.functions.PreferenceKeys.INCLUDE_ADULT_CONTENT;
import static com.miz.functions.PreferenceKeys.MOVIE_RATINGS_SOURCE;

public class TMDbMovieService extends MovieApiService {

	private static TMDbMovieService mService;

	private final String mTmdbApiKey;
	private final Context mContext;

	public static TMDbMovieService getInstance(Context context) {
		if (mService == null)
			mService = new TMDbMovieService(context);
		return mService;
	}

	private TMDbMovieService(Context context) {
		mContext = context;
		mTmdbApiKey = MizLib.getTmdbApiKey(mContext);
	}

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     * @return
     */
    public String getRatingsProvider() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(MOVIE_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_1));
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
			if (TextUtils.isEmpty(json))
				jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + mTmdbApiKey + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,credits,images");
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

				if (TextUtils.isEmpty(movie.getTitle()))
					movie.setTitle(MizLib.getStringFromJSONObject(englishResults, "title", ""));

				if (TextUtils.isEmpty(movie.getPlot()))
					movie.setPlot(MizLib.getStringFromJSONObject(englishResults, "overview", ""));

				if (TextUtils.isEmpty(movie.getTagline()))
					movie.setTagline(MizLib.getStringFromJSONObject(englishResults, "tagline", ""));

				if (TextUtils.isEmpty(movie.getRating()))
					movie.setRating(MizLib.getStringFromJSONObject(englishResults, "vote_average", "0.0"));

				if (TextUtils.isEmpty(movie.getReleasedate()))
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

			if (!TextUtils.isEmpty(movie.getCollectionId()) && json == null) {
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

				JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
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
			if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2)) && json == null) {
				try {
					com.miz.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
					double rating = (double) movieSummary.getRating() / 10;

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

			// OMDb API / IMDb
			if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3)) && json == null) {
				try {
					jObject = MizLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + movie.getImdbId());
					double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

					if (rating > 0 || movie.getRating().equals("0.0"))
						movie.setRating(String.valueOf(rating));	
				} catch (Exception e) {}
			}

		} catch (Exception e) {
			// If something goes wrong here, i.e. API error, we won't get any details
			// about the movie - in other words, it's unidentified
			movie.setId(DbAdapterMovies.UNIDENTIFIED_ID);
		}

		return movie;
	}

	@Override
	public Movie get(String id, String language) {
		return get(id, null, language);
	}
	
	public Movie getCompleteMovie(String id, String language) {
		Movie movie = new Movie();
		movie.setId(id);

		if (id.equals(DbAdapterMovies.UNIDENTIFIED_ID))
			return movie;

		try {
			// Get the base URL from the preferences
			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + mTmdbApiKey + (language.equals("en") ? "" : "&language=" + language) + "&append_to_response=releases,trailers,credits,images,similar_movies");

			movie.setTitle(MizLib.getStringFromJSONObject(jObject, "title", ""));

			movie.setPlot(MizLib.getStringFromJSONObject(jObject, "overview", ""));

			movie.setImdbId(MizLib.getStringFromJSONObject(jObject, "imdb_id", ""));

			movie.setRating(MizLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

			movie.setTagline(MizLib.getStringFromJSONObject(jObject, "tagline", ""));

			movie.setReleasedate(MizLib.getStringFromJSONObject(jObject, "release_date", ""));

			movie.setRuntime(MizLib.getStringFromJSONObject(jObject, "runtime", "0"));

			if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
				JSONObject englishResults = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + mTmdbApiKey + "&language=en&append_to_response=releases");

				if (TextUtils.isEmpty(movie.getTitle()))
					movie.setTitle(MizLib.getStringFromJSONObject(englishResults, "title", ""));

				if (TextUtils.isEmpty(movie.getPlot()))
					movie.setPlot(MizLib.getStringFromJSONObject(englishResults, "overview", ""));

				if (TextUtils.isEmpty(movie.getTagline()))
					movie.setTagline(MizLib.getStringFromJSONObject(englishResults, "tagline", ""));

				if (TextUtils.isEmpty(movie.getRating()))
					movie.setRating(MizLib.getStringFromJSONObject(englishResults, "vote_average", "0.0"));

				if (TextUtils.isEmpty(movie.getReleasedate()))
					movie.setReleasedate(MizLib.getStringFromJSONObject(englishResults, "release_date", ""));

				if (movie.getRuntime().equals("0"))
					movie.setRuntime(MizLib.getStringFromJSONObject(englishResults, "runtime", "0"));
			}

			try {
				movie.setCover(baseUrl + MizLib.getImageUrlSize(mContext) + jObject.getString("poster_path"));
			} catch (Exception e) {}

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
				ArrayList<Actor> actors = new ArrayList<Actor>();

				JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
				Set<String> actorIds = new HashSet<String>();

				for (int i = 0; i < array.length(); i++) {
					if (!actorIds.contains(array.getJSONObject(i).getString("id"))) {
						actorIds.add(array.getJSONObject(i).getString("id"));

						actors.add(new Actor(
								array.getJSONObject(i).getString("name"),
								array.getJSONObject(i).getString("character"),
								array.getJSONObject(i).getString("id"),
								baseUrl + MizLib.getActorUrlSize(mContext) + array.getJSONObject(i).getString("profile_path")));
					}
				}

				movie.setActors(actors);
			} catch (Exception e) {}

			try {
				ArrayList<WebMovie> similarMovies = new ArrayList<WebMovie>();
				JSONArray jArray = jObject.getJSONObject("similar_movies").getJSONArray("results");

				for (int i = 0; i < jArray.length(); i++) {
					if (!MizLib.isAdultContent(mContext, jArray.getJSONObject(i).getString("title")) && !MizLib.isAdultContent(mContext, jArray.getJSONObject(i).getString("original_title"))) {
						similarMovies.add(new WebMovie(mContext,
								jArray.getJSONObject(i).getString("original_title"),
								jArray.getJSONObject(i).getString("id"),
								baseUrl + MizLib.getImageUrlSize(mContext) + jArray.getJSONObject(i).getString("poster_path"),
								jArray.getJSONObject(i).getString("release_date")));
					}
				}
				
				movie.setSimilarMovies(similarMovies);
			} catch (Exception ignored) {}

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

		} catch (Exception e) {
			// If something goes wrong here, i.e. API error, we won't get any details
			// about the movie - in other words, it's unidentified
			movie.setId(DbAdapterMovies.UNIDENTIFIED_ID);
		}

		return movie;
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

	@Override
	public List<Actor> getActors(String id) {
		ArrayList<Actor> results = new ArrayList<Actor>();

		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "/credits?api_key=" + mTmdbApiKey);	
			JSONArray jArray = jObject.getJSONArray("cast");

			Set<String> actorIds = new HashSet<String>();

			for (int i = 0; i < jArray.length(); i++) {
				if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
					actorIds.add(jArray.getJSONObject(i).getString("id"));

					results.add(new Actor(
							jArray.getJSONObject(i).getString("name"),
							jArray.getJSONObject(i).getString("character"),
							jArray.getJSONObject(i).getString("id"),
							baseUrl + MizLib.getActorUrlSize(mContext) + jArray.getJSONObject(i).getString("profile_path")));
				}
			}
		} catch (Exception ignored) {}

		return results;
	}

	@Override
	public List<WebMovie> getSimilarMovies(String id) {
		ArrayList<WebMovie> results = new ArrayList<WebMovie>();

		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + id + "/similar_movies?api_key=" + mTmdbApiKey);	
			JSONArray jArray = jObject.getJSONArray("results");

			for (int i = 0; i < jArray.length(); i++) {
				if (!MizLib.isAdultContent(mContext, jArray.getJSONObject(i).getString("title")) && !MizLib.isAdultContent(mContext, jArray.getJSONObject(i).getString("original_title"))) {
					results.add(new WebMovie(mContext,
							jArray.getJSONObject(i).getString("original_title"),
							jArray.getJSONObject(i).getString("id"),
							baseUrl + MizLib.getImageUrlSize(mContext) + jArray.getJSONObject(i).getString("poster_path"),
							jArray.getJSONObject(i).getString("release_date")));
				}
			}
		} catch (Exception ignored) {}

		return results;
	}

	public CompleteActor getCompleteActorDetails(final String actorId) {
		JSONObject json = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/person/" + actorId + "?api_key=" + mTmdbApiKey + "&append_to_response=movie_credits,tv_credits,images,tagged_images");
		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
		boolean includeAdult = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(INCLUDE_ADULT_CONTENT, false);

		// Set up actor details
		CompleteActor actor = new CompleteActor(actorId);
		actor.setName(MizLib.getStringFromJSONObject(json, "name", ""));
		actor.setBiography(MizLib.getStringFromJSONObject(json, "biography", ""));
		actor.setBirthday(MizLib.getStringFromJSONObject(json, "birthday", ""));
		actor.setDayOfDeath(MizLib.getStringFromJSONObject(json, "deathday", ""));
		actor.setPlaceOfBirth(MizLib.getStringFromJSONObject(json, "place_of_birth", ""));

		String profilePhoto = MizLib.getStringFromJSONObject(json, "profile_path", "");
		if (!TextUtils.isEmpty(profilePhoto))
			profilePhoto = baseUrl + "w500" + profilePhoto;
		actor.setProfilePhoto(profilePhoto);

        String profilePhotoThumb = MizLib.getStringFromJSONObject(json, "profile_path", "");
        if (!TextUtils.isEmpty(profilePhoto))
            profilePhotoThumb = baseUrl + MizLib.getActorUrlSize(mContext) + profilePhoto;
        actor.setProfilePhotoThumb(profilePhotoThumb);

		// Set up movies
		List<WebMovie> movies = new ArrayList<WebMovie>();
		try {
			JSONArray movieArray = json.getJSONObject("movie_credits").getJSONArray("cast");
			for (int i = 0; i < movieArray.length(); i++) {

				final JSONObject thisObject = movieArray.getJSONObject(i);

				boolean isAdult = thisObject.getBoolean("adult") |
						MizLib.isAdultContent(mContext, MizLib.getStringFromJSONObject(thisObject, "title", "")) |
						MizLib.isAdultContent(mContext, MizLib.getStringFromJSONObject(thisObject, "original_title", ""));

				// Continue to the next loop iteration if this is an adult title
				if (!includeAdult && isAdult)
					continue;

				WebMovie movie = new WebMovie(mContext,
						MizLib.getStringFromJSONObject(thisObject, "title", ""),
						String.valueOf(thisObject.getInt("id")),
						baseUrl + MizLib.getImageUrlSize(mContext) + MizLib.getStringFromJSONObject(thisObject, "poster_path", ""),
						MizLib.getStringFromJSONObject(thisObject, "release_date", ""));

				movies.add(movie);
			}
		} catch (JSONException ignored) {} finally {
			actor.setMovies(movies);
		}

		// Set up TV shows
		List<WebMovie> shows = new ArrayList<WebMovie>();
		try {
			JSONArray showArray = json.getJSONObject("tv_credits").getJSONArray("cast");
			for (int i = 0; i < showArray.length(); i++) {

				final JSONObject thisObject = showArray.getJSONObject(i);

				boolean isAdult =
						MizLib.isAdultContent(mContext, MizLib.getStringFromJSONObject(thisObject, "name", "")) |
						MizLib.isAdultContent(mContext, MizLib.getStringFromJSONObject(thisObject, "original_name", ""));

				// Continue to the next loop iteration if this is an adult title
				if (!includeAdult && isAdult)
					continue;

				WebMovie show = new WebMovie(mContext,
						MizLib.getStringFromJSONObject(thisObject, "name", ""),
						String.valueOf(thisObject.getInt("id")),
						baseUrl + MizLib.getImageUrlSize(mContext) + MizLib.getStringFromJSONObject(thisObject, "poster_path", ""),
						MizLib.getStringFromJSONObject(thisObject, "first_air_date", ""));

				shows.add(show);
			}
		} catch (JSONException ignored) {} finally {
			actor.setTvShows(shows);
		}

		int count = 0;

		try {
			count += json.getJSONObject("movie_credits").getJSONArray("cast").length();
			count += json.getJSONObject("tv_credits").getJSONArray("cast").length();
		} catch (JSONException ignored) {}

		actor.setKnownCreditCount(count);

		List<String> photos = new ArrayList<String>();
		try {
			JSONArray photoArray = json.getJSONObject("images").getJSONArray("profiles");
			for (int i = 0; i < photoArray.length(); i++) {
				photos.add(baseUrl + MizLib.getImageUrlSize(mContext) + photoArray.getJSONObject(i).getString("file_path"));
			}
		} catch (JSONException ignored) {} finally {
			actor.setPhotos(photos);
		}

		List<String> taggedPhotos = new ArrayList<String>();
		try {
			JSONArray photoArray = json.getJSONObject("tagged_images").getJSONArray("results");
			for (int i = 0; i < photoArray.length(); i++) {
				if (photoArray.getJSONObject(i).getString("image_type").equals("backdrop"))
					taggedPhotos.add(baseUrl + MizLib.getBackdropThumbUrlSize(mContext) + photoArray.getJSONObject(i).getString("file_path"));
			}
		} catch (JSONException ignored) {} finally {
			actor.setTaggedPhotos(taggedPhotos);
		}

		return actor;
	}
}