/*
 * Copyright (C) 2014 Chris Banes (heavily modified by Michell Bak)
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

package com.miz.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.functions.Actor;
import com.miz.functions.GridSeason;
import com.miz.functions.WebMovie;
import com.miz.mizuu.R;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

public class HorizontalCardLayout extends LinearLayout {

	public static final int MOVIES = 1000, TV_SHOWS = 1100, PHOTOS = 1200,
			TAGGED_PHOTOS = 1300, RELATED_MOVIES = 1400, ACTORS = 1500, SEASONS = 1600;

	private final View mTitleLayout;
	private final TextView mTitleTextView, mSeeMoreTextView;
	private LinearLayout mCardContent;
	private ProgressBar mProgressBar;
	private TextView mErrorTextView;

	public HorizontalCardLayout(Context context) {
		this(context, null);
	}

	public HorizontalCardLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalCardLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setOrientation(VERTICAL);
		LayoutInflater.from(context).inflate(R.layout.horizontal_grid_items, this, true);

		mTitleLayout = getChildAt(0);
		mTitleLayout.setClickable(true);
		mTitleLayout.setFocusable(true);

		mTitleTextView = (TextView) mTitleLayout.findViewById(R.id.title);
		if (!isInEditMode())
			mTitleTextView.setTypeface(TypefaceUtils.getRobotoMedium(context));

		mSeeMoreTextView = (TextView) mTitleLayout.findViewById(R.id.see_more);
		if (!isInEditMode())
			mSeeMoreTextView.setTypeface(TypefaceUtils.getRobotoCondensedRegular(context));

		if (!isInEditMode()) {
			mCardContent = (LinearLayout) getChildAt(1).findViewById(R.id.linear_layout);
			mProgressBar = (ProgressBar) getChildAt(1).findViewById(R.id.progress_bar);
			mErrorTextView = (TextView) getChildAt(1).findViewById(R.id.error_message);
		}
	}

	public void setSeeMoreVisibility(boolean visible) {
		mSeeMoreTextView.setVisibility(visible ? VISIBLE : GONE);
	}

	public void setSeeMoreOnClickListener(OnClickListener listener) {
		mTitleLayout.setOnClickListener(listener);
	}

	public View getSeeMoreView() {
		return mSeeMoreTextView;
	}

	public void setTitle(CharSequence title) {
		mTitleTextView.setText(title);
	}

	public void setTitle(int titleResId) {
		setTitle(getResources().getString(titleResId));
	}

	public void setProgressBarVisibility(boolean visible) {
		mProgressBar.setVisibility(visible ? VISIBLE : GONE);
	}

	public void setNoActorsVisibility(boolean visible) {
		mErrorTextView.setVisibility(visible ? VISIBLE : GONE);
		mCardContent.setVisibility(visible ? GONE : VISIBLE);
		
		if (visible)
			setProgressBarVisibility(false);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		if (mCardContent != null) {
			mCardContent.addView(child, index, params);

			// Automatically hide the ProgressBar when items are added
			if (mProgressBar.getVisibility() == VISIBLE)
				setProgressBarVisibility(false);
		} else {
			super.addView(child, index, params);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadItems(Activity context, Picasso picasso, int capacity, int width, List<?> items, int type, int toolbarColor) {
		if (items.size() > 0) {
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, 0, getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing), 0);

			for (int i = 0; i < items.size() && i < capacity; i++) {
				if (type == MOVIES || type == RELATED_MOVIES) {
					addView(ViewUtils.setupMovieCard(context, picasso, (WebMovie) items.get(i)), i, lp);
				} else if (type == TV_SHOWS) {
					addView(ViewUtils.setupTvShowCard(context, picasso, (WebMovie) items.get(i)), i, lp);
				} else if (type == PHOTOS) {
					addView(ViewUtils.setupPhotoCard(context, picasso, (String) items.get(i), (List<String>) items, i), i, lp);
				} else if (type == TAGGED_PHOTOS) {
					addView(ViewUtils.setupTaggedPhotoCard(context, picasso, (String) items.get(i), (List<String>) items, i), i, lp);
				} else if (type == ACTORS) {
					addView(ViewUtils.setupActorCard(context, picasso, (Actor) items.get(i)), i, lp);
				} else if (type == SEASONS) {
                    addView(ViewUtils.setupTvShowSeasonCard(context, picasso, (GridSeason) items.get(i), toolbarColor), i, lp);
                }
			}

			setSeeMoreVisibility(items.size() > capacity);
		} else {
			setVisibility(GONE);
		}
	}
}