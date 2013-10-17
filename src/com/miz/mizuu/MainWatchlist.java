package com.miz.mizuu;

import com.miz.mizuu.fragments.MovieLibraryFragment;
import com.miz.mizuu.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class MainWatchlist extends MainMenuActivity {

	private static String TAG = "MovieLibraryFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(null);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.content_frame, MovieLibraryFragment.newInstance(MovieLibraryFragment.OTHER), TAG);
			ft.commit();
		}
		
		selectListIndex(WATCHLIST);
	}
}