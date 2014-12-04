package com.miz.apis.tmdb;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.abstractclasses.TvShowApiService;
import com.miz.apis.thetvdb.Episode;
import com.miz.apis.thetvdb.Season;
import com.miz.apis.thetvdb.TvShow;
import com.miz.apis.trakt.Trakt;
import com.miz.functions.Actor;
import com.miz.functions.MizLib;
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

import static com.miz.functions.PreferenceKeys.TVSHOWS_RATINGS_SOURCE;

public class TMDbTvShowService extends TvShowApiService {

	private static TMDbTvShowService mService;
	
	private final String mTmdbApiKey;
	private final Context mContext;

	public static TMDbTvShowService getInstance(Context context) {
		if (mService == null)
			mService = new TMDbTvShowService(context);
		return mService;
	}
	
	private TMDbTvShowService(Context context) {
		mContext = context;
		mTmdbApiKey = MizLib.getTmdbApiKey(mContext);
	}

    /**
     * Get the ratings provider. This isn't a static value, so it should be reloaded when needed.
     * @return
     */
    public String getRatingsProvider() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_RATINGS_SOURCE, mContext.getString(R.string.ratings_option_4));
    }

	@Override
	public List<TvShow> search(String query, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<TvShow> search(String query, String year, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&first_air_date_year=" + year + "&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<TvShow> searchByImdbId(String imdbId, String language) {
		language = getLanguage(language);

		ArrayList<TvShow> results = new ArrayList<TvShow>();

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + imdbId + "?language=" + language + "&external_source=imdb_id&api_key=" + mTmdbApiKey);
			JSONArray array = jObject.getJSONArray("tv_results");

			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
			String imageSizeUrl = MizLib.getImageUrlSize(mContext);

			for (int i = 0; i < array.length(); i++) {
				TvShow show = new TvShow();
				show.setTitle(array.getJSONObject(i).getString("name"));
				show.setOriginalTitle(array.getJSONObject(i).getString("original_name"));
				show.setFirstAired(array.getJSONObject(i).getString("first_air_date"));
				show.setDescription(""); // TMDb doesn't support descriptions in search results
				show.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
				show.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
				show.setCoverUrl(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
				results.add(show);
			}
		} catch (JSONException e) {}

		return results;
	}

	@Override
	public TvShow get(String id, String language) {
		language = getLanguage(language);

		TvShow show = new TvShow();
		show.setId("tmdb_" + id); // this is a hack to store the TMDb ID for the show in the database without a separate column for it

		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=" + language + "&append_to_response=credits,images,external_ids");

		// Set title
		show.setTitle(MizLib.getStringFromJSONObject(jObject, "name", ""));

		// Set description
		show.setDescription(MizLib.getStringFromJSONObject(jObject, "overview", ""));

		if (!language.equals("en")) { // This is a localized search - let's fill in the blanks
			JSONObject englishResults = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "?api_key=" + mTmdbApiKey + "&language=en");

			if (TextUtils.isEmpty(show.getTitle()))
				show.setTitle(MizLib.getStringFromJSONObject(englishResults, "name", ""));

			if (TextUtils.isEmpty(show.getDescription()))
				show.setDescription(MizLib.getStringFromJSONObject(englishResults, "overview", ""));
		}

		// Set actors
		try {
			StringBuilder actors = new StringBuilder();

			JSONArray array = jObject.getJSONObject("credits").getJSONArray("cast");
			for (int i = 0; i < array.length(); i++) {
				actors.append(array.getJSONObject(i).getString("name"));
				actors.append("|");
			}

			show.setActors(actors.toString());
		} catch (Exception e) {}

		// Set genres
		try {
			String genres = "";
			for (int i = 0; i < jObject.getJSONArray("genres").length(); i++)
				genres = genres + jObject.getJSONArray("genres").getJSONObject(i).getString("name") + ", ";
			show.setGenres(genres.substring(0, genres.length() - 2));
		} catch (Exception e) {}

		// Set rating
		show.setRating(MizLib.getStringFromJSONObject(jObject, "vote_average", "0.0"));

		// Set cover path
		show.setCoverUrl(baseUrl + MizLib.getImageUrlSize(mContext) + MizLib.getStringFromJSONObject(jObject, "poster_path", ""));

		// Set backdrop path
		show.setBackdropUrl(baseUrl + MizLib.getBackdropUrlSize(mContext) + MizLib.getStringFromJSONObject(jObject, "backdrop_path", ""));

		// Set certification - not available with TMDb
		show.setCertification("");

		try {
			// Set runtime
			show.setRuntime(String.valueOf(jObject.getJSONArray("episode_run_time").getInt(0)));
		} catch (JSONException e) {}

		// Set first aired date
		show.setFirstAired(MizLib.getStringFromJSONObject(jObject, "first_air_date", ""));

		try {
			// Set IMDb ID
			show.setIMDbId(jObject.getJSONObject("external_ids").getString("imdb_id"));
		} catch (JSONException e) {}

		// Trakt.tv
		if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_2))) {
			try {
				com.miz.apis.trakt.Movie movieSummary = Trakt.getMovieSummary(mContext, id);
				double rating = (double) (movieSummary.getRating() / 10);

				if (rating > 0 || show.getRating().equals("0.0"))
					show.setRating(String.valueOf(rating));	
			} catch (Exception e) {}
		}

		// OMDb API / IMDb
		if (getRatingsProvider().equals(mContext.getString(R.string.ratings_option_3))) {
			try {
				jObject = MizLib.getJSONObject(mContext, "http://www.omdbapi.com/?i=" + show.getImdbId());
				double rating = Double.valueOf(MizLib.getStringFromJSONObject(jObject, "imdbRating", "0"));

				if (rating > 0 || show.getRating().equals("0.0"))
					show.setRating(String.valueOf(rating));	
			} catch (Exception e) {}
		}

		// Seasons
		try {
			JSONArray seasons = jObject.getJSONArray("seasons");

			for (int i = 0; i < seasons.length(); i++) {
				Season s = new Season();

				s.setSeason(seasons.getJSONObject(i).getInt("season_number"));
				s.setCoverPath(baseUrl + MizLib.getImageUrlSize(mContext) + MizLib.getStringFromJSONObject(seasons.getJSONObject(i), "poster_path", ""));

				show.addSeason(s);
			}
		} catch (JSONException e) {}

		// Episode details
		for (Season s : show.getSeasons()) {
			jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "?api_key=" + mTmdbApiKey);
			try {
				JSONArray episodes = jObject.getJSONArray("episodes");
				for (int i = 0; i < episodes.length(); i++) {
					Episode ep = new Episode();
					ep.setSeason(s.getSeason());
					ep.setEpisode(episodes.getJSONObject(i).getInt("episode_number"));
					ep.setTitle(episodes.getJSONObject(i).getString("name"));
					ep.setAirdate(episodes.getJSONObject(i).getString("air_date"));
					ep.setDescription(episodes.getJSONObject(i).getString("overview"));
					ep.setRating(MizLib.getStringFromJSONObject(episodes.getJSONObject(i), "vote_average", "0.0"));

					try {
						// This is quite nasty... An HTTP call for each episode, yuck!
						// Sadly, this is needed in order to get proper screenshot URLS
						// and info about director, writer and guest stars
						JSONObject episodeCall = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/season/" + s.getSeason() + "/episode/" + ep.getEpisode() + "?api_key=" + mTmdbApiKey + "&append_to_response=credits,images");

						// Screenshot URL in the correct size
						JSONArray images = episodeCall.getJSONObject("images").getJSONArray("stills");
						if (images.length() > 0) {
							JSONObject firstImage = images.getJSONObject(0);
							int width = firstImage.getInt("width");
							if (width < 500) {
								ep.setScreenshotUrl(baseUrl + "original" + MizLib.getStringFromJSONObject(firstImage, "file_path", ""));
							} else {
								ep.setScreenshotUrl(baseUrl + MizLib.getBackdropThumbUrlSize(mContext) + MizLib.getStringFromJSONObject(firstImage, "file_path", ""));
							}
						}

						try {
							// Guest stars
							StringBuilder actors = new StringBuilder();
							JSONArray guest_stars = episodeCall.getJSONObject("credits").getJSONArray("guest_stars");

							for (int j = 0; j < guest_stars.length(); j++) {
								actors.append(guest_stars.getJSONObject(j).getString("name"));
								actors.append("|");
							}

							ep.setGueststars(actors.toString());
						} catch (Exception e) {}

						try {
							// Crew information
							StringBuilder director = new StringBuilder(), writer = new StringBuilder();
							JSONArray crew = episodeCall.getJSONObject("credits").getJSONArray("crew");

							for (int j = 0; j < crew.length(); j++) {
								if (crew.getJSONObject(j).getString("job").equals("Director")) {
									director.append(crew.getJSONObject(j).getString("name"));
									director.append("|");
								} else if (crew.getJSONObject(j).getString("job").equals("Writer")) {
									writer.append(crew.getJSONObject(j).getString("name"));
									writer.append("|");
								}
							}

							ep.setDirector(director.toString());
							ep.setWriter(writer.toString());
							
						} catch (Exception e) {}

					} catch (Exception e) {}

					show.addEpisode(ep);
				}
			} catch (JSONException e) {}
		}

		return show;
	}

	private ArrayList<TvShow> getListFromUrl(String serviceUrl) {
		ArrayList<TvShow> results = new ArrayList<TvShow>();

		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, serviceUrl);
			JSONArray array = jObject.getJSONArray("results");

			String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
			String imageSizeUrl = MizLib.getImageUrlSize(mContext);

			for (int i = 0; i < array.length(); i++) {
				TvShow show = new TvShow();
				show.setTitle(array.getJSONObject(i).getString("name"));
				show.setOriginalTitle(array.getJSONObject(i).getString("original_name"));
				show.setFirstAired(array.getJSONObject(i).getString("first_air_date"));
				show.setDescription(""); // TMDb doesn't support descriptions in search results
				show.setRating(String.valueOf(array.getJSONObject(i).getDouble("vote_average")));
				show.setId(String.valueOf(array.getJSONObject(i).getInt("id")));
				show.setCoverUrl(baseUrl + imageSizeUrl + array.getJSONObject(i).getString("poster_path"));
				results.add(show);
			}
		} catch (JSONException e) {}

		return results;
	}

	@Override
	public List<String> getCovers(String id) {
		ArrayList<String> covers = new ArrayList<String>();
		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
		
		try {
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/images" + "?api_key=" + mTmdbApiKey);
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
			JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/images" + "?api_key=" + mTmdbApiKey);
			JSONArray jArray = jObject.getJSONArray("backdrops");
			for (int i = 0; i < jArray.length(); i++) {
				covers.add(baseUrl + MizLib.getBackdropThumbUrlSize(mContext) + MizLib.getStringFromJSONObject(jArray.getJSONObject(i), "file_path", ""));
			}
		} catch (JSONException e) {}

		return covers;
	}
	
	/**
	 * Get the language code or a default one if
	 * the supplied one is empty or {@link null}.
	 * @param language
	 * @return Language code
	 */
	@Override
	public String getLanguage(String language) {
		if (TextUtils.isEmpty(language))
			language = "en";
		return language;
	}

	@Override
	public List<TvShow> searchNgram(String query, String language) {
		language = getLanguage(language);

		String serviceUrl = "";

		try {
			serviceUrl = "https://api.themoviedb.org/3/search/tv?query=" + URLEncoder.encode(query, "utf-8") + "&language=" + language + "&search_type=ngram&api_key=" + mTmdbApiKey;
		} catch (UnsupportedEncodingException e) {}

		return getListFromUrl(serviceUrl);
	}

	@Override
	public List<Actor> getActors(String id) {
		ArrayList<Actor> results = new ArrayList<Actor>();

		String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);

		try {
			JSONObject jObject;
			
			if (!id.startsWith("tmdb_")) {
				jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/find/" + id + "?api_key=" + mTmdbApiKey + "&external_source=tvdb_id");
				id = MizLib.getStringFromJSONObject(jObject.getJSONArray("tv_results").getJSONObject(0), "id", "");
			} else {
				id = id.replace("tmdb_", "");
			}
			
			jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/tv/" + id + "/credits?api_key=" + mTmdbApiKey);	
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
}