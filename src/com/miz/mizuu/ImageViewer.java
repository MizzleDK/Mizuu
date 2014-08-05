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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import com.miz.functions.MizLib;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.FrameLayout.LayoutParams;

import com.miz.mizuu.fragments.ActorPhotoFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class ImageViewer extends MizActivity {

	private ViewPager awesomePager;
	private boolean mPortraitPhotos;
	private String[] photos;
	private ImageView mActionBarOverlay;
	private Bus mBus;

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

		mActionBarOverlay = (ImageView) findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));
		mActionBarOverlay.setImageResource(R.drawable.transparent_actionbar);
		mActionBarOverlay.setVisibility(View.VISIBLE);

		getActionBar().setBackgroundDrawable(null);

		mPortraitPhotos = getIntent().getBooleanExtra("portraitPhotos", true);
		photos = getIntent().getStringArrayExtra("photos");

		setTitle(getIntent().getStringExtra("actorName"));
		getActionBar().setSubtitle((getIntent().getIntExtra("selectedIndex", 0) + 1) + " " + getString(R.string.of) + " " + photos.length);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setPageMargin(MizLib.convertDpToPixels(getApplicationContext(), 16));
		awesomePager.setAdapter(new ActorPhotosAdapter(getSupportFragmentManager()));
		awesomePager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}

			@Override
			public void onPageSelected(int arg0) {
				getActionBar().setSubtitle((arg0 + 1) + " " + getString(R.string.of) + " " + photos.length);
			}

		});
		awesomePager.setCurrentItem(getIntent().getIntExtra("selectedIndex", 0));



		View decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				if ((visibility & View.SYSTEM_UI_FLAG_VISIBLE) == 0) {
					getActionBar().show();
					mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(getApplicationContext()) : MizLib.getActionBarAndStatusBarHeight(getApplicationContext())));
				}
			}
		});
	}

	@Subscribe
	public void tappedImage(Boolean visible) {
		View decorView = getWindow().getDecorView();
		int uiOptions = 0;

		if (visible) {
			if (MizLib.hasICS())
				uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
			mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));
		} else {
			if (MizLib.hasICS())
				uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN;

			if (MizLib.hasKitKat())
				mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, MizLib.convertDpToPixels(this, 25)));
		}

		if (MizLib.hasICS())
			decorView.setSystemUiVisibility(uiOptions);		
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
			return ActorPhotoFragment.newInstance(photos[index], mPortraitPhotos);
		}  

		@Override  
		public int getCount() { 
			return photos.length;
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
		i.setData(Uri.parse(photos[awesomePager.getCurrentItem()]));
		startActivity(i);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
}