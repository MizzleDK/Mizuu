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

import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.miz.functions.BlurTransformation;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.miz.views.PanningView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ActorBiographyFragment extends Fragment {

	private int mRandomInt = -1;
	private String mName, mBio, mBirth, mBirthday, mImage, mThumb;
	private TextView mActorName, mActorBio, mActorBirth, mActorBirthday;
	private ImageView mActorImage, mActorImageBackground;
	private Typeface mCondensedTypeface, mLightTypeface;
	private String json, mBackgroundImage;
	private Picasso mPicasso;
	private Bus mBus;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBiographyFragment() {}

	public static ActorBiographyFragment newInstance(String json, String thumbUrl) { 
		ActorBiographyFragment pageFragment = new ActorBiographyFragment();
		Bundle bundle = new Bundle();
		bundle.putString("json", json);
		bundle.putString("thumbUrl", thumbUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mBus = MizuuApplication.getBus();

		mCondensedTypeface = MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf");
		mLightTypeface = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");

		json = getArguments().getString("json");
		mThumb = getArguments().getString("thumbUrl");

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();

		mBus.register(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actor_bio, container, false);
	}

	public void onViewCreated(final View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mActorImageBackground = (ImageView) v.findViewById(R.id.imageView2);
		mActorBirthday = (TextView) v.findViewById(R.id.birthday);
		mActorBirth = (TextView) v.findViewById(R.id.birth);
		mActorName = (TextView) v.findViewById(R.id.actor_name);
		mActorBio = (TextView) v.findViewById(R.id.bio);
		mActorImage = (ImageView) v.findViewById(R.id.traktIcon);
		mActorImageBackground = (ImageView) v.findViewById(R.id.imageView2);

		if (!MizuuApplication.isFullscreen(getActivity())) {
			if (MizLib.isPortrait(getActivity()))
				MizLib.addActionBarAndStatusBarMargin(getActivity(), mActorImage, (LayoutParams) mActorImage.getLayoutParams());
			else
				MizLib.addActionBarAndStatusBarPadding(getActivity(), v.findViewById(R.id.linearLayout1));
		} else
			MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.linearLayout1));

		if (MizLib.isPortrait(getActivity())) {
			final boolean fullscreen = MizuuApplication.isFullscreen(getActivity());
			final int height = fullscreen ? MizLib.getActionBarHeight(getActivity()) : MizLib.getActionBarAndStatusBarHeight(getActivity());

			ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = mActorImageBackground.getHeight() - height;
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);

					// We only want to update the ActionBar if it would actually make a change (times 1.2 to handle fast flings)
					if (t <= headerHeight * 1.2) {
						mBus.post(Integer.valueOf(newAlpha));

						// Such parallax, much wow
						mActorImageBackground.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		}

		mActorName.setTypeface(mCondensedTypeface);
		mActorName.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		if (MizLib.isTablet(getActivity()))
			mActorName.setTextSize(48f);
		mActorBio.setTypeface(mLightTypeface);
		mActorBio.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		if (!MizLib.isPortrait(getActivity())) {
			mActorImageBackground.setImageResource(R.drawable.bg);
		}

		loadData();
	}

	private void loadData() {
		try {
			JSONObject jObject = new JSONObject(json);

			try {
				mName = jObject.getString("name").trim();
			} catch (Exception e) {}

			try {
				mBio = jObject.getString("biography").trim();
			} catch (Exception e) {
				mBio = "";
			}

			if (mBio.equals("null")) mBio = "";

			mBio = MizLib.removeWikipediaNotes(mBio);

			try {
				mBirth = jObject.getString("place_of_birth").trim();
			} catch (Exception e) {
				mBirth = "";
			}

			if (mBirth.equals("null")) mBirth = "";

			try {
				mBirthday = jObject.getString("birthday").trim();
			} catch (Exception e) {
				mBirthday = "";
			}

			try {
				JSONArray resultArray = jObject.getJSONObject("tagged_images").getJSONArray("results");
				if (mRandomInt == -1)
					mRandomInt = new Random().nextInt(resultArray.length());
				mBackgroundImage = MizLib.getTmdbImageBaseUrl(getActivity()) + "w300" + resultArray.getJSONObject(mRandomInt).getString("file_path");
			} catch (Exception ignored) {
				mBackgroundImage = "";
			}

			if (mBirthday.equals("null")) mBirthday = "";

			mImage = mThumb.replace("/" + MizLib.getActorUrlSize(getActivity()), "/h632");

			mActorName.setText(mName);

			if (!MizLib.isEmpty(mBio))
				mActorBio.setText(mBio);
			else
				mActorBio.setText(R.string.no_biography);

			if (MizLib.isTablet(getActivity()) && MizLib.isPortrait(getActivity()))
				mActorBio.setLineSpacing(0, 1.15f);

			if (!MizLib.isEmpty(mBirth))
				mActorBirth.setText(mBirth);
			else
				mActorBirth.setVisibility(View.GONE);

			if (!MizLib.isEmpty(mBirthday))
				mActorBirthday.setText(MizLib.getPrettyDate(getActivity(), mBirthday));
			else
				mActorBirthday.setVisibility(View.GONE);

			if (!MizLib.isEmpty(mImage)) {
				mPicasso.load(mImage).placeholder(R.drawable.bg).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(mActorImage);

				if (!mBackgroundImage.isEmpty()) {
					mPicasso.load(mBackgroundImage).placeholder(R.drawable.bg).transform(new BlurTransformation(getActivity(), mBackgroundImage + "-blur", 2)).config(MizuuApplication.getBitmapConfig()).into(mActorImageBackground, new Callback() {
						@Override
						public void onError() {
							mPicasso.load(mImage).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(getActivity(), mImage + "-blur", 2)).config(MizuuApplication.getBitmapConfig()).into(mActorImageBackground);
							mActorImageBackground.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
							if (MizLib.isPortrait(getActivity()))
								((PanningView) mActorImageBackground).setScaleType(ScaleType.CENTER_CROP);
						}

						@Override
						public void onSuccess() {
							mActorImageBackground.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
							if (MizLib.isPortrait(getActivity()))
								((PanningView) mActorImageBackground).startPanning();
						}

					});
				} else {
					mPicasso.load(mImage).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(getActivity(), mImage + "-blur", 2)).config(MizuuApplication.getBitmapConfig()).into(mActorImageBackground);
					mActorImageBackground.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
					if (MizLib.isPortrait(getActivity()))
						((PanningView) mActorImageBackground).setScaleType(ScaleType.CENTER_CROP);
				}
			}
		} catch (Exception ignored) {}
	}
}