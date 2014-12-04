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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.functions.Filepath;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.MovieDatabaseUtils;

import java.util.ArrayList;

public class UnidentifiedMovies extends MizActivity {

	private ArrayList<Filepath> mFilepaths = new ArrayList<Filepath>();
	private ListView mList;

	@Override
	protected int getLayoutResource() {
		return R.layout.unidentified_files_layout;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mList = (ListView) findViewById(R.id.listView1);
		mList.setChoiceMode(ListView.CHOICE_MODE_NONE);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent(UnidentifiedMovies.this, IdentifyMovie.class);
				intent.putExtra("fileName", mFilepaths.get(arg2).getFullFilepath());
				startActivityForResult(intent, 0);
			}
		});

		loadData();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadData();
		}
	};

	private void loadData() {
		mFilepaths.clear();

		Cursor cursor = MizuuApplication.getMovieMappingAdapter().getAllUnidentifiedFilepaths();
		try {
			while (cursor.moveToNext()) {
				mFilepaths.add(new Filepath(cursor.getString(cursor.getColumnIndex(DbAdapterMovieMappings.KEY_FILEPATH))));
			}
		} catch (Exception e) {} finally {
			cursor.close();
		}

		mList.setAdapter(new ListAdapter(this));

		enableNoFilesMessage(mFilepaths.size() == 0);
	}

	private void enableNoFilesMessage(boolean showMessage) {
		if (showMessage)
			findViewById(R.id.noFileSourcesTitle).setVisibility(View.VISIBLE);
		else
			findViewById(R.id.noFileSourcesTitle).setVisibility(View.GONE);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.unidentified_files, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();			
			return true;
		case R.id.remove_all:

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.areYouSure))
			.setTitle(getString(R.string.removeAllUnidentifiedFiles))
			.setCancelable(false)
			.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					MovieDatabaseUtils.removeAllUnidentifiedFiles();

					mFilepaths.clear();
					((BaseAdapter) mList.getAdapter()).notifyDataSetChanged();
					enableNoFilesMessage(true);
				}
			})
			.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.create().show();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	static class ViewHolder {
		TextView name, size;
	}

	private class ListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public ListAdapter(Context c) {
			mInflater = LayoutInflater.from(c);
		}

		@Override
		public int getCount() {
			return mFilepaths.size();
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			final Filepath filepath = mFilepaths.get(position);
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.file_list_item, parent, false);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.name.setText(filepath.getFilepathName());
			holder.size.setText(filepath.getFilepath());

			return convertView;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
}