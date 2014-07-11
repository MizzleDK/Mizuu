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
import android.widget.TextView;

import com.miz.db.DbAdapter;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.MovieVersion;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.miz.functions.PreferenceKeys.SHOW_FILE_LOCATION;

public class MovieDetailsFragment extends Fragment {

	private Movie thisMovie;
	private DbAdapter db;
	private int movieId;
	private TextView textTitle, textPlot, textSrc, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private boolean ignorePrefixes, ignoreNfo, mShowFileLocation;
	private ImageView background, cover;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium, mBoldItalic;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDetailsFragment() {}

	public static MovieDetailsFragment newInstance(int movieId) { 
		MovieDetailsFragment pageFragment = new MovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("movieId", movieId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_TITLE_PREFIXES, false);
		ignoreNfo = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_NFO_FILES, true);
		mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SHOW_FILE_LOCATION, true);

		// Get the database ID of the movie in question
		movieId = getArguments().getInt("movieId");

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		mLightItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-LightItalic.ttf");
		mMedium = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Medium.ttf");
		mBoldItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-BoldItalic.ttf");

		// Set up database and open it
		db = MizuuApplication.getMovieAdapter();

		Cursor cursor = null;
		try {
			cursor = db.fetchMovie(movieId);
			thisMovie = new Movie(getActivity(),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_IMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TRAILER)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CAST)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COLLECTION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
					ignorePrefixes,
					ignoreNfo
					);
		} catch (Exception e) {
			getActivity().finish();
			return;
		} finally {
			cursor.close();
		}

		if (db.hasMultipleVersions(thisMovie.getTmdbId()) && !thisMovie.isUnidentified()) {
			thisMovie.setMultipleVersions(db.getRowIdsForMovie(thisMovie.getTmdbId()));
		}

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-backdrop-change"));
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
		return inflater.inflate(R.layout.new_movie_details, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		background = (ImageView) view.findViewById(R.id.imageBackground);
		textTitle = (TextView) view.findViewById(R.id.movieTitle);
		textPlot = (TextView) view.findViewById(R.id.textView2);
		textSrc = (TextView) view.findViewById(R.id.textView3);
		textGenre = (TextView) view.findViewById(R.id.textView7);
		textRuntime = (TextView) view.findViewById(R.id.textView9);
		textReleaseDate = (TextView) view.findViewById(R.id.textReleaseDate);
		textRating = (TextView) view.findViewById(R.id.textView12);
		textTagline = (TextView) view.findViewById(R.id.textView6);
		textCertification = (TextView) view.findViewById(R.id.textView11);
		cover = (ImageView) view.findViewById(R.id.traktIcon);

		// Set the movie title
		textTitle.setVisibility(View.VISIBLE);
		textTitle.setText(thisMovie.getTitle());
		textTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		textPlot.setTypeface(mLight);
		textSrc.setTypeface(mLight);
		textGenre.setTypeface(mLight);
		textRuntime.setTypeface(mLight);
		textReleaseDate.setTypeface(mLight);
		textRating.setTypeface(mLight);
		textCertification.setTypeface(mLight);

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

		// Set the movie file source
		if (mShowFileLocation) {
			if (thisMovie.hasMultipleVersions() && !thisMovie.isUnidentified()) {
				StringBuilder sb = new StringBuilder();
				MovieVersion[] versions = thisMovie.getMultipleVersions();
				for (int i = 0; i < versions.length; i++)
					sb.append(MizLib.transformSmbPath(versions[i].getFilepath()) + "\n");
				textSrc.setText(sb.toString().trim());
			} else {
				textSrc.setText(thisMovie.getFilepath());
			}
		} else {
			textSrc.setVisibility(View.GONE);
		}

		// Set movie tag line
		textTagline.setTypeface(mBoldItalic);
		if (thisMovie.getTagline().isEmpty())
			textTagline.setVisibility(TextView.GONE);
		else
			textTagline.setText(thisMovie.getTagline());

		// Set the movie genre
		textGenre.setTypeface(mLightItalic);
		if (!MizLib.isEmpty(thisMovie.getGenres())) {
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
		if (!thisMovie.getRating().equals("0.0/10")) {
			if (thisMovie.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(thisMovie.getRating().substring(0, thisMovie.getRating().indexOf("/"))) * 10);
					textRating.setText(Html.fromHtml("<b>" + rating + "</b><small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(Html.fromHtml(thisMovie.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				textRating.setText(thisMovie.getRating());
			}
		} else {
			textRating.setText(R.string.stringNA);
		}

		// Set the movie certification
		if (!MizLib.isEmpty(thisMovie.getCertification())) {
			textCertification.setText(Html.fromHtml("<b>" + thisMovie.getCertification() + "</b>"));
		} else {
			textCertification.setText(Html.fromHtml("<b>" + getString(R.string.stringNA) + "</b>"));
		}

		loadImages();
	}

	private void loadImages() {
		if (!MizLib.isPortrait(getActivity())) {
			if (!ignoreNfo && thisMovie.isNetworkFile()) {
				int height = (int) (MizLib.getDisplaySize(getActivity(), MizLib.HEIGHT) * 0.6);
				int width = (int) (height * 0.67);
				mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).placeholder(R.drawable.loading_image).error(R.drawable.loading_image).resize(width, height).into(cover);
				mPicasso.load(thisMovie.getFilepath() + "MIZ_BG<MiZ>" + thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
			} else {
				mPicasso.load(thisMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
				mPicasso.load(thisMovie.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
			}
		} else {
			if (!ignoreNfo && thisMovie.isNetworkFile()) {
				int height = (int) (MizLib.getDisplaySize(getActivity(), MizLib.HEIGHT) * 0.6);
				int width = (int) (height * 0.67);
				mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).placeholder(R.drawable.loading_image).error(R.drawable.loading_image).resize(width, height).into(cover);
				mPicasso.load(thisMovie.getFilepath() + "MIZ_BG<MiZ>" + thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
					}

					@Override
					public void onSuccess() {}					
				});
			} else {
				mPicasso.load(thisMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
				mPicasso.load(thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						mPicasso.load(thisMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
					}

					@Override
					public void onSuccess() {}					
				});
			}
		}
	}
}