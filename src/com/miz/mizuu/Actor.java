package com.miz.mizuu;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.ViewParent;
import android.view.Window;

import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ActorBiographyFragment;
import com.miz.mizuu.fragments.ActorMoviesFragment;
import com.miz.mizuu.fragments.ActorPhotosFragment;
import com.miz.mizuu.R;

public class Actor extends MizActivity implements ActionBar.TabListener {

	private ViewPager awesomePager;
	private String actorId, actorName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MizLib.runsInPortraitMode(this))
			if (isFullscreen())
				setTheme(R.style.Theme_Example_NoBackGround_FullScreen);
			else
				setTheme(R.style.Theme_Example_NoBackGround);

		if (!MizLib.runsInPortraitMode(this))
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		actorId = getIntent().getExtras().getString("actorID");
		actorName = getIntent().getExtras().getString("actorName");

		setTitle(actorName);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setAdapter(new ActorDetailsAdapter(getSupportFragmentManager()));
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bar.getTabAt(position).select();
				ViewParent root = findViewById(android.R.id.content).getParent();
				MizLib.findAndUpdateSpinner(root, position);
			}
		});

		bar.addTab(bar.newTab()
				.setText(getString(R.string.actorBiography))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.chooserMovies))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.actorsShowAllPhotos))
				.setTabListener(this));
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
			switch (index) {
			case 0: return ActorBiographyFragment.newInstance(actorId);
			case 1: return ActorMoviesFragment.newInstance(actorId, true);
			case 2: return ActorPhotosFragment.newInstance(actorId, actorName, true);
			default: return null;
			}
		}  

		@Override  
		public int getCount() { 
			return 3;
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		awesomePager.setCurrentItem(getActionBar().getSelectedTab().getPosition(), true);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
}