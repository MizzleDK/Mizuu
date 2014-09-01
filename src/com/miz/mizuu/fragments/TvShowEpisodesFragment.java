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

import static com.miz.functions.PreferenceKeys.TVSHOWS_COLLECTION_LAYOUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.miz.functions.CoverItem;
import com.miz.functions.GridEpisode;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisodeDetails;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

public class TvShowEpisodesFragment extends Fragment {

	private static final String SHOW_ID = "showId";
	private static final String SEASON = "season";

	private List<GridEpisode> mItems = new ArrayList<GridEpisode>();
	private Context mContext;
	private GridView mGridView;
	private ProgressBar mProgressBar;
	private ImageAdapter mAdapter;
	private String mShowId;
	private boolean mUseGridView;
	private int mSeason, mImageThumbSize, mImageThumbSpacing, mResizedWidth, mResizedHeight;
	private Picasso mPicasso;
	private Config mConfig;
	private Bus mBus;

	public static TvShowEpisodesFragment newInstance(String showId, int season) {
		TvShowEpisodesFragment frag = new TvShowEpisodesFragment();
		Bundle b = new Bundle();
		b.putString(SHOW_ID, showId);
		b.putInt(SEASON, season);
		frag.setArguments(b);
		return frag;
	}

	public TvShowEpisodesFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

		mPicasso = MizuuApplication.getPicasso(mContext);
		mConfig = MizuuApplication.getBitmapConfig();

		mBus = MizuuApplication.getBus();
		mBus.register(this);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.backdrop_thumbnail_width);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mShowId = getArguments().getString(SHOW_ID);
		mSeason = getArguments().getInt(SEASON);
		mUseGridView = PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_COLLECTION_LAYOUT, getString(R.string.gridView)).equals(getString(R.string.gridView));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mBus.unregister(this);
	}

	@Subscribe
	public void refreshData(com.miz.mizuu.TvShowEpisode episode) {		
		loadEpisodes();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (MizLib.isTablet(mContext) && !MizLib.isPortrait(mContext)) {
			v.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#05FFFFFF"));
		}
		
		mAdapter = new ImageAdapter(mContext);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setColumnWidth(mImageThumbSize);
		mGridView.setEmptyView(v.findViewById(R.id.progress));
		mGridView.setAdapter(mAdapter);

		if (mUseGridView) {
			// Calculate the total column width to set item heights by factor 1.5
			mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
					new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							if (mAdapter.getNumColumns() == 0) {
								final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
								if (numColumns > 0) {
									mAdapter.setNumColumns(numColumns);
									mResizedWidth = (int) (((mGridView.getWidth() - (numColumns * mImageThumbSpacing))
											/ numColumns) * 1.1); // * 1.1 is a hack to make images look slightly less blurry
									mResizedHeight = (int) (mResizedWidth / 1.778);
								}
							}
						}
					});
		} else {
			mGridView.setNumColumns(1);
			mAdapter.setNumColumns(1);
		}
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {	
				Intent i = new Intent(mContext, TvShowEpisodeDetails.class);
				i.putExtra(SHOW_ID, mShowId);
				i.putExtra("episode", mItems.get(arg2).getEpisode());
				i.putExtra("season", mSeason);
				startActivity(i);
			}
		});

		// The layout has been created - let's load the data
		loadEpisodes();
	}

	private void loadEpisodes() {
		new EpisodeLoader().execute();
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
			return mItems.size();
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
		public View getView(final int position, View convertView, ViewGroup container) {
			final CoverItem holder;
			final GridEpisode episode = mItems.get(position);

			if (convertView == null) {
				if (mUseGridView)
					convertView = inflater.inflate(R.layout.grid_episode, container, false);
				else
					convertView = inflater.inflate(R.layout.list_episode, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				if (mUseGridView)
					holder.text.setSingleLine(true);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
				if (mUseGridView)
					holder.subtext.setSingleLine(true);

				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.text.setText(episode.getTitle());
			
			if (mUseGridView)
			holder.subtext.setText(episode.getSubtitleText());
			else
				holder.subtext.setText(episode.getSubtitleText() + "\n" + MizLib.getPrettyDatePrecise(mContext, episode.getAirDate()));
			
			if (mResizedWidth > 0)
				mPicasso.load(episode.getCover()).placeholder(R.color.card_background_dark).error(R.drawable.nobackdrop).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder.cover);
			else
				mPicasso.load(episode.getCover()).placeholder(R.color.card_background_dark).error(R.drawable.nobackdrop).config(mConfig).into(holder.cover);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private class EpisodeLoader extends LibrarySectionAsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			mItems.clear();
			mItems.addAll(MizuuApplication.getTvEpisodeDbAdapter().getEpisodesInSeason(mContext, mShowId, mSeason));

			Collections.sort(mItems);

			return null;
		}

		@Override
		public void onPostExecute(Void result) {
			mProgressBar.setVisibility(View.GONE);
			mAdapter.notifyDataSetChanged();
		}
	}
}