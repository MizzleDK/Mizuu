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

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ActorBiographyFragment extends Fragment {

	private String mName, mBio, mBirth, mBirthday, mImage;
	private TextView mActorName, mActorBio, mActorBirth, mActorBirthday;
	private ImageView mActorImage, mActorImageBackground;
	private Typeface tf;
	private String json, baseUrl;
	private Picasso mPicasso;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBiographyFragment() {}

	public static ActorBiographyFragment newInstance(String json, String baseUrl) { 
		ActorBiographyFragment pageFragment = new ActorBiographyFragment();
		Bundle bundle = new Bundle();
		bundle.putString("json", json);
		bundle.putString("baseUrl", baseUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		tf = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Thin.ttf");

		json = getArguments().getString("json");
		baseUrl = getArguments().getString("baseUrl");
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actor_bio, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (MizLib.isPortrait(getActivity()))
			MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.linearLayout1));
		else
			MizLib.addActionBarMargin(getActivity(), v.findViewById(R.id.linearLayout1));

		mActorBirthday = (TextView) v.findViewById(R.id.overviewMessage);
		mActorBirth = (TextView) v.findViewById(R.id.textView2);
		mActorName = (TextView) v.findViewById(R.id.textView3);
		mActorName.setTypeface(tf);
		mActorName.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		if (MizLib.isTablet(getActivity()))
			mActorName.setTextSize(48f);
		mActorBio = (TextView) v.findViewById(R.id.textView4);
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

			if (mBirthday.equals("null")) mBirthday = "";

			try {
				mImage = baseUrl + "h632" + jObject.getString("profile_path");
			} catch (Exception e) {
				mImage = "";
			}

			mActorName.setText(mName);

			if (!MizLib.isEmpty(mBio))
				mActorBio.setText(mBio);
			else
				mActorBio.setVisibility(View.GONE);

			if (!MizLib.isEmpty(mBirth))
				mActorBirth.setText(mBirth);
			else
				mActorBirth.setVisibility(View.GONE);

			if (!MizLib.isEmpty(mBirthday))
				mActorBirthday.setText(mBirthday);
			else
				mActorBirthday.setVisibility(View.GONE);

			mPicasso.load(mImage).placeholder(R.drawable.gray).error(R.drawable.noactor).noFade().config(MizuuApplication.getBitmapConfig()).into(mActorImage, new Callback() {
				@Override
				public void onError() {
					mActorImage.setVisibility(View.VISIBLE);
				}

				@Override
				public void onSuccess() {
					mActorImage.setVisibility(View.VISIBLE);
					new BlurImage().execute();
				}
			});
		} catch (Exception ignored) {}
	}

	protected class BlurImage extends AsyncTask<String, String, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... params) {			
			Bitmap image = drawableToBitmap(mActorImage.getDrawable());

			if (image != null)
				return MizLib.fastblur(getActivity(), Bitmap.createScaledBitmap(image, image.getWidth() / 3, image.getHeight() / 3, false), 4);
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				mActorImageBackground.setImageBitmap(result);
				mActorImageBackground.setColorFilter(Color.parseColor("#AA181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
			}
		}
	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap); 
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}
}