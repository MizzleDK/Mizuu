
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
