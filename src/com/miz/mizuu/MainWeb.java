package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.miz.mizuu.fragments.WebVideosViewPagerFragment;

public class MainWeb extends MainMenuActivity {

	private static String TAG = "WebVideosViewPagerFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(null);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.content_frame, WebVideosViewPagerFragment.newInstance(), TAG);
			ft.commit();
		}
		
		selectListIndex(WEB_VIDEOS);
	}
}