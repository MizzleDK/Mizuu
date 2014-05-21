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

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.miz.functions.MizLib;
import com.miz.functions.TMDb;
import com.miz.functions.TMDbMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class TmdbMovieDetailsFragment extends Fragment {

	private String movieId;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private ImageView background, cover;
	private TMDb tmdb;
	private TMDbMovie thisMovie;
	private View movieDetailsLayout, progressBar;
	private FrameLayout container;
	private boolean isRetained = false;
	private Picasso mPicasso;
	private Typeface mLight;
	private Drawable mActionBarBackgroundDrawable;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public TmdbMovieDetailsFragment() {}

	public static TmdbMovieDetailsFragment newInstance(String movieId) { 
		TmdbMovieDetailsFragment pageFragment = new TmdbMovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	public static TmdbMovieDetailsFragment newInstance(String movieId, String json) { 
		TmdbMovieDetailsFragment pageFragment = new TmdbMovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		bundle.putString("json", json);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		
		tmdb = new TMDb(getActivity());

		// Get the database ID of the movie in question
		movieId = getArguments().getString("movieId");

		mPicasso = MizuuApplication.getPicasso(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.movie_details, container, false);

		movieDetailsLayout = v.findViewById(R.id.movieDetailsLayout);
		progressBar = v.findViewById(R.id.progressBar1);

		this.container = (FrameLayout) v.findViewById(R.id.container);
		background = (ImageView) v.findViewById(R.id.imageBackground);
		textTitle = (TextView) v.findViewById(R.id.movieTitle);
		textPlot = (TextView) v.findViewById(R.id.textView2);
		textGenre = (TextView) v.findViewById(R.id.textView7);
		textRuntime = (TextView) v.findViewById(R.id.textView9);
		textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
		textRating = (TextView) v.findViewById(R.id.textView12);
		textTagline = (TextView) v.findViewById(R.id.textView6);
		textCertification = (TextView) v.findViewById(R.id.textView11);
		cover = (ImageView) v.findViewById(R.id.traktIcon);
		cover.setImageResource(R.drawable.loading_image);

		// Get rid of these...
		v.findViewById(R.id.textView3).setVisibility(View.GONE); // File

		if (!isRetained) { // Nothing has been retained - load the data
			setLoading(true);
			new MovieLoader().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, getArguments().getString("json"));
			isRetained = true;
		} else {
			setupFields();
		}

		return v;
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		if (MizLib.isPortrait(getActivity())) {
			mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x00000000, 0xaa000000});
			getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
			
			ObservableScrollView sv = (ObservableScrollView) view.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = view.findViewById(R.id.imageBackground).getHeight() - getActivity().getActionBar().getHeight();
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);
					mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "080808"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "080808") : 0xaa080808});
					getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
				}
			});
		}
	}

	private class MovieLoader extends AsyncTask<String, Object, Object> {
		@Override
		protected Object doInBackground(String... params) {
			thisMovie = tmdb.getMovie(movieId, params[0], "en");
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			setupFields();
		}
	}

	private void setupFields() {
		if (isAdded() && thisMovie != null) {
			// Set the movie title
			textTitle.setVisibility(View.VISIBLE);
			textTitle.setText(thisMovie.getTitle());
			textTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
			textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			textPlot.setTypeface(mLight);
			textGenre.setTypeface(mLight);
			textRuntime.setTypeface(mLight);
			textReleaseDate.setTypeface(mLight);
			textRating.setTypeface(mLight);
			textCertification.setTypeface(mLight);
			
			// Set the movie plot
			textPlot.setText(thisMovie.getPlot());

			// Set movie tag line
			if (thisMovie.getTagline().equals("NOTAGLINE") || thisMovie.getTagline().isEmpty())
				textTagline.setVisibility(TextView.GONE);
			else
				textTagline.setText(thisMovie.getTagline());

			// Set the movie genre
			if (!MizLib.isEmpty(thisMovie.getGenres())) {
				textGenre.setText(thisMovie.getGenres());
			} else {
				textGenre.setText(R.string.stringNA);
			}

			// Set the movie runtime
			textRuntime.setText(MizLib.getPrettyTime(getActivity(), Integer.parseInt(thisMovie.getRuntime())));

			// Set the movie release date
			textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisMovie.getReleasedate()));

			// Set the movie rating
			if (!thisMovie.getRating().equals("0.0")) {
				try {
					int rating = (int) (Double.parseDouble(thisMovie.getRating()) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					thisMovie.setRating(thisMovie.getRating() + "/10");
					textRating.setText(Html.fromHtml(thisMovie.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				textRating.setText(R.string.stringNA);
			}

			// Set the movie certification
			if (!MizLib.isEmpty(thisMovie.getCertification())) {
				textCertification.setText(thisMovie.getCertification());
			} else {
				textCertification.setText(R.string.stringNA);
			}

			setLoading(false);

			if (!thisMovie.getCover().isEmpty())
				mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.gray).error(R.drawable.loading_image).into(cover);
			else
				cover.setImageResource(R.drawable.gray);

			if (!thisMovie.getBackdrop().isEmpty())
				mPicasso.load(thisMovie.getBackdrop()).placeholder(R.drawable.gray).error(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						if (!thisMovie.getCover().isEmpty())
							mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
						else
							background.setImageResource(R.drawable.bg);
					}

					@Override
					public void onSuccess() {}
				});
			else {
				if (!thisMovie.getCover().isEmpty())
					mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
				else
					background.setImageResource(R.drawable.bg);
			}
		}
	}

	private void setLoading(boolean isLoading) {
		if (isLoading) {
			progressBar.setVisibility(View.VISIBLE);
			movieDetailsLayout.setVisibility(View.GONE);
			container.setBackgroundResource(R.drawable.bg);
		} else {
			progressBar.setVisibility(View.GONE);
			movieDetailsLayout.setVisibility(View.VISIBLE);
			container.setBackgroundResource(0);
		}
	}
}
