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

package com.miz.mizuu;

import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBrowserFragment;
import com.miz.mizuu.fragments.RelatedMoviesFragment;
import com.miz.mizuu.fragments.TmdbMovieDetailsFragment;

public class TMDbMovieDetails extends MizActivity implements OnNavigationListener {

	private ViewPager awesomePager;
	private ProgressBar pbar;
	private String movieId, baseUrl = "", json = "";
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private ActionBar actionBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!MizLib.isPortrait(this))
			if (isFullscreen())
				setTheme(R.style.Theme_Example_Transparent_NoBackGround_FullScreen);
			else
				setTheme(R.style.Theme_Example_Transparent_NoBackGround);
		else
			if (isFullscreen())
				setTheme(R.style.Theme_Example_Transparent_FullScreen);
			else
				setTheme(R.style.Theme_Example_Transparent);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		setTitle(getIntent().getExtras().getString("title"));

		// Fetch the database ID of the movie to view
		movieId = getIntent().getExtras().getString("tmdbid");

		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(R.drawable.bg);

		pbar = (ProgressBar) findViewById(R.id.progressbar);
		pbar.setVisibility(View.VISIBLE);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		if (savedInstanceState != null) {	
			json = savedInstanceState.getString("json", "");
			baseUrl = savedInstanceState.getString("baseUrl");
			setupActionBarStuff();

			awesomePager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new MovieLoader().execute(movieId);
		}
	}

	private void setupSpinnerItems() {
		String movieName = getIntent().getExtras().getString("title");
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(movieName, getString(R.string.overview)));
		spinnerItems.add(new SpinnerItem(movieName, getString(R.string.detailsActors)));
		spinnerItems.add(new SpinnerItem(movieName, getString(R.string.relatedMovies)));

		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", awesomePager.getCurrentItem());
		outState.putString("json", json);
		outState.putString("baseUrl", baseUrl);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.detailstrailer, menu);
		return true;
	}

	public void shareMovie(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + movieId);
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public void openInBrowser(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://www.themoviedb.org/movie/" + movieId));
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public void watchTrailer(MenuItem item) {
		Toast.makeText(this, getString(R.string.searching), Toast.LENGTH_SHORT).show();
		new TmdbTrailerSearch().execute(movieId);
	}

	private class TmdbTrailerSearch extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				JSONObject jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/movie/" + params[0] + "/trailers?api_key=" + MizLib.TMDB_API);
				JSONArray trailers = jObject.getJSONArray("youtube");

				if (trailers.length() > 0)
					return trailers.getJSONObject(0).getString("source");
				else
					return null;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getApplicationContext()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(TMDbMovieDetails.this, MizLib.YOUTUBE_API, MizLib.getYouTubeId(result), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(result));
				}
			} else {
				new YoutubeTrailerSearch().execute(getIntent().getExtras().getString("title"));
			}
		}
	}

	private class YoutubeTrailerSearch extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				JSONObject jObject = MizLib.getJSONObject("https://gdata.youtube.com/feeds/api/videos?q=" + params[0] + "&max-results=20&alt=json&v=2");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item0 = aitems.getJSONObject(i);

					// Check if the video can be embedded or viewed on a mobile device
					boolean embedding = false, mobile = false;
					JSONArray access = item0.getJSONArray("yt$accessControl");

					for (int j = 0; j < access.length(); j++) {
						if (access.getJSONObject(i).getString("action").equals("embed"))
							embedding = access.getJSONObject(i).getString("permission").equals("allowed") ? true : false;

						if (access.getJSONObject(i).getString("action").equals("syndicate"))
							mobile = access.getJSONObject(i).getString("permission").equals("allowed") ? true : false;
					}

					// Add the video ID if it's accessible from a mobile device
					if (embedding || mobile) {
						JSONObject id = item0.getJSONObject("id");
						String fullYTlink = id.getString("$t");
						return fullYTlink.substring(fullYTlink.lastIndexOf("video:") + 6);
					}
				}
			} catch (Exception e) {}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getApplicationContext()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(TMDbMovieDetails.this, MizLib.YOUTUBE_API, MizLib.getYouTubeId(result), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(result));
				}
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.share:
			shareMovie(item);
		case R.id.openInBrowser:
			openInBrowser(item);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class MovieDetailsAdapter extends FragmentPagerAdapter {

		private String jsonString;

		public MovieDetailsAdapter(FragmentManager fm, String json) {
			super(fm);
			jsonString = json;
		}

		@Override  
		public Fragment getItem(int index) {			
			switch (index) {
			case 0:
				return TmdbMovieDetailsFragment.newInstance(movieId, jsonString);
			case 1:
				return ActorBrowserFragment.newInstance(movieId, jsonString, baseUrl);
			case 2:
				return RelatedMoviesFragment.newInstance(movieId, true, jsonString, baseUrl);
			}
			return null;
		}  

		@Override  
		public int getCount() {  
			return 3;
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (itemPosition == 0)
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_actionbar));
		else
			actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#aa000000")));
		
		awesomePager.setCurrentItem(itemPosition);
		return true;
	}

	private class MovieLoader extends AsyncTask<Object, Object, String> {
		@Override
		protected String doInBackground(Object... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet("https://api.themoviedb.org/3/configuration?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				baseUrl = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(baseUrl);
				try { baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) { baseUrl = MizLib.TMDB_BASE_URL; }
				
				httppost = new HttpGet("https://api.themoviedb.org/3/movie/" + params[0] + "?api_key=" + MizLib.TMDB_API + "&append_to_response=releases,trailers,images,casts,similar_movies");
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();

				json = httpclient.execute(httppost, responseHandler);
				
				return json;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				setupActionBarStuff();
			} else {
				Toast.makeText(getApplicationContext(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void setupActionBarStuff() {
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(getApplicationContext(), spinnerItems);

		setTitle(null);

		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(0);
		pbar.setVisibility(View.GONE);

		awesomePager.setAdapter(new MovieDetailsAdapter(getSupportFragmentManager(), json));
		setupSpinnerItems();
	}
}