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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.mizuu.R;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Looks for trailers on TMDb for a given movie and starts playback of the first found trailer.
 * If none are found, it'll attempt to look on YouTube using a {@link YoutubeTrailerSearch}.
 * @author Michell
 *
 */
public class TmdbTrailerSearch extends AsyncTask<String, Integer, String> {

	private final String mMovieId, mSearchQuery;
	private final Activity mActivity;
	
	public TmdbTrailerSearch(Activity activity, String movieId, String searchQuery) {
		mActivity = activity;
		mMovieId = movieId;
		mSearchQuery = searchQuery;
	}
	
	@Override
	protected void onPreExecute() {
		Toast.makeText(mActivity, mActivity.getString(R.string.searching), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected String doInBackground(String... params) {
		try {
			JSONObject jObject = MizLib.getJSONObject(mActivity, "https://api.themoviedb.org/3/movie/" + mMovieId + "/trailers?api_key=" + MizLib.getTmdbApiKey(mActivity));
			JSONArray trailers = jObject.getJSONArray("youtube");

			if (trailers.length() > 0)
				return trailers.getJSONObject(0).getString("source");
		} catch (Exception ignored) {}
		
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		if (result != null) {
			if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(mActivity).equals(YouTubeInitializationResult.SUCCESS)) {
				Intent intent = YouTubeStandalonePlayer.createVideoIntent(mActivity, MizLib.getYouTubeApiKey(mActivity), MizLib.getYouTubeId(result), 0, false, true);
				mActivity.startActivity(intent);
			} else {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(result));
			}
		} else {
			new YoutubeTrailerSearch(mActivity, mSearchQuery).execute();
		}
	}
}