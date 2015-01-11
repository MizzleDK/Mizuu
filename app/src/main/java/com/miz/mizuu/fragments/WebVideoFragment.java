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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class WebVideoFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebVideo> mVideos = new ArrayList<WebVideo>();
	private ObservableGridView mGridView = null;
	private ProgressBar mProgressBar;
	private String mType;
	private Picasso mPicasso;
	private Config mConfig;
	private Context mContext;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public WebVideoFragment() {}

	public static WebVideoFragment newInstance(String type) {
		WebVideoFragment pageFragment = new WebVideoFragment();
		Bundle bundle = new Bundle();
		bundle.putString("type", type);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.backdrop_thumbnail_width) * 1.25);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
		
		mContext = getActivity().getApplicationContext();
		
		mType = getArguments().getString("type");
		if (mType.equals(getString(R.string.choiceYouTube))) {
			new GetYouTubeVideos().execute();
		} else if (mType.equals(getString(R.string.choiceReddit))) {
			new RedditSearch().execute();
		} else { // Ted Talks
			new TEDSearch().execute();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.backdrop_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mVideos.size() > 0) mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (ObservableGridView) v.findViewById(R.id.gridView);
		mGridView.setClipToPadding(false);
		mGridView.setAdapter(mAdapter);
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(
									mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							mGridView.setColumnWidth(mImageThumbSize);
							if (numColumns > 0) {
								mAdapter.setNumColumns(numColumns);
							}
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getActivity()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(getActivity(), MizLib.getYouTubeApiKey(getActivity()), mVideos.get(arg2).getId(), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("http://www.youtube.com/watch?v=" + mVideos.get(arg2).getId()));
					startActivity(intent);
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onStart() {
		super.onStart();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mVideos.size();
		}

		@Override
		public Object getItem(int position) {
			return mVideos.get(position).getUrl();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			final WebVideo mWebVideo = mVideos.get(position);
			CoverItem holder;
			
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.webvideo_item, container, false);
				holder = new CoverItem();

				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				
				holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));
				
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.text.setText(mWebVideo.getTitle());
			
			mPicasso.load(mWebVideo.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.nobackdrop).config(mConfig).into(holder.cover);
			
			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	protected class GetYouTubeVideos extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			mVideos.clear();

			try {
				JSONObject jObject = MizLib.getJSONObject(mContext, "http://gdata.youtube.com/feeds/api/standardfeeds/most_popular?time=today&alt=json&start-index=1&max-results=50");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item = aitems.getJSONObject(i);
					JSONObject id = item.getJSONObject("id");

					mVideos.add(new WebVideo(
							item.getJSONObject("title").getString("$t"),
							id.getString("$t").substring(id.getString("$t").lastIndexOf("videos/") + 7, id.getString("$t").length()),
							item.getJSONObject("media$group").getJSONArray("media$thumbnail").getJSONObject(0).getString("url"))
							);
				}
			} catch (Exception e) {}

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

	protected class RedditSearch extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			mVideos.clear();

			try {
				JSONObject jsonObject = MizLib.getJSONObject(mContext, "http://www.reddit.com/r/videos/hot.json?sort=hot&limit=100");
				JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("children");

				for (int i = 0; i < jsonArray.length(); i++) {
					if (jsonArray.getJSONObject(i).getJSONObject("data").getString("domain").equals("youtube.com") || jsonArray.getJSONObject(i).getJSONObject("data").getString("domain").equals("youtu.be")) {
						String youtubeId = MizLib.getYouTubeId(jsonArray.getJSONObject(i).getJSONObject("data").getString("url"));
						if (!youtubeId.isEmpty()) {
							mVideos.add(new WebVideo(
									jsonArray.getJSONObject(i).getJSONObject("data").getString("title"),
									youtubeId,
									jsonArray.getJSONObject(i).getJSONObject("data").getJSONObject("media").getJSONObject("oembed").getString("thumbnail_url"))
									);
						}
					}
				}
			} catch (Exception e) {}

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

	protected class TEDSearch extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			mVideos.clear();

			try {
				JSONObject jObject = MizLib.getJSONObject(mContext, "http://gdata.youtube.com/feeds/api/users/TEDtalksDirector/uploads?alt=json&start-index=1&max-results=50");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item = aitems.getJSONObject(i);
					JSONObject id = item.getJSONObject("id");

					mVideos.add(new WebVideo(
							item.getJSONObject("title").getString("$t"),
							id.getString("$t").substring(id.getString("$t").lastIndexOf("videos/") + 7, id.getString("$t").length()),
							item.getJSONObject("media$group").getJSONArray("media$thumbnail").getJSONObject(0).getString("url"))
							);
				}
			} catch (Exception e) {}

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

	private class WebVideo {
		String title, id, url;

		public WebVideo(String title, String id, String url) {
			this.title = title;
			this.id = id;
			this.url = url;
		}

		public String getTitle() { return title; }
		public String getId() { return id; }
		public String getUrl() { return url; }
	}
}