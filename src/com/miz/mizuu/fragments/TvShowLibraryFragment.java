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

import static com.miz.functions.SortingKeys.CERTIFICATION;
import static com.miz.functions.SortingKeys.GENRES;
import static com.miz.functions.SortingKeys.ALL_SHOWS;
import static com.miz.functions.SortingKeys.FAVORITES;
import static com.miz.functions.SortingKeys.UNWATCHED_SHOWS;
import static com.miz.functions.SortingKeys.DURATION;
import static com.miz.functions.SortingKeys.RATING;
import static com.miz.functions.SortingKeys.RELEASE;
import static com.miz.functions.SortingKeys.TITLE;
import static com.miz.functions.SortingKeys.WEIGHTED_RATING;
import static com.miz.functions.SortingKeys.NEWEST_EPISODE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.db.DbHelperTvShow;
import com.miz.functions.ActionBarSpinnerViewHolder;
import com.miz.functions.AsyncTask;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.CoverItem;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.functions.MizLib;
import com.miz.functions.SQLiteCursorLoader;
import com.miz.functions.SpinnerItem;
import com.miz.functions.TvShowSortHelper;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.Preferences;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowDetails;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowActorSearchActivity;
import com.miz.mizuu.UnidentifiedFiles;
import com.miz.mizuu.Update;
import com.squareup.picasso.Picasso;

public class TvShowLibraryFragment extends Fragment implements OnNavigationListener, OnSharedPreferenceChangeListener {

