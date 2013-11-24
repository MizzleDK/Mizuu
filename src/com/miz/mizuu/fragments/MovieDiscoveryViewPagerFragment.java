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
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.MainMenuActivity;
import com.miz.mizuu.Preferences;
import com.miz.mizuu.R;
import com.miz.mizuu.SearchWebMovies;

public class MovieDiscoveryViewPagerFragment extends Fragment implements OnNavigationListener {

	private ActionBar actionBar;
	private ViewPager awesomePager;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;

	public MovieDiscoveryViewPagerFragment() {}

	public static MovieDiscoveryViewPagerFragment newInstance() {		
		return new MovieDiscoveryViewPagerFragment();
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
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.stringUpcoming)));
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.stringNowPlaying)));
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.stringPopular)));
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.stringTopRated)));
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
		awesomePager.setPageMargin(MizLib.convertDpToPixels(getActivity(), 16));
		awesomePager.setAdapter(new WebVideosAdapter(getChildFragmentManager()));
		awesomePager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}

			@Override
			public void onPageSelected(int arg0) {
				if (isAdded())
					actionBar.setSelectedNavigationItem(arg0);
			}
		});

		if (savedInstanceState != null) {
			awesomePager.setCurrentItem(savedInstanceState.getInt("selectedIndex", 0));
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (((MainMenuActivity) getActivity()).isDrawerOpen()) {
			actionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
			((MainMenuActivity) getActivity()).showDrawerOptionsMenu(menu, inflater);
		} else {
			setupActionBar();
			inflater.inflate(R.menu.menu_web_movies, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuSettings:
			startActivity(new Intent(getActivity(), Preferences.class));
			break;
		case R.id.menuContact:
			MizLib.contactDev(getActivity());
			break;
		case R.id.search_textbox:
			Intent i = new Intent(getActivity().getApplicationContext(), SearchWebMovies.class);
			startActivity(i);
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
			switch (index) {
			case 0: return MovieDiscoveryFragment.newInstance("upcoming");
			case 1: return MovieDiscoveryFragment.newInstance("now_playing");
			case 2: return MovieDiscoveryFragment.newInstance("popular");
			default: return MovieDiscoveryFragment.newInstance("top_rated");
			}
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