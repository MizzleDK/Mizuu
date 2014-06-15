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

import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
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

import com.miz.db.DbAdapter;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.squareup.picasso.Picasso;

public class RelatedMoviesFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebMovie> pics_sources = new ArrayList<WebMovie>();
	private SparseBooleanArray movieMap = new SparseBooleanArray();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private Picasso mPicasso;
	private DbAdapter db;
	private String json, mTmdbApiKey;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public RelatedMoviesFragment() {}

	public static RelatedMoviesFragment newInstance(String tmdbId, boolean setBackground) { 
		RelatedMoviesFragment pageFragment = new RelatedMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("tmdbId", tmdbId);
		bundle.putBoolean("setBackground", setBackground);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	public static RelatedMoviesFragment newInstance(String tmdbId, boolean setBackground, String json, String baseUrl) { 
		RelatedMoviesFragment pageFragment = new RelatedMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("tmdbId", tmdbId);
		bundle.putBoolean("setBackground", setBackground);
		bundle.putString("json", json);
		bundle.putString("baseUrl", baseUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		db = MizuuApplication.getMovieAdapter();

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mTmdbApiKey = MizLib.getTmdbApiKey(getActivity());
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		v.findViewById(R.id.container).setBackgroundResource(MizuuApplication.getBackgroundColorResource(getActivity()));

		MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (pics_sources.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(
								mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
						if (numColumns > 0) {
							mGridView.setNumColumns(numColumns);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (movieMap.get(Integer.valueOf(pics_sources.get(arg2).getId()))) {
					Intent intent = new Intent();
					intent.setClass(getActivity(), MovieDetails.class);
					intent.putExtra("tmdbId", pics_sources.get(arg2).getId());

					// Start the Intent for result
					startActivityForResult(intent, 0);
				} else {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setClass(getActivity(), TMDbMovieDetails.class);
					i.putExtra("tmdbid", pics_sources.get(arg2).getId());
					i.putExtra("title", pics_sources.get(arg2).getTitle());
					startActivity(i);
				}
			}
		});

		if (getArguments().containsKey("json")) {
			json = getArguments().getString("json");
			loadJson(getArguments().getString("baseUrl"));
		} else {
			new GetMovies().execute(getArguments().getString("tmdbId"));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mCard, mCardBackground, mCardTitleColor;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mCard = MizuuApplication.getCardDrawable(mContext);
			mCardBackground = MizuuApplication.getCardColor(mContext);
			mCardTitleColor = MizuuApplication.getCardTitleColor(mContext);
		}

		@Override
		public int getCount() {
			return pics_sources.size();
		}

		@Override
		public Object getItem(int position) {
			return pics_sources.get(position).getUrl();
		}

		@Override
		public long getItemId(int position) {
			return position;
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

			holder.cover.setImageResource(mCardBackground);
			
			holder.text.setText(pics_sources.get(position).getTitle());

			if (movieMap.get(Integer.valueOf(pics_sources.get(position).getId()))) {
				holder.subtext.setText(MizLib.getPrettyDate(mContext, pics_sources.get(position).getDate()) + " (" + getString(R.string.inLibrary) + ")");
			} else {
				holder.subtext.setText(MizLib.getPrettyDate(mContext, pics_sources.get(position).getDate()));
			}

			if (!pics_sources.get(position).getUrl().contains("null"))
				mPicasso.load(pics_sources.get(position).getUrl()).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}
	}

	protected class GetMovies extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet("https://api.themoviedb.org/3/configuration?api_key=" + mTmdbApiKey);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String baseUrl = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(baseUrl);
				try { baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) { baseUrl = MizLib.TMDB_BASE_URL; }

				httppost = new HttpGet("https://api.themoviedb.org/3/movie/" + params[0] + "/similar_movies?api_key=" + mTmdbApiKey);
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				jObject = new JSONObject(html);

				JSONArray jArray = jObject.getJSONArray("results");

				pics_sources.clear();
				for (int i = 0; i < jArray.length(); i++) {
					if (!MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("title")) && !MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("original_title"))) {
						pics_sources.add(new WebMovie(getActivity().getApplicationContext(),
								jArray.getJSONObject(i).getString("original_title"),
								jArray.getJSONObject(i).getString("id"),
								baseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path"),
								jArray.getJSONObject(i).getString("release_date")));
					}
				}
			} catch (Exception e) { e.printStackTrace(); }

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				new MoviesInLibraryCheck(pics_sources).execute();
			}
		}
	}

	private void loadJson(String baseUrl) {
		try {
			JSONObject jObject = new JSONObject(json);

			JSONArray jArray = jObject.getJSONObject("similar_movies").getJSONArray("results");

			pics_sources.clear();

			for (int i = 0; i < jArray.length(); i++) {
				pics_sources.add(new WebMovie(getActivity().getApplicationContext(), 
						jArray.getJSONObject(i).getString("original_title"),
						jArray.getJSONObject(i).getString("id"),
						baseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path"),
						jArray.getJSONObject(i).getString("release_date")));
			}
		} catch (Exception e) {}

		Collections.sort(pics_sources, MizLib.getWebMovieDateComparator());

		new MoviesInLibraryCheck(pics_sources).execute();
	}

	private class MoviesInLibraryCheck extends AsyncTask<Void, Void, Void> {

		private ArrayList<WebMovie> movies = new ArrayList<WebMovie>();

		public MoviesInLibraryCheck(ArrayList<WebMovie> movies) {
			this.movies = movies;
			movieMap.clear();
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < movies.size(); i++)
				movieMap.put(Integer.valueOf(movies.get(i).getId()), db.movieExists(movies.get(i).getId()));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}