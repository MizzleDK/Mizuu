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

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.db.DbAdapterMovieMappings;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

import java.util.ArrayList;

public class IgnoredFilesFragment extends Fragment {

	private ArrayList<String> movies = new ArrayList<String>();
	private ArrayList<String> mEpisodes = new ArrayList<String>();
	private ViewPager awesomePager;
	private IgnoredFilesAdapter mAdapter;

	public IgnoredFilesFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		refreshData();
	}

	private void refreshData() {	
		// Clear the movies array
		movies.clear();

		ColumnIndexCache cache = new ColumnIndexCache();

		DbAdapterMovieMappings db = MizuuApplication.getMovieMappingAdapter();
		Cursor cursor = db.getAllIgnoredFilepaths();
		try {
			while (cursor.moveToNext()) {
				movies.add(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovieMappings.KEY_FILEPATH)));
			}
		} catch (Exception e) {
		} finally {
			cursor.close();
			cache.clear();
		}

		// Clear the episodes array
		mEpisodes.clear();

		Cursor cursor2 = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllIgnoredFilepaths();

		try {
			while (cursor2.moveToNext()) {
				mEpisodes.add(cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));
			}
		} catch (Exception e) {
		} finally {
			cursor2.close();
			cache.clear();
		}

		if (awesomePager != null) {
			if (mAdapter == null)
				mAdapter = new IgnoredFilesAdapter();

			if (awesomePager.getAdapter() == null)
				awesomePager.setAdapter(mAdapter);

			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);

		awesomePager = (ViewPager) v.findViewById(R.id.awesomepager);
		awesomePager.setAdapter(new IgnoredFilesAdapter());

		return v;
	}

	private class IgnoredFilesAdapter extends PagerAdapter {

		@Override
		public View instantiateItem(ViewGroup container, int position) {

			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View v = inflater.inflate(R.layout.list, container, false);
			ListView lv = (ListView) v.findViewById(android.R.id.list);
			lv.setBackgroundResource(0);

			if (position == 0) {
				lv.setAdapter(new MovieAdapter(getActivity()));
			} else {
				lv.setAdapter(new EpisodeAdapter(getActivity()));
			}

			((ViewPager) container).addView(v);
			return v;
		}

		@Override  
		public int getCount() {  
			return 2;  
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0: return getString(R.string.chooserMovies);
			default: return getString(R.string.chooserTVShows);
			}
		}

		@Override
		public void destroyItem(View collection, int position, Object view) {
			((ViewPager) collection).removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}

	static class ViewHolder {
		TextView folderName, fullPath;
		ImageView remove, icon;
	}

	public class MovieAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private Context c;

		public MovieAdapter(Context c) {
			this.c = c;
			inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return movies.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.filesource_list, parent, false);

				holder = new ViewHolder();
				holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.folderName.setTextAppearance(c, android.R.style.TextAppearance_Medium);
				holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.fullPath.setVisibility(View.GONE);
				holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
				holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);
				holder.icon.setVisibility(View.GONE);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

            String filepath = movies.get(position);
            String temp = filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[1] : filepath;

			holder.folderName.setText(MizLib.transformSmbPath(temp));
			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedMovie(movies.get(position));
				}
			});

			return convertView;
		}
	}

	public class EpisodeAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private Context c;

		public EpisodeAdapter(Context c) {
			this.c = c;
			inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return mEpisodes.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.filesource_list, parent, false);

				holder = new ViewHolder();
				holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.folderName.setTextAppearance(c, android.R.style.TextAppearance_Medium);
				holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.fullPath.setVisibility(View.GONE);
				holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
				holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);
				holder.icon.setVisibility(View.GONE);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String filepath = mEpisodes.get(position);
			String temp = filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[1] : filepath;
			
			holder.folderName.setText(MizLib.transformSmbPath(temp));
			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedEpisode(mEpisodes.get(position));
				}
			});

			return convertView;
		}
	}

	private void removeSelectedMovie(String filepath) {
        MizuuApplication.getMovieMappingAdapter().deleteIgnoredFilepath(filepath);

		refreshData();
	}

	private void removeSelectedEpisode(String filepath) {
		// This one also deals with checking if the mapped episode has any other mappings
		// - if not, it will delete the mapped episode as well
		MizuuApplication.getTvShowEpisodeMappingsDbAdapter().deleteFilepath(filepath);
		refreshData();
	}
}