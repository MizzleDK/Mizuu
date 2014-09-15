package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.ActorBrowserTvFragment;

public class ActorBrowserTv extends MizActivity {

	private static String TAG = "ActorBrowserTvFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String showId = getIntent().getExtras().getString("showId");
		String title = getIntent().getExtras().getString("title");
		
		getActionBar().setSubtitle(title);

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null && savedInstanceState == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(android.R.id.content, ActorBrowserTvFragment.newInstance(showId), TAG);
			ft.commit();
		}
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
}
