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
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.miz.abstractclasses.MovieApiService;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SimilarMoviesFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private List<WebMovie> mSimilarMovies = new ArrayList<WebMovie>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public SimilarMoviesFragment() {}

	public static SimilarMoviesFragment newInstance(String tmdbId) { 
		SimilarMoviesFragment pageFragment = new SimilarMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("tmdbId", tmdbId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mGridView = (GridView) v.findViewById(R.id.gridView);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mSimilarMovies.size() > 0)
			mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

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
				Intent i = IntentUtils.getTmdbMovieDetails(getActivity(), mSimilarMovies.get(arg2));
				startActivityForResult(i, 0);
			}
		});

		new GetMovies(getActivity()).execute(getArguments().getString("tmdbId"));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mSimilarMovies.size();
		}

		@Override
		public Object getItem(int position) {
			return mSimilarMovies.get(position).getUrl();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

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
			holder.text.setText(mSimilarMovies.get(position).getTitle());
			holder.subtext.setText(mSimilarMovies.get(position).getSubtitle());

			if (!mSimilarMovies.get(position).getUrl().contains("null"))
				mPicasso.load(mSimilarMovies.get(position).getUrl()).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}
	}

	protected class GetMovies extends AsyncTask<String, String, String> {
		
		private Context mContext;
		
		public GetMovies(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				MovieApiService service = MizuuApplication.getMovieService(mContext);
				mSimilarMovies = service.getSimilarMovies(params[0]);

				for (int i = 0; i < mSimilarMovies.size(); i++) {
					String id = mSimilarMovies.get(i).getId();
					mSimilarMovies.get(i).setInLibrary(MizuuApplication.getMovieAdapter().movieExists(id));
				}
			} catch (Exception e) { e.printStackTrace(); }

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}