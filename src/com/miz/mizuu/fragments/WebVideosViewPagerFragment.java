package com.miz.mizuu.fragments;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.Main;
import com.miz.mizuu.Preferences;
import com.miz.mizuu.R;

public class WebVideosViewPagerFragment extends Fragment implements OnNavigationListener {

	private ActionBar actionBar;
	private ViewPager awesomePager;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;

	public WebVideosViewPagerFragment() {}

	public static WebVideosViewPagerFragment newInstance() {		
		return new WebVideosViewPagerFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		setupSpinnerItems();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Setup ActionBar with the action list
		setupActionBar();
		if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	private void setupActionBar() {
		actionBar = getActivity().getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(getActivity(), spinnerItems);
	}

	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserWebVideos), getString(R.string.choiceYouTube)));
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserWebVideos), getString(R.string.choiceReddit)));
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserWebVideos), getString(R.string.choiceTedTalks)));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		return inflater.inflate(R.layout.viewpager, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		awesomePager = (ViewPager) v.findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(3);
		awesomePager.setAdapter(new WebVideosAdapter(getChildFragmentManager()));
		awesomePager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}

			@Override
			public void onPageSelected(int arg0) {
				actionBar.setSelectedNavigationItem(arg0);
			}
		});

		if (savedInstanceState != null) {
			awesomePager.setCurrentItem(savedInstanceState.getInt("selectedIndex", 0));
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (((Main) getActivity()).isDrawerOpen()) {
			actionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
			((Main) getActivity()).showDrawerOptionsMenu(menu, inflater);
		} else {
			setupActionBar();
			inflater.inflate(R.menu.menuweb, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuSettings:
			startActivity(new Intent(getActivity(), Preferences.class));
			break;
		}

		return super.onOptionsItemSelected(item);	
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("selectedIndex", awesomePager.getCurrentItem());
	}

	private class WebVideosAdapter extends FragmentPagerAdapter {

		public WebVideosAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int index) {
			return WebVideoFragment.newInstance(spinnerItems.get(index).getSubtitle());
		}  

		@Override  
		public int getCount() {
			return spinnerItems.size();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		awesomePager.setCurrentItem(itemPosition);
		return true;
	}
}