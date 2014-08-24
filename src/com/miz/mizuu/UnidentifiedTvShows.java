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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShows;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.MizLib;

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

public class UnidentifiedTvShows extends MizActivity {

	private ArrayList<TvShowEpisode> mEpisodes = new ArrayList<TvShowEpisode>();
	private ListView mList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.unidentified_files_layout);

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

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("tvshow-episode-changed"));
	}
	
	private void identifySelectedFiles() {
		SparseBooleanArray sba = mList.getCheckedItemPositions();
		ArrayList<String> filepaths = new ArrayList<String>();
		for (int i = 0; i < sba.size(); i++) {
			filepaths.add(mEpisodes.get(sba.keyAt(i)).getFilepaths().get(0).getFullFilepath());
		}

		Intent i = new Intent();
		i.setClass(this, IdentifyTvShow.class);
		i.putExtra("files", filepaths.toArray(new String[filepaths.size()]));
		i.putExtra("showName", "");
		i.putExtra("oldShowId", "0");
		i.putExtra("includeShowData", true);
		startActivity(i);
	}

	private void loadData() {
		mEpisodes.clear();

		DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();
		Cursor cursor = db.getAllUnidentifiedEpisodesInDatabase();
		while (cursor.moveToNext()) {
			mEpisodes.add(new TvShowEpisode(this,
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
					));
		}
		cursor.close();

		mList.setAdapter(new ListAdapter(this));

		enableNoFilesMessage(mEpisodes.size() == 0);
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
					DbAdapterTvShows dbTvShow = MizuuApplication.getTvDbAdapter();
					dbTvShow.deleteAllUnidentifiedShows();

					DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();
					db.deleteAllUnidentifiedEpisodes();

					mEpisodes.clear();
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
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
			return mEpisodes.size();
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

			holder.name.setText(mEpisodes.get(position).getTitle().substring(MizLib.indexOfLastSeparator(mEpisodes.get(position).getTitle()), mEpisodes.get(position).getTitle().length()));
			holder.size.setText(mEpisodes.get(position).getFilepaths().get(0).getFullFilepath());

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