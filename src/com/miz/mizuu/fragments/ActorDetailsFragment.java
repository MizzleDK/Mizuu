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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.functions.BlurTransformation;
import com.miz.functions.CompleteActor;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteTransformation;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Picasso;

public class ActorDetailsFragment extends Fragment {

	private String mActorId;
	private CompleteActor mActor;
	private Context mContext;
	private TextView mName, mPlaceOfBirth, mBirthday, mKnownCredits, mBiography;
	private ImageView mPhoto, mBackdrop, mActionBarOverlay;
	private HorizontalCardLayout mMovieCards, mTvCards, mPhotoCards, mTaggedPhotoCards;
	private int mImageThumbSize, mImageThumbBackdropSize, mImageThumbSpacing;
	private View mProgressLayout, mDetailsLayout;
	private Picasso mPicasso;
	private Typeface mMedium;
	private Drawable mActionBarBackgroundDrawable;
	private ActionBar mActionBar;

	public ActorDetailsFragment() {}

	public static ActorDetailsFragment newInstance(String actorId) {
		ActorDetailsFragment frag = new ActorDetailsFragment();

		Bundle args = new Bundle();
		args.putString("actorId", actorId);

		frag.setArguments(args);

		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mContext = getActivity();

		mActionBar = getActivity().getActionBar();

		mActorId = getArguments().getString("actorId");

		mPicasso = MizuuApplication.getPicassoDetailsView(mContext);

		mMedium = MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actor_details, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// This needs to be re-initialized here and not in onCreate()
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
		mImageThumbBackdropSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_backdrop_width);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		// Progress layout
		mProgressLayout = view.findViewById(R.id.progress_layout);

		// Details layout
		mDetailsLayout = view.findViewById(R.id.details_area);

