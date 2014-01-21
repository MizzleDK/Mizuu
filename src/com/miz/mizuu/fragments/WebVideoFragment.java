package com.miz.mizuu.fragments;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class WebVideoFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebVideo> videos = new ArrayList<WebVideo>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private SharedPreferences settings;
	private String type;
	private Picasso mPicasso;

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

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.backdrop_thumbnail_width) * 1.4);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		mPicasso = MizuuApplication.getPicassoForWeb(getActivity());
		
		type = getArguments().getString("type");
		if (type.equals(getString(R.string.choiceYouTube))) {
			new GetYouTubeVideos().execute();
		} else if (type.equals(getString(R.string.choiceReddit))) {
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

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (videos.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
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
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getActivity()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(getActivity(), MizLib.YOUTUBE_API, videos.get(arg2).getId(), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("http://www.youtube.com/watch?v=" + videos.get(arg2).getId()));
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
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	static class CoverItem {
		TextView text;
		ImageView cover;
		FrameLayout layout;
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mItemHeight = 0;
		private int mNumColumns = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			super();
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		public int getCount() {
			return videos.size();
		}

		@Override
		public Object getItem(int position) {
			return videos.get(position).getUrl();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.webvideo_item, container, false);
				holder = new CoverItem();
				holder.layout = (FrameLayout) convertView.findViewById(R.id.layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.image);
				holder.text = (TextView) convertView.findViewById(R.id.widgetTitle);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
				holder.cover.setImageBitmap(null);
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			holder.text.setText(videos.get(position).getTitle());
			
			mPicasso.load(videos.get(position).getUrl()).placeholder(R.drawable.gray).error(R.drawable.nobackdrop).into(holder.cover);
			
			return convertView;
		}

		/**
		 * Sets the item height. Useful for when we know the column width so the height can be set
		 * to match.
		 *
		 * @param height
		 */
		public void setItemHeight(int height) {
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 0.45));
			notifyDataSetChanged();
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
			videos.clear();

			try {
				JSONObject jObject = MizLib.getJSONObject("http://gdata.youtube.com/feeds/api/standardfeeds/most_popular?time=today&alt=json&start-index=1&max-results=50");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item = aitems.getJSONObject(i);
					JSONObject id = item.getJSONObject("id");

					videos.add(new WebVideo(
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
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	protected class RedditSearch extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			videos.clear();

			try {
				JSONObject jsonObject = MizLib.getJSONObject("http://www.reddit.com/r/videos/hot.json?sort=hot&limit=100");
				JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("children");

				for (int i = 0; i < jsonArray.length(); i++) {
					if (jsonArray.getJSONObject(i).getJSONObject("data").getString("domain").equals("youtube.com") || jsonArray.getJSONObject(i).getJSONObject("data").getString("domain").equals("youtu.be")) {
						String youtubeId = MizLib.getYouTubeId(jsonArray.getJSONObject(i).getJSONObject("data").getString("url"));
						if (!youtubeId.isEmpty()) {
							videos.add(new WebVideo(
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
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	protected class TEDSearch extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			videos.clear();

			try {
				JSONObject jObject = MizLib.getJSONObject("http://gdata.youtube.com/feeds/api/users/TEDtalksDirector/uploads?alt=json&start-index=1&max-results=50");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item = aitems.getJSONObject(i);
					JSONObject id = item.getJSONObject("id");

					videos.add(new WebVideo(
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
				pbar.setVisibility(View.GONE);
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
}