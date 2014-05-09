package com.miz.mizuu.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
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
import android.widget.TextView;

import com.miz.functions.CoverItem;
import com.miz.functions.GridSeason;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class TvShowSeasonsFragment extends Fragment {
	
	private List<GridSeason> mItems = new ArrayList<GridSeason>();
	private int mImageThumbSize, mImageThumbSpacing;
	private GridView mGridView;
	private ImageAdapter mAdapter;
	private String mShowId;
	private Picasso mPicasso;
	private Config mConfig;
	
	public static TvShowSeasonsFragment newInstance(String showId) {
		TvShowSeasonsFragment frag = new TvShowSeasonsFragment();
		Bundle b = new Bundle();
		b.putString("showId", showId);
		frag.setArguments(b);
		return frag;
	}

	public TvShowSeasonsFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mShowId = getArguments().getString("showId");
		
		HashMap<String, Integer> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(mShowId);
		for (String key : seasons.keySet()) {
			mItems.add(new GridSeason(Integer.valueOf(key), seasons.get(key), MizLib.getTvShowSeasonFolder(getActivity()).getAbsolutePath() + "/" + mShowId + "_S" + key + ".jpg"));
		}
		
		Collections.sort(mItems);
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setEmptyView(v.findViewById(R.id.progress));
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(
									mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								mAdapter.setNumColumns(numColumns);
							}
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				
			}
		});
	}
	
	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mNumColumns = 0;

		public ImageAdapter(Context context) {
			super();
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
		public View getView(int position, View convertView, ViewGroup container) {
			CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);				
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
				holder.cover.setImageBitmap(null);
			}

			if (mItems.get(position).getSeason() == 0)
				holder.text.setText(R.string.stringSpecials);
			else
				holder.text.setText(getString(R.string.showSeason) + " " + mItems.get(position).getSeasonZeroIndex());
			
			int episodeCount = mItems.get(position).getEpisodeCount();
			holder.subtext.setText(episodeCount + " " + getResources().getQuantityString(R.plurals.episodes, episodeCount, episodeCount));

			mPicasso.load(mItems.get(position).getPoster()).config(mConfig).into(holder);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}
}
