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

package com.miz.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

import static com.miz.functions.PreferenceKeys.FULLSCREEN_TAG;

public abstract class MizActivity extends ActionBarActivity implements OnSharedPreferenceChangeListener {

	public Toolbar mToolbar;
	private boolean mFullscreen = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFullscreen = MizuuApplication.isFullscreen(this);

		if (isFullscreen())
			setTheme(R.style.Mizuu_Theme_FullScreen);

		if (getLayoutResource() > 0) {
            setContentView(getLayoutResource());

            mToolbar = (Toolbar) findViewById(R.id.toolbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
            }
        }

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	protected abstract int getLayoutResource();

	protected boolean isFullscreen() {
		return mFullscreen;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (FULLSCREEN_TAG.equals(key)) {
			mFullscreen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(FULLSCREEN_TAG, false);
		}
	}
}
