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
import static com.miz.functions.PreferenceKeys.SORTING_COLLECTIONS_OVERVIEW;
import static com.miz.functions.PreferenceKeys.SORTING_MOVIES;
import static com.miz.functions.SortingKeys.ALL_MOVIES;
import static com.miz.functions.SortingKeys.AVAILABLE_FILES;
import static com.miz.functions.SortingKeys.CERTIFICATION;
import static com.miz.functions.SortingKeys.COLLECTIONS;
import static com.miz.functions.SortingKeys.DATE;
import static com.miz.functions.SortingKeys.DURATION;
import static com.miz.functions.SortingKeys.FAVORITES;
import static com.miz.functions.SortingKeys.FILE_SOURCES;
import static com.miz.functions.SortingKeys.FOLDERS;
import static com.miz.functions.SortingKeys.GENRES;
import static com.miz.functions.SortingKeys.OFFLINE_COPIES;
import static com.miz.functions.SortingKeys.RATING;
import static com.miz.functions.SortingKeys.RELEASE;
import static com.miz.functions.SortingKeys.TITLE;
import static com.miz.functions.SortingKeys.UNWATCHED_MOVIES;
import static com.miz.functions.SortingKeys.WATCHED_MOVIES;
import static com.miz.functions.SortingKeys.WEIGHTED_RATING;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jcifs.smb.SmbFile;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
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
import android.text.TextUtils;
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

import com.google.common.collect.ArrayListMultimap;
import com.miz.db.DatabaseHelper;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterSources;
import com.miz.functions.ActionBarSpinnerViewHolder;
import com.miz.functions.AsyncTask;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.CoverItem;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.functions.MovieSortHelper;
import com.miz.functions.SQLiteCursorLoader;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCollection;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.UnidentifiedMovies;
import com.miz.mizuu.Update;
import com.squareup.picasso.Picasso;

public class MovieLibraryFragment extends Fragment implements OnNavigationListener, OnSharedPreferenceChangeListener {

	public static final int MAIN = 0, OTHER = 1;

	private SharedPreferences mSharedPreferences;
	private int mImageThumbSize, mImageThumbSpacing, mType, mResizedWidth, mResizedHeight, mCurrentSort;
	private LoaderAdapter mAdapter;
	private ArrayList<MediumMovie> mMovies = new ArrayList<MediumMovie>();
	private ArrayList<Integer> mMovieKeys = new ArrayList<Integer>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private boolean mIgnorePrefixes, mLoading, mShowTitles;
	private ActionBar mActionBar;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdapter;
	private Picasso mPicasso;
	private Config mConfig;
	private MovieSectionLoader mMovieSectionLoader;
	private SearchTask mSearch;
	private View mEmptyLibraryLayout;
	private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieLibraryFragment() {}

