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
import android.view.MenuItem;
import android.view.Window;

import com.miz.functions.ActionBarSpinner;
import com.miz.functions.MizLib;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBiographyFragment;
import com.miz.mizuu.fragments.ActorMoviesFragment;
import com.miz.mizuu.fragments.ActorPhotosFragment;
import com.miz.mizuu.R;

public class Actor extends MizActivity implements OnNavigationListener {

	private ViewPager awesomePager;
	private String actorId, actorName;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private ActionBar actionBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MizLib.runsInPortraitMode(this))
			if (isFullscreen())
				setTheme(R.style.Theme_Example_NoBackGround_FullScreen);
			else
				setTheme(R.style.Theme_Example_NoBackGround);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);
		
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner(this, spinnerItems);
		
		setTitle(null);

		actorId = getIntent().getExtras().getString("actorID");
		actorName = getIntent().getExtras().getString("actorName");

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setOffscreenPageLimit(2);
		awesomePager.setAdapter(new ActorDetailsAdapter(getSupportFragmentManager()));
		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});
		
		setupSpinnerItems();
		
		if (savedInstanceState != null) {
			awesomePager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		}
	}
	
	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(actorName, getString(R.string.actorBiography)));
		spinnerItems.add(new SpinnerItem(actorName, getString(R.string.chooserMovies)));
		spinnerItems.add(new SpinnerItem(actorName, getString(R.string.actorsShowAllPhotos)));
		
		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", awesomePager.getCurrentItem());
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
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		awesomePager.setCurrentItem(itemPosition);
		return true;
	}
}