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

public class ActorBrowserFragmentTv extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<Actor> actors = new ArrayList<Actor>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private String mTmdbApiKey;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBrowserFragmentTv() {}

	public static ActorBrowserFragmentTv newInstance(String showId) { 
		ActorBrowserFragmentTv pageFragment = new ActorBrowserFragmentTv();
		Bundle bundle = new Bundle();
		bundle.putString("showId", showId);
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
		
		new GetActorDetails(getActivity(), getArguments().getString("showId")).execute();
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

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (actors.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

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
				Intent intent = new Intent();
				intent.setClass(getActivity(), com.miz.mizuu.Actor.class);

				// Add the actor ID of the selected actor into a Bundle
				Bundle bundle = new Bundle();
				bundle.putString("actorName", actors.get(arg2).getName());
				bundle.putString("actorID", actors.get(arg2).getId());
				bundle.putString("thumb", actors.get(arg2).getUrl());

				// Create a new Intent with the Bundle
				intent.putExtras(bundle);

				// Start the Intent for result
				startActivity(intent);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
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
			return actors.size();
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
				convertView = inflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.text.setSingleLine(true);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
				holder.subtext.setSingleLine(true);

				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			holder.text.setText(actors.get(position).getName());
			holder.subtext.setText(actors.get(position).getCharacter().equals("null") ? "" : actors.get(position).getCharacter());

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			if (!actors.get(position).getUrl().endsWith("null"))
				mPicasso.load(actors.get(position).getUrl()).error(R.drawable.noactor).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.noactor);

			return convertView;
		}
	}

	protected class GetActorDetails extends AsyncTask<String, String, String> {

		private String mId;
		private Context mContext;

		public GetActorDetails(Context context, String id) {
			mContext = context;
			mId = id;
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				String baseUrl = MizLib.getTmdbImageBaseUrl(mContext);
				
				HttpClient httpclient = null;
				HttpGet httppost = null;
				ResponseHandler<String> responseHandler = null;
				JSONObject jObject = null;
				
				if (!mId.startsWith("tmdb_")) {
					httpclient = new DefaultHttpClient();
					httppost = new HttpGet("https://api.themoviedb.org/3/find/" + mId + "?api_key=" + mTmdbApiKey + "&external_source=tvdb_id");
					httppost.setHeader("Accept", "application/json");
					responseHandler = new BasicResponseHandler();
					String results = httpclient.execute(httppost, responseHandler);
					
					jObject = new JSONObject(results);
					mId = MizLib.getStringFromJSONObject(jObject.getJSONArray("tv_results").getJSONObject(0), "id", "");
				} else {
					httpclient = new DefaultHttpClient();
					mId = mId.replace("tmdb_", "");
				}

				httppost = new HttpGet("https://api.themoviedb.org/3/tv/" + mId + "/credits?api_key=" + mTmdbApiKey);
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);
				
				jObject = new JSONObject(html);

				JSONArray jArray = jObject.getJSONArray("cast");

				actors.clear();

				Set<String> actorIds = new HashSet<String>();

				for (int i = 0; i < jArray.length(); i++) {
					if (!actorIds.contains(jArray.getJSONObject(i).getString("id"))) {
						actorIds.add(jArray.getJSONObject(i).getString("id"));

						actors.add(new Actor(
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
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}