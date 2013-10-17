package com.miz.mizuu;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;

public class Preferences extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
	public void onBuildHeaders(List<Header> target) {		
		loadHeadersFromResource(R.xml.preference_headers, target);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}