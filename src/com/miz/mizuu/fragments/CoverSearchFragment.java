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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.miz.functions.AsyncTask;
import com.miz.functions.Cover;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieCoverWidgetProvider;
import com.miz.widgets.MovieStackWidgetProvider;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class CoverSearchFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<Cover> covers = new ArrayList<Cover>();
	private ArrayList<String> pics_sources = new ArrayList<String>();
	private GridView mGridView = null;
	private String TMDB_ID;
	private String[] items = new String[]{};
	private ProgressBar pbar;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public CoverSearchFragment() {}

	public static CoverSearchFragment newInstance(String tmdbId) {
		CoverSearchFragment pageFragment = new CoverSearchFragment();
		Bundle b = new Bundle();
		b.putString("tmdbId", tmdbId);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		imageLoader = ImageLoader.getInstance();
		options = MizuuApplication.getDefaultCoverLoadingOptions();

		TMDB_ID = getArguments().getString("tmdbId");

		new GetCoverImages().execute(TMDB_ID);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		for (int i = 0; i < items.length; i++)
			menu.add(0, i, i, items[i]);
		menu.setGroupCheckable(0, true, true);

		super.onCreateOptionsMenu(menu, inflater);
	}	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (pics_sources.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mGridView = (GridView) v.findViewById(R.id.gridView);

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
							final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
							mAdapter.setItemHeight(columnWidth);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				new DownloadThread(pics_sources.get(arg2)).start();
			}
		});
		mGridView.setOnScrollListener(MizuuApplication.getPauseOnScrollListener(imageLoader));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();

		imageLoader.stop();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			super();
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
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
			// Now handle the main ImageView thumbnails
			ImageView imageView;

			if (convertView == null) { // if it's not recycled, instantiate and initialize
				imageView = new ImageView(mContext);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(mImageViewLayoutParams);
			} else { // Otherwise re-use the converted view
				imageView = (ImageView) convertView;
			}

			// Check the height matches our calculated column width
			if (imageView.getLayoutParams().height != mItemHeight) {
				imageView.setLayoutParams(mImageViewLayoutParams);
			}

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs

			imageLoader.displayImage(pics_sources.get(position), imageView, options);

			return imageView;
		}

		/**
		 * Sets the item height. Useful for when we know the column width so the height can be set
		 * to match.
		 *
		 * @param height
		 */
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 1.5));

			notifyDataSetChanged();
		}
	}

	protected class GetCoverImages extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			String TMDBID = params[0];
			try {
				String baseUrl;

				JSONObject jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/configuration?api_key=" + MizLib.TMDB_API);
				try { baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) { baseUrl = "http://cf2.imgobject.com/t/p/"; }

				jObject = MizLib.getJSONObject("https://api.themoviedb.org/3/movie/" + TMDBID + "/images?api_key=" + MizLib.TMDB_API);

				JSONArray array = jObject.getJSONArray("posters");
				for (int i = 0; i < array.length(); i++) {
					JSONObject o = array.getJSONObject(i);
					covers.add(new Cover(baseUrl + MizLib.getImageUrlSize(getActivity()) + o.getString("file_path"), o.getString("iso_639_1")));
					pics_sources.add(baseUrl + MizLib.getImageUrlSize(getActivity()) + o.getString("file_path"));
				}
			} catch (Exception e) {}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();

				TreeSet<String> languages = new TreeSet<String>();
				for (Cover c : covers) {
					if (!c.getLanguage().equals("Null"))
						languages.add(c.getLanguage());
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

				Intent intent = new Intent("mizuu-cover-search-fragment");
				intent.putExtra("coverCount", pics_sources.size());
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
			}
		}
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

				MizLib.downloadFile(url, new File(MizLib.getMovieThumbFolder(getActivity()), TMDB_ID + ".jpg").getAbsolutePath());
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-movie-cover-change"));

				updateWidgets();
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
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(getActivity(), MovieStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(getActivity(), MovieCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
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
			for (Cover c : covers)
				pics_sources.add(c.getUrl());
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		} else {
			pics_sources.clear();
			for (Cover c : covers) {
				if (c.getLanguage().equals(items[item.getItemId()])) {
					pics_sources.add(c.getUrl());
				}
			}
			if (mAdapter != null)
				((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
		}

		return super.onOptionsItemSelected(item);
	}
}