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

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ScrollView;
import android.widget.TextView;

import com.miz.abstractclasses.MovieApiService;
import com.miz.apis.tmdb.Movie;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteTransformation;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.miz.views.PanningView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class TmdbMovieDetailsFragment extends Fragment {

	private String movieId;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private ImageView background, cover;
	private Movie thisMovie;
	private View movieDetailsLayout, progressBar, mDetailsArea;
	private FrameLayout container;
	private boolean isRetained = false;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium, mBoldItalic;
	private Bus mBus;
	private MovieApiService mMovieApiService;

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

		mBus = MizuuApplication.getBus();

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		mLightItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-LightItalic.ttf");
		mMedium = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Medium.ttf");
		mBoldItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-BoldItalic.ttf");

		mMovieApiService = MizuuApplication.getMovieService(getActivity());

		// Get the database ID of the movie in question
		movieId = getArguments().getString("movieId");

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();

		mBus.register(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.movie_and_tv_show_details, container, false);

		movieDetailsLayout = MizLib.isPortrait(getActivity()) ? v.findViewById(R.id.movieDetailsLayout_portrait) : v.findViewById(R.id.movieDetailsLayout);
		progressBar = v.findViewById(R.id.progressBar1);
		mDetailsArea = v.findViewById(R.id.details_area);
		
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

		if (MizLib.isPortrait(getActivity())) {
			final boolean fullscreen = MizuuApplication.isFullscreen(getActivity());
			final int height = fullscreen ? MizLib.getActionBarHeight(getActivity()) : MizLib.getActionBarAndStatusBarHeight(getActivity());

			ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = background.getHeight() - height;
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);

					// We only want to update the ActionBar if it would actually make a change (times 1.2 to handle fast flings)
					if (t <= headerHeight * 1.2) {
						mBus.post(Integer.valueOf(newAlpha));

						// Such parallax, much wow
						background.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		}

		if (!isRetained) { // Nothing has been retained - load the data
			setLoading(true);
			new MovieLoader().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, getArguments().getString("json"));
			isRetained = true;
		} else {
			setupFields();
		}

		return v;
	}

	private class MovieLoader extends AsyncTask<String, Object, Object> {
		@Override
		protected Object doInBackground(String... params) {
			thisMovie = mMovieApiService.get(movieId, params[0], "en");
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
			textReleaseDate.setTypeface(mLight);

			textRuntime.setTypeface(mMedium);
			textCertification.setTypeface(mMedium);
			textRating.setTypeface(mMedium);

			// Set the movie plot
			if (!MizLib.isPortrait(getActivity())) {
				textPlot.setBackgroundResource(R.drawable.selectable_background);
				if (!thisMovie.getTagline().isEmpty())
					textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.movie_details_max_lines));
				else
					textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
				textPlot.setTag(true); // true = collapsed
				textPlot.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (((Boolean) textPlot.getTag())) {
							textPlot.setMaxLines(1000);
							textPlot.setTag(false);
						} else {
							if (!thisMovie.getTagline().isEmpty())
								textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.movie_details_max_lines));
							else
								textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
							textPlot.setTag(true);
						}
					}
				});
				textPlot.setEllipsize(TextUtils.TruncateAt.END);
				textPlot.setFocusable(true);
			} else {
				if (MizLib.isTablet(getActivity()))
					textPlot.setLineSpacing(0, 1.15f);
			}
			textPlot.setText(thisMovie.getPlot());

			// Set movie tag line
			textTagline.setTypeface(mBoldItalic);
			if (thisMovie.getTagline().equals("NOTAGLINE") || thisMovie.getTagline().isEmpty())
				textTagline.setVisibility(TextView.GONE);
			else
				textTagline.setText(thisMovie.getTagline());

			// Set the movie genre
			textGenre.setTypeface(mLightItalic);
			if (!TextUtils.isEmpty(thisMovie.getGenres())) {
				textGenre.setText(thisMovie.getGenres());
			} else {
				textGenre.setVisibility(View.GONE);
			}

			// Set the movie runtime
			textRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(thisMovie.getRuntime())));

			// Set the movie release date
			textReleaseDate.setTypeface(mMedium);
			textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisMovie.getReleasedate()));

			// Set the movie rating
			if (!thisMovie.getRating().equals("0.0")) {
				try {
					int rating = (int) (Double.parseDouble(thisMovie.getRating()) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(thisMovie.getRating());
				}
			} else {
				textRating.setText(R.string.stringNA);
			}

			// Set the movie certification
			if (!TextUtils.isEmpty(thisMovie.getCertification())) {
				textCertification.setText(thisMovie.getCertification());
			} else {
				textCertification.setText(R.string.stringNA);
			}

			setLoading(false);

			if (!thisMovie.getCover().isEmpty())
				mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.gray).error(R.drawable.loading_image).transform(new PaletteTransformation(thisMovie.getCover(), mDetailsArea)).into(cover);
			else
				cover.setImageResource(R.drawable.gray);

			if (!thisMovie.getBackdrop().isEmpty())
				mPicasso.load(thisMovie.getBackdrop()).placeholder(R.drawable.gray).error(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						if (!isAdded())
							return;

						if (MizLib.isPortrait(getActivity())) {
							((PanningView) background).setScaleType(ScaleType.CENTER_CROP);
						}

						if (!thisMovie.getCover().isEmpty())
							mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
						else
							background.setImageResource(R.drawable.bg);
					}

					@Override
					public void onSuccess() {						
						if (!isAdded())
							return;

						if (MizLib.isPortrait(getActivity())) {
							((PanningView) background).startPanning();
						}
					}
				});
			else {
				if (!thisMovie.getCover().isEmpty()) {
					if (MizLib.isPortrait(getActivity())) {
						((PanningView) background).setScaleType(ScaleType.CENTER_CROP);
					}
					mPicasso.load(thisMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
				} else
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
