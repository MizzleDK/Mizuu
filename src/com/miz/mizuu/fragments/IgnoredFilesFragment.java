package com.miz.mizuu.fragments;

import java.util.ArrayList;

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

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.Movie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShowEpisode;
import com.miz.mizuu.R;

public class IgnoredFilesFragment extends Fragment {

	private ArrayList<Movie> movies = new ArrayList<Movie>();
	private ArrayList<TvShowEpisode> episodes = new ArrayList<TvShowEpisode>();
	private ViewPager awesomePager;

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
		
		DbAdapter db = MizuuApplication.getMovieAdapter();
		Cursor cursor = db.getAllIgnoredMovies();
		while (cursor.moveToNext()) {
			movies.add(new Movie(getActivity(),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_IMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TRAILER)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CAST)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COLLECTION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
					false,
					false
					));
		}
		cursor.close();
		
		// Clear the episodes array
		episodes.clear();
		
		DbAdapterTvShowEpisode db2 = MizuuApplication.getTvEpisodeDbAdapter();
		Cursor cursor2 = db2.getAllIgnoredEpisodesInDatabase();
		while (cursor2.moveToNext()) {
			episodes.add(new TvShowEpisode(getActivity(),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_TITLE)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_PLOT)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_DIRECTOR)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_WRITER)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_GUESTSTARS)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_RATING)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)),
					cursor2.getString(cursor2.getColumnIndex(DbAdapterTvShowEpisode.KEY_EXTRA_1))
					));
		}
		cursor2.close();
		
		if (awesomePager != null) {
			awesomePager.setAdapter(new IgnoredFilesAdapter());
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
				convertView = inflater.inflate(R.layout.filesource_list, null);

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

			holder.folderName.setText(movies.get(position).getFilepath());
			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedMovie(movies.get(position).getRowId());
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
			return episodes.size();
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
				convertView = inflater.inflate(R.layout.filesource_list, null);

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

			holder.folderName.setText(episodes.get(position).getFilepath());
			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedEpisode(episodes.get(position).getRowId());
				}
			});

			return convertView;
		}
	}
	
	private void removeSelectedMovie(String rowId) {
		DbAdapter db = MizuuApplication.getMovieAdapter();
		db.deleteMovie(Long.parseLong(rowId));
		refreshData();
	}
	
	private void removeSelectedEpisode(String rowId) {
		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
		db.deleteEpisode(rowId);
		refreshData();
	}
}