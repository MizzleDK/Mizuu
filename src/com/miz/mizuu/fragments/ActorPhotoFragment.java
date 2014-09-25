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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ActorPhotoFragment extends Fragment {

	private String mPhotoUrl;
	private Picasso mPicasso;
	private Bus mBus;
	private boolean mPortraitPhotos;
	
	public static ActorPhotoFragment newInstance(String url, boolean portraitPhotos) {
		ActorPhotoFragment frag = new ActorPhotoFragment();
		Bundle b = new Bundle();
		b.putString("photo", url);
		b.putBoolean("portrait", portraitPhotos);
		frag.setArguments(b);
		return frag;
	}

	public ActorPhotoFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		

		setRetainInstance(true);
		
		mBus = MizuuApplication.getBus();

		mPhotoUrl = getArguments().getString("photo");
		mPortraitPhotos = getArguments().getBoolean("portrait");

		mPicasso = MizuuApplication.getPicasso(getActivity());
	}
	
	@Override
	public void onResume() {
		super.onResume();

		mBus.register(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.photo, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		final ProgressBar pbar = (ProgressBar) v.findViewById(R.id.progressBar1);
		ImageView img = (ImageView) v.findViewById(R.id.imageView1);
		
		if (MizLib.isPortrait(getActivity()) && mPortraitPhotos || !MizLib.isPortrait(getActivity()) && !mPortraitPhotos)
			img.setScaleType(ScaleType.CENTER_CROP);
		
		mPicasso.load(mPhotoUrl).error(R.drawable.noactor).into(img, new Callback() {

			@Override
			public void onError() {
				pbar.setVisibility(View.GONE);
			}

			@Override
			public void onSuccess() {
				pbar.setVisibility(View.GONE);
			}
			
		});
		img.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAdded()) {
					mBus.post(new Object());
				}
			}
		});
	}

}