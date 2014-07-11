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

import java.io.IOException;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class ActorBiographyFragment extends Fragment {

	private int mRandomInt = -1;
	private String mName, mBio, mBirth, mBirthday, mImage, mThumb;
	private TextView mActorName, mActorBio, mActorBirth, mActorBirthday;
	private ImageView mActorImage, mActorImageBackground;
	private Typeface mCondensedTypeface, mLightTypeface;
	private String json, mBackgroundImage;
	private Picasso mPicasso;

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

		mCondensedTypeface = MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf");
		mLightTypeface = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");

		json = getArguments().getString("json");
		mThumb = getArguments().getString("thumbUrl");

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actor_bio, container, false);
	}

	public void onViewCreated(final View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (!MizuuApplication.isFullscreen(getActivity())) {
			if (MizLib.isPortrait(getActivity()))
				MizLib.addActionBarAndStatusBarMargin(getActivity(), v.findViewById(R.id.traktIcon), (LayoutParams) v.findViewById(R.id.traktIcon).getLayoutParams());
			else
				MizLib.addActionBarAndStatusBarPadding(getActivity(), v.findViewById(R.id.linearLayout1));
		} else
			MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.linearLayout1));

		mActorBirthday = (TextView) v.findViewById(R.id.birthday);
		mActorBirth = (TextView) v.findViewById(R.id.birth);
		mActorName = (TextView) v.findViewById(R.id.actor_name);
		mActorName.setTypeface(mCondensedTypeface);
		mActorName.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		if (MizLib.isTablet(getActivity()))
			mActorName.setTextSize(48f);
		mActorBio = (TextView) v.findViewById(R.id.bio);
		mActorBio.setTypeface(mLightTypeface);
		mActorBio.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mActorImage = (ImageView) v.findViewById(R.id.traktIcon);
		mActorImageBackground = (ImageView) v.findViewById(R.id.imageView2);

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
				mBackgroundImage = MizLib.TMDB_BASE_URL + "w300" + resultArray.getJSONObject(mRandomInt).getString("file_path");
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

			if (!mThumb.isEmpty() || !mThumb.contains("null") || !mBackgroundImage.isEmpty()) {
				new BlurImage().execute();
			}
		} catch (Exception ignored) {}
	}

	protected class BlurImage extends AsyncTask<String, Integer, Bitmap> {
		private Bitmap mBitmap;

		@Override
		protected Bitmap doInBackground(String... params) {
			try {
				if (!mThumb.contains("null"))
					mBitmap = mPicasso.load(mThumb).config(MizuuApplication.getBitmapConfig()).get();

				// Set low-quality image
				publishProgress(0);

				if (!mBackgroundImage.isEmpty()) {
					Bitmap temp = mPicasso.load(mBackgroundImage).config(MizuuApplication.getBitmapConfig()).get();
					if (temp != null)
						return MizLib.fastblur(getActivity(), Bitmap.createScaledBitmap(temp, temp.getWidth(), temp.getHeight(), false), 4);
				}

				if (mBitmap != null)
					return MizLib.fastblur(getActivity(), Bitmap.createScaledBitmap(mBitmap, mBitmap.getWidth(), mBitmap.getHeight(), false), 4);
			} catch (IOException e) {}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... params) {
			// Set the low-quality version of the portrait photo and start loading the HQ version
			if (!mThumb.contains("null"))
				mActorImage.setImageBitmap(mBitmap);
			else
				mActorImage.setImageResource(R.drawable.noactor);
			new HighQualityImageLoader().execute();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				// Set the blurred version as the backdrop image
				mActorImageBackground.setImageBitmap(result);
				mActorImageBackground.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
			}
		}
	}

	protected class HighQualityImageLoader extends AsyncTask<Void, Void, Bitmap> {		
		@Override
		protected Bitmap doInBackground(Void... params) {
			try {
				return mPicasso.load(mImage).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).get();
			} catch (IOException e) {}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null)
				mActorImage.setImageBitmap(result);
		}
	}
}