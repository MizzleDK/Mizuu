package com.miz.mizuu;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.ViewParent;
import android.widget.Toast;

import com.miz.functions.MizLib;
//import com.miz.mizuu.fragments.CalendarFragment;
import com.miz.mizuu.fragments.MenuListFragment;
import com.miz.mizuu.fragments.WebVideoFragment;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class Calendar extends SlidingFragmentActivity implements ActionBar.TabListener {

	private ViewPager awesomePager;
	private boolean confirmExit, hasTriedOnce = false;
	private String startup;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.viewpager);
		
		setTitle(R.string.calendar);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		confirmExit = settings.getBoolean("prefsConfirmBackPress", false);
		startup = settings.getString("prefsStartup", "1");

		awesomePager = (ViewPager) findViewById(R.id.awesomepager);
		awesomePager.setAdapter(new WebVideosAdapter(getSupportFragmentManager()));

		// set the Behind View
		setBehindContentView(R.layout.frame);
		FragmentTransaction t = getSupportFragmentManager().beginTransaction();
		t.replace(R.id.frame, MenuListFragment.newInstance(5));
		t.commit();

		// customize the SlidingMenu
		this.setSlidingActionBarEnabled(true);
		getSlidingMenu().setShadowWidthRes(R.dimen.shadow_width);
		getSlidingMenu().setShadowDrawable(R.drawable.drawer_shadow);
		getSlidingMenu().setBehindOffsetRes(R.dimen.actionbar_home_width);
		getSlidingMenu().setBehindScrollScale(0.25f);
		getSlidingMenu().setBehindWidth(MizLib.getMenuWidth(this));
		getSlidingMenu().setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				getActionBar().setDisplayHomeAsUpEnabled(false);
			}
		});
		getSlidingMenu().setOnCloseListener(new OnCloseListener() {
			@Override
			public void onClose() {
				getActionBar().setDisplayHomeAsUpEnabled(true);
			}
		});

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		bar.addTab(bar.newTab()
				.setText(getString(R.string.upcomingEpisodes))
				.setTabListener(this));
		bar.addTab(bar.newTab()
				.setText(getString(R.string.chooserTVShows))
				.setTabListener(this));

		awesomePager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bar.getTabAt(position).select();
				ViewParent root = findViewById(android.R.id.content).getParent();
				MizLib.findAndUpdateSpinner(root, position);
			}
		});

		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
	}

	private class WebVideosAdapter extends FragmentPagerAdapter {

		public WebVideosAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {		
			switch (index) {
			case 0:
				//return CalendarFragment.newInstance();
			case 1:
				return WebVideoFragment.newInstance(getString(R.string.choiceReddit));
			default:
				return null;
			}
		}  

		@Override  
		public int getCount() {
			return 2;
		}
	}

	@Override
	public void onBackPressed() {
		if (startup.equals("0") && !getSlidingMenu().isMenuShowing()) { // Welcome screen
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.setClass(getApplicationContext(), Welcome.class);
			startActivity(i);
			finish();
		}

		if (!getSlidingMenu().isMenuShowing() && confirmExit) {
			if (hasTriedOnce) {
				super.onBackPressed();
			} else {
				Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
				hasTriedOnce = true;
			}
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		if (itemId == android.R.id.home) {
			toggle();
			return true;
		} else if (itemId == R.id.menuSettings) {
			startActivity(new Intent(this, Preferences.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}	
	}

	public void contactDev(MenuItem mi) {
		MizLib.contactDev(this);
	}

	@Override
	public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
		awesomePager.setCurrentItem(getActionBar().getSelectedTab().getPosition(), true);

		switch (getActionBar().getSelectedTab().getPosition()) {
		case 0:
			getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
			break;
		default:
			getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
			break;
		}	
	}

	@Override
	public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {}

	@Override
	public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {}
}