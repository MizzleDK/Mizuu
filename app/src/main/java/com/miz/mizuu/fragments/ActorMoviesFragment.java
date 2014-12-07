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
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.functions.CompleteActor;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ActorMoviesFragment extends Fragment {

	private Context mContext;
	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private GridView mGridView;
	private Picasso mPicasso;
	private Config mConfig;
	private ProgressBar mProgressBar;
	private String mActorId;
	private CompleteActor mActor;
    private boolean mChecked = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorMoviesFragment() {}

	public static ActorMoviesFragment newInstance(String actorId) { 
		ActorMoviesFragment pageFragment = new ActorMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("actorId", actorId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
        setHasOptionsMenu(true);

		mContext = getActivity();
		
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();

		mActorId = getArguments().getString("actorId");
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.switch_button, menu);

        int padding = MizLib.convertDpToPixels(getActivity(), 16);

        SwitchCompat switchCompat = (SwitchCompat) menu.findItem(R.id.switch_button).getActionView();
        switchCompat.setChecked(mChecked);
        switchCompat.setText(R.string.inLibrary);
        switchCompat.setSwitchPadding(padding);
        switchCompat.setPadding(0, 0, padding, 0);

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mChecked = isChecked;
                mAdapter.notifyDataSetChanged();
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		mGridView = (GridView) v.findViewById(R.id.gridView);

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
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), arg1.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(getActivity(), IntentUtils.getTmdbMovieDetails(mContext, mAdapter.getItem(arg2)), options.toBundle());
			}
		});

		new MovieLoader(mContext, mActorId).execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

        private ArrayList<WebMovie> mMovies;
		private LayoutInflater mInflater;
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mMovies.size();
		}

		@Override
		public WebMovie getItem(int position) {
			return mMovies.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			final WebMovie movie = getItem(position);
			
			CoverItem holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_cover_two_line, container, false);
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

			holder.text.setText(movie.getTitle());
			holder.subtext.setText(movie.getSubtitle());

			if (!movie.getUrl().contains("null"))
				mPicasso.load(movie.getUrl()).error(R.drawable.loading_image).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}

        @Override
        public void notifyDataSetChanged() {

            // Initialize
            mMovies = new ArrayList<WebMovie>();

            // Go through all movies
            for (WebMovie movie : mActor.getMovies()) {
                if (mChecked && !movie.isInLibrary())
                    continue;

                mMovies.add(movie);
            }

            super.notifyDataSetChanged();
        }
	}

	private class MovieLoader extends AsyncTask<Void, Void, Void> {

		private final Context mContext;
		private final String mActorId;

		public MovieLoader(Context context, String actorId) {
			mContext = context;
			mActorId = actorId;
		}

		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			mActor = TMDbMovieService.getInstance(mContext).getCompleteActorDetails(mActorId);

			for (int i = 0; i < mActor.getMovies().size(); i++) {
				String id = mActor.getMovies().get(i).getId();
				mActor.getMovies().get(i).setInLibrary(MizuuApplication.getMovieAdapter().movieExists(id));
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				
				mAdapter = new ImageAdapter(getActivity());
				mGridView.setAdapter(mAdapter);
			}
		}
	}
}