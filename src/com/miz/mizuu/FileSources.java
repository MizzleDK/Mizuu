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

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterSources;

import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.functions.FileSource;

public class FileSources extends MizActivity {

	private ArrayList<FileSource> sources = new ArrayList<FileSource>();
	private ListView list;
	private LinearLayout noFileSources;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filesources_layout);

		noFileSources = (LinearLayout) findViewById(R.id.noFileSources);
		list = (ListView) findViewById(R.id.listView1);
		list.setAdapter(new ListAdapter());

		loadSources();
		
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-filesource-change"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadSources();
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.add_source:
			addFileSource();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void addFileSource() {
		Intent i = new Intent(this, AddFileSource.class);
		startActivity(i);
	}

	private void loadSources() {
		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();

		sources.clear();

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

		if (sources.size() == 0)
			noFileSources.setVisibility(View.VISIBLE);
		else
			noFileSources.setVisibility(View.GONE);

		notifyDataSetChanged();
	}

	public void removeSelectedSource(final int id) {
		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
		dbHelper.deleteSource(sources.get(id).getRowId());

		loadSources();
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.filebrowser, menu);
		return true;
	}

	private void notifyDataSetChanged() {
		try {
			list.setAdapter(new ListAdapter());
		} catch (Exception e) {}
	}

	static class ViewHolder {
		TextView folderName, fullPath;
		ImageView remove, icon;
	}

	public class ListAdapter extends BaseAdapter {
		private LayoutInflater inflater;

		public ListAdapter() {
			inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return sources.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.filesource_list, null);

				holder = new ViewHolder();
				holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.folderName.setTextAppearance(FileSources.this, android.R.style.TextAppearance_Medium);
				holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
				holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.folderName.setText(sources.get(position).getTitle());
			holder.fullPath.setText(sources.get(position).getFilepath());

			if (sources.get(position).isMovie())
				holder.icon.setImageResource(R.drawable.ic_action_movie);
			else
				holder.icon.setImageResource(R.drawable.ic_action_tv);

			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedSource(position);
				}
			});

			return convertView;
		}
	}
}