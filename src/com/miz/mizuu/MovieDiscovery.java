package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.miz.mizuu.fragments.MovieDiscoveryViewPagerFragment;

public class MovieDiscovery extends MainMenuActivity {

	private static String TAG = "MovieDiscoveryViewPagerFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(null);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.content_frame, MovieDiscoveryViewPagerFragment.newInstance(), TAG);
			ft.commit();
		}
		
		selectListIndex(WEB_MOVIES);
	}
}