/*
 * Copyright (C) 2014 Chris Banes (modified by Michell Bak)
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class HorizontalCardLayout extends LinearLayout {

	private final View mTitleLayout;
	private final TextView mTitleTextView;
	private final TextView mSeeMoreTextView;
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
			mTitleTextView.setTypeface(MizuuApplication.getOrCreateTypeface(context, "RobotoCondensed-LightItalic.ttf"));

		mSeeMoreTextView = (TextView) mTitleLayout.findViewById(R.id.see_more);
		if (!isInEditMode())
			mSeeMoreTextView.setTypeface(MizuuApplication.getOrCreateTypeface(context, "RobotoCondensed-Regular.ttf"));

		mCardContent = (LinearLayout) getChildAt(1).findViewById(R.id.linear_layout);
		mProgressBar = (ProgressBar) getChildAt(1).findViewById(R.id.progress_bar);
		mErrorTextView = (TextView) getChildAt(1).findViewById(R.id.error_message);
		
		//setClickable(true);
		//setFocusable(true);
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
		if (visible) {
			mErrorTextView.setText(R.string.no_actors);
			mErrorTextView.setVisibility(VISIBLE);
			setProgressBarVisibility(false);
			mCardContent.setVisibility(GONE);
		} else {
			mErrorTextView.setVisibility(GONE);
		}
	}
	
	public void setNoSimilarMoviesVisibility(boolean visible) {
		setVisibility(visible ? GONE : VISIBLE);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		if (mCardContent != null) {
			mCardContent.addView(child, index, params);
			
			// Automatically hide the ProgressBar when items are added
			setProgressBarVisibility(false);
		} else {
			super.addView(child, index, params);
		}
	}
}