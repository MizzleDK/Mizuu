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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.MovieDetailsFragment;
import com.miz.utils.ViewUtils;

public class MovieDetails extends MizActivity {

	private static String TAG = "MovieDetailsFragment";
	private String mMovieId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme
        setTheme(R.style.Mizuu_Theme_NoBackground);

        ViewUtils.setupWindowFlagsForStatusbarOverlay(getWindow(), true);

		setTitle(null);

		// Fetch the database ID of the movie to view
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			mMovieId = getIntent().getStringExtra(SearchManager.EXTRA_DATA_KEY);
		} else {
			mMovieId = getIntent().getExtras().getString("tmdbId");
		}

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, MovieDetailsFragment.newInstance(mMovieId), TAG);
			ft.commit();
		}
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
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == 4) {
			Toast.makeText(this, getString(R.string.updatedMovie), Toast.LENGTH_SHORT).show();

			// Create a new Intent with the Bundle
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), MovieDetails.class);
			intent.putExtra("tmdbId", mMovieId);

			// Start the Intent for result
			startActivity(intent);

			finish();
		}
	}

	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			// Nasty...
			((MovieDetailsFragment) getSupportFragmentManager().findFragmentByTag(TAG)).onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected int getLayoutResource() {
		return 0;
	}
}