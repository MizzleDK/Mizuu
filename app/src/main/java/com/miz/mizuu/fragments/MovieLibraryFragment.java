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

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.miz.functions.CoverItem;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.loader.MovieFilter;
import com.miz.loader.MovieLoader;
import com.miz.loader.MovieLibraryType;
import com.miz.loader.MovieSortType;
import com.miz.loader.OnLoadCompletedCallback;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCollection;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.UnidentifiedMovies;
import com.miz.mizuu.Update;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.MovieDatabaseUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class MovieLibraryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private int mImageThumbSize, mImageThumbSpacing;
    private LoaderAdapter mAdapter;
    private ObservableGridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles, mIgnorePrefixes, mLoading = true;
    private Picasso mPicasso;
    private Config mConfig;
    private MovieLoader mMovieLoader;
    private SearchView mSearchView;
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

        setHasOptionsMenu(true);

        mContext = getActivity().getApplicationContext();

        // Set OnSharedPreferenceChange listener
        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);

        // Initialize the PreferenceManager variable and preference variable(s)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
        mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);

        mImageThumbSize = ViewUtils.getGridViewThumbSize(mContext);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mPicasso = MizuuApplication.getPicasso(mContext);
        mConfig = MizuuApplication.getBitmapConfig();

        mAdapter = new LoaderAdapter(mContext);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-actor-search"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMovieLoader != null) {
                if (intent.filterEquals(new Intent("mizuu-movie-actor-search"))) {
                    mMovieLoader.search("actor: " + intent.getStringExtra("intent_extra_data_key"));
                } else {
                    mMovieLoader.load();
                }
                showProgressBar();
            }
        }
    };

    private OnLoadCompletedCallback mCallback = new OnLoadCompletedCallback() {
        @Override
        public void onLoadCompleted() {
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

        mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
        mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
        mEmptyLibraryTitle.setTypeface(TypefaceUtils.getRobotoCondensedRegular(mContext));
        mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);
        mEmptyLibraryDescription.setTypeface(TypefaceUtils.getRobotoLight(mContext));

        mAdapter = new LoaderAdapter(mContext);

        mGridView = (ObservableGridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setColumnWidth(mImageThumbSize);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                viewMovieDetails(arg2, arg1);
            }
        });

        // We only want to display the contextual menu if we're showing movies, not collections
        if (getArguments().getInt("type") != MovieLoader.COLLECTIONS) {
            mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
            mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    mAdapter.setItemChecked(position, checked);

                    mode.setTitle(String.format(getString(R.string.selected),
                            mAdapter.getCheckedItemCount()));
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    getActivity().getMenuInflater().inflate(R.menu.movie_library_cab, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                    int id = item.getItemId();

                    switch (id) {
                        case R.id.movie_add_fav:
                            MovieDatabaseUtils.setMoviesFavourite(mContext, mAdapter.getCheckedMovies(), true);
                            break;
                        case R.id.movie_remove_fav:
                            MovieDatabaseUtils.setMoviesFavourite(mContext, mAdapter.getCheckedMovies(), false);
                            break;
                        case R.id.movie_watched:
                            MovieDatabaseUtils.setMoviesWatched(mContext, mAdapter.getCheckedMovies(), true);
                            break;
                        case R.id.movie_unwatched:
                            MovieDatabaseUtils.setMoviesWatched(mContext, mAdapter.getCheckedMovies(), false);
                            break;
                        case R.id.add_to_watchlist:
                            MovieDatabaseUtils.setMoviesWatchlist(mContext, mAdapter.getCheckedMovies(), true);
                            break;
                        case R.id.remove_from_watchlist:
                            MovieDatabaseUtils.setMoviesWatchlist(mContext, mAdapter.getCheckedMovies(), false);
                            break;
                    }

                    if (!(id == R.id.watched_menu ||
                            id == R.id.watchlist_menu ||
                            id == R.id.favorite_menu)) {
                        mode.finish();

                        LocalBroadcastUtils.updateMovieLibrary(mContext);
                    }

                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mAdapter.clearCheckedItems();
                }
            });
        }

        mMovieLoader = new MovieLoader(mContext, MovieLibraryType.fromInt(getArguments().getInt("type")), mCallback);
        mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
        mMovieLoader.load();
        showProgressBar();

        return v;
    }

    private void viewMovieDetails(int position, View view) {
        Intent intent = new Intent();
        if (mMovieLoader.getType() == MovieLibraryType.COLLECTIONS) { // Collection
            intent.putExtra("collectionId", mAdapter.getItem(position).getCollectionId());
            intent.putExtra("collectionTitle", mAdapter.getItem(position).getCollection());
            intent.setClass(mContext, MovieCollection.class);
            startActivity(intent);
        } else {
            intent.putExtra("tmdbId", mAdapter.getItem(position).getTmdbId());
            intent.setClass(mContext, MovieDetails.class);

            if (view != null) {
                Pair<View, String> pair = new Pair<>(view.findViewById(R.id.cover), "cover");
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair);
                ActivityCompat.startActivityForResult(getActivity(), intent, 0, options.toBundle());
            } else {
                startActivityForResult(intent, 0);
            }
        }
    }

    private class LoaderAdapter extends BaseAdapter {

        private Set<Integer> mChecked = new HashSet<>();
        private LayoutInflater mInflater;
        private final Context mContext;
        private Typeface mTypeface;

        public LoaderAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTypeface = TypefaceUtils.getRobotoMedium(mContext);
        }

        public void setItemChecked(int index, boolean checked) {
            if (checked)
                mChecked.add(index);
            else
                mChecked.remove(index);

            notifyDataSetChanged();
        }

        public void clearCheckedItems() {
            mChecked.clear();
            notifyDataSetChanged();
        }

        public int getCheckedItemCount() {
            return mChecked.size();
        }

        public List<MediumMovie> getCheckedMovies() {
            List<MediumMovie> movies = new ArrayList<>(mChecked.size());
            for (Integer i : mChecked)
                movies.add(getItem(i));
            return movies;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0 && !mLoading;
        }

        @Override
        public int getCount() {
            if (mMovieLoader != null)
                return mMovieLoader.getResults().size();
            return 0;
        }

        @Override
        public MediumMovie getItem(int position) {
            return mMovieLoader.getResults().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            final MediumMovie movie = getItem(position);

            CoverItem holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.grid_cover, container, false);
                holder = new CoverItem();

                holder.cardview = (CardView) convertView.findViewById(R.id.card);
                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.text.setTypeface(mTypeface);

                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            if (!mShowTitles) {
                holder.text.setVisibility(View.GONE);
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.text.setText(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
                        movie.getCollection() : movie.getTitle());
            }

            holder.cover.setImageResource(R.color.card_background_dark);

            mPicasso.load(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
                    movie.getCollectionPoster() : movie.getThumbnail()).placeholder(R.drawable.bg).config(mConfig).into(holder);

            if (mChecked.contains(position)) {
                holder.cardview.setForeground(getResources().getDrawable(R.drawable.checked_foreground_drawable));
            } else {
                holder.cardview.setForeground(null);
            }

            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();

            // Hide the progress bar once the data set has been changed
            hideProgressBar();

            if (isEmpty()) {
                showEmptyView();
            } else {
                hideEmptyView();
            }
        }
    }

    private void onSearchViewCollapsed() {
        mMovieLoader.load();
        showProgressBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        menu.findItem(R.id.random).setVisible(mMovieLoader.getType() != MovieLibraryType.COLLECTIONS);

        if (mMovieLoader.getType() == MovieLibraryType.COLLECTIONS) {
            menu.findItem(R.id.sort).setVisible(false);
            menu.findItem(R.id.filters).setVisible(false);
        }

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

        mSearchView = (SearchView) menu.findItem(R.id.search_textbox).getActionView();
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 0) {
                    mMovieLoader.search(newText);
                    showProgressBar();
                } else {
                    onSearchViewCollapsed();
                }
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
        });

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.update:
                Intent intent = new Intent();
                intent.setClass(mContext, Update.class);
                intent.putExtra("isMovie", true);
                startActivityForResult(intent, 0);
                break;
            case R.id.menuSortAdded:
                mMovieLoader.setSortType(MovieSortType.DATE_ADDED);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRating:
                mMovieLoader.setSortType(MovieSortType.RATING);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortWeightedRating:
                mMovieLoader.setSortType(MovieSortType.WEIGHTED_RATING);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortRelease:
                mMovieLoader.setSortType(MovieSortType.RELEASE);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortTitle:
                mMovieLoader.setSortType(MovieSortType.TITLE);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.menuSortDuration:
                mMovieLoader.setSortType(MovieSortType.DURATION);
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.genres:
                mMovieLoader.showGenresFilterDialog(getActivity());
                break;
            case R.id.certifications:
                mMovieLoader.showCertificationsFilterDialog(getActivity());
                break;
            case R.id.folders:
                mMovieLoader.showFoldersFilterDialog(getActivity());
                break;
            case R.id.fileSources:
                mMovieLoader.showFileSourcesFilterDialog(getActivity());
                break;
            case R.id.release_year:
                mMovieLoader.showReleaseYearFilterDialog(getActivity());
                break;
            case R.id.offline_files:
                mMovieLoader.addFilter(new MovieFilter(MovieFilter.OFFLINE_FILES));
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.available_files:
                mMovieLoader.addFilter(new MovieFilter(MovieFilter.AVAILABLE_FILES));
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.clear_filters:
                mMovieLoader.clearFilters();
                mMovieLoader.load();
                showProgressBar();
                break;
            case R.id.random:
                if (mAdapter.getCount() > 0) {
                    int random = new Random().nextInt(mAdapter.getCount());
                    viewMovieDetails(random, null);
                }
                break;
            case R.id.unidentifiedFiles:
                startActivity(new Intent(mContext, UnidentifiedMovies.class));
                break;
        }

        return true;
    }

    private void hideProgressBar() {
        mGridView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mLoading = false;
    }

    private void showProgressBar() {
        mGridView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mLoading = true;
    }

    private void showEmptyView() {
        mGridView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mEmptyLibraryLayout.setVisibility(View.VISIBLE);

        if (mMovieLoader.isShowingSearchResults()) {
            mEmptyLibraryTitle.setText(R.string.no_search_results);
            mEmptyLibraryDescription.setText(R.string.no_search_results_description);
        } else {
            switch (mMovieLoader.getType()) {
                case ALL_MOVIES:
                    mEmptyLibraryTitle.setText(R.string.no_movies);
                    mEmptyLibraryDescription.setText(MizLib.isTablet(mContext) ?
                            R.string.no_movies_description_tablet : R.string.no_movies_description);
                    break;
                case FAVORITES:
                    mEmptyLibraryTitle.setText(R.string.no_favorites);
                    mEmptyLibraryDescription.setText(R.string.no_favorites_description);
                    break;
                case NEW_RELEASES:
                    mEmptyLibraryTitle.setText(R.string.no_new_releases);
                    mEmptyLibraryDescription.setText(R.string.no_new_releases_description);
                    break;
                case WATCHLIST:
                    mEmptyLibraryTitle.setText(R.string.empty_watchlist);
                    mEmptyLibraryDescription.setText(R.string.empty_watchlist_description);
                    break;
                case WATCHED:
                    mEmptyLibraryTitle.setText(R.string.no_watched_movies);
                    mEmptyLibraryDescription.setText(R.string.no_watched_movies_description);
                    break;
                case UNWATCHED:
                    mEmptyLibraryTitle.setText(R.string.no_unwatched_movies);
                    mEmptyLibraryDescription.setText(R.string.no_unwatched_movies_description);
                    break;
                case COLLECTIONS:
                    mEmptyLibraryTitle.setText(R.string.no_movie_collections);
                    mEmptyLibraryDescription.setText(R.string.no_movie_collections_description);
                    break;
            }
        }
    }

    private void hideEmptyView() {
        mEmptyLibraryLayout.setVisibility(View.GONE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(IGNORED_TITLE_PREFIXES)) {
            mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);

            if (mMovieLoader != null) {
                mMovieLoader.setIgnorePrefixes(mIgnorePrefixes);
                mMovieLoader.load();
            }

        } else if (key.equals(GRID_ITEM_SIZE)) {
            mImageThumbSize = ViewUtils.getGridViewThumbSize(mContext);

            if (mGridView != null)
                mGridView.setColumnWidth(mImageThumbSize);

            mAdapter.notifyDataSetChanged();
        } else if (key.equals(SHOW_TITLES_IN_GRID)) {
            mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
            mAdapter.notifyDataSetChanged();
        }
    }
}