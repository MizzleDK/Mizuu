package com.miz.mizuu;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.ViewParent;

import com.google.analytics.tracking.android.EasyTracker;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.CoverSearchFragmentTv;
import com.miz.mizuu.fragments.FanartSearchFragmentTv;

public class ShowCoverFanartBrowser extends FragmentActivity implements ActionBar.TabListener  {

	private int coverCount, backdropCount, startPosition;
	private String tvdbId;
	private ViewPager awesomePager;
	private ActionBar bar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.viewpager);

		tvdbId = getIntent().getExtras().getString("id");
		startPosition = getIntent().getExtras().getInt("startPosition");

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

		bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		bar.addTab(bar.newTab()
				.setText(getString(R.string.coverart))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.backdrop))
				.setTabListener(this));

		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bar.getTabAt(position).select();
				ViewParent root = findViewById(android.R.id.content).getParent();
				MizLib.findAndUpdateSpinner(root, position);
			}
		});

		bar.setSelectedNavigationItem(startPosition);
		
		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
			coverCount = savedInstanceState.getInt("coverCount");
			backdropCount = savedInstanceState.getInt("backdropCount");
			displayCount();
		}
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-cover-search-fragment-tv"));
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().containsKey("coverCount")) {
				coverCount = intent.getExtras().getInt("coverCount");
			} else {
				backdropCount = intent.getExtras().getInt("backdropCount");
			}
			displayCount();
		}
	};
	
	private void displayCount() {
		if (awesomePager.getCurrentItem() == 0) {
			bar.setSubtitle(coverCount + " " + getString(R.string.numberOfCovers));
		} else {
			bar.setSubtitle(backdropCount + " " + getString(R.string.numberOfBackdrops));
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
		outState.putInt("coverCount", coverCount);
		outState.putInt("backdropCount", backdropCount);
	}

	private class PagerAdapter extends FragmentPagerAdapter {

		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return CoverSearchFragmentTv.newInstance(tvdbId);
			default:
				return FanartSearchFragmentTv.newInstance(tvdbId);
			}
		}  

		@Override  
		public int getCount() {  
			return 2;  
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		awesomePager.setCurrentItem(getActionBar().getSelectedTab().getPosition(), true);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}