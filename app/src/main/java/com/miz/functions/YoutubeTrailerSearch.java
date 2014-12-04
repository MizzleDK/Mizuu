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
 * Searches YouTube for a given query and starts playback of the given
 * @author Michell
 *
 */
public class YoutubeTrailerSearch extends AsyncTask<Void, Void, String> {

	private final String mSearchQuery;
	private final Activity mActivity;
	
	public YoutubeTrailerSearch(Activity activity, String searchQuery) {
		mActivity = activity;
		mSearchQuery = searchQuery;
	}
	
	@Override
	protected String doInBackground(Void... params) {
		try {
			JSONObject jObject = MizLib.getJSONObject(mActivity, "https://gdata.youtube.com/feeds/api/videos?q=" + mSearchQuery + "&max-results=20&alt=json&v=2");
			JSONObject jdata = jObject.getJSONObject("feed");
			JSONArray aitems = jdata.getJSONArray("entry");

			for (int i = 0; i < aitems.length(); i++) {
				JSONObject item0 = aitems.getJSONObject(i);

				// Check if the video can be embedded or viewed on a mobile device
				boolean embedding = false, mobile = false;
				JSONArray access = item0.getJSONArray("yt$accessControl");

				for (int j = 0; j < access.length(); j++) {
					if (access.getJSONObject(i).getString("action").equals("embed"))
						embedding = access.getJSONObject(i).getString("permission").equals("allowed");

					if (access.getJSONObject(i).getString("action").equals("syndicate"))
						mobile = access.getJSONObject(i).getString("permission").equals("allowed");
				}

				// Add the video ID if it's accessible from a mobile device
				if (embedding || mobile) {
					JSONObject id = item0.getJSONObject("id");
					String fullYTlink = id.getString("$t");
					return fullYTlink.substring(fullYTlink.lastIndexOf("video:") + 6);
				}
			}
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
			Toast.makeText(mActivity, mActivity.getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
		}
	}

}
