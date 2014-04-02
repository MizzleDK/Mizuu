package com.miz.mizuu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;

import com.miz.base.MizActivity;
import com.miz.functions.FileSource;
import com.miz.mizuu.fragments.FileSourceBrowserFragment;

import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.TYPE;
import static com.miz.functions.MizLib.FILESOURCE;
import static com.miz.functions.MizLib.DOMAIN;
import static com.miz.functions.MizLib.USER;
import static com.miz.functions.MizLib.PASSWORD;
import static com.miz.functions.MizLib.SERVER;
import static com.miz.functions.MizLib.SERIAL_NUMBER;

public class FileSourceBrowser extends MizActivity {

	private static String TAG = "";
	private boolean isMovie = true;
	private int mFilesource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		isMovie = extras.getString(TYPE).equals(MOVIE);
		mFilesource = extras.getInt(FILESOURCE, FileSource.FILE);
		
		TAG = "source" + mFilesource;

		Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
		if (frag == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
			switch (mFilesource) {
			case FileSource.FILE:
				ft.replace(android.R.id.content, FileSourceBrowserFragment.newInstanceFile(isMovie), TAG);
				break;
			case FileSource.SMB:
				ft.replace(android.R.id.content, FileSourceBrowserFragment.newInstanceSmbFile(extras.getString(SERVER), extras.getString(USER), extras.getString(PASSWORD), extras.getString(DOMAIN), isMovie), TAG);
				break;
			case FileSource.UPNP:
				ft.replace(android.R.id.content, FileSourceBrowserFragment.newInstanceUpnp(extras.getString(SERVER), extras.getString(SERIAL_NUMBER), isMovie), TAG);
				break;
			}
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
	
	@Override
	public void onBackPressed() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("onBackPressed"));
	}
}