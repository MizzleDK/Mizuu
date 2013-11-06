package com.miz.mizuu;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ActorBrowserFragment;
import com.miz.mizuu.fragments.RelatedMoviesFragment;
import com.miz.mizuu.fragments.TmdbMovieDetailsFragment;

public class TMDbMovieDetails extends FragmentActivity implements ActionBar.TabListener {

	private ViewPager awesomePager;
	private String movieId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MizLib.runsInPortraitMode(this))
			setTheme(R.style.Theme_Example_NoBackGround);
		
		if (!MizLib.runsInPortraitMode(this))
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		// Fetch the database ID of the movie to view
		movieId = getIntent().getExtras().getString("tmdbid");

		setTitle(getIntent().getExtras().getString("title"));

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setAdapter(new MovieDetailsAdapter(getSupportFragmentManager()));
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bar.getTabAt(position).select();
				ViewParent root = findViewById(android.R.id.content).getParent();
				MizLib.findAndUpdateSpinner(root, position);
			}
		});

		bar.addTab(bar.newTab()
				.setText(getString(R.string.overview))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.detailsActors))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.relatedMovies))
				.setTabListener(this));
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

		public MovieDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return TmdbMovieDetailsFragment.newInstance(movieId);
			case 1:
				return ActorBrowserFragment.newInstance(movieId, true);
			case 2:
				return RelatedMoviesFragment.newInstance(movieId, true);
			}
			return null;
		}  

		@Override  
		public int getCount() {  
			return 3;
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		awesomePager.setCurrentItem(getActionBar().getSelectedTab().getPosition(), true);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
}