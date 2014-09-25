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

package com.miz.utils;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.miz.functions.Actor;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class ViewUtils {

	private ViewUtils() {} // No instantiation
	
	/**
	 * Returns a actor card with name, character, image and click listener.
	 * @param context
	 * @param picasso
	 * @param actor
	 * @return
	 */
	@SuppressLint("InflateParams")
	public static View setupActorCard(final Context context, Picasso picasso, final Actor actor) {
		View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

		// Load image
		picasso.load(actor.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

		// Set title
		((TextView) v.findViewById(R.id.text)).setText(actor.getName());
		((TextView) v.findViewById(R.id.text)).setTypeface(MizuuApplication.getOrCreateTypeface(context, "Roboto-Medium.ttf"));

		// Set subtitle
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(actor.getCharacter());
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

		// Set click listener
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(IntentUtils.getActorIntent(context, actor));
			}
		});

		return v;
	}
	
	/**
	 * Returns a movie card with title, release date, image and click listener.
	 * @param context
	 * @param picasso
	 * @param movie
	 * @return
	 */
	@SuppressLint("InflateParams")
	public static View setupMovieCard(final Context context, Picasso picasso, final WebMovie movie) {
		View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

		// Load image
		picasso.load(movie.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

		// Set title
		((TextView) v.findViewById(R.id.text)).setText(movie.getTitle());
		((TextView) v.findViewById(R.id.text)).setTypeface(MizuuApplication.getOrCreateTypeface(context, "Roboto-Medium.ttf"));

		// Set subtitle
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(movie.getSubtitle());
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

		// Set click listener
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(IntentUtils.getTmdbMovieDetails(context, movie));
			}
		});

		return v;
	}
	
	/**
	 * Returns a TV show card with title, release date, image and click listener.
	 * @param context
	 * @param picasso
	 * @param movie
	 * @return
	 */
	@SuppressLint("InflateParams")
	public static View setupTvShowCard(final Context context, Picasso picasso, final WebMovie show) {
		View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

		// Load image
		picasso.load(show.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

		// Set title
		((TextView) v.findViewById(R.id.text)).setText(show.getTitle());
		((TextView) v.findViewById(R.id.text)).setTypeface(MizuuApplication.getOrCreateTypeface(context, "Roboto-Medium.ttf"));

		// Set subtitle
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(show.getSubtitle());
		((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

		// Set click listener
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(IntentUtils.getTmdbTvShowLink(show));
			}
		});

		return v;
	}
	
	/**
	 * Returns a photo card with image and click listener.
	 * @param context
	 * @param picasso
	 * @param movie
	 * @return
	 */
	@SuppressLint("InflateParams")
	public static View setupPhotoCard(final Context context, Picasso picasso, final String url, final List<String> items, final int index) {
		View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_no_text, null);

		// Load image
		picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

		// Set click listener
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(IntentUtils.getActorPhotoIntent(context, items, index));
			}
		});

		return v;
	}
	
	/**
	 * Returns a photo card with image and click listener.
	 * @param context
	 * @param picasso
	 * @param movie
	 * @return
	 */
	@SuppressLint("InflateParams")
	public static View setupTaggedPhotoCard(final Context context, Picasso picasso, final String url, final List<String> items, final int index) {
		View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_landscape_no_text, null);

		// Load image
		picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

		// Set click listener
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(IntentUtils.getActorTaggedPhotoIntent(context, items, index));
			}
		});

		return v;
	}
	
	/**
	 * Used to update the ActionBar for movie details view as the user scrolls.
	 * @param context
	 * @param actionBarOverlay
	 * @param actionBarBackgroundDrawable
	 * @param actionBar
	 * @param title
	 * @param newAlpha
	 * @param setBackground
	 * @param showActionBar
	 */
	public static void updateActionBarDrawable(Context context, ImageView actionBarOverlay, Drawable actionBarBackgroundDrawable,
			ActionBar actionBar, String title, int newAlpha, boolean setBackground, boolean showActionBar) {
		actionBarOverlay.setVisibility(View.VISIBLE);

		if (!MizLib.isPortrait(context)) {
			newAlpha = 0;
			setBackground = true;
			showActionBar = true;
		} else {
			if (!MizLib.isTablet(context) && !MizLib.usesNavigationControl(context)) {
				if (newAlpha == 0) {
					actionBar.hide();
					actionBarOverlay.setVisibility(View.GONE);
				} else
					actionBar.show();
			}
		}

		actionBar.setTitle(newAlpha > 127 ? title : null);

		if (setBackground) {
			actionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
			actionBarOverlay.setImageDrawable(actionBarBackgroundDrawable);
		}

		if (showActionBar) {
			actionBar.show();
		}
	}
}
