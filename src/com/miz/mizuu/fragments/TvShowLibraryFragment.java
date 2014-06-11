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

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;
import static com.miz.functions.PreferenceKeys.SORTING_TVSHOWS;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.db.DbHelperTvShow;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.SQLiteCursorLoader;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.Preferences;
import com.miz.mizuu.R;
import com.miz.mizuu.ShowDetails;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowActorSearchActivity;
import com.miz.mizuu.UnidentifiedFiles;
import com.miz.mizuu.Update;
import com.squareup.picasso.Picasso;

public class TvShowLibraryFragment extends Fragment implements OnNavigationListener, OnSharedPreferenceChangeListener {

	private SharedPreferences settings;
	private int mImageThumbSize, mImageThumbSpacing, mResizedWidth, mResizedHeight;
	private LoaderAdapter mAdapter;
	private ArrayList<TvShow> shows = new ArrayList<TvShow>(), shownShows = new ArrayList<TvShow>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private TextView overviewMessage;
	private Button updateMovieLibrary;
	private boolean ignorePrefixes, mLoading, mShowTitles;
	private ActionBar actionBar;
	private Picasso mPicasso;
	private ArrayList<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner spinnerAdapter;
	private SearchTask mSearch;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public TvShowLibraryFragment() {}

	public static TvShowLibraryFragment newInstance() {
		return new TvShowLibraryFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		setupSpinnerItems();

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

		ignorePrefixes = settings.getBoolean(IGNORED_TITLE_PREFIXES, false);
		mShowTitles = settings.getBoolean(SHOW_TITLES_IN_GRID, true);

		String thumbnailSize = settings.getString(GRID_ITEM_SIZE, getString(R.string.normal));
		if (thumbnailSize.equals(getString(R.string.normal))) 
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1);
		else
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();

