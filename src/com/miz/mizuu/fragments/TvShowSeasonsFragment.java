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

package com.miz.mizuu.fragments;

import static com.miz.functions.PreferenceKeys.ALWAYS_DELETE_FILE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.CoverItem;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.GridEpisode;
import com.miz.functions.GridSeason;
import com.miz.functions.MizLib;
import com.miz.functions.TvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisodes;
import com.miz.service.DeleteFile;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

public class TvShowSeasonsFragment extends Fragment {

	private static final String SHOW_ID = "showId";
	private static final String DUAL_PANE = "dualPane";
	private static final String SEASON = "season";
	private static final String EPISODE_COUNT = "episodeCount";

	private Set<Integer> mCheckedSeasons = new HashSet<Integer>();
	private List<GridSeason> mItems = new ArrayList<GridSeason>();
	private int mImageThumbSize, mImageThumbSpacing, mSelectedIndex;
	private GridView mGridView;
	private ProgressBar mProgressBar;
	private ImageAdapter mAdapter;
	private String mShowId;
	private Picasso mPicasso;
	private Config mConfig;
	private boolean mDualPane, mContextualActionBarEnabled;
	private Bus mBus;
	private Context mContext;

	public static TvShowSeasonsFragment newInstance(String showId, boolean dualPane) {
		TvShowSeasonsFragment frag = new TvShowSeasonsFragment();
		Bundle b = new Bundle();
		b.putString(SHOW_ID, showId);
		b.putBoolean(DUAL_PANE, dualPane);
		frag.setArguments(b);
		return frag;
	}

	public TvShowSeasonsFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

		mBus = MizuuApplication.getBus();
		mBus.register(this);

		mPicasso = MizuuApplication.getPicasso(mContext);
		mConfig = MizuuApplication.getBitmapConfig();

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mShowId = getArguments().getString(SHOW_ID);
		mDualPane = getArguments().getBoolean(DUAL_PANE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mBus.unregister(this);
	}
	
