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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.miz.functions.Cover;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.squareup.picasso.Picasso;

public class FanartSearchFragment extends Fragment {

	private ImageAdapter mAdapter;
	private ArrayList<Cover> covers = new ArrayList<Cover>();
	private ArrayList<String> pics_sources = new ArrayList<String>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private String[] items = new String[]{};
	private String TMDB_ID, json;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FanartSearchFragment() {}
	
	public static FanartSearchFragment newInstance(String tmdbId, String json, String baseUrl) {
		FanartSearchFragment pageFragment = new FanartSearchFragment();
		Bundle b = new Bundle();
		b.putString("tmdbId", tmdbId);
		b.putString("json", json);
		b.putString("baseUrl", baseUrl);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);   

		setHasOptionsMenu(true);
		setRetainInstance(true);

		TMDB_ID = getArguments().getString("tmdbId");
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		for (int i = 0; i < items.length; i++)
			menu.add(1, i, i, items[i]);
		menu.setGroupCheckable(1, true, true);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.backdrop_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (pics_sources.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mAdapter = new ImageAdapter(getActivity());
		mGridView.setAdapter(mAdapter);

		
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				new DownloadThread(pics_sources.get(arg2).replace(MizLib.getBackdropThumbUrlSize(getActivity()), MizLib.getBackdropUrlSize(getActivity()))).start();
			}
		});
		
		json = getArguments().getString("json");
		loadJson(getArguments().getString("baseUrl"));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private LayoutInflater inflater;
		private int mCardBackground;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mCardBackground = MizuuApplication.getCardColor(mContext);
		}

		@Override
		public int getCount() {
			return pics_sources.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_landscape_photo, container, false);
				holder = new CoverItem();
				
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}
			
			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(pics_sources.get(position)).placeholder(mCardBackground).error(R.drawable.nobackdrop).config(mConfig).into(holder.cover);

			return convertView;
		}
	}

	private void loadJson(String baseUrl) {
		try {
			pics_sources.clear();
			
			JSONObject jObject = new JSONObject(json);
			JSONArray array = jObject.getJSONArray("backdrops");

			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				covers.add(new Cover(baseUrl + MizLib.getBackdropThumbUrlSize(getActivity()) + o.getString("file_path"), o.getString("iso_639_1")));
				pics_sources.add(baseUrl + MizLib.getBackdropThumbUrlSize(getActivity()) + o.getString("file_path"));
			}
		} catch (Exception e) {}

		if (isAdded()) {
			showContent();
		}
	}
	
	private void showContent() {
		pbar.setVisibility(View.GONE);
		mAdapter.notifyDataSetChanged();

		TreeSet<String> languages = new TreeSet<String>();
		for (int i = 0; i < covers.size(); i++) {
			if (!covers.get(i).getLanguage().equals("Null"))
				languages.add(covers.get(i).getLanguage());
		}

		items = new String[languages.size() + 1];
		items[0] = getString(R.string.stringShowAllLanguages);
		Iterator<String> itr = languages.iterator();
		int i = 1;
		while (itr.hasNext()) {
			items[i] = itr.next();
			i++;
		}

		getActivity().invalidateOptionsMenu();
	}
	
	public class DownloadThread extends Thread {

		private String url;

		public DownloadThread(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			if (isAdded()) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity(), getString(R.string.addingCover), Toast.LENGTH_SHORT).show();
					}}
						);

				MizLib.downloadFile(url, new File(MizLib.getMovieBackdropFolder(getActivity()), TMDB_ID + "_bg.jpg").getAbsolutePath());
				
				updateWidgets();

				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-movie-backdrop-change"));
			}

			if (isAdded()) {
				getActivity().finish();
				return;
			}
		}
	}

	private void updateWidgets() {
		if (isAdded()) {
			AppWidgetManager awm = AppWidgetManager.getInstance(getActivity());
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(getActivity(), ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			getActivity().onBackPressed();
			return true;
		}

		item.setChecked(true);

		if (item.getItemId() == 0) {
			pics_sources.clear();
			for (int i = 0; i < covers.size(); i++)
				pics_sources.add(covers.get(i).getUrl());
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		} else {
			pics_sources.clear();
			for (int i = 0; i < covers.size(); i++) {
				if (covers.get(i).getLanguage().equals(items[item.getItemId()])) {
					pics_sources.add(covers.get(i).getUrl());
				}
			}
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		}

		return super.onOptionsItemSelected(item);
	}
}