		mAdapter = new LoaderAdapter(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Setup ActionBar with the action list
		setupActionBar();
		if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			actionBar.setListNavigationCallbacks(spinnerAdapter, this);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-update"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-actor-search"));
	}

	private void setupActionBar() {
		actionBar = getActivity().getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (spinnerAdapter == null)
			spinnerAdapter = new ActionBarSpinner();
	}

	private void setupSpinnerItems() {
		spinnerItems.clear();
		spinnerItems.add(new SpinnerItem(getString(R.string.chooserTVShows), getString(R.string.choiceAllShows)));
		spinnerItems.add(new SpinnerItem(getString(R.string.choiceFavorites), getString(R.string.choiceFavorites)));
		spinnerItems.add(new SpinnerItem(getString(R.string.tvShowsWithUnwatchedEpisodes), getString(R.string.tvShowsWithUnwatchedEpisodes)));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.filterEquals(new Intent("mizuu-shows-actor-search"))) {
				search("actor: " + intent.getStringExtra("intent_extra_data_key"));
			} else {
				if (intent.filterEquals(new Intent("mizuu-show-cover-change"))) {
					clearCaches();
				}	

				forceLoaderLoad();
			}
		}
	};

	LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			mLoading = true;
			return new SQLiteCursorLoader(getActivity(), DbHelperTvShow.getHelper(getActivity()), DbAdapterTvShow.DATABASE_TABLE, DbAdapterTvShow.SELECT_ALL, "NOT(" + DbAdapterTvShow.KEY_SHOW_ID + " = 'invalid')", null, null, null, DbAdapterTvShow.KEY_SHOW_TITLE + " ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> arg0, final Cursor cursor) {
			AsyncTask<Void, Void, Void> load = new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPreExecute() {
					shows.clear();
					shownShows.clear();
				}

				@Override
				protected Void doInBackground(Void... params) {
					try {
						while (cursor.moveToNext()) {
							shows.add(new TvShow(
									getActivity(),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
									ignorePrefixes,
									cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1))
									));
						}
					} catch (Exception e) {}
					finally {
						cursor.close();
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					shownShows.addAll(shows);

					showCollectionBasedOnNavigationIndex(actionBar.getSelectedNavigationIndex());

					mLoading = false;
				}

			};
			load.execute();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			shows.clear();
			shownShows.clear();
			notifyDataSetChanged();
		}
	};

	private void clearCaches() {
		if (isAdded())
			MizuuApplication.getLruCache(getActivity()).clear();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

		pbar = (ProgressBar) v.findViewById(R.id.progress);

		overviewMessage = (TextView) v.findViewById(R.id.overviewMessage);

		updateMovieLibrary = (Button) v.findViewById(R.id.addMoviesButton);
		updateMovieLibrary.setText(getString(R.string.mainUpdateBtTv));
		updateMovieLibrary.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(getUpdateIntent(), 0);
			}
		});

		mAdapter = new LoaderAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setFastScrollEnabled(true);
		mGridView.setAdapter(mAdapter);
		mGridView.setColumnWidth(mImageThumbSize);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@SuppressLint("NewApi")
					@SuppressWarnings("deprecation")
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								mAdapter.setNumColumns(numColumns);
								mResizedWidth = (int) (((mGridView.getWidth() - (numColumns * mImageThumbSpacing))
										/ numColumns) * 1.1); // * 1.1 is a hack to make images look slightly less blurry
								mResizedHeight = (int) (mResizedWidth * 1.5);
							}
							if (MizLib.hasJellyBean()) {
								mGridView.getViewTreeObserver()
								.removeOnGlobalLayoutListener(this);
							} else {
								mGridView.getViewTreeObserver()
								.removeGlobalOnLayoutListener(this);
							}
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent();
				intent.putExtra("showId", shownShows.get(arg2).getId());
				intent.setClass(getActivity(), ShowDetails.class);
				startActivityForResult(intent, 0);
			}
		});

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (shows.size() == 0)
			forceLoaderLoad();

		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}

	private class LoaderAdapter extends BaseAdapter implements SectionIndexer {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0, mSidePadding, mBottomPadding, mCard, mCardBackground, mCardTitleColor;
		private Object[] sections;

		public LoaderAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mSidePadding = MizLib.convertDpToPixels(mContext, 1);
			mBottomPadding = MizLib.convertDpToPixels(mContext, 2);
			mCard = MizuuApplication.getCardDrawable(mContext);
			mCardBackground = MizuuApplication.getCardColor(mContext);
			mCardTitleColor = MizuuApplication.getCardTitleColor(mContext);
		}

		@Override
		public int getCount() {
			return shownShows.size();
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
		public View getView(int position, View convertView, ViewGroup container) {

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
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

			if (!mShowTitles) {
				holder.text.setVisibility(View.GONE);
				holder.subtext.setVisibility(View.GONE);
				holder.cover.setPadding(mSidePadding, 0, mSidePadding, mBottomPadding);
			} else {
				holder.text.setVisibility(View.VISIBLE);
				holder.subtext.setVisibility(View.VISIBLE);

				holder.text.setText(shownShows.get(position).getTitle());
				holder.subtext.setText(shownShows.get(position).getFirstAirdateYear());
			}

			holder.cover.setImageResource(mCardBackground);

			if (mResizedWidth > 0)
				mPicasso.load(shownShows.get(position).getThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
			else
				mPicasso.load(shownShows.get(position).getThumbnail()).config(mConfig).into(holder);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}

		@Override
		public int getPositionForSection(int section) {
			return section;
		}

		@Override
		public int getSectionForPosition(int position) {
			return position;
		}

		@Override
		public Object[] getSections() {
			return sections;
		}

		@Override
		public void notifyDataSetChanged() {
			ArrayList<TvShow> tempShows = new ArrayList<TvShow>(shownShows);
			sections = new Object[tempShows.size()];

			String SORT_TYPE = settings.getString(SORTING_TVSHOWS, "sortTitle");
			if (SORT_TYPE.equals("sortRating")) {
				DecimalFormat df = new DecimalFormat("#.#");
				for (int i = 0; i < sections.length; i++)
					sections[i] = df.format(tempShows.get(i).getRawRating());
			} else if (SORT_TYPE.equals("sortWeightedRating")) {
				DecimalFormat df = new DecimalFormat("#.#");
				for (int i = 0; i < sections.length; i++)
					sections[i] = df.format(tempShows.get(i).getWeightedRating());
			} else if (SORT_TYPE.equals("sortDuration")) {
				String hour = getResources().getQuantityString(R.plurals.hour, 1, 1).substring(0,1);
				String minute = getResources().getQuantityString(R.plurals.minute, 1, 1).substring(0,1);

				for (int i = 0; i < sections.length; i++)
					sections[i] = MizLib.getRuntimeInMinutesOrHours(tempShows.get(i).getRuntime(), hour, minute);
			} else {
				String temp = "";
				for (int i = 0; i < sections.length; i++)
					if (!MizLib.isEmpty(tempShows.get(i).getTitle())) {
						temp = tempShows.get(i).getTitle().substring(0,1);
						if (Character.isLetter(temp.charAt(0)))
							sections[i] = tempShows.get(i).getTitle().substring(0,1);
						else
							sections[i] = "#";
					} else
						sections[i] = "";
			}

			tempShows.clear();
			tempShows = null;

			super.notifyDataSetChanged();
		}
	}

	private void notifyDataSetChanged() {

		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();

		if (spinnerAdapter != null)
			spinnerAdapter.notifyDataSetChanged();

		try {
			if (shows.size() == 0) {
				overviewMessage.setVisibility(View.VISIBLE);
				if (MizLib.isTvShowLibraryBeingUpdated(getActivity())) {
					overviewMessage.setText(getString(R.string.updatingLibraryDescription));
					updateMovieLibrary.setVisibility(View.GONE);
				} else {
					overviewMessage.setText(getString(R.string.noShowsInLibrary));
					updateMovieLibrary.setVisibility(View.VISIBLE);
				}
			} else {
				overviewMessage.setVisibility(View.GONE);
				updateMovieLibrary.setVisibility(View.GONE);
			}
		} catch (Exception e) {} // Can happen sometimes, I guess...
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!mLoading)
			showCollectionBasedOnNavigationIndex(itemPosition);
		return true;
	}

	private void showCollectionBasedOnNavigationIndex(int itemPosition) {
		if (spinnerAdapter != null)
			spinnerAdapter.notifyDataSetChanged(); // To show "0 shows" when loading

		switch (itemPosition) {
		case 0:
			showAllShows();
			break;
		case 1:
			showFavorites();
			break;
		case 2:
			showUnwatchedShows();
			break;
		}
	}

	private void showAllShows() {
		showProgressBar();

		shownShows.clear();

		shownShows.addAll(shows);

		sortShows();

		notifyDataSetChanged();

		hideProgressBar();
	}

	private void showFavorites() {
		showProgressBar();

		shownShows.clear();

		for (int i = 0; i < shows.size(); i++) {
			if (shows.get(i).isFavorite())
				shownShows.add(shows.get(i));
		}

		sortShows();

		notifyDataSetChanged();

		hideProgressBar();
	}

	private void showUnwatchedShows() {

		shownShows.clear();

		new AsyncTask<Void, Void, Void>() {
			ArrayList<TvShow> tempShows = new ArrayList<TvShow>();

			@Override
			protected void onPreExecute() {
				showProgressBar();
			}

			@Override
			protected Void doInBackground(Void... params) {				
				DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
				for (int i = 0; i < shows.size(); i++)
					if (db.hasUnwatchedEpisodes(shows.get(i).getId()))
						tempShows.add(shows.get(i));
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				shownShows.addAll(tempShows);

				// Clean up...
				tempShows.clear();
				tempShows = null;

				sortShows();
				notifyDataSetChanged();
				hideProgressBar();
			}
		}.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (((Main) getActivity()).isDrawerOpen()) {
			if (actionBar == null && getActivity() != null)
				actionBar = getActivity().getActionBar();
			actionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
			((Main) getActivity()).showDrawerOptionsMenu(menu, inflater);
		} else {
			setupActionBar();
			inflater.inflate(R.menu.menutv, menu);
			SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) menu.findItem(R.id.search_textbox).getActionView();
			ComponentName cn = new ComponentName(getActivity(), TvShowActorSearchActivity.class);
			searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));
			searchView.setOnQueryTextListener(new OnQueryTextListener() {
				@Override
				public boolean onQueryTextChange(String newText) {
					if (newText.length() > 0) {
						search(newText);
					} else {
						showAllShows();
					}
					return true;
				}
				@Override
				public boolean onQueryTextSubmit(String query) { return false; }
			});
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.update:
			startActivityForResult(getUpdateIntent(), 0);
			break;
		case R.id.menuSortRating:
			sortByRating();
			break;
		case R.id.menuSortWeightedRating:
			sortByWeightedRating();
			break;
		case R.id.menuSortTitle:
			sortByTitle();
			break;
		case R.id.menuSortRelease:
			sortByRelease();
			break;
		case R.id.menuSortNewestEpisode:
			sortByNewestEpisode();
			break;
		case R.id.menuSortDuration:
			sortByDuration();
			break;
		case R.id.genres:
			showGenres();
			break;
		case R.id.certifications:
			showCertifications();
			break;
		case R.id.unidentifiedFiles:
			startActivity(new Intent(getActivity(), UnidentifiedFiles.class));
			break;
		case R.id.menuSettings:
			startActivity(new Intent(getActivity(), Preferences.class));
			break;
		}

		return true;
	}

	private Intent getUpdateIntent() {
		Intent intent = new Intent();
		intent.setClass(getActivity(), Update.class);
		intent.putExtra("isMovie", false);
		return intent;
	}

	private void sortShows() {
		String SORT_TYPE = settings.getString(SORTING_TVSHOWS, "sortTitle");
		if (SORT_TYPE.equals("sortRelease")) {
			sortByRelease();
		} else if (SORT_TYPE.equals("sortRating")) {
			sortByRating();
		} else if (SORT_TYPE.equals("sortWeightedRating")) {
			sortByWeightedRating();
		} else if (SORT_TYPE.equals("sortNewestEpisode")) {
			sortByNewestEpisode();
		} else if (SORT_TYPE.equals("sortDuration")) {
			sortByDuration();
		} else {
			sortByTitle();
		}
	}

	public void sortByTitle() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortTitle");
		editor.apply();

		sortBy(TITLE);
	}

	public void sortByRelease() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortRelease");
		editor.apply();

		sortBy(RELEASE);
	}

	public void sortByNewestEpisode() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortNewestEpisode");
		editor.apply();

		sortBy(NEWEST_EPISODE);
	}

	public void sortByRating() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortRating");
		editor.apply();

		sortBy(RATING);
	}

	public void sortByWeightedRating() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortWeightedRating");
		editor.apply();

		sortBy(WEIGHTED_RATING);
	}

	public void sortByDuration() {
		Editor editor = settings.edit();
		editor.putString(SORTING_TVSHOWS, "sortDuration");
		editor.apply();

		sortBy(DURATION);
	}

	private final int TITLE = 10, RELEASE = 11, RATING = 12, NEWEST_EPISODE = 13, WEIGHTED_RATING = 14, DURATION = 15;

	public void sortBy(final int sort) {

		showProgressBar();

		ArrayList<TvShow> tempShows = new ArrayList<TvShow>(shownShows);

		switch (sort) {	
		case TITLE:

			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {
					String s1 = o1.getTitle(), s2 = o2.getTitle();
					if (s1 != null && s2 != null)
						return s1.compareToIgnoreCase(s2);
					return 0;
				}
			});

			break;

		case RELEASE:
			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {	
					try {
						return o1.getFirstAirdate().compareTo(o2.getFirstAirdate()) * -1;
					} catch (Exception e) {
						return 0;
					}
				}
			});

			break;

		case RATING:

			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {
					try {
						if (o1.getRawRating() < o2.getRawRating())
							return 1;
						else if (o1.getRawRating() > o2.getRawRating())
							return -1;
					} catch (Exception e) {}
					return 0;
				}
			});

			break;

		case WEIGHTED_RATING:

			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {
					try {
						if (o1.getWeightedRating() < o2.getWeightedRating()) {
							return 1;
						} else if (o1.getWeightedRating() > o2.getWeightedRating())
							return -1;
					} catch (Exception e) {}
					return 0;
				}
			});

			break;

		case NEWEST_EPISODE:

			final DbAdapterTvShowEpisode dbTvShowEpisode = MizuuApplication.getTvEpisodeDbAdapter();
			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {
					String first = dbTvShowEpisode.getLatestEpisodeAirdate(o1.getId());
					String second = dbTvShowEpisode.getLatestEpisodeAirdate(o2.getId());
					return first.compareTo(second) * -1;
				}
			});

			break;

		case DURATION:

			Collections.sort(tempShows, new Comparator<TvShow>() {
				@Override
				public int compare(TvShow o1, TvShow o2) {

					int first = Integer.valueOf(o1.getRuntime());
					int second = Integer.valueOf(o2.getRuntime());

					if (first < second)
						return 1;
					else if (first > second)
						return -1;

					return 0;
				}
			});
		}

		shownShows = new ArrayList<TvShow>(tempShows);

		// Clean up on aisle three...
		tempShows.clear();

		hideProgressBar();
		notifyDataSetChanged();
	}

	private void showGenres() {
		showCollectionBasedOnNavigationIndex(actionBar.getSelectedNavigationIndex());

		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		String[] split;
		for (int i = 0; i < shownShows.size(); i++) {
			if (!shownShows.get(i).getGenres().isEmpty()) {
				split = shownShows.get(i).getGenres().split(",");
				for (int j = 0; j < split.length; j++) {
					if (map.containsKey(split[j].trim())) {
						map.put(split[j].trim(), map.get(split[j].trim()) + 1);
					} else {
						map.put(split[j].trim(), 1);
					}
				}
			}
		}

		final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);	
		for (int i = 0; i < tempArray.length; i++)
			tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) +  ")";

		final CharSequence[] temp = new CharSequence[tempArray.length + 1];
		temp[0] = getString(R.string.allGenres);

		for (int i = 1; i < temp.length; i++)
			temp[i] = tempArray[i-1];


		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectGenre)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which > 0) {
					ArrayList<TvShow> currentlyShown = new ArrayList<TvShow>();
					currentlyShown.addAll(shownShows);

					shownShows.clear();

					String selectedGenre = temp[which].toString();
					selectedGenre = selectedGenre.substring(0, selectedGenre.lastIndexOf("(")).trim();

					for (int i = 0; i < currentlyShown.size(); i++)
						if (currentlyShown.get(i).getGenres().contains(selectedGenre))
							shownShows.add(currentlyShown.get(i));

					sortShows();
					notifyDataSetChanged();
				}

				dialog.dismiss();
			}
		});
		builder.show();
	}

	private void showCertifications() {
		showCollectionBasedOnNavigationIndex(actionBar.getSelectedNavigationIndex());

		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < shownShows.size(); i++) {
			String certification = shownShows.get(i).getCertification();
			if (!MizLib.isEmpty(certification) && !certification.equalsIgnoreCase(getString(R.string.stringNA))) {
				if (map.containsKey(certification.trim())) {
					map.put(certification.trim(), map.get(certification.trim()) + 1);
				} else {
					map.put(certification.trim(), 1);
				}
			}
		}

		final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);	
		for (int i = 0; i < tempArray.length; i++)
			tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) +  ")";

		final CharSequence[] temp = new CharSequence[tempArray.length + 1];
		temp[0] = getString(R.string.allCertifications);

		for (int i = 1; i < temp.length; i++)
			temp[i] = tempArray[i-1];


		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectCertification)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which > 0) {
					ArrayList<TvShow> currentlyShown = new ArrayList<TvShow>();
					currentlyShown.addAll(shownShows);

					shownShows.clear();

					String selectedGenre = temp[which].toString();
					selectedGenre = selectedGenre.substring(0, selectedGenre.lastIndexOf("(")).trim();

					for (int i = 0; i < currentlyShown.size(); i++)
						if (currentlyShown.get(i).getCertification().trim().contains(selectedGenre))
							shownShows.add(currentlyShown.get(i));

					sortShows();
					notifyDataSetChanged();
				}

				dialog.dismiss();
			}
		});
		builder.show();
	}

	private void search(String query) {
		showProgressBar();

		if (mSearch != null)
			mSearch.cancel(true);

		mSearch = new SearchTask(query);
		mSearch.execute();
	}

	private class SearchTask extends AsyncTask<String, String, String> {

		private String searchQuery = "";

		public SearchTask(String query) {
			searchQuery = query.toLowerCase(Locale.ENGLISH);
		}

		@Override
		protected String doInBackground(String... params) {
			shownShows.clear();

			if (searchQuery.startsWith("actor:")) {
				for (int i = 0; i < shows.size(); i++) {
					if (isCancelled())
						return null;

					if (shows.get(i).getActors().toLowerCase(Locale.ENGLISH).contains(searchQuery.replace("actor:", "").trim()))
						shownShows.add(shows.get(i));
				}
			} else {
				String lowerCase = ""; // Reuse String variable
				Pattern p = Pattern.compile(MizLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

				for (int i = 0; i < shows.size(); i++) {
					if (isCancelled())
						return null;

					lowerCase = shows.get(i).getTitle().toLowerCase(Locale.ENGLISH);

					if (lowerCase.indexOf(searchQuery) != -1 ||  p.matcher(lowerCase).replaceAll("").indexOf(searchQuery) != -1)
						shownShows.add(shows.get(i));
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			sortShows();
			notifyDataSetChanged();
			hideProgressBar();
		}
	}

	private void showProgressBar() {
		pbar.setVisibility(View.VISIBLE);
		mGridView.setVisibility(View.GONE);
	}

	private void hideProgressBar() {
		pbar.setVisibility(View.GONE);
		mGridView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == 2) { // Favourite removed
			if (actionBar.getSelectedNavigationIndex() == 1) {
				showFavorites();
			}
		} else if (resultCode == 3) {
			notifyDataSetChanged();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(IGNORED_TITLE_PREFIXES)) {
			ignorePrefixes = settings.getBoolean(IGNORED_TITLE_PREFIXES, false);
			forceLoaderLoad();
		} else if (key.equals(GRID_ITEM_SIZE)) {
			String thumbnailSize = settings.getString(GRID_ITEM_SIZE, getString(R.string.normal));
			if (thumbnailSize.equals(getString(R.string.normal))) 
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1);
			else
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);

			mGridView.setColumnWidth(mImageThumbSize);

			final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
			if (numColumns > 0) {
				mAdapter.setNumColumns(numColumns);
			}

			notifyDataSetChanged();
		} else if (key.equals(SHOW_TITLES_IN_GRID)) {
			mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
			notifyDataSetChanged();
		}
	}

	private void forceLoaderLoad() {
		if (isAdded())
			if (getLoaderManager().getLoader(0) == null)
				getLoaderManager().initLoader(0, null, loaderCallbacks);
			else
				getLoaderManager().restartLoader(0, null, loaderCallbacks);
	}

	private class ActionBarSpinner extends BaseAdapter {

		private LayoutInflater inflater;

		public ActionBarSpinner() {
			inflater = LayoutInflater.from(getActivity());
		}

		@Override
		public int getCount() {
			return spinnerItems.size();
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
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.spinner_header, parent, false);
			((TextView) convertView.findViewById(R.id.title)).setText(spinnerItems.get(position).getTitle());

			int size = shownShows.size();
			((TextView) convertView.findViewById(R.id.subtitle)).setText(size + " " + getResources().getQuantityString(R.plurals.showsInLibrary, size, size));
			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return spinnerItems.size() == 0;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {			
			convertView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
			((TextView) convertView.findViewById(android.R.id.text1)).setText(spinnerItems.get(position).getSubtitle());

			return convertView;
		}
	}
}