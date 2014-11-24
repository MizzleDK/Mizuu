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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
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
import android.widget.TextView;

import com.miz.functions.CoverItem;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.functions.MovieLoader;
import com.miz.functions.MovieLoader.MovieLibraryType;
import com.miz.functions.MovieLoader.OnLoadCompletedCallback;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCollection;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.UnidentifiedMovies;
import com.miz.mizuu.Update;
import com.miz.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class NewMovieLibraryFragment extends Fragment {

    private SharedPreferences mSharedPreferences;
    private int mImageThumbSize, mImageThumbSpacing;
    private LoaderAdapter mAdapter;
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles, mLoading = true;
    private Picasso mPicasso;
    private Config mConfig;
    private View mEmptyLibraryLayout;
    private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;
    private MovieLoader mMovieLoader;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public NewMovieLibraryFragment() {}

    public static NewMovieLibraryFragment newInstance(int type) {
        NewMovieLibraryFragment frag = new NewMovieLibraryFragment();
        Bundle b = new Bundle();
        b.putInt("type", type);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Initialize the PreferenceManager variable and preference variable(s)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

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

        mAdapter = new LoaderAdapter(getActivity().getApplicationContext());
    }

    private OnLoadCompletedCallback callback = new OnLoadCompletedCallback() {
        @Override
        public void onLoadCompleted(List<MediumMovie> movieList) {
            System.out.println("SIZE: " + movieList.size());
            mLoading = false;
            mAdapter.updateDataSet(movieList);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

        mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
        mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
        mEmptyLibraryTitle.setTypeface(TypefaceUtils.getRobotoCondensedRegular(getActivity()));
        mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);

        mEmptyLibraryDescription.setTypeface(TypefaceUtils.getRobotoLight(getActivity()));

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
                            }

                            MizLib.removeViewTreeObserver(mGridView.getViewTreeObserver(), this);
                        }
                    }
                });
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                viewMovieDetails(arg2, arg1);
            }
        });

        mMovieLoader = new MovieLoader(getActivity(), MovieLibraryType.fromInt(getArguments().getInt("type")), callback);
        mMovieLoader.load();
        mLoading = true;

        return v;
    }

    private void viewMovieDetails(int position, View view) {
        Intent intent = new Intent();
        if (mMovieLoader.getType() == MovieLibraryType.COLLECTIONS) { // Collection
            intent.putExtra("collectionId", mAdapter.getItem(position).getCollectionId());
            intent.putExtra("collectionTitle", mAdapter.getItem(position).getCollection());
            intent.setClass(getActivity(), MovieCollection.class);
            startActivity(intent);
        } else {
            intent.putExtra("tmdbId", mAdapter.getItem(position).getTmdbId());
            intent.setClass(getActivity(), MovieDetails.class);

            if (view != null) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), view.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivityForResult(getActivity(), intent, 0, options.toBundle());
            } else {
                startActivityForResult(intent, 0);
            }
        }
    }

    private class LoaderAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private final Context mContext;
        private int mNumColumns = 0;
        private ArrayList<MediumMovie> mMovies = new ArrayList<MediumMovie>();

        public LoaderAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void updateDataSet(List<MediumMovie> movies) {
            mMovies.clear();
            mMovies.addAll(movies);
            notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
        }

        @Override
        public boolean isEmpty() {
            return mMovies.size() == 0 && !mLoading;
        }

        @Override
        public int getCount() {
            return mMovies.size();
        }

        @Override
        public MediumMovie getItem(int position) {
            return mMovies.get(position);
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
                convertView = mInflater.inflate(R.layout.grid_item, container, false);
                holder = new CoverItem();

                holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
                holder.subtext.setSingleLine(true);

                holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            if (!mShowTitles) {
                holder.text.setVisibility(View.GONE);
                holder.subtext.setVisibility(View.GONE);
            } else {
                holder.text.setVisibility(View.VISIBLE);

                holder.text.setText(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
                        movie.getCollection() : movie.getTitle());
                holder.text.setSingleLine(true);
                holder.text.setLines(1);
                holder.text.setMaxLines(1);
                holder.subtext.setVisibility(View.VISIBLE);
            }

            holder.cover.setImageResource(R.color.card_background_dark);

            mPicasso.load(mMovieLoader.getType() == MovieLibraryType.COLLECTIONS ?
                    movie.getCollectionPoster() : movie.getThumbnail()).config(mConfig).into(holder);

            return convertView;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }

    private void onSearchViewCollapsed() {
        mMovieLoader.load();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        menu.findItem(R.id.random).setVisible(mMovieLoader.getType() != MovieLibraryType.COLLECTIONS);

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
                    mMovieLoader.search(newText);
                } else {
                    onSearchViewCollapsed();
                }
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.update:
                Intent intent = new Intent();
                intent.setClass(getActivity(), Update.class);
                intent.putExtra("isMovie", true);
                startActivityForResult(intent, 0);
                break;
            case R.id.clear_filters:
                break;
            case R.id.random:
                if (mAdapter.getCount() > 0) {
                    int random = new Random().nextInt(mAdapter.getCount());
                    viewMovieDetails(random, null);
                }
                break;
            case R.id.unidentifiedFiles:
                startActivity(new Intent(getActivity(), UnidentifiedMovies.class));
                break;
        }

        return true;
    }
}