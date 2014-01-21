package com.miz.mizuu.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miz.db.DbAdapter;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.squareup.picasso.Picasso;

public class ActorMoviesFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<WebMovie> pics_sources = new ArrayList<WebMovie>();
	private SparseBooleanArray movieMap = new SparseBooleanArray();
	private GridView mGridView = null;
	private boolean showGridTitles, setBackground;
	private SharedPreferences settings;
	private DbAdapter db;
	private Picasso mPicasso;
	private String json, baseUrl;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorMoviesFragment() {}

	public static ActorMoviesFragment newInstance(String json, boolean setBackground, String baseUrl) { 
		ActorMoviesFragment pageFragment = new ActorMoviesFragment();
		Bundle bundle = new Bundle();
		bundle.putString("json", json);
		bundle.putString("baseUrl", baseUrl);
		bundle.putBoolean("setBackground", setBackground);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		db = MizuuApplication.getMovieAdapter();
		setBackground = getArguments().getBoolean("setBackground");

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

		showGridTitles = settings.getBoolean("prefsShowGridTitles", false);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicassoForWeb(getActivity());

		json = getArguments().getString("json");
		baseUrl = getArguments().getString("baseUrl");
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

		v.findViewById(R.id.progress).setVisibility(View.GONE);

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

		loadData();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
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
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			if (showGridTitles || pics_sources.get(position).getUrl().isEmpty() || pics_sources.get(position).getUrl().endsWith("null")) {
				holder.text.setVisibility(TextView.VISIBLE);
				holder.subtext.setVisibility(TextView.GONE);
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

			if (!pics_sources.get(position).getUrl().contains("null"))
				mPicasso.load(pics_sources.get(position).getUrl()).placeholder(R.drawable.gray).error(R.drawable.loading_image).into(holder.cover);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

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

	private void loadData() {
		try {
			JSONObject jObject = new JSONObject(json);

			JSONArray jArray = jObject.getJSONObject("credits").getJSONArray("cast");

			for (int i = 0; i < jArray.length(); i++) {
				if (!isInArray(jArray.getJSONObject(i).getString("id")))
					pics_sources.add(new WebMovie(
							jArray.getJSONObject(i).getString("original_title"),
							jArray.getJSONObject(i).getString("id"),
							baseUrl + MizLib.getImageUrlSize(getActivity()) + jArray.getJSONObject(i).getString("poster_path"),
							jArray.getJSONObject(i).getString("release_date")));
			}

			Collections.sort(pics_sources, getMovieComparator());

			new MoviesInLibraryCheck(pics_sources).execute();
		} catch (Exception ignored) {}
	}

	private Comparator<WebMovie> getMovieComparator() {
		return new Comparator<WebMovie>() {
			@Override
			public int compare(WebMovie o1, WebMovie o2) {
				// Dates are always presented as YYYY-MM-DD, so removing
				// the hyphens will easily provide a great way of sorting.

				int firstDate = 0, secondDate = 0;
				String first = "", second = "";

				if (o1.getDate() != null)
					first = o1.getDate().replace("-", "");

				if (!(first.equals("null") | first.isEmpty()))
					firstDate = Integer.valueOf(first);

				if (o2.getDate() != null)
					second = o2.getDate().replace("-", "");

				if (!(second.equals("null") | second.isEmpty()))
					secondDate = Integer.valueOf(second);

				// This part is reversed to get the highest numbers first
				if (firstDate < secondDate)
					return 1; // First date is lower than second date - put it second
				else if (firstDate > secondDate)
					return -1; // First date is greater than second date - put it first

				return 0; // They're equal
			}
		};
	}

	private boolean isInArray(String id) {
		for (int i = 0; i < pics_sources.size(); i++)
			if (id.equals(pics_sources.get(i).getId()))
				return true;
		return false;
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