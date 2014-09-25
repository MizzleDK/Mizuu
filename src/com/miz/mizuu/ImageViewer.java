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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import com.miz.functions.MizLib;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;

import com.miz.mizuu.fragments.ActorPhotoFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class ImageViewer extends MizActivity {

	private ViewPager mViewPager;
	private boolean mPortraitPhotos;
	private String[] mPhotos;
	private Bus mBus;
	private Handler mHandler = new Handler();
	private Runnable mHideSystemUiRunnable = new Runnable() {
		@Override
		public void run() {
			hideSystemUi();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (MizLib.hasKitKat()) {
			if (isFullscreen())
				setTheme(R.style.Mizuu_Theme_Translucent_FullScreen);
			else
				setTheme(R.style.Mizuu_Theme_Translucent);
		} else {
			if (isFullscreen())
				setTheme(R.style.Mizuu_Theme_Transparent_FullScreen);
			else
				setTheme(R.style.Mizuu_Theme_Transparent);
		}

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		mBus = MizuuApplication.getBus();

		getActionBar().setBackgroundDrawable(null);

		mPortraitPhotos = getIntent().getBooleanExtra("portraitPhotos", true);
		mPhotos = getIntent().getStringArrayExtra("photos");

		setTitle((getIntent().getIntExtra("selectedIndex", 0) + 1) + " " + getString(R.string.of) + " " + mPhotos.length);

		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setPageMargin(MizLib.convertDpToPixels(getApplicationContext(), 16));
		mViewPager.setAdapter(new ActorPhotosAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				setTitle((arg0 + 1) + " " + getString(R.string.of) + " " + mPhotos.length);
			}

		});
		mViewPager.setCurrentItem(getIntent().getIntExtra("selectedIndex", 0));

		View decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				if (visibility == 0) {
					// The UI is visible due to user interaction - let's hide it again after three seconds
					mHandler.postDelayed(mHideSystemUiRunnable, 3000);
				}
			}
		});
	}

	@SuppressLint("InlinedApi")
	@Subscribe
	public void tappedImage(Object event) {
		boolean visible = getActionBar().isShowing();

		if (visible) {
			getActionBar().hide();
			hideSystemUi();
		} else {
			showSystemUi();
			getActionBar().show();
		}		
	}

	@SuppressLint("InlinedApi")
	private void hideSystemUi() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE);
		
		mHandler.removeCallbacks(mHideSystemUiRunnable);
	}

	private void showSystemUi() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
	}

	@Override
	public void onResume() {
		super.onResume();

		mBus.register(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.open_in_browser, menu);
		return true;
	}

	private class ActorPhotosAdapter extends FragmentPagerAdapter {

		public ActorPhotosAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			return ActorPhotoFragment.newInstance(mPhotos[index], mPortraitPhotos);
		}  

		@Override  
		public int getCount() { 
			return mPhotos.length;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.openInBrowser:
			openInBrowser();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openInBrowser() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(mPhotos[mViewPager.getCurrentItem()]));
		startActivity(i);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
}