	@Subscribe
	public void refreshData(com.miz.mizuu.TvShowEpisode episode) {		
		loadSeasons(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mAdapter = new ImageAdapter(mContext);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setEmptyView(v.findViewById(R.id.progress));
		mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								mAdapter.setNumColumns(numColumns);
							}
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// Update the selected index variable
				mSelectedIndex = arg2;

				if (!mDualPane) {
					// Show the episode browser Activity for the given season

					Intent i = new Intent(getActivity(), TvShowEpisodes.class);
					i.putExtra(SHOW_ID, mShowId);
					i.putExtra(SEASON, mItems.get(arg2).getSeason());
					i.putExtra(EPISODE_COUNT, mItems.get(arg2).getEpisodeCount());
					startActivity(i);

				} else {
					// Notify the parent fragment that the right-hand fragment should be swapped
					mBus.post(mItems.get(arg2));

					// Nasty hack to update the selected item highlight...
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mGridView.setItemChecked(position, true);
				return true;
			}
		});
		mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.seasons_contextual, menu);

				mContextualActionBarEnabled = true;
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.watched:
					changeWatchedStatus(true);
					break;
				case R.id.unwatched:
					changeWatchedStatus(false);
					break;
				case R.id.remove:
					removeSelectedSeasons();
					break;
				}

				mode.finish();

				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mContextualActionBarEnabled = false;
				mCheckedSeasons.clear();
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {				
				if (checked)
					mCheckedSeasons.add(position);
				else
					mCheckedSeasons.remove(position);

				int count = mCheckedSeasons.size();
				mode.setTitle(count + " " + getResources().getQuantityString(R.plurals.seasons_selected, count, count));

				// Nasty hack to update the selected items highlight...
				mAdapter.notifyDataSetChanged();
			}
		});

		// The layout has been created - let's load the data
		loadSeasons(true);
	}

	private void loadSeasons(boolean selectFirstSeason) {
		new SeasonLoader(selectFirstSeason).execute();
	}

	private void changeWatchedStatus(boolean watched) {
		// Create and open database
		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

		// This ought to be done in the background, but performance is fairly decent
		// - roughly 0.7 seconds to update 600 entries on a Sony Xperia Tablet Z2
		for (int season : mCheckedSeasons) {
			db.setSeasonWatchStatus(mShowId, mItems.get(season).getSeasonZeroIndex(), watched);
		}

		if (MizLib.isOnline(mContext) && MizLib.hasTraktAccount(mContext))
			syncWatchedStatusWithTrakt(mCheckedSeasons, watched);

		loadSeasons(true);
	}

	private void removeSelectedSeasons() {
		// TODO IMPLEMENT THIS SHIT!
		/*
		 * AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		View dialogLayout = getActivity().getLayoutInflater().inflate(R.layout.delete_file_dialog_layout, null);
		final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);
		cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ALWAYS_DELETE_FILE, true));

		builder.setTitle(getString(R.string.removeEpisode) + " S" + thisEpisode.getSeason() + "E" + thisEpisode.getEpisode())
		.setView(dialogLayout)
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				// Create and open database
				dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
				boolean deleted = false;
				if (ignoreDeletedFiles)
					deleted = dbHelper.ignoreEpisode(thisEpisode.getRowId());
				else
					deleted = dbHelper.deleteEpisode(thisEpisode.getRowId());

				if (deleted) {
					try {
						// Delete episode images
						File episodePhoto = new File(MizLib.getTvShowEpisodeFolder(getActivity()), thisEpisode.getShowId() + "_S" + thisEpisode.getSeason() + "E" + thisEpisode.getEpisode() + ".jpg");
						if (episodePhoto.exists()) {
							MizLib.deleteFile(episodePhoto);
						}
					} catch (NullPointerException e) {} // No file to delete

					if (dbHelper.getEpisodeCount(thisEpisode.getShowId()) == 0) { // No more episodes for this show
						DbAdapterTvShow dbShow = MizuuApplication.getTvDbAdapter();
						boolean deletedShow = dbShow.deleteShow(thisEpisode.getShowId());

						if (deletedShow) {
							MizLib.deleteFile(new File(MizLib.getTvShowThumbFolder(getActivity()), thisEpisode.getShowId() + ".jpg"));
							MizLib.deleteFile(new File(MizLib.getTvShowBackdropFolder(getActivity()), thisEpisode.getShowId() + "_tvbg.jpg"));
						}
					}

					if (cb.isChecked()) {
						Intent deleteIntent = new Intent(getActivity(), DeleteFile.class);
						deleteIntent.putExtra("filepath", thisEpisode.getFilepath());
						getActivity().startService(deleteIntent);
					}

					notifyDatasetChanges();
					getActivity().finish();
					return;
				} else {
					Toast.makeText(getActivity(), getString(R.string.failedToRemoveEpisode), Toast.LENGTH_SHORT).show();
				}
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.show();
		 */
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0, mCard, mCardBackground, mCardTitleColor;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mCard = MizuuApplication.getCardDrawable(mContext);
			mCardBackground = MizuuApplication.getCardColor(mContext);
			mCardTitleColor = MizuuApplication.getCardTitleColor(mContext);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup container) {
			final CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_season_cover, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.highlight = (ImageView) convertView.findViewById(R.id.highlight);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);

				holder.mLinearLayout.setBackgroundResource(mCard);
				holder.text.setBackgroundResource(mCardBackground);
				holder.text.setTextColor(mCardTitleColor);
				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));
				holder.subtext.setBackgroundResource(mCardBackground);

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(mCardBackground);

			// Android's GridView is pretty stupid regarding selectors
			// so we have to highlight the selected view manually - yuck!
			if (!mContextualActionBarEnabled) {
				if (position == mSelectedIndex && mDualPane) {
					holder.highlight.setVisibility(View.VISIBLE);
				} else {
					holder.highlight.setVisibility(View.GONE);
				}
			} else {
				if (mCheckedSeasons.contains(position)) {
					holder.highlight.setVisibility(View.VISIBLE);
				} else {
					holder.highlight.setVisibility(View.GONE);
				}
			}

			if (mItems.get(position).getSeason() == 0)
				holder.text.setText(R.string.stringSpecials);
			else
				holder.text.setText(getString(R.string.showSeason) + " " + mItems.get(position).getSeasonZeroIndex());

			holder.subtext.setText(mItems.get(position).getSubtitleText());

			mPicasso.load(mItems.get(position).getCover()).error(R.drawable.loading_image).fit().config(mConfig).into(holder.cover);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private class SeasonLoader extends AsyncTask<Void, Void, Void> {
		
		private boolean mSelectFirstSeason;
		
		public SeasonLoader(boolean selectFirstSeason) {
			mSelectFirstSeason = selectFirstSeason;
		}
		
		@Override
		protected void onPreExecute() {
			mItems.clear();
		}

		@Override
		protected Void doInBackground(Void... params) {
			HashMap<String, EpisodeCounter> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(mShowId);

			File temp = null;
			for (String key : seasons.keySet()) {
				temp = MizLib.getTvShowSeason(mContext, mShowId, key);				
				mItems.add(new GridSeason(mContext, Integer.valueOf(key), seasons.get(key).getEpisodeCount(), seasons.get(key).getWatchedCount(),
						temp.exists() ? temp :
							MizLib.getTvShowThumb(mContext, mShowId)));
			}

			seasons.clear();
			seasons = null;

			Collections.sort(mItems);

			return null;
		}

		@Override
		public void onPostExecute(Void result) {
			mProgressBar.setVisibility(View.GONE);
			mAdapter.notifyDataSetChanged();

			// This is a dual-pane device, so tell the parent fragment that
			// we want to display the episodes of the first available season
			if (mDualPane && mItems.size() > 0 && mSelectFirstSeason)
				mBus.post(mItems.get(0));
		}
	}

	private void syncWatchedStatusWithTrakt(final Set<Integer> checkedSeasons, final boolean watched) {
		new com.miz.functions.AsyncTask<Void, Boolean, Boolean>() {

			private Set<Integer> mSelectedSeasons;
			private List<TvShowEpisode> mEpisodes = new ArrayList<TvShowEpisode>();

			@Override
			protected void onPreExecute() {
				mSelectedSeasons = new HashSet<Integer>(checkedSeasons);

				DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();

				for (int season : mSelectedSeasons) {
					List<GridEpisode> temp = db.getEpisodesInSeason(mContext, mShowId, mItems.get(season).getSeason());
					for (int i = 0; i < temp.size(); i++) {
						mEpisodes.add(new TvShowEpisode(mShowId, temp.get(i).getEpisode(), temp.get(i).getSeason()));
					}
					temp.clear();
					temp = null;
				}
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				boolean result = MizLib.markEpisodeAsWatched(mShowId, mEpisodes, mContext, watched);
				if (!result) // Try again if it failed
					result = MizLib.markEpisodeAsWatched(mShowId, mEpisodes, mContext, watched);

				return result;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (!result)
					Toast.makeText(mContext, R.string.sync_error, Toast.LENGTH_LONG).show();
			}
		}.execute();
	}
}