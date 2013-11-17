package com.miz.mizuu.fragments;

import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.ImageViewer;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ActorPhotosFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<String> pics_sources = new ArrayList<String>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;
	private boolean setBackground;
	
	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorPhotosFragment() {}

	public static ActorPhotosFragment newInstance(String actorId, String actorName, boolean setBackground) { 
		ActorPhotosFragment pageFragment = new ActorPhotosFragment();
		Bundle bundle = new Bundle();
		bundle.putString("actorId", actorId);
		bundle.putString("actorName", actorName);
		bundle.putBoolean("setBackground", setBackground);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		setBackground = getArguments().getBoolean("setBackground");
		
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		imageLoader = ImageLoader.getInstance();
		options = MizuuApplication.getDefaultActorLoadingOptions();
		
		new GetActorDetails().execute(getArguments().getString("actorId"));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (setBackground && !MizLib.runsInPortraitMode(getActivity()))
			v.findViewById(R.id.container).setBackgroundResource(R.drawable.bg);
		
		MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (pics_sources.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
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
				String[] photos = new String[pics_sources.size()];
				for (int i = 0; i < photos.length; i++)
					photos[i] = pics_sources.get(i).replace("w185", "original");

				Intent intent = new Intent();
				intent.setClass(getActivity(), ImageViewer.class);
				intent.putExtra("photos", photos);
				intent.putExtra("actorName", getArguments().getString("actorName"));
				intent.putExtra("selectedIndex", arg2);
				startActivity(intent);
			}
		});
		mGridView.setOnScrollListener(MizuuApplication.getPauseOnScrollListener(imageLoader));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onPause() {
		super.onPause();

		imageLoader.stop();
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
			return pics_sources.size();
		}

		@Override
		public Object getItem(int position) {
			return pics_sources.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_item_cover, container, false);
				holder = new CoverItem();
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.cover_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.text.setVisibility(View.GONE);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			imageLoader.displayImage(pics_sources.get(position), holder.cover, options);

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
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 1.5));
			notifyDataSetChanged();
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	protected class GetActorDetails extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet("https://api.themoviedb.org/3/configuration?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String baseUrl = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(baseUrl);
				try { baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) { baseUrl = "http://cf2.imgobject.com/t/p/"; }

				httpclient = new DefaultHttpClient();
				httppost = new HttpGet("https://api.themoviedb.org/3/person/" + params[0] + "/images?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				jObject = new JSONObject(html);

				JSONArray jArray = jObject.getJSONArray("profiles");

				for (int i = 0; i < jArray.length(); i++) {
					pics_sources.add(baseUrl + MizLib.getActorUrlSize(getActivity()) + jArray.getJSONObject(i).getString("file_path"));
				}				

			} catch (Exception e) { e.printStackTrace(); }

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