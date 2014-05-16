/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.mizuu;

import static com.miz.functions.MizLib.TYPE;
import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.TV_SHOW;
import static com.miz.functions.MizLib.FILESOURCE;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.miz.base.MizActivity;
import com.miz.functions.FileSource;

public class AddFileSource extends MizActivity {

	private Button mContinue;
	private TextView mContentType, mContentLocation;
	private RadioGroup mContent, mFilesource;
	private Typeface mTypeface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_file_source);

		setTitle(R.string.addFileSourceTitle);

		mTypeface = MizuuApplication.getOrCreateTypeface(this, "RobotoCondensed-Regular.ttf");

		mContentType = (TextView) findViewById(R.id.contentType);
		mContentType.setTypeface(mTypeface);

		mContent = (RadioGroup) findViewById(R.id.content_type);
		mFilesource = (RadioGroup) findViewById(R.id.filesource_type);

		mContentLocation = (TextView) findViewById(R.id.contentLocation);
		mContentLocation.setTypeface(mTypeface);

		mContinue = (Button) findViewById(R.id.continue_button);
		mContinue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.putExtra(TYPE, mContent.getCheckedRadioButtonId() == R.id.content_movie ? MOVIE : TV_SHOW);
				if (mFilesource.getCheckedRadioButtonId() == R.id.source_smb) {
					i.setClass(getApplicationContext(), AddNetworkFilesourceDialog.class);
				} else if (mFilesource.getCheckedRadioButtonId() == R.id.source_upnp) {
					i.setClass(getApplicationContext(), AddUpnpFilesourceDialog.class);
				} else {
					i.setClass(getApplicationContext(), FileSourceBrowser.class);
					i.putExtra(FILESOURCE, mFilesource.getCheckedRadioButtonId() == R.id.source_device ? FileSource.FILE : FileSource.UPNP);
				}
				startActivity(i);
				finish();
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
}
