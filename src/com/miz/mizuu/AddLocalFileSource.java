package com.miz.mizuu;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.miz.mizuu.fragments.FileBrowserFragment;

public class AddLocalFileSource extends FragmentActivity {

	private static String TAG = "FileBrowserFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, FileBrowserFragment.newInstance(
					Environment.getExternalStorageDirectory().getAbsolutePath(),
					getIntent().getExtras().getString("type").equals("movie")
					), TAG);
			ft.commit();
		}	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	public void onBackPressed() {
		FileBrowserFragment frag = (FileBrowserFragment) getSupportFragmentManager().findFragmentByTag(TAG);
		frag.onBackPressed();
	}
}