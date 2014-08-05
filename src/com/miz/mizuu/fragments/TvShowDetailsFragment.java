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

import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.miz.views.PanningView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

public class TvShowDetailsFragment extends Fragment {

	private DbAdapterTvShow dbHelper;
	private TvShow thisShow;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textCertification;
	private ImageView background, cover;
	private boolean ignorePrefixes;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium;
	private Bus mBus;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public TvShowDetailsFragment() {}

	public static TvShowDetailsFragment newInstance(String showId) {
		TvShowDetailsFragment pageFragment = new TvShowDetailsFragment();
		Bundle b = new Bundle();
		b.putString("showId", showId);
		pageFragment.setArguments(b);
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

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_TITLE_PREFIXES, false);

		// Create and open database
		dbHelper = MizuuApplication.getTvDbAdapter();

		Cursor cursor = dbHelper.getShow(getArguments().getString("showId"));
		try {
			if (cursor.moveToFirst()) {
				thisShow = new TvShow(getActivity(),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
						ignorePrefixes,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1)),
						MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)))
						);
			}
		} catch (Exception e) {
		} finally {
			cursor.close();
		}

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-backdrop-change"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadImages();
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		mBus.register(getActivity());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
	}

	public void onViewCreated(final View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		// TV shows don't include a tagline or filepath
		v.findViewById(R.id.textView3).setVisibility(View.GONE); // Filepath
		v.findViewById(R.id.textView6).setVisibility(View.GONE); // Tagline

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

		background = (ImageView) v.findViewById(R.id.imageBackground);
		textTitle = (TextView) v.findViewById(R.id.movieTitle);
		textPlot = (TextView) v.findViewById(R.id.textView2);
		textGenre = (TextView) v.findViewById(R.id.textView7);
		textRuntime = (TextView) v.findViewById(R.id.textView9);
		textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
		textRating = (TextView) v.findViewById(R.id.textView12);
		textCertification = (TextView) v.findViewById(R.id.textView11);
		cover = (ImageView) v.findViewById(R.id.traktIcon);

		// Set the show title
		textTitle.setVisibility(View.VISIBLE);
		textTitle.setText(thisShow.getTitle());
		textTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		textPlot.setTypeface(mLight);
		textRuntime.setTypeface(mLight);
		textRating.setTypeface(mLight);
		textCertification.setTypeface(mLight);

		textRuntime.setTypeface(mMedium);
		textCertification.setTypeface(mMedium);
		textRating.setTypeface(mMedium);

		// Set the show plot
		if (!MizLib.isPortrait(getActivity())) {
			textPlot.setBackgroundResource(R.drawable.selectable_background);
			textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
			textPlot.setTag(true); // true = collapsed
			textPlot.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (((Boolean) textPlot.getTag())) {
						textPlot.setMaxLines(1000);
						textPlot.setTag(false);
					} else {
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
		textPlot.setText(thisShow.getDescription());

		// Set the show genres
		textGenre.setTypeface(mLightItalic);
		if (!MizLib.isEmpty(thisShow.getGenres())) {
			textGenre.setText(thisShow.getGenres());
		} else {
			textGenre.setVisibility(View.GONE);
		}

		// Set the show runtime
		textRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(thisShow.getRuntime())));

		// Set the show release date
		textReleaseDate.setTypeface(mMedium);
		textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisShow.getFirstAirdate()));

		// Set the show rating
		if (!thisShow.getRating().equals("0.0/10")) {
			if (thisShow.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(thisShow.getRating().substring(0, thisShow.getRating().indexOf("/"))) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(Html.fromHtml(thisShow.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				textRating.setText(thisShow.getRating());
			}
		} else {
			textRating.setText(R.string.stringNA);
		}

		// Set the show certification
		if (!MizLib.isEmpty(thisShow.getCertification())) {
			textCertification.setText(thisShow.getCertification());
		} else {
			textCertification.setText(R.string.stringNA);
		}

		loadImages();
	}

	private void loadImages() {
		if (!MizLib.isPortrait(getActivity())) {
			mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
			mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
		} else {
			mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
			mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
				@Override
				public void onError() {
					if (!isAdded())
						return;
					((PanningView) background).setScaleType(ScaleType.CENTER_CROP);
					mPicasso.load(thisShow.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
				}

				@Override
				public void onSuccess() {
					if (!isAdded())
						return;
					((PanningView) background).startPanning();
				}
			});
		}
	}
}