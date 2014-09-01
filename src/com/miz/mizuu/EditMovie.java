package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.EditMovieFragment;

public class EditMovie extends MizActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String movieId = getIntent().getStringExtra("movieId");
		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, EditMovieFragment.newInstance(movieId)).commit();
		}
	}
	
}