		mName = (TextView) view.findViewById(R.id.actor_name);
		mName.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "RobotoCondensed-Regular.ttf"));

		mPlaceOfBirth = (TextView) view.findViewById(R.id.place_of_birth);
		mPlaceOfBirth.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-LightItalic.ttf"));

		mBirthday = (TextView) view.findViewById(R.id.actor_birthday);
		mBirthday.setTypeface(mMedium);

		mKnownCredits = (TextView) view.findViewById(R.id.actor_known_credits);
		mKnownCredits.setTypeface(mMedium);

		mBiography = (TextView) view.findViewById(R.id.actor_biography);

		mBiography.setBackgroundResource(R.drawable.selectable_background);
		mBiography.setMaxLines(mContext.getResources().getInteger(R.integer.show_details_max_lines));
		mBiography.setTag(true); // true = collapsed
		mBiography.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((Boolean) mBiography.getTag())) {
					mBiography.setMaxLines(1000);
					mBiography.setTag(false);
				} else {
					mBiography.setMaxLines(mContext.getResources().getInteger(R.integer.show_details_max_lines));
					mBiography.setTag(true);
				}
			}
		});
		mBiography.setEllipsize(TextUtils.TruncateAt.END);
		mBiography.setFocusable(true);

		mPhoto = (ImageView) view.findViewById(R.id.actor_image);
		mBackdrop = (ImageView) view.findViewById(R.id.imageBackground);

		mMovieCards = (HorizontalCardLayout) view.findViewById(R.id.actor_movie_cards);
		mTvCards = (HorizontalCardLayout) view.findViewById(R.id.actor_tv_cards);
		mPhotoCards = (HorizontalCardLayout) view.findViewById(R.id.actor_photo_cards);
		mTaggedPhotoCards = (HorizontalCardLayout) view.findViewById(R.id.actor_tagged_photo_cards);

		mActionBarOverlay = (ImageView) view.findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				MizuuApplication.isFullscreen(mContext) ? MizLib.getActionBarHeight(mContext) : MizLib.getActionBarAndStatusBarHeight(mContext)));

		if (MizLib.isPortrait(mContext)) {
			final boolean fullscreen = MizuuApplication.isFullscreen(mContext);
			final int height = fullscreen ? MizLib.getActionBarHeight(mContext) : MizLib.getActionBarAndStatusBarHeight(mContext);

			ObservableScrollView sv = (ObservableScrollView) view.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = mPhoto.getHeight() - height;
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);

					// We only want to update the ActionBar if it would actually make a change (times 1.2 to handle fast flings)
					if (t <= headerHeight * 1.2) {
						ViewUtils.updateActionBarDrawable(mContext, mActionBarOverlay, mActionBarBackgroundDrawable, mActionBar, mActor.getName(), newAlpha, true, false);

						// Such parallax, much wow
						mPhoto.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		}

		ViewUtils.updateActionBarDrawable(mContext, mActionBarOverlay, mActionBarBackgroundDrawable, mActionBar, "", 1, true, true);

		if (mActor == null)
			new ActorLoader(mActorId).execute();
		else
			fillViews();
	}

	private void fillViews() {
		mName.setText(mActor.getName());

		if (!TextUtils.isEmpty(mActor.getPlaceOfBirth()))
			mPlaceOfBirth.setText(mActor.getPlaceOfBirth());
		else
			mPlaceOfBirth.setVisibility(View.GONE);

		if (!TextUtils.isEmpty(mActor.getBiography()))
			mBiography.setText(mActor.getBiography());
		else
			mBiography.setText(R.string.no_biography);

		mBirthday.setText(MizLib.getPrettyDatePrecise(mContext, mActor.getBirthday()));
		mKnownCredits.setText(String.valueOf(mActor.getKnownCreditCount()));

		mPicasso.load(mActor.getProfilePhoto()).placeholder(R.drawable.noactor).error(R.drawable.noactor).transform(new PaletteTransformation(mActorId, mDetailsLayout,
				mMovieCards.getSeeMoreView(), mTvCards.getSeeMoreView(), mPhotoCards.getSeeMoreView(), mTaggedPhotoCards.getSeeMoreView())).into(mPhoto);

		if (!MizLib.isPortrait(mContext)) {
			String backdropImage = mActor.getBackdropImage();
			mPicasso.load(backdropImage).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(mContext, backdropImage, 2)).into(mBackdrop);
			mBackdrop.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
		}

		mMovieCards.setTitle(R.string.chooserMovies);
		mMovieCards.setSeeMoreVisibility(true);
		mMovieCards.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mMovieCards.getWidth() > 0) {
							final int numColumns = (int) Math.floor(mMovieCards.getWidth() / (mImageThumbSize + mImageThumbSpacing));	
							mImageThumbSize = (mMovieCards.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

							mMovieCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getMovies(), HorizontalCardLayout.MOVIES);
							MizLib.removeViewTreeObserver(mMovieCards.getViewTreeObserver(), this);
						}
					}
				});
		mMovieCards.setSeeMoreOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(IntentUtils.getActorMoviesIntent(mContext, mActor.getName(), mActor.getId()));
			}
		});

		mTvCards.setTitle(R.string.chooserTVShows);
		mTvCards.setSeeMoreVisibility(true);
		mTvCards.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mTvCards.getWidth() > 0) {
							final int numColumns = (int) Math.floor(mTvCards.getWidth() / (mImageThumbSize + mImageThumbSpacing));	
							mImageThumbSize = (mTvCards.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

							mTvCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getTvShows(), HorizontalCardLayout.TV_SHOWS);
							MizLib.removeViewTreeObserver(mTvCards.getViewTreeObserver(), this);
						}
					}
				});
		mTvCards.setSeeMoreOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(IntentUtils.getActorTvShowsIntent(mContext, mActor.getName(), mActor.getId()));
			}
		});

		mPhotoCards.setTitle(R.string.actorsShowAllPhotos);
		mPhotoCards.setSeeMoreVisibility(true);
		mPhotoCards.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mPhotoCards.getWidth() > 0) {
							final int numColumns = (int) Math.floor(mPhotoCards.getWidth() / (mImageThumbSize + mImageThumbSpacing));	
							mImageThumbSize = (mPhotoCards.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

							mPhotoCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getPhotos(), HorizontalCardLayout.PHOTOS);
							MizLib.removeViewTreeObserver(mPhotoCards.getViewTreeObserver(), this);
						}
					}
				});
		mPhotoCards.setSeeMoreOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(IntentUtils.getActorPhotosIntent(mContext, mActor.getName(), mActor.getId()));
			}
		});

		mTaggedPhotoCards.setTitle(R.string.actorsTaggedPhotos);
		mTaggedPhotoCards.setSeeMoreVisibility(true);
		mTaggedPhotoCards.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mTaggedPhotoCards.getWidth() > 0) {
							final int numColumns = (int) Math.floor(mTaggedPhotoCards.getWidth() / (mImageThumbBackdropSize + mImageThumbSpacing));	
							mImageThumbBackdropSize = (mTaggedPhotoCards.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

							mTaggedPhotoCards.loadItems(mContext, mPicasso, numColumns, mImageThumbBackdropSize, mActor.getTaggedPhotos(), HorizontalCardLayout.TAGGED_PHOTOS);
							MizLib.removeViewTreeObserver(mTaggedPhotoCards.getViewTreeObserver(), this);
						}
					}
				});
		mTaggedPhotoCards.setSeeMoreOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(IntentUtils.getActorTaggedPhotosIntent(mContext, mActor.getName(), mActor.getId()));
			}
		});
	}

	private class ActorLoader extends AsyncTask<Void, Void, Void> {

		private final String mActorId;

		public ActorLoader(String actorId) {
			mActorId = actorId;
		}

		@Override
		protected void onPreExecute() {
			// Show that we're loading
			mProgressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {

			// Load the actor details
			TMDbMovieService service = TMDbMovieService.getInstance(mContext);
			mActor = service.getCompleteActorDetails(mActorId);
			
			for (int i = 0; i < mActor.getMovies().size(); i++) {
				String id = mActor.getMovies().get(i).getId();
				mActor.getMovies().get(i).setInLibrary(MizuuApplication.getMovieAdapter().movieExists(id));
			}

			for (int i = 0; i < mActor.getTvShows().size(); i++) {
				String id = mActor.getTvShows().get(i).getId();
				String title = mActor.getTvShows().get(i).getTitle();
				mActor.getTvShows().get(i).setInLibrary(MizuuApplication.getTvDbAdapter().showExists(id, title));
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// No longer loading, so hide the layout
			mProgressLayout.setVisibility(View.GONE);

			// Fill in all the details
			fillViews();
		}
	}
}