	private SharedPreferences mSharedPreferences;
	private int mImageThumbSize, mImageThumbSpacing, mResizedWidth, mResizedHeight, mCurrentSort;
	private LoaderAdapter mAdapter;
	private ArrayList<TvShow> mTvShows = new ArrayList<TvShow>();
	private ArrayList<Integer> mTvShowKeys = new ArrayList<Integer>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private boolean mIgnorePrefixes, mLoading, mShowTitles;
	private ActionBar mActionBar;
	private Picasso mPicasso;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdapter;
	private SearchTask mSearch;
	private Config mConfig;
	private TvShowSectionLoader mTvShowSectionLoader;
	private View mEmptyLibraryLayout;
	private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;

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
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
		mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);

		String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
		if (thumbnailSize.equals(getString(R.string.large))) 
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
		else if (thumbnailSize.equals(getString(R.string.normal))) 
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
		if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-update"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-actor-search"));
	}

	private void setupActionBar() {
		mActionBar = getActivity().getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdapter == null)
			mSpinnerAdapter = new ActionBarSpinner();
	}

	private void setupSpinnerItems() {
		mSpinnerItems.clear();
		mSpinnerItems.add(new SpinnerItem(getString(R.string.chooserTVShows), getString(R.string.choiceAllShows)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceFavorites), getString(R.string.choiceFavorites)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.tvShowsWithUnwatchedEpisodes), getString(R.string.tvShowsWithUnwatchedEpisodes)));
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
					mTvShows.clear();
					mTvShowKeys.clear();
				}

				@Override
				protected Void doInBackground(Void... params) {
					
					ColumnIndexCache cache = new ColumnIndexCache();
					
					try {
						while (cursor.moveToNext()) {
							mTvShows.add(new TvShow(
									getActivity(),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_ID)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_TITLE)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_PLOT)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_RATING)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_GENRES)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_ACTORS)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_RUNTIME)),
									mIgnorePrefixes,
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_EXTRA1)),
									MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cache.getColumnIndex(cursor, DbAdapterTvShow.KEY_SHOW_ID)))
									));
						}
					} catch (Exception e) {
					} finally {
						cursor.close();
						cache.clear();
					}
					
					for (int i = 0; i < mTvShows.size(); i++)
						mTvShowKeys.add(i);

					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					showTvShowSection(mActionBar.getSelectedNavigationIndex());

					mLoading = false;
				}

			};
			load.execute();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			mTvShows.clear();
			mTvShowKeys.clear();
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

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

		mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
		mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
		mEmptyLibraryTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);
		mEmptyLibraryDescription.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf"));

		mAdapter = new LoaderAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setEmptyView(mEmptyLibraryLayout);
		mGridView.setColumnWidth(mImageThumbSize);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@SuppressLint("NewApi")
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

							MizLib.removeViewTreeObserver(mGridView.getViewTreeObserver(), this);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent();
				intent.putExtra("showId", mTvShows.get(mTvShowKeys.get(arg2)).getId());
				intent.setClass(getActivity(), TvShowDetails.class);
				startActivityForResult(intent, 0);
			}
		});

		return v;
	}

	private void showDetails(int arg2) {
		Intent intent = new Intent();
		intent.putExtra("showId", mTvShows.get(mTvShowKeys.get(arg2)).getId());
		intent.setClass(getActivity(), TvShowDetails.class);
		startActivityForResult(intent, 0);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mTvShows.size() == 0)
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

	private class LoaderAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0;

		public LoaderAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public boolean isEmpty() {
			return (!mLoading && mTvShowKeys.size() == 0);
		}

		@Override
		public int getCount() {
			return mTvShowKeys.size();
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
		public View getView(int position, View convertView, ViewGroup container) {

			final TvShow mTvShow = mTvShows.get(mTvShowKeys.get(position));

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);

				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			if (!mShowTitles) {
				holder.text.setVisibility(View.GONE);
				holder.subtext.setVisibility(View.GONE);
			} else {
				holder.text.setVisibility(View.VISIBLE);
				holder.subtext.setVisibility(View.VISIBLE);

				holder.text.setText(mTvShow.getTitle());
				holder.subtext.setText(mTvShow.getSubText(mCurrentSort));
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			if (mResizedWidth > 0)
				mPicasso.load(mTvShow.getThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
			else
				mPicasso.load(mTvShow.getThumbnail()).config(mConfig).into(holder);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private void notifyDataSetChanged() {
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();

		if (mSpinnerAdapter != null)
			mSpinnerAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!mLoading)
			showTvShowSection(itemPosition);

		return true;
	}

	private void showTvShowSection(int position) {
		if (mSpinnerAdapter != null)
			mSpinnerAdapter.notifyDataSetChanged(); // To show "0 shows" when loading

		if (mTvShowSectionLoader != null)
			mTvShowSectionLoader.cancel(true);
		mTvShowSectionLoader = new TvShowSectionLoader(position);
		mTvShowSectionLoader.execute();
	}

	private class TvShowSectionLoader extends LibrarySectionAsyncTask<Void, Void, Boolean> {
		private int mPosition;
		private ArrayList<Integer> mTempKeys = new ArrayList<Integer>();

		public TvShowSectionLoader(int position) {
			mPosition = position;
		}

		@Override
		protected void onPreExecute() {
			setProgressBarVisible(true);
			mTvShowKeys.clear();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (isCancelled())
				return false;

			switch (mPosition) {
			case ALL_SHOWS:
				for (int i = 0; i < mTvShows.size(); i++)
					mTempKeys.add(i);
				break;

			case FAVORITES:
				for (int i = 0; i < mTvShows.size(); i++) {
					if (mTvShows.get(i).isFavorite())
						mTempKeys.add(i);
				}
				break;

			case UNWATCHED_SHOWS:
				DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
				for (int i = 0; i < mTvShows.size(); i++)
					if (db.hasUnwatchedEpisodes(mTvShows.get(i).getId()))
						mTempKeys.add(i);
				break;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			// Make sure that the loading was successful, that the Fragment is still added and
			// that the currently selected navigation index is the same as when we started loading
			if (success && isAdded() && mActionBar.getSelectedNavigationIndex() == mPosition) {
				mTvShowKeys.addAll(mTempKeys);

				sortTvShows();
				notifyDataSetChanged();
				setProgressBarVisible(false);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (((Main) getActivity()).isDrawerOpen()) {
			if (mActionBar == null && getActivity() != null)
				mActionBar = getActivity().getActionBar();
			mActionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
			((Main) getActivity()).showDrawerOptionsMenu(menu, inflater);
		} else {
			setupActionBar();
			inflater.inflate(R.menu.menutv, menu);
			
			if (mTvShows.size() == 0)
				menu.findItem(R.id.random).setVisible(false);
			else
				menu.findItem(R.id.random).setVisible(true);
			
			MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.search_textbox), new OnActionExpandListener() {
				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					onSearchViewCollapsed();
					return true;
				}
			});
			
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
						onSearchViewCollapsed();
					}
					return true;
				}
				@Override
				public boolean onQueryTextSubmit(String query) { return false; }
			});
		}
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	private void onSearchViewCollapsed() {
		if (isAdded() && mActionBar != null) {
			if (mActionBar.getSelectedNavigationIndex() == ALL_SHOWS)
				showTvShowSection(ALL_SHOWS);
			else
				mActionBar.setSelectedNavigationItem(ALL_SHOWS);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.update:
			startActivityForResult(getUpdateIntent(), 0);
			break;
		case R.id.menuSortRating:
			sortBy(RATING);
			break;
		case R.id.menuSortWeightedRating:
			sortBy(WEIGHTED_RATING);
			break;
		case R.id.menuSortTitle:
			sortBy(TITLE);
			break;
		case R.id.menuSortRelease:
			sortBy(RELEASE);
			break;
		case R.id.menuSortNewestEpisode:
			sortBy(NEWEST_EPISODE);
			break;
		case R.id.menuSortDuration:
			sortBy(DURATION);
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
		case R.id.clear_filters:
			showTvShowSection(mActionBar.getSelectedNavigationIndex());
			break;
		case R.id.random:
			if (mTvShowKeys.size() > 0) {
				int random = new Random().nextInt(mTvShowKeys.size());
				showDetails(random);
			}
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

	private void sortTvShows() {
		if (!isAdded())
			return;

		String SORT_TYPE = mSharedPreferences.getString(SORTING_TVSHOWS, "sortTitle");

		if (SORT_TYPE.equals("sortRelease")) {
			sortBy(RELEASE);
		} else if (SORT_TYPE.equals("sortRating")) {
			sortBy(RATING);
		} else if (SORT_TYPE.equals("sortWeightedRating")) {
			sortBy(WEIGHTED_RATING);
		} else if (SORT_TYPE.equals("sortNewestEpisode")) {
			sortBy(NEWEST_EPISODE);
		} else if (SORT_TYPE.equals("sortDuration")) {
			sortBy(DURATION);
		} else { // if SORT_TYPE equals "sortTitle"
			sortBy(TITLE);
		}
	}

	public void sortBy(int sort) {
		if (!isAdded())
			return;

		mCurrentSort = sort;

		Editor editor = mSharedPreferences.edit();
		switch (mCurrentSort) {
		case TITLE:
			editor.putString(SORTING_TVSHOWS, "sortTitle");
			break;
		case RELEASE:
			editor.putString(SORTING_TVSHOWS, "sortRelease");
			break;
		case RATING:
			editor.putString(SORTING_TVSHOWS, "sortRating");
			break;
		case WEIGHTED_RATING:
			editor.putString(SORTING_TVSHOWS, "sortWeightedRating");
			break;
		case NEWEST_EPISODE:
			editor.putString(SORTING_TVSHOWS, "sortNewestEpisode");
			break;
		case DURATION:
			editor.putString(SORTING_TVSHOWS, "sortDuration");
			break;
		}
		editor.apply();

		ArrayList<TvShowSortHelper> tempHelper = new ArrayList<TvShowSortHelper>();
		for (int i = 0; i < mTvShowKeys.size(); i++) {
			tempHelper.add(new TvShowSortHelper(mTvShows.get(mTvShowKeys.get(i)), mTvShowKeys.get(i), mCurrentSort));
		}

		Collections.sort(tempHelper);

		mTvShowKeys.clear();
		for (int i = 0; i < tempHelper.size(); i++) {
			mTvShowKeys.add(tempHelper.get(i).getIndex());
		}

		tempHelper.clear();

		setProgressBarVisible(false);
		notifyDataSetChanged();
	}

	private void showGenres() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		String[] split;
		for (int i = 0; i < mTvShowKeys.size(); i++) {
			if (!mTvShows.get(mTvShowKeys.get(i)).getGenres().isEmpty()) {
				split = mTvShows.get(mTvShowKeys.get(i)).getGenres().split(",");
				for (int j = 0; j < split.length; j++) {
					if (map.containsKey(split[j].trim())) {
						map.put(split[j].trim(), map.get(split[j].trim()) + 1);
					} else {
						map.put(split[j].trim(), 1);
					}
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allGenres), R.string.selectGenre, GENRES);
	}

	private void showCertifications() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < mTvShowKeys.size(); i++) {
			String certification = mTvShows.get(mTvShowKeys.get(i)).getCertification();
			if (!MizLib.isEmpty(certification) && !certification.equalsIgnoreCase(getString(R.string.stringNA))) {
				if (map.containsKey(certification.trim())) {
					map.put(certification.trim(), map.get(certification.trim()) + 1);
				} else {
					map.put(certification.trim(), 1);
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allCertifications), R.string.selectCertification, CERTIFICATION);
	}

	private void createAndShowAlertDialog(final CharSequence[] temp, int title, final int type) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				handleDialogOnClick(dialog, which, type, temp);
			}
		});
		builder.show();
	}

	private CharSequence[] setupItemArray(TreeMap<String, Integer> map, int stringId) {
		final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);	
		for (int i = 0; i < tempArray.length; i++)
			tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) +  ")";

		final CharSequence[] temp = new CharSequence[tempArray.length + 1];
		temp[0] = getString(stringId);

		for (int i = 1; i < temp.length; i++)
			temp[i] = tempArray[i-1];

		return temp;
	}

	private void handleDialogOnClick(DialogInterface dialog, int which, int type, CharSequence[] temp) {
		if (which > 0) {
			ArrayList<Integer> currentlyShown = new ArrayList<Integer>(mTvShowKeys);
			mTvShowKeys.clear();

			String selected = temp[which].toString();
			selected = selected.substring(0, selected.lastIndexOf("(")).trim();

			boolean condition;
			for (int i = 0; i < currentlyShown.size(); i++) {
				condition = false;

				switch (type) {
				case GENRES:
					if (mTvShows.get(currentlyShown.get(i)).getGenres().contains(selected)) {
						String[] genres = mTvShows.get(currentlyShown.get(i)).getGenres().split(",");
						for (String genre : genres) {
							if (genre.trim().equals(selected)) {
								condition = true;
								break;
							}
						}
					}
					break;
				case CERTIFICATION:
					condition = mTvShows.get(currentlyShown.get(i)).getCertification().trim().contains(selected);
					break;
				}

				if (condition)
					mTvShowKeys.add(currentlyShown.get(i));
			}

			sortTvShows();
			notifyDataSetChanged();
		}

		dialog.dismiss();
	}

	private void search(String query) {
		setProgressBarVisible(true);

		if (mSearch != null)
			mSearch.cancel(true);

		mSearch = new SearchTask(query);
		mSearch.execute();
	}

	private class SearchTask extends AsyncTask<String, String, String> {

		private String mSearchQuery = "";
		private List<Integer> mTempKeys;

		public SearchTask(String query) {
			mSearchQuery = query.toLowerCase(Locale.ENGLISH);
		}

		@Override
		protected void onPreExecute() {
			setProgressBarVisible(true);
			mTvShowKeys.clear();
		}

		@Override
		protected String doInBackground(String... params) {
			mTempKeys = new ArrayList<Integer>();

			if (mSearchQuery.startsWith("actor:")) {
				for (int i = 0; i < mTvShows.size(); i++) {
					if (isCancelled())
						return null;

					if (mTvShows.get(i).getActors().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
						mTempKeys.add(i);
				}
			} else {
				Pattern p = Pattern.compile(MizLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x)

				for (int i = 0; i < mTvShows.size(); i++) {
					if (isCancelled())
						return null;

					String lowerCase = mTvShows.get(i).getTitle().toLowerCase(Locale.ENGLISH);

					if (lowerCase.indexOf(mSearchQuery) != -1 ||  p.matcher(lowerCase).replaceAll("").indexOf(mSearchQuery) != -1)
						mTempKeys.add(i);
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mTvShowKeys.addAll(mTempKeys);

			sortTvShows();
			notifyDataSetChanged();
			setProgressBarVisible(false);
		}
	}

	private void setProgressBarVisible(boolean visible) {
		mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		mGridView.setVisibility(visible ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == 2) { // Favourite removed
			if (mActionBar.getSelectedNavigationIndex() == FAVORITES) {
				showTvShowSection(FAVORITES);
			}
		} else if (resultCode == 3) {
			notifyDataSetChanged();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(IGNORED_TITLE_PREFIXES)) {
			mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
			forceLoaderLoad();
		} else if (key.equals(GRID_ITEM_SIZE)) {
			String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
			if (thumbnailSize.equals(getString(R.string.large))) 
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
			else if (thumbnailSize.equals(getString(R.string.normal))) 
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

		private LayoutInflater mInflater;

		public ActionBarSpinner() {
			mInflater = LayoutInflater.from(getActivity());
		}

		@Override
		public int getCount() {
			return mSpinnerItems.size();
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
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ActionBarSpinnerViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.spinner_header, parent, false);
				
				holder = new ActionBarSpinnerViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.subtitle = (TextView) convertView.findViewById(R.id.subtitle);
				
				convertView.setTag(holder);
			} else {
				holder = (ActionBarSpinnerViewHolder) convertView.getTag();
			}
			
			holder.title.setText(mSpinnerItems.get(position).getTitle());

			int size = mTvShowKeys.size();
			holder.subtitle.setText(size + " " + getResources().getQuantityString(R.plurals.showsInLibrary, size, size));

			return convertView;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			
			if (convertView == null) {
				convertView = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
			}
			
			((TextView) convertView.findViewById(android.R.id.text1)).setText(mSpinnerItems.get(position).getSubtitle());
			
			return convertView;
		}
	}
}