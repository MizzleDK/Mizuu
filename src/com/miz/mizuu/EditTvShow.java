package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.EditTvShowFragment;

public class EditTvShow extends MizActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String showId = getIntent().getStringExtra("showId");
		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, EditTvShowFragment.newInstance(showId)).commit();
		}
	}
	
}
