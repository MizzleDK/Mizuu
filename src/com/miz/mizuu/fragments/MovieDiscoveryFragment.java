package com.miz.mizuu.fragments;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.db.DbAdapter;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.squareup.picasso.Picasso;

public class MovieDiscoveryFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebMovie> pics_sources = new ArrayList<WebMovie>();
	private SparseBooleanArray movieMap = new SparseBooleanArray();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private SharedPreferences settings;
	private DbAdapter db;
	private Picasso mPicasso;
	private String json, baseUrl;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDiscoveryFragment() {}

	public static MovieDiscoveryFragment newInstance(String type, String json, String baseUrl) { 
		MovieDiscoveryFragment pageFragment = new MovieDiscoveryFragment();
		Bundle bundle = new Bundle();
		bundle.putString("type", type);
		bundle.putString("json", json);
		bundle.putString("baseUrl", baseUrl);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		db = MizuuApplication.getMovieAdapter();

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		pbar.setVisibility(View.GONE);

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
								mAdapter.setNumColumns(numColumns);
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

		if (getArguments().containsKey("json")) {
			json = getArguments().getString("json");
			baseUrl = getArguments().getString("baseUrl");
			loadJson();
		}
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
				convertView = inflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(android.R.color.white);
			holder.text.setText(pics_sources.get(position).getTitle());

			if (movieMap.get(Integer.valueOf(pics_sources.get(position).getId()))) {
				holder.subtext.setText(pics_sources.get(position).getDate() + " (" + getString(R.string.inLibrary) + ")");
			} else {
				holder.subtext.setText(pics_sources.get(position).getDate());
			}

			if (!pics_sources.get(position).getUrl().contains("null"))
				mPicasso.load(pics_sources.get(position).getUrl()).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private void loadJson() {
		try {
			JSONObject jObject = new JSONObject(json);

			JSONArray jArray = jObject.getJSONObject(getArguments().getString("type")).getJSONArray("results");

			pics_sources.clear();
			for (int i = 0; i < jArray.length(); i++) {
				if (!MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("title")) && !MizLib.isAdultContent(getActivity(), jArray.getJSONObject(i).getString("original_title"))) {
					pics_sources.add(new WebMovie(
							jArray.getJSONObject(i).getString("original_title"),
							jArray.getJSONObject(i).getString("id"),
							baseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path"),
							MizLib.getPrettyDate(getActivity(), jArray.getJSONObject(i).getString("release_date"))));
				}
			}

			new MoviesInLibraryCheck(pics_sources).execute();
		} catch (Exception e) {}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("prefsRootAccess")) {
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
			for (int i = 0; i < movies.size(); i++)
				movieMap.put(Integer.valueOf(movies.get(i).getId()), db.movieExists(movies.get(i).getId()));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}