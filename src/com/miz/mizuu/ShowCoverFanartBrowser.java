package com.miz.mizuu;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.CoverSearchFragmentTv;
import com.miz.mizuu.fragments.FanartSearchFragmentTv;

public class ShowCoverFanartBrowser extends MizActivity implements OnNavigationListener  {

	private String tvdbId;
	private ViewPager awesomePager;
	private ActionBar actionBar;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.viewpager);

		tvdbId = getIntent().getExtras().getString("id");

		actionBar = getActionBar();

		setupSpinnerItems();
		
		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});
		awesomePager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(getApplicationContext(), spinnerItems);

		setTitle(null);
		
		setupSpinnerItems();
		
		if (savedInstanceState != null) {
			getActionBar().setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}
	
	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(getString(R.string.browseMedia), getString(R.string.coverart)));
		spinnerItems.add(new SpinnerItem(getString(R.string.browseMedia), getString(R.string.backdrop)));

		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
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
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		awesomePager.setCurrentItem(itemPosition);
		return true;
	}
}