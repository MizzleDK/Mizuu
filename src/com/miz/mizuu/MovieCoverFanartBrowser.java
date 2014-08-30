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

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.CollectionCoverSearchFragment;
import com.miz.mizuu.fragments.CoverSearchFragment;
import com.miz.mizuu.fragments.FanartSearchFragment;

public class MovieCoverFanartBrowser extends MizActivity implements OnNavigationListener  {

	private String tmdbId, collectionId, baseUrl = "", json = "", collection = "", mTmdbApiKey;
	private ViewPager awesomePager;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private ActionBar actionBar;
	private ProgressBar pbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MizuuApplication.setupTheme(this);
		
		setContentView(R.layout.viewpager);

		tmdbId = getIntent().getExtras().getString("tmdbId");
		collectionId = getIntent().getExtras().getString("collectionId");
		mTmdbApiKey = MizLib.getTmdbApiKey(this);

		pbar = (ProgressBar) findViewById(R.id.progressbar);
		pbar.setVisibility(View.VISIBLE);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setPageMargin(MizLib.convertDpToPixels(this, 16));
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		if (savedInstanceState != null) {	
			json = savedInstanceState.getString("json", "");
			baseUrl = savedInstanceState.getString("baseUrl");
			collection = savedInstanceState.getString("collection");
			setupActionBarStuff();

			awesomePager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			new MovieLoader(getApplicationContext()).execute(tmdbId, collectionId);
		}
	}

	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(getString(R.string.browseMedia), getString(R.string.coverart)));
		spinnerItems.add(new SpinnerItem(getString(R.string.browseMedia), getString(R.string.backdrop)));
		spinnerItems.add(new SpinnerItem(getString(R.string.browseMedia), getString(R.string.collectionart)));

		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", awesomePager.getCurrentItem());
		outState.putString("json", json);
		outState.putString("baseUrl", baseUrl);
		outState.putString("collection", collection);
	}

	private class PagerAdapter extends FragmentPagerAdapter {

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return CoverSearchFragment.newInstance(tmdbId, json, baseUrl);
			case 1:
				return FanartSearchFragment.newInstance(tmdbId, json, baseUrl);
			default:
				return CollectionCoverSearchFragment.newInstance(collectionId, collection, baseUrl);
			}
		}  

		@Override  
		public int getCount() {
			if (!MizLib.isValidTmdbId(collectionId))
				return 2;
			return 3;  
		}
	}

	private class MovieLoader extends AsyncTask<Object, Object, String> {
		
		private Context mContext;
		
		public MovieLoader(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(Object... params) {			
			try {				
				baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
				
				json = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + params[0] + "/images?api_key=" + mTmdbApiKey).toString();

				if (MizLib.isValidTmdbId(collectionId)) {
					collection = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/collection/" + params[1] + "/images?api_key=" + mTmdbApiKey).toString();
				}

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

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		awesomePager.setCurrentItem(itemPosition);
		return true;
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

		awesomePager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
		setupSpinnerItems();
	}
}