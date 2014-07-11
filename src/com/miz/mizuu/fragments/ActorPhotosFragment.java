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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.ImageViewer;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class ActorPhotosFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<String> pics_sources = new ArrayList<String>();
	private GridView mGridView = null;
	private String json, baseUrl;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorPhotosFragment() {}

	public static ActorPhotosFragment newInstance(String json, String actorName, String baseUrl) { 
		ActorPhotosFragment pageFragment = new ActorPhotosFragment();
		Bundle bundle = new Bundle();
		bundle.putString("json", json);
		bundle.putString("actorName", actorName);
		bundle.putString("baseUrl", baseUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		json = getArguments().getString("json");
		baseUrl = getArguments().getString("baseUrl");
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (!MizLib.isPortrait(getActivity()))
			v.findViewById(R.id.container).setBackgroundResource(MizuuApplication.getBackgroundColorResource(getActivity()));

		if (!MizuuApplication.isFullscreen(getActivity()))
			MizLib.addActionBarAndStatusBarPadding(getActivity(), v.findViewById(R.id.container));
		else
			MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		v.findViewById(R.id.progress).setVisibility(View.GONE);

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(
								mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
						if (numColumns > 0) {
							mGridView.setNumColumns(numColumns);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String[] photos = new String[pics_sources.size()];
				for (int i = 0; i < photos.length; i++)
					photos[i] = pics_sources.get(i).replace(MizLib.getActorUrlSize(getActivity()), "original");

				Intent intent = new Intent();
				intent.setClass(getActivity(), ImageViewer.class);
				intent.putExtra("photos", photos);
				intent.putExtra("actorName", getArguments().getString("actorName"));
				intent.putExtra("selectedIndex", arg2);
				startActivity(intent);
			}
		});

		loadData();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return pics_sources.size();
		}

		@Override
		public Object getItem(int position) {
			return pics_sources.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_portrait_photo, container, false);
				holder = new CoverItem();
				
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}
			
			holder.cover.setImageResource(R.color.card_background_dark);

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(pics_sources.get(position)).placeholder(R.color.card_background_dark).config(mConfig).into(holder.cover);

			return convertView;
		}
	}

	private void loadData() {
		try {
			pics_sources.clear();
			
			JSONObject jObject = new JSONObject(json);

			JSONArray jArray = jObject.getJSONObject("images").getJSONArray("profiles");

			for (int i = 0; i < jArray.length(); i++) {
				pics_sources.add(baseUrl + MizLib.getActorUrlSize(getActivity()) + jArray.getJSONObject(i).getString("file_path"));
			}

			mAdapter.notifyDataSetChanged();
		} catch (Exception ignored) {}
	}
}