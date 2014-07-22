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
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.functions.Actor;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class ActorBrowserFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<Actor> mActors = new ArrayList<Actor>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private String mJson, mTmdbApiKey;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBrowserFragment() {}

	public static ActorBrowserFragment newInstance(String movieId) { 
		ActorBrowserFragment pageFragment = new ActorBrowserFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		pageFragment.setArguments(bundle);		
		return pageFragment;
	}

	public static ActorBrowserFragment newInstance(String movieId, String json, String baseUrl) { 
		ActorBrowserFragment pageFragment = new ActorBrowserFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		bundle.putString("json", json);
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

		mTmdbApiKey = MizLib.getTmdbApiKey(getActivity());

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();

		if (!getArguments().containsKey("json")) {		
			if (getArguments().getString("movieId") == null) {
				new GetActorDetails(getActivity()).execute(getActivity().getIntent().getExtras().getString("movieId"));
			} else {
				new GetActorDetails(getActivity()).execute(getArguments().getString("movieId"));
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mGridView = (GridView) v.findViewById(R.id.gridView);

		if (!MizLib.isPortrait(getActivity()))
			v.findViewById(R.id.container).setBackgroundResource(MizuuApplication.getBackgroundColorResource(getActivity()));

		if (!MizuuApplication.isFullscreen(getActivity()))
			MizLib.addActionBarAndStatusBarPadding(getActivity(), v.findViewById(R.id.container));
		else
			MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mActors.size() > 0)
			mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());
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
				Intent intent = new Intent(getActivity(), com.miz.mizuu.Actor.class);
				intent.putExtra("actorName", mActors.get(arg2).getName());
				intent.putExtra("actorID", mActors.get(arg2).getId());
				intent.putExtra("thumb", mActors.get(arg2).getUrl());
				startActivity(intent);
			}
		});

		if (getArguments().containsKey("json")) {
			mJson = getArguments().getString("json");
			loadJson(getArguments().getString("baseUrl"));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mActors.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			CoverItem holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);

				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			holder.text.setText(mActors.get(position).getName());
			holder.subtext.setText(mActors.get(position).getCharacter().equals("null") ? "" : mActors.get(position).getCharacter());

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			if (!mActors.get(position).getUrl().endsWith("null"))
				mPicasso.load(mActors.get(position).getUrl()).error(R.drawable.noactor).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.noactor);

			return convertView;
		}
	}

	protected class GetActorDetails extends AsyncTask<String, String, String> {
		
		private Context mContext;
		
		public GetActorDetails(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
				
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet  httppost = new HttpGet("https://api.themoviedb.org/3/movie/" + params[0] + "/casts?api_key=" + mTmdbApiKey);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(html);

				JSONArray jArray = jObject.getJSONArray("cast");

				mActors.clear();

				Set<String> actorIds = new HashSet<String>();

				for (int i = 0; i < jArray.length(); i++) {
					if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
						actorIds.add(jArray.getJSONObject(i).getString("id"));

						mActors.add(new Actor(
								jArray.getJSONObject(i).getString("name"),
								jArray.getJSONObject(i).getString("character"),
								jArray.getJSONObject(i).getString("id"),
								baseUrl + MizLib.getActorUrlSize(getActivity()) + jArray.getJSONObject(i).getString("profile_path")));
					}
				}

				actorIds.clear();
				actorIds = null;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	private void loadJson(String baseUrl) {		
		try {
			JSONObject jObject = new JSONObject(mJson);

			JSONArray jArray = jObject.getJSONObject("casts").getJSONArray("cast");

			mActors.clear();

			Set<String> actorIds = new HashSet<String>();

			for (int i = 0; i < jArray.length(); i++) {
				if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
					actorIds.add(jArray.getJSONObject(i).getString("id"));

					mActors.add(new Actor(
							jArray.getJSONObject(i).getString("name"),
							jArray.getJSONObject(i).getString("character"),
							jArray.getJSONObject(i).getString("id"),
							baseUrl + MizLib.getActorUrlSize(getActivity()) + jArray.getJSONObject(i).getString("profile_path")));
				}
			}

			actorIds.clear();
			actorIds = null;
		} catch (Exception e) {}

		if (isAdded()) {
			mProgressBar.setVisibility(View.GONE);
			mAdapter.notifyDataSetChanged();
		}
	}
}