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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.miz.db.DbAdapterMovies;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.PaletteTransformation;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.views.ObservableScrollView;
import com.miz.views.PanningView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.SHOW_FILE_LOCATION;

public class MovieDetailsFragment extends Fragment {

	private Movie mMovie;
	private DbAdapterMovies mDatabase;
	private TextView mTitle, mPlot, mSrc, mGenre, mRuntime, mReleaseDate, mRating, mTagline, mCertification;
	private View mDetailsArea;
	private boolean mIgnorePrefixes, mShowFileLocation;
	private ImageView mBackground, mCover;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium, mBoldItalic;
	private Bus mBus;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDetailsFragment() {}

	public static MovieDetailsFragment newInstance(String tmdbId) { 
		MovieDetailsFragment pageFragment = new MovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("tmdbId", tmdbId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mBus = MizuuApplication.getBus();

		mIgnorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_TITLE_PREFIXES, false);
		mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SHOW_FILE_LOCATION, true);

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		mLightItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-LightItalic.ttf");
		mMedium = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Medium.ttf");
		mBoldItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-BoldItalic.ttf");

		// Set up database and open it
		mDatabase = MizuuApplication.getMovieAdapter();

		Cursor cursor = null;
		try {
			cursor = mDatabase.fetchMovie(getArguments().getString("tmdbId"));
			mMovie = new Movie(getActivity(),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TAGLINE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_IMDB_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RELEASEDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_CERTIFICATION)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RUNTIME)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TRAILER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_GENRES)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_FAVOURITE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_ACTORS)),
					MizuuApplication.getCollectionsAdapter().getCollection(cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID))),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TO_WATCH)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_DATE_ADDED)),
					mIgnorePrefixes
					);
		} catch (Exception e) {
			getActivity().finish();
			return;
		} finally {
			cursor.close();
		}

		if (MizuuApplication.getMovieMappingAdapter().hasMultipleFilepaths(mMovie.getTmdbId())) {
			mMovie.setFilepaths(MizuuApplication.getMovieMappingAdapter().getMovieFilepaths(mMovie.getTmdbId()));
		}

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
	}

	@Override
	public void onResume() {
		super.onResume();

		mBus.register(getActivity());
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadImages();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mDetailsArea = view.findViewById(R.id.details_area);
		mBackground = (ImageView) view.findViewById(R.id.imageBackground);
		mTitle = (TextView) view.findViewById(R.id.movieTitle);
		mPlot = (TextView) view.findViewById(R.id.textView2);
		mSrc = (TextView) view.findViewById(R.id.textView3);
		mGenre = (TextView) view.findViewById(R.id.textView7);
		mRuntime = (TextView) view.findViewById(R.id.textView9);
		mReleaseDate = (TextView) view.findViewById(R.id.textReleaseDate);
		mRating = (TextView) view.findViewById(R.id.textView12);
		mTagline = (TextView) view.findViewById(R.id.textView6);
		mCertification = (TextView) view.findViewById(R.id.textView11);
		mCover = (ImageView) view.findViewById(R.id.traktIcon);

		if (MizLib.isPortrait(getActivity())) {
			final boolean fullscreen = MizuuApplication.isFullscreen(getActivity());
			final int height = fullscreen ? MizLib.getActionBarHeight(getActivity()) : MizLib.getActionBarAndStatusBarHeight(getActivity());

			ObservableScrollView sv = (ObservableScrollView) view.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = mBackground.getHeight() - height;
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);

					// We only want to update the ActionBar if it would actually make a change (times 1.2 to handle fast flings)
					if (t <= headerHeight * 1.2) {
						mBus.post(Integer.valueOf(newAlpha));

						// Such parallax, much wow
						mBackground.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		}

		// Set the movie title
		mTitle.setVisibility(View.VISIBLE);
		mTitle.setText(mMovie.getTitle());
		mTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		mTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		mPlot.setTypeface(mLight);
		mSrc.setTypeface(mLight);

		mRuntime.setTypeface(mMedium);
		mCertification.setTypeface(mMedium);
		mRating.setTypeface(mMedium);

		// Set the movie plot
		if (!MizLib.isPortrait(getActivity())) {
			mPlot.setBackgroundResource(R.drawable.selectable_background);
			if (!mMovie.getTagline().isEmpty())
				mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.movie_details_max_lines));
			else
				mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
			mPlot.setTag(true); // true = collapsed
			mPlot.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (((Boolean) mPlot.getTag())) {
						mPlot.setMaxLines(1000);
						mPlot.setTag(false);
					} else {
						if (!mMovie.getTagline().isEmpty())
							mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.movie_details_max_lines));
						else
							mPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
						mPlot.setTag(true);
					}
				}
			});
			mPlot.setEllipsize(TextUtils.TruncateAt.END);
			mPlot.setFocusable(true);
		} else {
			if (MizLib.isTablet(getActivity()))
				mPlot.setLineSpacing(0, 1.15f);
		}
		mPlot.setText(mMovie.getPlot());

		// Set the movie file source
		if (mShowFileLocation) {
			mSrc.setText(mMovie.getAllFilepaths());
		} else {
			mSrc.setVisibility(View.GONE);
		}

		// Set movie tag line
		mTagline.setTypeface(mBoldItalic);
		if (mMovie.getTagline().isEmpty())
			mTagline.setVisibility(TextView.GONE);
		else
			mTagline.setText(mMovie.getTagline());

		// Set the movie genre
		mGenre.setTypeface(mLightItalic);
		if (!TextUtils.isEmpty(mMovie.getGenres())) {
			mGenre.setText(mMovie.getGenres());
		} else {
			mGenre.setVisibility(View.GONE);
		}

		// Set the movie runtime
		mRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(mMovie.getRuntime())));

		// Set the movie release date
		mReleaseDate.setTypeface(mMedium);
		mReleaseDate.setText(MizLib.getPrettyDate(getActivity(), mMovie.getReleasedate()));

		// Set the movie rating
		if (!mMovie.getRating().equals("0.0/10")) {
			if (mMovie.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(mMovie.getRating().substring(0, mMovie.getRating().indexOf("/"))) * 10);
					mRating.setText(Html.fromHtml(+ rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					mRating.setText(Html.fromHtml(mMovie.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				mRating.setText(mMovie.getRating());
			}
		} else {
			mRating.setText(R.string.stringNA);
		}

		// Set the movie certification
		if (!TextUtils.isEmpty(mMovie.getCertification())) {
			mCertification.setText(mMovie.getCertification());
		} else {
			mCertification.setText(R.string.stringNA);
		}

		loadImages();
	}

	private void loadImages() {
		if (!MizLib.isPortrait(getActivity())) {
			mPicasso.load(mMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).transform(new PaletteTransformation(mMovie.getThumbnail().getAbsolutePath(), mDetailsArea)).into(mCover);
			mPicasso.load(mMovie.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(mBackground);		
		} else {
			mPicasso.load(mMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).transform(new PaletteTransformation(mMovie.getThumbnail().getAbsolutePath(), mDetailsArea)).into(mCover);
			mPicasso.load(mMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(mBackground, new Callback() {
				@Override
				public void onError() {
					if (!isAdded())
						return;
					((PanningView) mBackground).setScaleType(ScaleType.CENTER_CROP);
					mPicasso.load(mMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(mBackground);
				}

				@Override
				public void onSuccess() {
					if (!isAdded())
						return;
					((PanningView) mBackground).startPanning();
				}				
			});
		}
	}
}