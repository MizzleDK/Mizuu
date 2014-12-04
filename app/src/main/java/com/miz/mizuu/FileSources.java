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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
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

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterSources;
import com.miz.functions.FileSource;
import com.miz.utils.TypefaceUtils;

import java.util.ArrayList;

public class FileSources extends MizActivity {

	private DbAdapterSources mDatabase = MizuuApplication.getSourcesAdapter();
	private ArrayList<FileSourceListItem> mItems = new ArrayList<FileSourceListItem>();
	private ListView mListView;
	private LinearLayout mEmptyView;
	private ListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mEmptyView = (LinearLayout) findViewById(R.id.noFileSources);
		mListView = (ListView) findViewById(R.id.listView1);

		mAdapter = new ListAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mEmptyView);

		loadSources();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-filesource-change"));
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.filesources_layout;
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
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
		ArrayList<FileSource> sources = new ArrayList<FileSource>();
		boolean hasMovies = false, hasShows = false;

		// Fetch all movie sources and add them to the array
		Cursor cursor = mDatabase.fetchAllSources();
		try {
			while (cursor.moveToNext()) {
				FileSource fs = new FileSource(
						cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
						cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
						cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
						);
				sources.add(fs);
				if (fs.isMovie())
					hasMovies = true;
				if (!fs.isMovie())
					hasShows = true;
			}
		} catch (Exception ignored) {
		} finally {
			cursor.close();
		}

		mItems.clear();

		if (hasMovies) {
			mItems.add(new FileSourceListItem(null, getString(R.string.chooserMovies), true));

			for (int i = 0; i < sources.size(); i++) {
				if (sources.get(i).isMovie())
					mItems.add(new FileSourceListItem(sources.get(i), sources.get(i).getTitle(), false));
			}
		}

		if (hasShows) {
			mItems.add(new FileSourceListItem(null, getString(R.string.chooserTVShows), true));

			for (int i = 0; i < sources.size(); i++) {
				if (!sources.get(i).isMovie())
					mItems.add(new FileSourceListItem(sources.get(i), sources.get(i).getTitle(), false));
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	public void removeSelectedSource(int id) {
		mDatabase.deleteSource(mItems.get(id).getFileSource().getRowId());
		loadSources();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.filebrowser, menu);
		return true;
	}

	public class ListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public ListAdapter() {
			mInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return mItems.get(position).isHeader() ? 0 : 1;
		}

		@Override
		public boolean isEnabled(int position) {
			return !mItems.get(position).isHeader();
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			if (mItems.get(position).isHeader()) {
				convertView = mInflater.inflate(R.layout.file_source_list_header, parent, false);
				TextView title = (TextView) convertView.findViewById(R.id.title);
				title.setText(mItems.get(position).getTitle());
				title.setTypeface(TypefaceUtils.getRobotoMedium(getApplicationContext()));
			} else {
				convertView = mInflater.inflate(R.layout.filesource_list, parent, false);
				((TextView) convertView.findViewById(R.id.txtListTitle)).setText(mItems.get(position).getTitle());
                ((TextView) convertView.findViewById(R.id.txtListTitle)).setTypeface(TypefaceUtils.getRobotoCondensedRegular(getApplicationContext()));
				((TextView) convertView.findViewById(R.id.txtListPlot)).setText(mItems.get(position).getFileSource().getFilepath());
                ((TextView) convertView.findViewById(R.id.txtListPlot)).setTypeface(TypefaceUtils.getRobotoLight(getApplicationContext()));
				((ImageView) convertView.findViewById(R.id.traktIcon)).setImageResource(mItems.get(position).getFileSource().isMovie() ? R.drawable.ic_movie_white_24dp : R.drawable.ic_tv_white_24dp);
				convertView.findViewById(R.id.imageView2).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeSelectedSource(position);
                    }
                });
			}

			return convertView;
		}
	}

	private class FileSourceListItem {	
		private FileSource mFileSource;
		private String mTitle;
		private boolean mHeader;

		public FileSourceListItem(FileSource fileSource, String title, boolean header) {
			mFileSource = fileSource;
			mTitle = title;
			mHeader = header;
		}

		public FileSource getFileSource() {
			return mFileSource;
		}

		public String getTitle() {
			return mTitle;
		}

		public boolean isHeader() {
			return mHeader;
		}
	}

}