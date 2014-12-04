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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.miz.db.DbAdapterSources;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import static com.miz.functions.MizLib.DOMAIN;
import static com.miz.functions.MizLib.FILESOURCE;
import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.PASSWORD;
import static com.miz.functions.MizLib.SERVER;
import static com.miz.functions.MizLib.TV_SHOW;
import static com.miz.functions.MizLib.TYPE;
import static com.miz.functions.MizLib.USER;

public class AddNetworkFilesourceDialog extends Activity {

	private EditText server, domain, username, password;
	private CheckBox anonymous, guest;
	private String mDomain, mUser, mPass, mServer;
	private boolean isMovie = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.addnetwork);

		server = (EditText) findViewById(R.id.server);
		domain = (EditText) findViewById(R.id.domain);
		username = (EditText) findViewById(R.id.username);
		password  = (EditText) findViewById(R.id.password);
		password.setTypeface(Typeface.DEFAULT);
		password.setTransformationMethod(new PasswordTransformationMethod());
		anonymous = (CheckBox) findViewById(R.id.checkBox);
		guest = (CheckBox) findViewById(R.id.checkBox2);

		guest.setOnCheckedChangeListener(changeListener);
		anonymous.setOnCheckedChangeListener(changeListener);

		isMovie = getIntent().getExtras().getString("type").equals("movie");

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-network-search"));
	}

	OnCheckedChangeListener changeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			username.setEnabled(!isChecked);
			password.setEnabled(!isChecked);
			domain.setEnabled(!isChecked);

			if (buttonView.getId() == R.id.checkBox) { // anonymous

				username.setText("");
				password.setText("");
				domain.setText("");

				if (guest.isChecked())
					guest.setChecked(false);
			} else { // guest

				if (isChecked) {
					username.setText("guest");
					password.setText("");
					domain.setText("");
				} else {
					username.setText("");
					password.setText("");
					domain.setText("");
				}

				if (anonymous.isChecked())
					anonymous.setChecked(!anonymous.isChecked());
			}
		}
	};

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String ip = intent.getExtras().getString("ip");
			server.setText(ip);
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@SuppressLint("UseSparseArrays")
	public void search(View v) {
		final ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();

		// Fetch all movie sources and add them to the array
		Cursor cursor = dbHelper.fetchAllSources();
		while (cursor.moveToNext()) {
			if (cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)) == FileSource.SMB)
				sources.add(new FileSource(
						cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
						cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
						));
		}

		cursor.close();

		TreeSet<String> uniqueSources = new TreeSet<String>();

		int count = sources.size();
		for (int i = 0; i < count; i++) {
			String temp = sources.get(i).getFilepath().replace("smb://", "");
			temp = temp.substring(0, temp.indexOf("/"));
			uniqueSources.add(temp);
		}

		final CharSequence[] items = new CharSequence[uniqueSources.size() + 1];

		count = 0;
		Iterator<String> it = uniqueSources.iterator();
		while (it.hasNext()) {
			items[count] = it.next();
			count++;
		}

		items[items.length - 1] = getString(R.string.scanForSources);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.browseSources));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == (items.length - 1)) {
					Intent intent = new Intent();
					intent.setClass(getApplicationContext(), SearchForNetworkShares.class);
					startActivity(intent);
				} else {
					showUserDialog(items, which);
				}
			}});
		builder.show();
	}

	private void showUserDialog(final CharSequence[] items, final int which) {

		final ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();

		// Fetch all movie sources and add them to the array
		Cursor cursor = dbHelper.fetchAllSources();
		while (cursor.moveToNext()) {
			sources.add(new FileSource(
					cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
					cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
					));
		}

		cursor.close();

		HashMap<String, String> userPass = new HashMap<String, String>();

		int count = sources.size();
		for (int i = 0; i < count; i++) {
			String temp = sources.get(i).getFilepath().replace("smb://", "");
			temp = temp.substring(0, temp.indexOf("/"));

			if (temp.equals(items[which])) {
				userPass.put((sources.get(i).getUser().isEmpty() ? getString(R.string.anonymous) : sources.get(i).getUser()), sources.get(i).getPassword());
			}
		}

		if (userPass.size() == 1) {	
			String userTemp = userPass.keySet().iterator().next();
			userPass.get(userTemp);

			server.setText(items[which]);
			username.setText((userTemp.equals(getString(R.string.anonymous)) ? "" : userTemp));
			password.setText(userPass.get(userTemp));
		} else {

			final CharSequence[] usernames = new CharSequence[userPass.size()];
			final CharSequence[] passwords = new CharSequence[userPass.size()];
			int i = 0;
			Iterator<String> it = userPass.keySet().iterator();
			while (it.hasNext()) {
				String s = it.next();
				usernames[i] = s;
				passwords[i] = userPass.get(s);
				i++;
			}

			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle(getString(R.string.selectLogin));
			builder2.setItems(usernames, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int choice) {
					server.setText(items[which]);
					username.setText((usernames[choice].equals(getString(R.string.anonymous)) ? "" : usernames[choice]));
					password.setText(passwords[choice]);
				}
			});
			builder2.show();	
		}
	}

	public void cancel(View v) {
		finish();
	}

	public void ok(View v) {	
		if (server.getText().toString().isEmpty()) {
			Toast.makeText(AddNetworkFilesourceDialog.this, getString(R.string.enterNetworkAddress), Toast.LENGTH_LONG).show();
			return;
		}

		if (MizLib.isWifiConnected(this)) {
			mDomain = domain.getText().toString().trim();
			mUser = username.getText().toString().trim();
			mPass = password.getText().toString().trim();
			mServer = server.getText().toString().trim();
			attemptLogin();
		} else
			Toast.makeText(AddNetworkFilesourceDialog.this, getString(R.string.noConnection), Toast.LENGTH_LONG).show();
	}

	private void attemptLogin() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), FileSourceBrowser.class);
		intent.putExtra(USER, mUser);
		intent.putExtra(PASSWORD, mPass);
		intent.putExtra(DOMAIN, mDomain);
		intent.putExtra(SERVER, mServer);
		intent.putExtra(TYPE, isMovie ? MOVIE : TV_SHOW);
		intent.putExtra(FILESOURCE, FileSource.SMB);
		startActivity(intent);
		finish();
	}
}