	public static MovieLibraryFragment newInstance(int type) {
		MovieLibraryFragment frag = new MovieLibraryFragment();
		Bundle b = new Bundle();
		b.putInt("type", type);
		frag.setArguments(b);		
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mType = getArguments().getInt("type");

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

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-update"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-actor-search"));
	}

	private void setupActionBar() {
		mActionBar = getActivity().getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdapter == null)
			mSpinnerAdapter = new ActionBarSpinner();
	}

	private void setupSpinnerItems() {
		mSpinnerItems.clear();
		if (mType == MAIN)
			mSpinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.choiceAllMovies)));
		else
			mSpinnerItems.add(new SpinnerItem(getString(R.string.chooserWatchList), getString(R.string.choiceAllMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceFavorites), getString(R.string.choiceFavorites)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceAvailableFiles), getString(R.string.choiceAvailableFiles)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceCollections), getString(R.string.choiceCollections)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceWatchedMovies), getString(R.string.choiceWatchedMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceUnwatchedMovies), getString(R.string.choiceUnwatchedMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceOffline), getString(R.string.choiceOffline)));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.filterEquals(new Intent("mizuu-movie-actor-search"))) {
				search("actor: " + intent.getStringExtra("intent_extra_data_key"));
			} else {
				if (intent.filterEquals(new Intent("mizuu-library-change")) || intent.filterEquals(new Intent("mizuu-movie-cover-change"))) {
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
			return new SQLiteCursorLoader(getActivity(), DatabaseHelper.getHelper(getActivity()).getWritableDatabase(), DbAdapterMovies.DATABASE_TABLE,
					DbAdapterMovies.SELECT_ALL, "NOT(" + DbAdapterMovies.KEY_TITLE + " = '" + DbAdapterMovies.REMOVED_MOVIE_TITLE + "')", null, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> arg0, final Cursor cursor) {			
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPreExecute() {
					mMovies.clear();
					mMovieKeys.clear();
				}

				@Override
				protected Void doInBackground(Void... params) {
					// Normally we'd have to go through each movie and add filepaths mapped to that movie
					// one by one. This is a hacky approach that gets all filepaths at once and creates a
					// map of them. That way it's easy to get filepaths for a specific movie - and it's
					// 2-3x faster with ~750 movies.
					ArrayListMultimap<String, String> filepaths = ArrayListMultimap.create();
					Cursor paths = MizuuApplication.getMovieMappingAdapter().getAllFilepaths(false);
					if (paths != null) {
						try {
							while (paths.moveToNext()) {
								filepaths.put(paths.getString(paths.getColumnIndex(DbAdapterMovieMappings.KEY_TMDB_ID)),
										paths.getString(paths.getColumnIndex(DbAdapterMovieMappings.KEY_FILEPATH)));
							}
						} catch (Exception e) {} finally {
							MizuuApplication.setMovieFilepaths(filepaths);
							filepaths.clear();
						}
					}
					
					HashMap<String, String> collectionsMap = MizuuApplication.getCollectionsAdapter().getCollectionsMap();
					ColumnIndexCache cache = new ColumnIndexCache();

					try {
						while (cursor.moveToNext()) {							
							if (mType == OTHER && cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TO_WATCH)).equals("0"))
								continue;

							mMovies.add(new MediumMovie(getActivity(),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TITLE)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TMDB_ID)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RATING)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RELEASEDATE)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_GENRES)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_FAVOURITE)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_ACTORS)),
									collectionsMap.get(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID))),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_COLLECTION_ID)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_TO_WATCH)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_HAS_WATCHED)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_DATE_ADDED)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_CERTIFICATION)),
									cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovies.KEY_RUNTIME)),
									mIgnorePrefixes
									));
						}
					} catch (Exception e) {} finally {
						cursor.close();
						cache.clear();
						
						// Clear the hacky movie filepath map
						MizuuApplication.clearMovieFilepaths();
					}

					for (int i = 0; i < mMovies.size(); i++) {
						if (mType == OTHER)
							if (!mMovies.get(i).toWatch())
								continue;
						mMovieKeys.add(i);
					}

					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					showMovieSection(mActionBar.getSelectedNavigationIndex());

					mLoading = false;
				}
			}.execute();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			mMovies.clear();
			mMovieKeys.clear();
			notifyDataSetChanged();
		}
	};

	private void clearCaches() {
		if (isAdded())
			MizuuApplication.getLruCache(getActivity()).clear();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mMovieKeys.size() > 0)
			mProgressBar.setVisibility(View.GONE);

		mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
		mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
		mEmptyLibraryTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);
		if (mType == OTHER)
			mEmptyLibraryDescription.setText(R.string.empty_watchlist_description);
		mEmptyLibraryDescription.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf"));

		mAdapter = new LoaderAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setEmptyView(mEmptyLibraryLayout);
		mGridView.setColumnWidth(mImageThumbSize);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
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
				showDetails(arg2);
			}
		});
	}

	private void showDetails(int arg2) {
		Intent intent = new Intent();
		if (mActionBar.getSelectedNavigationIndex() == COLLECTIONS) { // Collection
			intent.putExtra("collectionId", mMovies.get(mMovieKeys.get(arg2)).getCollectionId());
			intent.putExtra("collectionTitle", mMovies.get(mMovieKeys.get(arg2)).getCollection());
			intent.setClass(getActivity(), MovieCollection.class);
			startActivity(intent);
		} else {
			intent.putExtra("tmdbId", mMovies.get(mMovieKeys.get(arg2)).getTmdbId());
			intent.setClass(getActivity(), MovieDetails.class);
			startActivityForResult(intent, 0);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mMovies.size() == 0)
			forceLoaderLoad();

		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onDestroy() {	
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	private class LoaderAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private final Context mContext;
		private int mNumColumns = 0;
		private boolean mCollectionsView = false;

		public LoaderAdapter(Context context) {
			mContext = context;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public boolean isEmpty() {
			return (!mLoading && mMovieKeys.size() == 0);
		}

		@Override
		public int getCount() {
			return mMovieKeys.size();
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

			final MediumMovie mMovie = mMovies.get(mMovieKeys.get(position));

			CoverItem holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
				holder.subtext.setSingleLine(true);

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

				holder.text.setText(mCollectionsView ? mMovie.getCollection() : mMovie.getTitle());
				if (mCollectionsView) {
					holder.text.setSingleLine(false);
					holder.text.setLines(2);
					holder.text.setMaxLines(2);
					holder.subtext.setVisibility(View.GONE);
				} else {
					holder.text.setSingleLine(true);
					holder.text.setLines(1);
					holder.text.setMaxLines(1);
					holder.subtext.setVisibility(View.VISIBLE);
					holder.subtext.setText(mMovie.getSubText(mCurrentSort));
				}
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			if (!mCollectionsView) {
				// Movie poster
				if (mResizedWidth > 0)
					mPicasso.load(mMovie.getThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
				else
					mPicasso.load(mMovie.getThumbnail()).config(mConfig).into(holder);
			} else {
				// Collection poster
				if (mResizedWidth > 0)
					mPicasso.load(mMovie.getCollectionPoster()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
				else
					mPicasso.load(mMovie.getCollectionPoster()).config(mConfig).into(holder);
			}

			return convertView;
		}

		@Override
		public void notifyDataSetChanged() {
			if (mActionBar != null)
				mCollectionsView = mActionBar.getSelectedNavigationIndex() == COLLECTIONS;

			super.notifyDataSetChanged();
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
			showMovieSection(itemPosition);

		return true;
	}

	private class MovieSectionLoader extends LibrarySectionAsyncTask<Void, Void, Boolean> {
		private int mPosition;
		private ArrayList<Integer> mTempKeys = new ArrayList<Integer>();

		public MovieSectionLoader(int position) {
			mPosition = position;
		}

		@Override
		protected void onPreExecute() {
			setProgressBarVisible(true);
			mMovieKeys.clear();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (isCancelled())
				return false;

			switch (mPosition) {
			case ALL_MOVIES:
				for (int i = 0; i < mMovies.size(); i++) 
					if (!mMovies.get(i).isUnidentified())
						mTempKeys.add(i);
				break;

			case FAVORITES:
				for (int i = 0; i < mMovies.size(); i++)
					if (mMovies.get(i).isFavourite())
						mTempKeys.add(i);
				break;

			case AVAILABLE_FILES:
				ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return false;

					for (Filepath path : mMovies.get(i).getFilepaths()) {
						if (path.isNetworkFile())
							if (mMovies.get(i).hasOfflineCopy(path)) {
								mTempKeys.add(i);
								break; // break inner loop to continue to the next movie
							} else {
								if (path.getType() == FileSource.SMB) {


									if (MizLib.isWifiConnected(getActivity())) {
										FileSource source = null;

										for (int j = 0; j < filesources.size(); j++)
											if (path.getFilepath().contains(filesources.get(j).getFilepath())) {
												source = filesources.get(j);
												continue;
											}

										if (source == null)
											continue;

										try {
											final SmbFile file = new SmbFile(
													MizLib.createSmbLoginString(
															URLEncoder.encode(source.getDomain(), "utf-8"),
															URLEncoder.encode(source.getUser(), "utf-8"),
															URLEncoder.encode(source.getPassword(), "utf-8"),
															path.getFilepath(),
															false
															));
											if (file.exists()) {
												mTempKeys.add(i);
												break; // break inner loop to continue to the next movie
											}
										} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
									}
								} else if (path.getType() == FileSource.UPNP) {
									if (MizLib.exists(path.getFilepath())) {
										mTempKeys.add(i);
										break; // break inner loop to continue to the next movie
									}
								}
							} else {
								if (new File(path.getFilepath()).exists()) {
									mTempKeys.add(i);
									break; // break inner loop to continue to the next movie
								}
							}
					}
				}
				break;

			case COLLECTIONS:
				HashMap<String, MediumMovie> map = new HashMap<String, MediumMovie>();
				for (int i = 0; i < mMovies.size(); i++)
					if (!TextUtils.isEmpty(mMovies.get(i).getCollection()) && !mMovies.get(i).getCollection().equals("null"))
						if (!map.containsKey(mMovies.get(i).getCollection())) {
							map.put(mMovies.get(i).getCollection(), mMovies.get(i));
							mTempKeys.add(i);
						}
				map.clear();
				break;

			case WATCHED_MOVIES:
				for (int i = 0; i < mMovies.size(); i++)
					if (mMovies.get(i).hasWatched())
						mTempKeys.add(i);
				break;

			case UNWATCHED_MOVIES:
				for (int i = 0; i < mMovies.size(); i++)
					if (!mMovies.get(i).hasWatched())
						mTempKeys.add(i);
				break;

			case OFFLINE_COPIES:
				// No point in running through all movies if the folder
				// doesn't contain any files at all - so let's check that first
				File offlineFolder = MizuuApplication.getAvailableOfflineFolder(getActivity());
				if (offlineFolder != null && offlineFolder.listFiles().length == 0)
					return true; // Return true in order to force the notifyDataSetChanged() call

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return false;

					for (Filepath path : mMovies.get(i).getFilepaths())
						if (mMovies.get(i).hasOfflineCopy(path)) {
							mTempKeys.add(i);
							break; // break inner loop to continue to the next movie
						}
				}
				break;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			// Make sure that the loading was successful, that the Fragment is still added and
			// that the currently selected navigation index is the same as when we started loading
			if (success && isAdded() && mActionBar.getSelectedNavigationIndex() == mPosition) {
				mMovieKeys.addAll(mTempKeys);

				sortMovies();
				notifyDataSetChanged();
				setProgressBarVisible(false);
			}
		}
	}

	private void showMovieSection(int position) {
		if (mSpinnerAdapter != null)
			mSpinnerAdapter.notifyDataSetChanged(); // To show "0 movies" when loading

		if (mMovieSectionLoader != null)
			mMovieSectionLoader.cancel(true);
		mMovieSectionLoader = new MovieSectionLoader(position);
		mMovieSectionLoader.execute();
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
			inflater.inflate(R.menu.menu, menu);
			if (mType == OTHER) // Don't show the Update icon if this is the Watchlist
				menu.removeItem(R.id.update);

			if (mMovies.size() == 0)
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
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
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
			if (mActionBar.getSelectedNavigationIndex() == ALL_MOVIES)
				showMovieSection(ALL_MOVIES);
			else
				mActionBar.setSelectedNavigationItem(ALL_MOVIES);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.update:
			startActivityForResult(getUpdateIntent(), 0);
			break;
		case R.id.menuSortAdded:
			sortBy(DATE);
			break;
		case R.id.menuSortRating:
			sortBy(RATING);
			break;
		case R.id.menuSortWeightedRating:
			sortBy(WEIGHTED_RATING);
			break;
		case R.id.menuSortRelease:
			sortBy(RELEASE);
			break;
		case R.id.menuSortTitle:
			sortBy(TITLE);
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
		case R.id.folders:
			showFolders();
			break;
		case R.id.fileSources:
			showFileSources();
			break;
		case R.id.clear_filters:
			showMovieSection(mActionBar.getSelectedNavigationIndex());
			break;
		case R.id.random:
			if (mMovieKeys.size() > 0) {
				int random = new Random().nextInt(mMovieKeys.size());
				showDetails(random);
			}
			break;
		case R.id.unidentifiedFiles:
			startActivity(new Intent(getActivity(), UnidentifiedMovies.class));
			break;
		}

		return true;
	}

	private Intent getUpdateIntent() {
		Intent intent = new Intent();
		intent.setClass(getActivity(), Update.class);
		intent.putExtra("isMovie", true);
		return intent;
	}

	private void sortMovies() {
		if (!isAdded())
			return;

		String SORT_TYPE = mSharedPreferences.getString((getActivity().getActionBar().getSelectedNavigationIndex() == COLLECTIONS) ? SORTING_COLLECTIONS_OVERVIEW : SORTING_MOVIES, "sortTitle");

		if (SORT_TYPE.equals("sortRelease")) {
			sortBy(RELEASE);
		} else if (SORT_TYPE.equals("sortRating")) {
			sortBy(RATING);
		} else if (SORT_TYPE.equals("sortWeightedRating")) {
			sortBy(WEIGHTED_RATING);
		} else if (SORT_TYPE.equals("sortAdded")) {
			sortBy(DATE);
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

		boolean isCollection = getActivity().getActionBar().getSelectedNavigationIndex() == COLLECTIONS;

		String key = isCollection ? SORTING_COLLECTIONS_OVERVIEW : SORTING_MOVIES;
		Editor editor = mSharedPreferences.edit();
		switch (mCurrentSort) {
		case TITLE:
			editor.putString(key, "sortTitle");
			break;
		case RELEASE:
			editor.putString(key, "sortRelease");
			break;
		case RATING:
			editor.putString(key, "sortRating");
			break;
		case WEIGHTED_RATING:
			editor.putString(key, "sortWeightedRating");
			break;
		case DATE:
			editor.putString(key, "sortAdded");
			break;
		case DURATION:
			editor.putString(key, "sortDuration");
			break;
		}
		editor.apply();

		ArrayList<MovieSortHelper> tempHelper = new ArrayList<MovieSortHelper>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			tempHelper.add(new MovieSortHelper(mMovies.get(mMovieKeys.get(i)), mMovieKeys.get(i), mCurrentSort, isCollection));
		}

		Collections.sort(tempHelper);

		mMovieKeys.clear();
		for (int i = 0; i < tempHelper.size(); i++) {
			mMovieKeys.add(tempHelper.get(i).getIndex());
		}

		tempHelper.clear();

		setProgressBarVisible(false);
		notifyDataSetChanged();
	}

	private void showGenres() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		String[] splitGenres;
		for (int i = 0; i < mMovieKeys.size(); i++) {
			if (!mMovies.get(mMovieKeys.get(i)).getGenres().isEmpty()) {
				splitGenres = mMovies.get(mMovieKeys.get(i)).getGenres().split(",");
				for (int j = 0; j < splitGenres.length; j++) {
					if (map.containsKey(splitGenres[j].trim())) {
						map.put(splitGenres[j].trim(), map.get(splitGenres[j].trim()) + 1);
					} else {
						map.put(splitGenres[j].trim(), 1);
					}
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allGenres), R.string.selectGenre, GENRES);
	}

	private void showCertifications() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			String certification = mMovies.get(mMovieKeys.get(i)).getCertification();
			if (!TextUtils.isEmpty(certification)) {
				if (map.containsKey(certification.trim())) {
					map.put(certification.trim(), map.get(certification.trim()) + 1);
				} else {
					map.put(certification.trim(), 1);
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allCertifications), R.string.selectCertification, CERTIFICATION);
	}

	private void showFileSources() {
		ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
		Cursor cursor = dbHelper.fetchAllMovieSources();
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

		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();

		for (int i = 0; i < mMovieKeys.size(); i++) {
			for (Filepath path : mMovies.get(mMovieKeys.get(i)).getFilepaths()) {
				for (int j = 0; j < sources.size(); j++) {
					String source = sources.get(j).getFilepath();

					if (!TextUtils.isEmpty(source) && !TextUtils.isEmpty(path.getFilepath()) && path.getFilepath().contains(source)) {
						if (map.containsKey(source.trim())) {
							map.put(source.trim(), map.get(source.trim()) + 1);
						} else {
							map.put(source.trim(), 1);
						}
					}
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allFileSources), R.string.selectFileSource, FILE_SOURCES);
	}

	private void showFolders() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			for (Filepath path : mMovies.get(mMovieKeys.get(i)).getFilepaths()) {
				String folder = path.getFilepath();
				if (!TextUtils.isEmpty(folder)) {
					if (map.containsKey(folder.trim())) {
						map.put(folder.trim(), map.get(folder.trim()) + 1);
					} else {
						map.put(folder.trim(), 1);
					}
				}
			}
		}

		createAndShowAlertDialog(setupItemArray(map, R.string.allFolders), R.string.selectFolder, FOLDERS);
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

	private void handleDialogOnClick(DialogInterface dialog, int which, int type, CharSequence[] temp) {
		if (which > 0) {
			ArrayList<Integer> currentlyShown = new ArrayList<Integer>(mMovieKeys);
			mMovieKeys.clear();

			String selected = temp[which].toString();
			selected = selected.substring(0, selected.lastIndexOf("(")).trim();

			boolean condition;
			for (int i = 0; i < currentlyShown.size(); i++) {
				condition = false;

				switch (type) {
				case GENRES:
					if (mMovies.get(currentlyShown.get(i)).getGenres().contains(selected)) {
						String[] genres = mMovies.get(currentlyShown.get(i)).getGenres().split(",");
						for (String genre : genres) {
							if (genre.trim().equals(selected)) {
								condition = true;
								break;
							}
						}
					}
					break;
				case CERTIFICATION:
					condition = mMovies.get(currentlyShown.get(i)).getCertification().trim().contains(selected);
					break;
				case FILE_SOURCES:
					for (Filepath path : mMovies.get(currentlyShown.get(i)).getFilepaths()) {
						condition = path.getFilepath().trim().contains(selected);
						if (condition)
							break;
					}
					break;
				case FOLDERS:
					for (Filepath path : mMovies.get(currentlyShown.get(i)).getFilepaths()) {
						condition = path.getFilepath().trim().startsWith(selected);
						if (condition)
							break;
					}
					break;
				}

				if (condition)
					mMovieKeys.add(currentlyShown.get(i));
			}

			sortMovies();
			notifyDataSetChanged();
		}

		dialog.dismiss();
	}

	private void search(String query) {
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
			mMovieKeys.clear();
		}

		@Override
		protected String doInBackground(String... params) {
			mTempKeys = new ArrayList<Integer>();

			if (mSearchQuery.startsWith("actor:")) {
				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (mMovies.get(i).getCast().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
						mTempKeys.add(i);
				}
			} else if (mSearchQuery.equalsIgnoreCase("missing_genres")) {
				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (TextUtils.isEmpty(mMovies.get(i).getGenres()))
						mTempKeys.add(i);
				}
			} else if (mSearchQuery.equalsIgnoreCase("multiple_versions")) {
				DbAdapterMovieMappings db = MizuuApplication.getMovieMappingAdapter();

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (db.hasMultipleFilepaths(mMovies.get(i).getTmdbId()))
						mTempKeys.add(i);
				}
			} else {
				Pattern p = Pattern.compile(MizLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					String lowerCaseTitle = mMovies.get(i).getTitle().toLowerCase(Locale.ENGLISH);

					boolean foundInTitle = false;
					
					if (lowerCaseTitle.indexOf(mSearchQuery) != -1 || p.matcher(lowerCaseTitle).replaceAll("").indexOf(mSearchQuery) != -1) {
						mTempKeys.add(i);
						foundInTitle = true;
					}
					
					if (!foundInTitle) {
						for (Filepath path : mMovies.get(i).getFilepaths()) {
							String filepath = path.getFilepath().toLowerCase(Locale.ENGLISH);
							if (filepath.indexOf(mSearchQuery) != -1) {
								mTempKeys.add(i);
								break; // Break the loop
							}
						}
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mMovieKeys.addAll(mTempKeys);

			sortMovies();
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

		if (resultCode == 1) { // Update
			forceLoaderLoad();
		} else if (resultCode == 2 && mActionBar.getSelectedNavigationIndex() == FAVORITES) { // Favourite removed
			showMovieSection(FAVORITES);
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
			if (getLoaderManager().getLoader(0) == null) {
				getLoaderManager().initLoader(0, null, loaderCallbacks);
			} else {
				getLoaderManager().restartLoader(0, null, loaderCallbacks);
			}
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

			int size = mMovieKeys.size();
			holder.subtitle.setText(size + " " + getResources().getQuantityString((mActionBar.getSelectedNavigationIndex() == COLLECTIONS) ?
					R.plurals.collectionsInLibrary : R.plurals.moviesInLibrary, size, size));

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