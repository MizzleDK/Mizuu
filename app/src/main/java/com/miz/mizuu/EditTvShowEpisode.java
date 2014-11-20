package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.EditTvShowEpisodeFragment;

public class EditTvShowEpisode extends MizActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String showId = getIntent().getStringExtra("showId");
		final int season = getIntent().getIntExtra("season", -1);
		final int episode = getIntent().getIntExtra("episode", -1);
		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, EditTvShowEpisodeFragment.newInstance(showId, season, episode)).commit();
		}
	}
	
	@Override
	protected int getLayoutResource() {
		return 0;
	}
}
