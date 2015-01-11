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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;

public class MovieDiscoveryFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebMovie> mMovies = new ArrayList<WebMovie>();
	private SparseBooleanArray mMovieMap = new SparseBooleanArray();
	private ObservableGridView mGridView = null;
	private ProgressBar mProgressBar;
	private DbAdapterMovies mDatabase;
	private Picasso mPicasso;
	private String mJson, mBaseUrl;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDiscoveryFragment() {}

	public static MovieDiscoveryFragment newInstance(String type, String json, String baseUrl) { 
		MovieDiscoveryFragment pageFragment = new MovieDiscoveryFragment();
		Bundle bundle = new Bundle();
		bundle.putString("type", type);
		bundle.putString("json", json);
		bundle.putString("baseUrl", baseUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		mDatabase = MizuuApplication.getMovieAdapter();

		String thumbnailSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(GRID_ITEM_SIZE, getString(R.string.normal));
		if (thumbnailSize.equals(getString(R.string.large))) 
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
		else if (thumbnailSize.equals(getString(R.string.normal))) 
			mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1;
		else
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		mProgressBar.setVisibility(View.GONE);

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (ObservableGridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setColumnWidth(mImageThumbSize);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0)
								mAdapter.setNumColumns(numColumns);

							MizLib.removeViewTreeObserver(mGridView.getViewTreeObserver(), this);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Intent i = new Intent();

				if (mMovieMap.get(Integer.valueOf(mMovies.get(arg2).getId()))) {
					i.setClass(getActivity(), MovieDetails.class);
					i.putExtra("tmdbId", mMovies.get(arg2).getId());
				} else {
					i.setClass(getActivity(), TMDbMovieDetails.class);
					i.putExtra("tmdbId", mMovies.get(arg2).getId());
					i.putExtra("title", mMovies.get(arg2).getTitle());
				}

                ActivityOptionsCompat  options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), arg1.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(getActivity(), i, options.toBundle());
			}
		});

		if (getArguments().containsKey("json")) {
			mJson = getArguments().getString("json");
			mBaseUrl = getArguments().getString("baseUrl");
			loadJson();
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mMovies.size();
		}

		@Override
		public Object getItem(int position) {
			return mMovies.get(position).getUrl();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			final WebMovie mMovie = mMovies.get(position);
			CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_cover_two_line, container, false);
				holder = new CoverItem();

				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.text.setSingleLine(true);
				holder.subtext = (TextView) convertView.findViewById(R.id.sub_text);
				holder.subtext.setSingleLine(true);

				holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(R.color.card_background_dark);
			holder.text.setText(mMovie.getTitle());

			if (mMovieMap.get(Integer.valueOf(mMovie.getId()))) {
				holder.subtext.setText(mMovie.getDate() + " (" + getString(R.string.inLibrary) + ")");
			} else {
				holder.subtext.setText(mMovie.getDate());
			}

			if (!mMovie.getUrl().contains("null"))
				mPicasso.load(mMovie.getUrl()).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private void loadJson() {
		try {
			JSONObject jObject = new JSONObject(mJson);

			JSONArray jArray = jObject.getJSONObject(getArguments().getString("type")).getJSONArray("results");

			mMovies.clear();
			for (int i = 0; i < jArray.length(); i++) {
				if (!MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("title")) && !MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("original_title"))) {
					mMovies.add(new WebMovie(getActivity().getApplicationContext(),
							jArray.getJSONObject(i).getString("original_title"),
							jArray.getJSONObject(i).getString("id"),
							mBaseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path"),
							MizLib.getPrettyDate(getActivity(), jArray.getJSONObject(i).getString("release_date"))));
				}
			}

			new MoviesInLibraryCheck(mMovies).execute();
		} catch (Exception e) {}
	}

	private class MoviesInLibraryCheck extends AsyncTask<Void, Void, Void> {

		private ArrayList<WebMovie> movies = new ArrayList<WebMovie>();

		public MoviesInLibraryCheck(ArrayList<WebMovie> movies) {
			this.movies = movies;
			mMovieMap.clear();
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < movies.size(); i++)
				mMovieMap.put(Integer.valueOf(movies.get(i).getId()), mDatabase.movieExists(movies.get(i).getId()));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(GRID_ITEM_SIZE)) {
			String thumbnailSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(GRID_ITEM_SIZE, getString(R.string.normal));
			if (thumbnailSize.equals(getString(R.string.large))) 
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
			else if (thumbnailSize.equals(getString(R.string.normal))) 
				mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1;
			else
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);

			mGridView.setColumnWidth(mImageThumbSize);

			final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
			if (numColumns > 0) {
				mAdapter.setNumColumns(numColumns);
			}

			mAdapter.notifyDataSetChanged();
		}
	}
}