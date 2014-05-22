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
import android.support.v4.app.FragmentActivity;

import com.miz.mizuu.R;

public class MizActivity extends FragmentActivity implements OnSharedPreferenceChangeListener {

	private boolean mFullscreen = true;
	public static final String FULLSCREEN_TAG = "prefsFullscreen";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFullscreen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(FULLSCREEN_TAG, false);
		
		if (isFullscreen())
			setTheme(R.style.Theme_Example_FullScreen);
		
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

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
