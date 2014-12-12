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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.functions.Filepath;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TvShowDatabaseUtils;

import java.util.ArrayList;

public class UnidentifiedTvShows extends MizActivity {

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
		mList.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			// Called when the user selects a contextual menu item
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.identify:
					identifySelectedFiles();
					break;
				}

				mode.finish();

				return true;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// Inflate a menu resource providing context menu items
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.unidentified, menu);

				return true;
			}

			// Called when the user exits the action mode
			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mode = null;

				// Unselected the item
				mList.clearFocus();
				mList.clearChoices();
				mList.invalidate();
			}

			// Called each time the action mode is shown. Always called after onCreateActionMode, but
			// may be called multiple times if the mode is invalidated.
			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return true; // Return false if nothing is done
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				mode.setTitle(mList.getCheckedItemCount() + " selected");
			}
		});
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				mList.setItemChecked(arg2, !mList.isItemChecked(arg2));
			}
		});

		loadData();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_LIBRARY));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}
	
	private void identifySelectedFiles() {
		SparseBooleanArray sba = mList.getCheckedItemPositions();
		ArrayList<String> filepaths = new ArrayList<String>();
		for (int i = 0; i < sba.size(); i++) {
			filepaths.add(mFilepaths.get(sba.keyAt(i)).getFullFilepath());
		}

		Intent i = new Intent();
		i.setClass(this, IdentifyTvShowEpisode.class);
        i.putExtra("filepaths", filepaths);
		i.putExtra("showTitle", "");
		i.putExtra("showId", ""); // Unidentified
		startActivity(i);
	}

	private void loadData() {
		mFilepaths.clear();

		Cursor cursor = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllUnidentifiedFilepaths();
		try {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					mFilepaths.add(new Filepath(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH))));
				}
			}
		} catch (Exception ignored) {} finally {
			if (cursor != null)
				cursor.close();
		}

		mList.setAdapter(new ListAdapter(this));

		enableNoFilesMessage(mFilepaths.size() == 0);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadData();
		}
	};

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
					
					TvShowDatabaseUtils.deleteAllUnidentifiedFiles();

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

		private LayoutInflater inflater;

		public ListAdapter(Context c) {
			inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mFilepaths.size();
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.file_list_item, parent, false);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.name.setText(mFilepaths.get(position).getFilepathName());
			holder.size.setText(mFilepaths.get(position).getFilepath());

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