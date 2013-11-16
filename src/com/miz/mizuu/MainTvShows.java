package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.miz.mizuu.fragments.TvShowLibraryFragment;
import com.miz.mizuu.R;

public class MainTvShows extends MainMenuActivity {

	private static String TAG = "TvShowLibraryFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(null);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.content_frame, TvShowLibraryFragment.newInstance(), TAG);
			ft.commit();
		}
		
		selectListIndex(SHOWS);
	}
}