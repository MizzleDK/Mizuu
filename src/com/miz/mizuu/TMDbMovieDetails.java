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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBrowserFragment;
import com.miz.mizuu.fragments.RelatedMoviesFragment;
import com.miz.mizuu.fragments.TmdbMovieDetailsFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class TMDbMovieDetails extends MizActivity implements OnNavigationListener {

	private ViewPager mViewPager;
	private ProgressBar mProgressBar;
	private String mMovieId, mBaseUrl = "", mJson = "", mTmdbApiKey, mYouTubeApiKey;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdapter;
	private ActionBar mActionBar;
	private Bus mBus;
	private Drawable mActionBarBackgroundDrawable;
	private ImageView mActionBarOverlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBus = MizuuApplication.getBus();
		mBus.register(getApplicationContext());
		
		if (isFullscreen())
			setTheme(R.style.Mizuu_Theme_Transparent_NoBackGround_FullScreen);
		else
			setTheme(R.style.Mizuu_Theme_NoBackGround_Transparent);

		if (MizLib.isPortrait(this)) {
			getWindow().setBackgroundDrawableResource(R.drawable.bg);
		}

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);
		
		mActionBarOverlay = (ImageView) findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));

		mActionBar = getActionBar();
		
		setTitle(getIntent().getExtras().getString("title"));

		// Fetch the database ID of the movie to view
		mMovieId = getIntent().getExtras().getString("tmdbid");
		mTmdbApiKey = MizLib.getTmdbApiKey(this);
		mYouTubeApiKey = MizLib.getYouTubeApiKey(this);

		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(R.drawable.bg);

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		mProgressBar.setVisibility(View.VISIBLE);

		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mActionBar.setSelectedNavigationItem(position);
				
				updateActionBarDrawable(1, false, false);
			}
		});

		if (savedInstanceState != null) {	
			mJson = savedInstanceState.getString("json", "");
			mBaseUrl = savedInstanceState.getString("baseUrl");
			setupActionBarStuff();

			mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new MovieLoader(getApplicationContext()).execute(mMovieId);
		}
	}

	@Subscribe
	public void onScrollChanged(Integer newAlpha) {
		updateActionBarDrawable(newAlpha, true, false);
	}

	private void updateActionBarDrawable(int newAlpha, boolean setBackground, boolean showActionBar) {
		if (mViewPager.getCurrentItem() == 0) { // Details page
			mActionBarOverlay.setVisibility(View.VISIBLE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				if (newAlpha == 0) {
					mActionBar.hide();
					mActionBarOverlay.setVisibility(View.GONE);
				} else
					mActionBar.show();

			if (setBackground) {
				mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
				mActionBarOverlay.setImageDrawable(mActionBarBackgroundDrawable);
			}
		} else { // Actors page
			mActionBarOverlay.setVisibility(View.GONE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				mActionBar.show();
		}
		
		if (showActionBar) {
			mActionBar.show();
		}
	}
	
	public void onResume() {
		super.onResume();

		mBus.register(this);
		updateActionBarDrawable(1, true, true);
	}

	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}
	
	private void setupSpinnerItems() {
		String movieName = getIntent().getExtras().getString("title");
		mSpinnerItems.clear();
		mSpinnerItems.add(new SpinnerItem(movieName, getString(R.string.overview), TmdbMovieDetailsFragment.newInstance(mMovieId, mJson)));
		
		try {
			JSONObject j = new JSONObject(mJson);
			if (j.getJSONObject("casts").getJSONArray("cast").length() > 0) {
				mSpinnerItems.add(new SpinnerItem(movieName, getString(R.string.detailsActors), ActorBrowserFragment.newInstance(mMovieId, mJson, mBaseUrl)));
			}
		} catch (JSONException e) {}
		
		try {
			JSONObject j = new JSONObject(mJson);
			if (j.getJSONObject("similar_movies").getJSONArray("results").length() > 0) {
				mSpinnerItems.add(new SpinnerItem(movieName, getString(R.string.relatedMovies), RelatedMoviesFragment.newInstance(mMovieId, true, mJson, mBaseUrl)));
			}
		} catch (JSONException e) {}

		mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
		outState.putString("json", mJson);
		outState.putString("baseUrl", mBaseUrl);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tmdb_details, menu);
		
		if (MizLib.isTablet(this)) {
			menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.checkIn).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.openInBrowser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		
		if (!Trakt.hasTraktAccount(this))
			menu.findItem(R.id.checkIn).setVisible(false);
		
		return true;
	}

	public void shareMovie(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovieId);
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public void openInBrowser(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovieId));
		startActivity(Intent.createChooser(intent, getString(R.string.openWith)));
	}

	public void watchTrailer(MenuItem item) {
		Toast.makeText(this, getString(R.string.searching), Toast.LENGTH_SHORT).show();
		new TmdbTrailerSearch().execute(mMovieId);
	}

	public void checkIn(MenuItem item) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return Trakt.performMovieCheckin(mMovieId, getApplicationContext());
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result)
					Toast.makeText(getApplicationContext(), getString(R.string.checked_in), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_SHORT).show();
			}
		}.execute();
	}

	private class TmdbTrailerSearch extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				JSONObject jObject = MizLib.getJSONObject(getApplicationContext(), "https://api.themoviedb.org/3/movie/" + params[0] + "/trailers?api_key=" + mTmdbApiKey);
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
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(TMDbMovieDetails.this, mYouTubeApiKey, MizLib.getYouTubeId(result), 0, false, true);
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
				JSONObject jObject = MizLib.getJSONObject(getApplicationContext(), "https://gdata.youtube.com/feeds/api/videos?q=" + params[0] + "&max-results=20&alt=json&v=2");
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
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(TMDbMovieDetails.this, mYouTubeApiKey, MizLib.getYouTubeId(result), 0, false, true);
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
			break;
		case R.id.share:
			shareMovie(item);
			break;
		case R.id.openInBrowser:
			openInBrowser(item);
			break;
		case R.id.checkIn:
			checkIn(item);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class MovieDetailsAdapter extends FragmentPagerAdapter {

		public MovieDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {			
			return mSpinnerItems.get(index).getFragment();
		}  

		@Override  
		public int getCount() {  
			return mSpinnerItems.size();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition);
		return true;
	}

	private class MovieLoader extends AsyncTask<Object, Object, String> {
		
		private Context mContext;
		
		public MovieLoader(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(Object... params) {
			try {
				mBaseUrl = MizLib.getTmdbImageBaseUrl(mContext);
				mJson = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + params[0] + "?api_key=" + mTmdbApiKey + "&append_to_response=releases,trailers,images,casts,similar_movies").toString();
				return "";
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
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdapter == null)
			mSpinnerAdapter = new ActionBarSpinner(getApplicationContext(), mSpinnerItems);

		setTitle(null);

		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(0);
		mProgressBar.setVisibility(View.GONE);

		setupSpinnerItems();
		
		mViewPager.setAdapter(new MovieDetailsAdapter(getSupportFragmentManager()));
	}
}