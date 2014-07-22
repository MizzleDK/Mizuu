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
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

import com.miz.base.MizActivity;
import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBiographyFragment;
import com.miz.mizuu.fragments.ActorMoviesFragment;
import com.miz.mizuu.fragments.ActorPhotosFragment;
import com.miz.mizuu.fragments.ActorTaggedPhotosFragment;
import com.miz.mizuu.fragments.ActorTvShowsFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class Actor extends MizActivity implements OnNavigationListener {

	private ViewPager mViewPager;
	private String mActorId, mActorName, mJson, mBaseUrl, mActorThumb, mTmdbApiKey;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdapter;
	private ActionBar mActionBar;
	private ProgressBar mProgressBar;
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

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);
		
		mActionBarOverlay = (ImageView) findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));

		findViewById(R.id.layout).setBackgroundResource(R.drawable.bg);

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		mProgressBar.setVisibility(View.VISIBLE);

		mActorId = getIntent().getExtras().getString("actorID");
		mActorName = getIntent().getExtras().getString("actorName");
		mActorThumb = getIntent().getExtras().getString("thumb");
		mTmdbApiKey = MizLib.getTmdbApiKey(this);

		setTitle(mActorName);

		if (MizLib.isPortrait(this)) {
			getWindow().setBackgroundDrawableResource(R.drawable.bg);
		}
		
		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mActionBar.setSelectedNavigationItem(position);
				
				updateActionBarDrawable(1, false);
			}
		});

		if (savedInstanceState != null) {	
			mJson = savedInstanceState.getString("json", "");
			mBaseUrl = savedInstanceState.getString("baseUrl");
			mActorThumb = savedInstanceState.getString("mActorThumb");
			setupActionBarStuff();

			mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new ActorLoader(getApplicationContext()).execute(mActorId);
		}
	}
	
	@Subscribe
	public void onScrollChanged(Integer newAlpha) {
		updateActionBarDrawable(newAlpha, true);
	}
	
	private void updateActionBarDrawable(int newAlpha, boolean setBackground) {
		if (mViewPager.getCurrentItem() == 0) { // Details page
			mActionBarOverlay.setVisibility(View.VISIBLE);

			if (setBackground) {
				mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
				mActionBarOverlay.setImageDrawable(mActionBarBackgroundDrawable);
			}
		} else { // Actors page
			mActionBarOverlay.setVisibility(View.GONE);
		}
	}
	
	public void onResume() {
		super.onResume();

		mBus.register(this);
		updateActionBarDrawable(0, true);
	}
	
	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}

	private void setupSpinnerItems() {
		mSpinnerItems.clear();
		mSpinnerItems.add(new SpinnerItem(mActorName, getString(R.string.actorBiography), ActorBiographyFragment.newInstance(mJson, mActorThumb)));
		mSpinnerItems.add(new SpinnerItem(mActorName, getString(R.string.chooserMovies), ActorMoviesFragment.newInstance(mJson, mBaseUrl)));

		try {
			JSONObject j = new JSONObject(mJson);
			if (j.getJSONObject("tv_credits").getJSONArray("cast").length() > 0) {
				mSpinnerItems.add(new SpinnerItem(mActorName, getString(R.string.chooserTVShows), ActorTvShowsFragment.newInstance(mJson, mBaseUrl)));
			}
		} catch (JSONException e) {}

		try {
			JSONObject j = new JSONObject(mJson);
			if (j.getJSONObject("images").getJSONArray("profiles").length() > 0)
				mSpinnerItems.add(new SpinnerItem(mActorName, getString(R.string.actorsShowAllPhotos), ActorPhotosFragment.newInstance(mJson, mActorName, mBaseUrl)));
		} catch (JSONException e) {}

		try {
			JSONObject j = new JSONObject(mJson);
			if (j.getJSONObject("tagged_images").getInt("total_results") > 0)
				mSpinnerItems.add(new SpinnerItem(mActorName, getString(R.string.actorsTaggedPhotos), ActorTaggedPhotosFragment.newInstance(mJson, mActorName, mBaseUrl)));
		} catch (JSONException e) {}

		mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
		outState.putString("json", mJson);
		outState.putString("baseUrl", mBaseUrl);
		outState.putString("mActorThumb", mActorThumb);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ActorDetailsAdapter extends FragmentPagerAdapter {

		public ActorDetailsAdapter(FragmentManager fm) {
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

	private class ActorLoader extends AsyncTask<Object, Object, String> {
		
		private Context mContext;
		
		public ActorLoader(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(Object... params) {
			try {
				mBaseUrl = MizLib.getTmdbImageBaseUrl(mContext);

				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet("https://api.themoviedb.org/3/person/" + params[0] + "?api_key=" + mTmdbApiKey + "&append_to_response=movie_credits,tv_credits,images,tagged_images");
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();

				mJson = httpclient.execute(httppost, responseHandler);

				return mJson;
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
		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdapter == null)
			mSpinnerAdapter = new ActionBarSpinner(getApplicationContext(), mSpinnerItems);

		setTitle(null);

		if (!MizLib.isPortrait(getApplicationContext()))
			findViewById(R.id.layout).setBackgroundResource(0);
		mProgressBar.setVisibility(View.GONE);

		setupSpinnerItems();

		mViewPager.setAdapter(new ActorDetailsAdapter(getSupportFragmentManager()));

		findViewById(R.id.layout).setBackgroundResource(0);
	}
}