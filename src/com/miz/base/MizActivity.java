
package com.miz.base;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;

@SuppressLint("NewApi")
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
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (MizLib.hasICS() && getActionBar() != null)
			getActionBar().setIcon(R.drawable.white_app_icon);
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
