package com.miz.mizuu;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.FileSourceBrowserFragment;

public class AddFileSource extends MizActivity {

	private Button mContinue;
	private TextView mContentType, mContentLocation;
	private Typeface mTypeface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_file_source);
		
		setTitle(R.string.addFileSourceTitle);
		
		mTypeface = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf");

		mContentType = (TextView) findViewById(R.id.contentType);
		mContentType.setTypeface(mTypeface);
		
		mContentLocation = (TextView) findViewById(R.id.contentLocation);
		mContentLocation.setTypeface(mTypeface);
		
		mContinue = (Button) findViewById(R.id.continue_button);
		mContinue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
				//ft.replace(android.R.id.content, FileSourceBrowserFragment.newInstanceFile());
				ft.replace(android.R.id.content, FileSourceBrowserFragment.newInstanceSmbFile("192.168.1.50", "admin", "Mizzle1", "", true), "test");
				ft.commit();
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("onBackPressed"));
	}
}
