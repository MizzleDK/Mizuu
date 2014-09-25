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

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.apis.tmdb.Movie;
import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.apis.trakt.Trakt;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteTransformation;
import com.miz.functions.TmdbTrailerSearch;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.miz.views.PanningView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class TmdbMovieDetailsFragment extends Fragment {

	private Context mContext;
	private String movieId;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private ImageView background, cover;
	private Movie mMovie;
	private View movieDetailsLayout, progressBar, mDetailsArea;
	private FrameLayout container;
	private boolean isRetained = false;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium, mBoldItalic;
	private TMDbMovieService mMovieApiService;
	private HorizontalCardLayout mActorsLayout, mSimilarMoviesLayout;
	private int mImageThumbSize, mImageThumbSpacing;
	private Drawable mActionBarBackgroundDrawable;
	private ImageView mActionBarOverlay;
	private ActionBar mActionBar;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		mContext = getActivity();

		mActionBar = getActivity().getActionBar();

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		mLightItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-LightItalic.ttf");
		mMedium = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Medium.ttf");
		mBoldItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-BoldItalic.ttf");

		mMovieApiService = TMDbMovieService.getInstance(mContext);

		// Get the database ID of the movie in question
		movieId = getArguments().getString("movieId");

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.movie_and_tv_show_details, container, false);

		// This needs to be re-initialized here and not in onCreate()
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		movieDetailsLayout = MizLib.isPortrait(getActivity()) ? v.findViewById(R.id.movieDetailsLayout_portrait) : v.findViewById(R.id.movieDetailsLayout);
		progressBar = v.findViewById(R.id.progress_layout);
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
		mActorsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout);
		mSimilarMoviesLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra);

		mActionBarOverlay = (ImageView) v.findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				MizuuApplication.isFullscreen(mContext) ? MizLib.getActionBarHeight(mContext) : MizLib.getActionBarAndStatusBarHeight(mContext)));

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
						ViewUtils.updateActionBarDrawable(mContext, mActionBarOverlay, mActionBarBackgroundDrawable, mActionBar, mMovie.getTitle(), newAlpha, true, false);

						// Such parallax, much wow
						background.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		}

		if (!isRetained) { // Nothing has been retained - load the data
			setLoading(true);
			new MovieLoader().execute();
			isRetained = true;
		} else {
			setupFields();
		}

		return v;
	}

	private class MovieLoader extends AsyncTask<String, Object, Object> {
		@Override
		protected Object doInBackground(String... params) {
			mMovie = mMovieApiService.getCompleteMovie(movieId, "en");

			for (int i = 0; i < mMovie.getSimilarMovies().size(); i++) {
				String id = mMovie.getSimilarMovies().get(i).getId();
				mMovie.getSimilarMovies().get(i).setInLibrary(MizuuApplication.getMovieAdapter().movieExists(id));
			}

			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			getActivity().invalidateOptionsMenu();
			
			setupFields();
		}
	}

	private void setupFields() {
		if (isAdded() && mMovie != null) {
			// Set the movie title
			textTitle.setVisibility(View.VISIBLE);
			textTitle.setText(mMovie.getTitle());
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
				if (!mMovie.getTagline().isEmpty())
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
							if (!mMovie.getTagline().isEmpty())
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
			textPlot.setText(mMovie.getPlot());

			// Set movie tag line
			textTagline.setTypeface(mBoldItalic);
			if (mMovie.getTagline().equals("NOTAGLINE") || mMovie.getTagline().isEmpty())
				textTagline.setVisibility(TextView.GONE);
			else
				textTagline.setText(mMovie.getTagline());

			// Set the movie genre
			textGenre.setTypeface(mLightItalic);
			if (!TextUtils.isEmpty(mMovie.getGenres())) {
				textGenre.setText(mMovie.getGenres());
			} else {
				textGenre.setVisibility(View.GONE);
			}

			// Set the movie runtime
			textRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(mMovie.getRuntime())));

			// Set the movie release date
			textReleaseDate.setTypeface(mMedium);
			textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), mMovie.getReleasedate()));

			// Set the movie rating
			if (!mMovie.getRating().equals("0.0")) {
				try {
					int rating = (int) (Double.parseDouble(mMovie.getRating()) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(mMovie.getRating());
				}
			} else {
				textRating.setText(R.string.stringNA);
			}

			// Set the movie certification
			if (!TextUtils.isEmpty(mMovie.getCertification())) {
				textCertification.setText(mMovie.getCertification());
			} else {
				textCertification.setText(R.string.stringNA);
			}

			mActorsLayout.setTitle(R.string.detailsActors);
			mActorsLayout.setSeeMoreVisibility(true);
			mActorsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
					new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							if (mActorsLayout.getWidth() > 0) {
								final int numColumns = (int) Math.floor(mActorsLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));	
								mImageThumbSize = (mActorsLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

								mActorsLayout.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mMovie.getActors(), HorizontalCardLayout.ACTORS);
								MizLib.removeViewTreeObserver(mActorsLayout.getViewTreeObserver(), this);
							}
						}
					});
			mActorsLayout.setSeeMoreOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(IntentUtils.getActorBrowserMovies(mContext, mMovie.getTitle(), mMovie.getId()));
				}
			});

			mSimilarMoviesLayout.setVisibility(View.VISIBLE);
			mSimilarMoviesLayout.setTitle(R.string.relatedMovies);
			mSimilarMoviesLayout.setSeeMoreVisibility(true);
			mSimilarMoviesLayout.getViewTreeObserver().addOnGlobalLayoutListener(
					new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							if (mSimilarMoviesLayout.getWidth() > 0) {
								final int numColumns = (int) Math.floor(mSimilarMoviesLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));	
								mImageThumbSize = (mSimilarMoviesLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

								mSimilarMoviesLayout.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mMovie.getSimilarMovies(), HorizontalCardLayout.RELATED_MOVIES);
								MizLib.removeViewTreeObserver(mSimilarMoviesLayout.getViewTreeObserver(), this);
							}
						}
					});
			mSimilarMoviesLayout.setSeeMoreOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(IntentUtils.getSimilarMovies(mContext, mMovie.getTitle(), mMovie.getId()));
				}
			});

			ViewUtils.updateActionBarDrawable(mContext, mActionBarOverlay, mActionBarBackgroundDrawable, mActionBar, mMovie.getTitle(), 1, true, true);

			setLoading(false);

			loadImages();
		}
	}

	private void loadImages() {
		if (!mMovie.getCover().isEmpty())
			mPicasso.load(mMovie.getCover()).placeholder(R.drawable.gray).error(R.drawable.loading_image).transform(new PaletteTransformation(mMovie.getCover(), mDetailsArea, mActorsLayout.getSeeMoreView(), mSimilarMoviesLayout.getSeeMoreView())).into(cover);
		else
			cover.setImageResource(R.drawable.gray);

		if (!mMovie.getBackdrop().isEmpty())
			mPicasso.load(mMovie.getBackdrop()).placeholder(R.drawable.gray).error(R.drawable.bg).into(background, new Callback() {
				@Override
				public void onError() {
					if (!isAdded())
						return;

					if (MizLib.isPortrait(getActivity())) {
						((PanningView) background).setScaleType(ScaleType.CENTER_CROP);
					}

					if (!mMovie.getCover().isEmpty())
						mPicasso.load(mMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
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
			if (!mMovie.getCover().isEmpty()) {
				if (MizLib.isPortrait(getActivity())) {
					((PanningView) background).setScaleType(ScaleType.CENTER_CROP);
				}
				mPicasso.load(mMovie.getCover()).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
			} else
				background.setImageResource(R.drawable.bg);
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (mMovie != null) {
			inflater.inflate(R.menu.tmdb_details, menu);

			if (MizLib.isTablet(mContext)) {
				menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menu.findItem(R.id.checkIn).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menu.findItem(R.id.openInBrowser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (!Trakt.hasTraktAccount(mContext))
				menu.findItem(R.id.checkIn).setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			getActivity().finish();
			break;
		case R.id.share:
			shareMovie();
			break;
		case R.id.openInBrowser:
			openInBrowser();
			break;
		case R.id.checkIn:
			checkIn();
			break;
		case R.id.trailer:
			watchTrailer();
		}

		return super.onOptionsItemSelected(item);
	}

	public void shareMovie() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovie.getId());
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public void openInBrowser() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovie.getId()));
		startActivity(Intent.createChooser(intent, getString(R.string.openWith)));
	}

	public void watchTrailer() {
		new TmdbTrailerSearch(getActivity(), mMovie.getId(), mMovie.getTitle()).execute();
	}

	public void checkIn() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return Trakt.performMovieCheckin(mMovie.getId(), mContext);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result)
					Toast.makeText(mContext, getString(R.string.checked_in), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext, getString(R.string.errorSomethingWentWrong), Toast.LENGTH_SHORT).show();
			}
		}.execute();
	}
}
