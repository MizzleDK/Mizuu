package com.miz.mizuu.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
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

import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.DbAdapter;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class MovieDiscoveryFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebMovie> pics_sources = new ArrayList<WebMovie>();
	private SparseBooleanArray movieMap = new SparseBooleanArray();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private boolean showGridTitles;
	private SharedPreferences settings;
	private DbAdapter db;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDiscoveryFragment() {}

	public static MovieDiscoveryFragment newInstance(String type) { 
		MovieDiscoveryFragment pageFragment = new MovieDiscoveryFragment();
		Bundle bundle = new Bundle();
		bundle.putString("type", type);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		db = MizuuApplication.getMovieAdapter();
		
		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		showGridTitles = settings.getBoolean("prefsShowGridTitles", false);

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		imageLoader = ImageLoader.getInstance();
		options = MizuuApplication.getDefaultCoverLoadingOptions();

		new GetTrailers().execute(getArguments().getString("type"));
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
				if (movieMap.get(Integer.valueOf(pics_sources.get(arg2).getId()))) {
					Intent intent = new Intent();
					intent.setClass(getActivity(), MovieDetails.class);
					intent.putExtra("tmdbId", pics_sources.get(arg2).getId());

					// Start the Intent for result
					startActivityForResult(intent, 0);
				} else {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setClass(getActivity(), TMDbMovieDetails.class);
					i.putExtra("tmdbid", pics_sources.get(arg2).getId());
					i.putExtra("title", pics_sources.get(arg2).getTitle());
					startActivity(i);
				}
			}
		});
		mGridView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
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
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
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
			return pics_sources.get(position).getUrl();
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
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
				holder.cover.setImageBitmap(null);
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			if (showGridTitles || pics_sources.get(position).getUrl().isEmpty() || pics_sources.get(position).getUrl().endsWith("null")) {
				holder.text.setVisibility(TextView.VISIBLE);
				holder.subtext.setVisibility(TextView.VISIBLE);
				holder.subtext.setText("");
				holder.text.setText(pics_sources.get(position).getTitle());
			} else {
				holder.text.setVisibility(TextView.GONE);
				holder.subtext.setVisibility(TextView.GONE);
			}
			
			if (movieMap.get(Integer.valueOf(pics_sources.get(position).getId()))) {
				holder.subtext.setVisibility(TextView.VISIBLE);
				holder.subtext.setText(getString(R.string.inLibrary).toUpperCase(Locale.getDefault()));
			}

			imageLoader.displayImage(pics_sources.get(position).getUrl().contains("null") ? "" : pics_sources.get(position).getUrl(), holder.cover, options);

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

	protected class GetTrailers extends AsyncTask<String, String, String> {
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
				httppost = new HttpGet("https://api.themoviedb.org/3/movie/" + params[0] + "?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				jObject = new JSONObject(html);

				JSONArray jArray = jObject.getJSONArray("results");

				for (int i = 0; i < jArray.length(); i++) {
						pics_sources.add(new WebMovie(
								jArray.getJSONObject(i).getString("original_title"),
								jArray.getJSONObject(i).getString("id"),
								baseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path")));
				}
			} catch (Exception e) { e.printStackTrace(); }

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				new MoviesInLibraryCheck(pics_sources).execute();
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("prefsShowGridTitles")) {
			showGridTitles = settings.getBoolean("prefsShowGridTitles", false);
			mAdapter.notifyDataSetChanged();
		} else if (key.equals("prefsRootAccess")) {
			if (settings.getBoolean("prefsRootAccess", false)) {
				try {
					Runtime.getRuntime().exec("su");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class MoviesInLibraryCheck extends AsyncTask<Void, Void, Void> {

		private ArrayList<WebMovie> movies = new ArrayList<WebMovie>();
		
		public MoviesInLibraryCheck(ArrayList<WebMovie> movies) {
			this.movies = movies;
			movieMap.clear();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			for (WebMovie m : movies)
				movieMap.put(Integer.valueOf(m.getId()), db.movieExists(m.getId()));
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}