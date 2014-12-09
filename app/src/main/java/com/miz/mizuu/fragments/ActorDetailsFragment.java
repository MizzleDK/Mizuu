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

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.base.MizActivity;
import com.miz.functions.BlurTransformation;
import com.miz.functions.CompleteActor;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteLoader;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ActorDetailsFragment extends Fragment {

    private String mActorId;
    private CompleteActor mActor;
    private Activity mContext;
    private TextView mName, mPlaceOfBirth, mBirthday, mKnownCredits, mBiography;
    private ImageView mPhoto, mBackdrop;
    private ObservableScrollView mScrollView;
    private HorizontalCardLayout mMovieCards, mTvCards, mPhotoCards, mTaggedPhotoCards;
    private int mImageThumbSize, mImageThumbBackdropSize, mImageThumbSpacing, mToolbarColor = 0;
    private Typeface mMedium, mBold, mCondensedRegular;
    private View mProgressLayout, mDetailsLayout;
    private Picasso mPicasso;
    private Toolbar mToolbar;
    private PaletteLoader mPaletteLoader;

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

        mActorId = getArguments().getString("actorId");

        mPicasso = MizuuApplication.getPicassoDetailsView(mContext);

        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.actor_details, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This needs to be re-initialized here and not in onCreate()
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbBackdropSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_backdrop_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        // Progress layout
        mProgressLayout = view.findViewById(R.id.progress_layout);

        // Details layout
        mDetailsLayout = view.findViewById(R.id.details_area);

        mName = (TextView) view.findViewById(R.id.actor_name);
        mName.setTypeface(mCondensedRegular);

        mPlaceOfBirth = (TextView) view.findViewById(R.id.place_of_birth);
        mPlaceOfBirth.setTypeface(mBold);

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
                    // Animate
                    ViewUtils.animateTextViewMaxLines(mBiography, 50); // It seems highly unlikely that there would every be more than 50 lines

                    // Reverse the tag
                    mBiography.setTag(false);
                } else {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(mBiography,
                            getResources().getInteger(R.integer.show_details_max_lines));

                    // Reverse the tag
                    mBiography.setTag(true);
                }
            }
        });
        mBiography.setEllipsize(TextUtils.TruncateAt.END);
        mBiography.setFocusable(true);
        mBiography.setTypeface(mCondensedRegular);

        mPhoto = (ImageView) view.findViewById(R.id.actor_image);
        mBackdrop = (ImageView) view.findViewById(R.id.imageBackground);

        mMovieCards = (HorizontalCardLayout) view.findViewById(R.id.actor_movie_cards);
        mTvCards = (HorizontalCardLayout) view.findViewById(R.id.actor_tv_cards);
        mPhotoCards = (HorizontalCardLayout) view.findViewById(R.id.actor_photo_cards);
        mTaggedPhotoCards = (HorizontalCardLayout) view.findViewById(R.id.actor_tagged_photo_cards);

        mScrollView = (ObservableScrollView) view.findViewById(R.id.observableScrollView);

        final int height = MizLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                ViewUtils.handleOnScrollChangedEvent(
                        getActivity(), view, mPhoto, mActor.getName(),
                        height, t, mToolbar, mToolbarColor);
            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, view,
                        mBackdrop, mScrollView, this);
            }
        });

        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, "", mToolbarColor);

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

        loadImages();

        mMovieCards.setTitle(R.string.chooserMovies);
        mMovieCards.setSeeMoreVisibility(true);
        mMovieCards.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mMovieCards.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mMovieCards.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mMovieCards.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                            mMovieCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getMovies(), HorizontalCardLayout.MOVIES, mToolbarColor);
                            MizLib.removeViewTreeObserver(mMovieCards.getViewTreeObserver(), this);
                        }
                    }
                });
        mMovieCards.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorMoviesIntent(mContext, mActor.getName(), mActor.getId(), mToolbarColor));
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

                            mTvCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getTvShows(), HorizontalCardLayout.TV_SHOWS, mToolbarColor);
                            MizLib.removeViewTreeObserver(mTvCards.getViewTreeObserver(), this);
                        }
                    }
                });
        mTvCards.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorTvShowsIntent(mContext, mActor.getName(), mActor.getId(), mToolbarColor));
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

                            mPhotoCards.loadItems(mContext, mPicasso, numColumns, mImageThumbSize, mActor.getPhotos(), HorizontalCardLayout.PHOTOS, mToolbarColor);
                            MizLib.removeViewTreeObserver(mPhotoCards.getViewTreeObserver(), this);
                        }
                    }
                });
        mPhotoCards.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorPhotosIntent(mContext, mActor.getName(), mActor.getId(), mToolbarColor));
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

                            mTaggedPhotoCards.loadItems(mContext, mPicasso, numColumns, mImageThumbBackdropSize, mActor.getTaggedPhotos(), HorizontalCardLayout.TAGGED_PHOTOS, mToolbarColor);
                            MizLib.removeViewTreeObserver(mTaggedPhotoCards.getViewTreeObserver(), this);
                        }
                    }
                });
        mTaggedPhotoCards.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorTaggedPhotosIntent(mContext, mActor.getName(), mActor.getId(), mToolbarColor));
            }
        });
    }

    private void loadImages() {
        mPicasso.load(mActor.getProfilePhoto()).placeholder(R.drawable.noactor).error(R.drawable.noactor).into(mPhoto, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.parse(mActor.getProfilePhotoThumb()), new PaletteLoader.OnPaletteLoadedCallback() {
                        @Override
                        public void onPaletteLoaded(int swatchColor) {
                            mToolbarColor = swatchColor;
                        }
                    });

                    mPaletteLoader.addView(mDetailsLayout);
                    mPaletteLoader.addView(mMovieCards.getSeeMoreView());
                    mPaletteLoader.addView(mTvCards.getSeeMoreView());
                    mPaletteLoader.addView(mPhotoCards.getSeeMoreView());
                    mPaletteLoader.addView(mTaggedPhotoCards.getSeeMoreView());

                    mPaletteLoader.execute();
                } else {
                    // Clear old views after configuration change
                    mPaletteLoader.clearViews();

                    // Add views after configuration change
                    mPaletteLoader.addView(mDetailsLayout);
                    mPaletteLoader.addView(mMovieCards.getSeeMoreView());
                    mPaletteLoader.addView(mTvCards.getSeeMoreView());
                    mPaletteLoader.addView(mPhotoCards.getSeeMoreView());
                    mPaletteLoader.addView(mTaggedPhotoCards.getSeeMoreView());

                    // Re-color the views
                    mPaletteLoader.colorViews();
                }
            }

            @Override
            public void onError() {

            }
        });

        if (!MizLib.isPortrait(mContext)) {
            String backdropImage = mActor.getBackdropImage();
            mPicasso.load(backdropImage).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(mContext, backdropImage, 2)).into(mBackdrop);
            mBackdrop.setColorFilter(Color.parseColor("#88181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
        }
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