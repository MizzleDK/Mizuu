package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.miz.base.MizActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.miz.mizuu.fragments.NetworkBrowserFragment;

public class AddNetworkFileSource extends MizActivity {

	private static String TAG = "NetworkBrowserFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, NetworkBrowserFragment.newInstance(
					getIntent().getExtras().getString("server"),
					getIntent().getExtras().getString("user"),
					getIntent().getExtras().getString("pass"),
					getIntent().getExtras().getString("domain"),
					getIntent().getExtras().getBoolean("isMovie")), TAG);
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
	}
}