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

package com.miz.apis.trakt;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowEpisode;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

import static com.miz.functions.PreferenceKeys.SYNC_WITH_TRAKT;
import static com.miz.functions.PreferenceKeys.TRAKT_PASSWORD;
import static com.miz.functions.PreferenceKeys.TRAKT_USERNAME;

public class Trakt {
	
	private Trakt() {} // No instantiation
	
	public static String getApiKey(Context context) {
		String key = context.getString(R.string.trakt_api_key);
		if (TextUtils.isEmpty(key) || key.equals("add_your_own"))
			throw new RuntimeException("You need to add a Trakt API key!");
		return key;
	}
	
	public static boolean performMovieCheckin(String tmdbId, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			// Cancel any current check-in
			Request request = MizLib.getTraktAuthenticationRequest("http://api.trakt.tv/movie/cancelcheckin/" + getApiKey(c), username, password);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			if (!response.isSuccessful())
				return false;
		} catch (Exception e) {
			return false;
		}

		try {
			// Perform the new check-in
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			holder.put("tmdb_id", tmdbId);
			holder.put("app_version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);
			holder.put("app_date", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/movie/checkin/" + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean performMovieCheckin(Movie movie, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			// Cancel any current check-in
			Request request = MizLib.getTraktAuthenticationRequest("http://api.trakt.tv/movie/cancelcheckin/" + getApiKey(c), username, password);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			if (!response.isSuccessful())
				return false;
		} catch (Exception e) {
			return false;
		}

		try {
			// Perform the new check-in
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			holder.put("imdb_id", movie.getImdbId());
			holder.put("tmdb_id", movie.getImdbId());
			holder.put("title", movie.getTitle());
			holder.put("year", movie.getReleaseYear().replace("(", "").replace(")", ""));
			holder.put("app_version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);
			holder.put("app_date", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/movie/checkin/" + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean performEpisodeCheckin(TvShowEpisode episode, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			// Cancel any current check-in
			Request request = MizLib.getTraktAuthenticationRequest("http://api.trakt.tv/show/cancelcheckin/" + getApiKey(c), username, password);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			if (!response.isSuccessful())
				return false;
		} catch (Exception e) {
			return false;
		}

		try {
			// Perform the new check-in
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			holder.put("tvdb_id", episode.getShowId());
			holder.put("title", "");
			holder.put("year", "");
			holder.put("season", episode.getSeason());
			holder.put("episode", episode.getEpisode());
			holder.put("app_version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);
			holder.put("app_date", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/show/checkin/" + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean markMovieAsWatched(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
			return false;

		try {
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			
			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				array.put(jsonMovie);
			}
			holder.put("movies", array);

			Request request = MizLib.getJsonPostRequest((movies.get(0).hasWatched() ? "http://api.trakt.tv/movie/seen/" : "http://api.trakt.tv/movie/unseen/") + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

    public static boolean markMoviesAsWatched(List<MediumMovie> movies, Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        String username = settings.getString(TRAKT_USERNAME, "").trim();
        String password = settings.getString(TRAKT_PASSWORD, "");

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
            return false;

        try {
            JSONObject holder = new JSONObject();
            holder.put("username", username);
            holder.put("password", password);

            JSONArray array = new JSONArray();
            int count = movies.size();
            for (int i = 0; i < count; i++) {
                JSONObject jsonMovie = new JSONObject();
                jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
                jsonMovie.put("year", movies.get(i).getReleaseYear());
                jsonMovie.put("title", movies.get(i).getTitle());
                array.put(jsonMovie);
            }
            holder.put("movies", array);

            Request request = MizLib.getJsonPostRequest((movies.get(0).hasWatched() ? "http://api.trakt.tv/movie/seen/" : "http://api.trakt.tv/movie/unseen/") + getApiKey(c), holder);
            Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

	public static boolean changeSeasonWatchedStatus(String showId, int season, Context c, boolean watched) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			holder.put("imdb_id", "");
			holder.put("tvdb_id", showId);
			holder.put("title", "");
			holder.put("year", "");
			holder.put("season", season);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/show/season/" + (!watched ? "un" : "") + "seen/" + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean markEpisodeAsWatched(String showId, List<com.miz.functions.TvShowEpisode> episodes, Context c, boolean watched) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || !settings.getBoolean(SYNC_WITH_TRAKT, false) || episodes.size() == 0)
			return false;

		try {
			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);
			holder.put("imdb_id", "");
			holder.put("tvdb_id", showId);
			holder.put("title", "");
			holder.put("year", "");

			JSONArray array = new JSONArray();
			int count = episodes.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("season", episodes.get(i).getSeason());
				jsonMovie.put("episode", episodes.get(i).getEpisode());
				array.put(jsonMovie);
			}
			holder.put("episodes", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/show/episode/" + (!watched ? "un" : "") + "seen/" + getApiKey(c), holder);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();

			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean markTvShowAsWatched(TraktTvShow show, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);
			json.put("tvdb_id", show.getId());
			json.put("title", show.getTitle());

			JSONArray array = new JSONArray();
			for (String season : show.getSeasons().keySet()) {
				Collection<String> episodes = show.getSeasons().get(season);
				for (String episode : episodes) {
					JSONObject jsonShow = new JSONObject();
					jsonShow.put("season", season);
					jsonShow.put("episode", episode);
					array.put(jsonShow);
				}
			}
			json.put("episodes", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/show/episode/seen/" + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean addMoviesToLibrary(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				array.put(jsonMovie);
			}
			json.put("movies", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/movie/library/" + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean movieWatchlist(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				array.put(jsonMovie);
			}
			json.put("movies", array);

			Request request = MizLib.getJsonPostRequest((movies.get(0).toWatch() ? "http://api.trakt.tv/movie/watchlist/" : "http://api.trakt.tv/movie/unwatchlist/") + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

    public static boolean moviesWatchlist(List<MediumMovie> movies, Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        String username = settings.getString(TRAKT_USERNAME, "").trim();
        String password = settings.getString(TRAKT_PASSWORD, "");

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
            return false;

        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);

            JSONArray array = new JSONArray();
            int count = movies.size();
            for (int i = 0; i < count; i++) {
                JSONObject jsonMovie = new JSONObject();
                jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
                jsonMovie.put("year", movies.get(i).getReleaseYear());
                jsonMovie.put("title", movies.get(i).getTitle());
                array.put(jsonMovie);
            }
            json.put("movies", array);

            Request request = MizLib.getJsonPostRequest((movies.get(0).toWatch() ? "http://api.trakt.tv/movie/watchlist/" : "http://api.trakt.tv/movie/unwatchlist/") + getApiKey(c), json);
            Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

	public static boolean movieFavorite(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				jsonMovie.put("rating", movies.get(i).isFavourite() ? "love" : "unrate");
				array.put(jsonMovie);
			}
			json.put("movies", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/rate/movies/" + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

    public static boolean moviesFavorite(List<MediumMovie> movies, Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        String username = settings.getString(TRAKT_USERNAME, "").trim();
        String password = settings.getString(TRAKT_PASSWORD, "");

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || movies.size() == 0)
            return false;

        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);

            JSONArray array = new JSONArray();
            int count = movies.size();
            for (int i = 0; i < count; i++) {
                JSONObject jsonMovie = new JSONObject();
                jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
                jsonMovie.put("year", movies.get(i).getReleaseYear());
                jsonMovie.put("title", movies.get(i).getTitle());
                jsonMovie.put("rating", movies.get(i).isFavourite() ? "love" : "unrate");
                array.put(jsonMovie);
            }
            json.put("movies", array);

            Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/rate/movies/" + getApiKey(c), json);
            Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

	public static boolean hasTraktAccount(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

        return !(TextUtils.isEmpty(username) || TextUtils.isEmpty(password));
    }

	public static boolean addTvShowToLibrary(TraktTvShow show, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);
			json.put("tvdb_id", show.getId());
			json.put("title", show.getTitle());

			JSONArray array = new JSONArray();
			for (String season : show.getSeasons().keySet()) {
				Collection<String> episodes = show.getSeasons().get(season);
				for (String episode : episodes) {
					JSONObject jsonShow = new JSONObject();
					jsonShow.put("season", season);
					jsonShow.put("episode", episode);
					array.put(jsonShow);
				}
			}
			json.put("episodes", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/show/episode/library/" + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean tvShowFavorite(List<TvShow> shows, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || shows.size() == 0)
			return false;

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = shows.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonShow = new JSONObject();
				jsonShow.put("tvdb_id", shows.get(i).getId());
				jsonShow.put("title", shows.get(i).getTitle());
				jsonShow.put("rating", shows.get(i).isFavorite() ? "love" : "unrate");
				array.put(jsonShow);
			}
			json.put("shows", array);

			Request request = MizLib.getJsonPostRequest("http://api.trakt.tv/rate/shows/" + getApiKey(c), json);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	public static int WATCHED = 1, RATINGS = 2, WATCHLIST = 3, COLLECTION = 4;
	public static JSONArray getMovieLibrary(Context c, int type) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return new JSONArray();

		try {
			String url = "";
			if (type == WATCHED) {
				url = "http://api.trakt.tv/user/library/movies/watched.json/" + getApiKey(c) + "/" + username;
			} else if (type == RATINGS) {
				url = "http://api.trakt.tv/user/ratings/movies.json/" + getApiKey(c) + "/" + username + "/love";
			} else if (type == WATCHLIST) {
				url = "http://api.trakt.tv/user/watchlist/movies.json/" + getApiKey(c) + "/" + username;
			} else if (type == COLLECTION) {
				url = "http://api.trakt.tv/user/library/movies/collection.json/" + getApiKey(c) + "/" + username;
			}

			Request request = MizLib.getTraktAuthenticationRequest(url, username, password);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			
			if (response.isSuccessful())
				return new JSONArray(response.body().string());
			return new JSONArray();
		} catch (Exception e) {
			return new JSONArray();
		}
	}

	public static JSONArray getTvShowLibrary(Context c, int type) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString(TRAKT_USERNAME, "").trim();
		String password = settings.getString(TRAKT_PASSWORD, "");

		if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
			return new JSONArray();

		try {
			String url = "";
			if (type == WATCHED) {
				url = "http://api.trakt.tv/user/library/shows/watched.json/" + getApiKey(c) + "/" + username;
			} else if (type == RATINGS) {
				url = "http://api.trakt.tv/user/ratings/shows.json/" + getApiKey(c) + "/" + username + "/love";
			} else if (type == COLLECTION) {
				url = "http://api.trakt.tv/user/library/shows/collection.json/" + getApiKey(c) + "/" + username;
			}

			Request request = MizLib.getTraktAuthenticationRequest(url, username, password);
			Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
			
			if (response.isSuccessful())
				return new JSONArray(response.body().string());
			return new JSONArray();
		} catch (Exception e) {
			return new JSONArray();
		}
	}
	
	public static Show getShowSummary(Context context, String showId) {
		return new Show(MizLib.getJSONObject(context, "http://api.trakt.tv/show/summary.json/" + getApiKey(context) + "/" + showId));
	}
	
	public static com.miz.apis.trakt.Movie getMovieSummary(Context context, String movieId) {
		return new com.miz.apis.trakt.Movie(MizLib.getJSONObject(context, "http://api.trakt.tv/movie/summary.json/" + getApiKey(context) + "/" + movieId));
	}
}