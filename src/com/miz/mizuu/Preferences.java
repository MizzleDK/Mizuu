package com.miz.mizuu;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

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
}