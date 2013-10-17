package com.miz.mizuu.fragments;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.bitmap.ImageFetcher;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.functions.MizLib;
import com.miz.mizuu.AsyncTask;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.fragments.ShowSeasonsFragment.CoverItem;
import com.miz.mizuu.fragments.ShowSeasonsFragment.RowItem;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapterWrapper;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

public class CalendarFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageFetcher mImageFetcher;
	private EpisodesAdapter mAdapter;
	private StickyGridHeadersGridView mGridView = null;
	private ProgressBar pbar;
	private SharedPreferences settings;
	private ArrayList<String> episodes = new ArrayList<String>();
	private ArrayList<String> days = new ArrayList<String>();
	private SparseIntArray dayEpisodeCount = new SparseIntArray();
	private ArrayList<CalendarItem> allEpisodes = new ArrayList<CalendarItem>();

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public CalendarFragment() {}

	public static CalendarFragment newInstance() {
		return new CalendarFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.backdrop_thumbnail_width) * 1.4);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
		mImageFetcher.setLoadingImage(null);
		mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), MizuuApplication.getMemoryCache());

		new GetCalendar().execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.calendar_grid_upcoming, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		pbar = (ProgressBar) v.findViewById(R.id.progressbar);
		if (episodes.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mGridView = (StickyGridHeadersGridView) v.findViewById(R.id.gridView);
		
		mAdapter = new EpisodesAdapter(getActivity().getApplicationContext(), mGridView, new SimpleAdapter());
		
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
								final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
								mAdapter.setNumColumns(numColumns);
								mAdapter.setItemHeight(columnWidth);
							}
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = YouTubeStandalonePlayer.createVideoIntent(getActivity(), MizLib.YOUTUBE_API, episodes.get(arg2), 0, false, true);
				startActivity(intent);
			}
		});
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView, int scrollState) {
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
					mImageFetcher.setPauseWork(true);
				} else {
					mImageFetcher.setPauseWork(false);
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
			}
		});
	}

	private class SimpleAdapter implements StickyGridHeadersBaseAdapter {

		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			int count = 0;

			for (String day : days)
				count += dayEpisodeCount.get(Integer.valueOf(day));

			return count;
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
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.episode_grid_item, parent, false);
				holder = new CoverItem();
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.watched = (ImageView) convertView.findViewById(R.id.watched);
				holder.title = (TextView) convertView.findViewById(R.id.text);
				holder.number = (TextView) convertView.findViewById(R.id.episodeText);
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout1);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.title.setText(allEpisodes.get(position).getTitle());
			holder.title.setVisibility(TextView.VISIBLE);
			holder.number.setText(allEpisodes.get(position).getEpisode());
			holder.number.setVisibility(TextView.VISIBLE);

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			mImageFetcher.loadImage(allEpisodes.get(position).getEpisodePhoto(), holder.cover, null, null);

			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {}

		@Override
		public int getCountForHeader(int header) {
			return dayEpisodeCount.get(Integer.valueOf(days.get(header)));
		}

		@Override
		public int getNumHeaders() {
			return dayEpisodeCount.size();
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			RowItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.seasons_row_header, parent, false);
				holder = new RowItem();
				holder.title = (TextView) convertView.findViewById(R.id.seasonTitle);
				holder.episodeCount = (TextView) convertView.findViewById(R.id.episodeCount);
				convertView.setTag(holder);
			} else {
				holder = (RowItem) convertView.getTag();
			}

			holder.title.setText(getString(R.string.showSeason) + " " + days.get(position));

			int size = dayEpisodeCount.get(Integer.valueOf(days.get(position)));
			holder.episodeCount.setText(size + " " + getResources().getQuantityString(R.plurals.episodes, size, size));

			convertView.setClickable(false);
			convertView.setFocusable(false);

			return convertView;
		}

	}

	private class EpisodesAdapter extends StickyGridHeadersBaseAdapterWrapper {

		public EpisodesAdapter(Context context,
				StickyGridHeadersGridView gridView,
				StickyGridHeadersBaseAdapter delegate) {
			super(context, gridView, delegate);
		}

		private int mItemHeight = 0;
		private int mNumColumns = 0;
		private GridView.LayoutParams mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			int count = 0;

			for (String day : days)
				count += dayEpisodeCount.get(Integer.valueOf(day));

			return count;
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
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.episode_grid_item, parent, false);
				holder = new CoverItem();
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.watched = (ImageView) convertView.findViewById(R.id.watched);
				holder.title = (TextView) convertView.findViewById(R.id.text);
				holder.number = (TextView) convertView.findViewById(R.id.episodeText);
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout1);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.title.setText(allEpisodes.get(position).getTitle());
			holder.title.setVisibility(TextView.VISIBLE);
			holder.number.setText(allEpisodes.get(position).getEpisode());
			holder.number.setVisibility(TextView.VISIBLE);

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			mImageFetcher.loadImage(allEpisodes.get(position).getEpisodePhoto(), holder.cover, null, null);

			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			RowItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.seasons_row_header, parent, false);
				holder = new RowItem();
				holder.title = (TextView) convertView.findViewById(R.id.seasonTitle);
				holder.episodeCount = (TextView) convertView.findViewById(R.id.episodeCount);
				convertView.setTag(holder);
			} else {
				holder = (RowItem) convertView.getTag();
			}

			holder.title.setText(getString(R.string.showSeason) + " " + days.get(position));

			int size = dayEpisodeCount.get(Integer.valueOf(days.get(position)));
			holder.episodeCount.setText(size + " " + getResources().getQuantityString(R.plurals.episodes, size, size));

			convertView.setClickable(false);
			convertView.setFocusable(false);

			return convertView;
		}

		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 0.5625));
			mImageFetcher.setImageSize(height);
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mImageFetcher != null) mImageFetcher.setExitTasksEarly(false);
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setExitTasksEarly(true);
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
		mImageFetcher.closeCache();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menuweb, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("prefsRootAccess")) {
			if (settings.getBoolean("prefsRootAccess", false)) {
				try {
					Runtime.getRuntime().exec("su");
				} catch (IOException e) {}
			}
		}
	}

	protected class GetCalendar extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			try {
				JSONArray json = MizLib.getTraktCalendar(getActivity().getApplicationContext());
				for (int i = 0; i < json.length(); i++) {

					JSONObject date = json.getJSONObject(i);
					JSONArray episodes = date.getJSONArray("episodes");
					
					for (int j = 0; j < episodes.length(); j++) {
						JSONObject jsonEpisode = episodes.getJSONObject(j);
						
						allEpisodes.add(new CalendarItem(
								jsonEpisode.getJSONObject("episode").getJSONObject("images").getString("screen"),
								jsonEpisode.getJSONObject("episode").getString("title"),
								jsonEpisode.getJSONObject("episode").getString("season"),
								jsonEpisode.getJSONObject("episode").getString("number")
								)
						);
						
					}

				}
				
				int tempCount = 0;
				
				for (CalendarItem episode : allEpisodes) {			
					if (dayEpisodeCount.get(Integer.valueOf(episode.getSeason())) == 0) {
						dayEpisodeCount.append(Integer.valueOf(episode.getSeason()), 1);
						days.add(episode.getSeason());
					} else {
						tempCount = dayEpisodeCount.get(Integer.valueOf(episode.getSeason()));
						dayEpisodeCount.append(Integer.valueOf(episode.getSeason()), tempCount + 1);
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
				
				System.out.println("Size: " + allEpisodes.size());
			}
		}
	}

	private class CalendarItem {

		private String photo, title, season, episode;
		
		public CalendarItem(String photo, String title, String season, String episode) {
			this.photo = photo;
			this.title = title;
			this.season = season;
			this.episode = episode;
		}
		
		public String getEpisodePhoto() {
			return photo;
		}

		public String getTitle() {
			return title;
		}

		public String getSeason() {
			return season;
		}

		public String getEpisode() {
			return episode;
		}

	}
}