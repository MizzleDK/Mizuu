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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.miz.base.MizActivity;
import com.miz.functions.IntentKeys;
import com.miz.mizuu.fragments.ActorBrowserFragment;
import com.miz.utils.ViewUtils;

public class ActorBrowser extends MizActivity {

	private static String TAG = "ActorBrowserFragment";
    private int mToolbarColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String movieId = getIntent().getExtras().getString("movieId");
		String title = getIntent().getExtras().getString("title");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

        getSupportActionBar().setSubtitle(title);

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null && savedInstanceState == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content, ActorBrowserFragment.newInstance(movieId), TAG);
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

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.empty_layout_with_toolbar;
	}
}
