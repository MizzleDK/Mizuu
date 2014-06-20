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

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.miz.db.DbAdapter;
import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.functions.TMDb;
import com.miz.functions.TMDbMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TMDbMovieDetails;
import com.squareup.picasso.Picasso;

import static com.miz.functions.PreferenceKeys.USE_LOCALIZED_DATA;

public class SearchWebMoviesFragment extends Fragment {

	public String filename;
	private ArrayList<Result> results = new ArrayList<Result>();
	private ListView lv;
	private EditText searchText;
	private ProgressBar pbar;
	private StartSearch startSearch;
	private boolean localizedInfo;
	private ListAdapter mAdapter;
	private CheckBox useSystemLanguage;
	private Locale locale;
	private Picasso mPicasso;
	private Config mConfig;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public SearchWebMoviesFragment() {}

	public static SearchWebMoviesFragment newInstance() { 
		SearchWebMoviesFragment pageFragment = new SearchWebMoviesFragment();
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		localizedInfo = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(USE_LOCALIZED_DATA, false);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();

		locale = Locale.getDefault();

		startSearch = new StartSearch();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.identify_movie, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.pbar);

		useSystemLanguage = (CheckBox) v.findViewById(R.id.searchLanguage);
		useSystemLanguage.setText(getString(R.string.searchIn) + " " + locale.getDisplayLanguage(Locale.ENGLISH));
		if (localizedInfo)
			useSystemLanguage.setChecked(true);

		lv = (ListView) v.findViewById(android.R.id.list);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				showMovie(arg2);
			}
		});
		lv.setEmptyView(v.findViewById(R.id.no_results));
		v.findViewById(R.id.no_results).setVisibility(View.GONE);

		searchText = (EditText) v.findViewById(R.id.search);
		searchText.setSelection(searchText.length());
		searchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.toString().length() > 0)
					searchForMovies();
				else {
					startSearch.cancel(true);
					results.clear();
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		searchText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_SEARCH)
					searchForMovies();
				return true;
			}
		});
	}

	protected void showMovie(int arg2) {
		DbAdapter db = MizuuApplication.getMovieAdapter();
		if (db.movieExists(results.get(arg2).getId())) {
			Intent intent = new Intent();
			intent.setClass(getActivity(), MovieDetails.class);
			intent.putExtra("tmdbId", results.get(arg2).getId());

			// Start the Intent for result
			startActivityForResult(intent, 0);
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setClass(getActivity(), TMDbMovieDetails.class);
			i.putExtra("tmdbid", results.get(arg2).getId());
			i.putExtra("title", results.get(arg2).getName());
			startActivity(i);
		}
	}

	public void searchForMovies(View v) {
		searchForMovies();
	}

	private void searchForMovies() {
		results.clear();
		if (MizLib.isOnline(getActivity())) {
			if (!searchText.getText().toString().isEmpty()) {
				startSearch.cancel(true);
				startSearch = new StartSearch();
				startSearch.execute(searchText.getText().toString());
			} else mAdapter.notifyDataSetChanged();
		} else Toast.makeText(getActivity().getApplicationContext(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
	}

	protected class StartSearch extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			showProgressBar();
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				if (isCancelled())
					return null;

				TMDb tmdb = new TMDb(getActivity().getApplicationContext());
				ArrayList<TMDbMovie> movieResults;
				if (useSystemLanguage.isChecked())
					movieResults = tmdb.searchForMovies(params[0], "", getLocaleShortcode());
				else
					movieResults = tmdb.searchForMovies(params[0], "", "en");

				if (isCancelled())
					return null;

				int count = movieResults.size();
				for (int i = 0; i < count; i++) {
					results.add(new Result(
							movieResults.get(i).getTitle(),
							movieResults.get(i).getId(),
							movieResults.get(i).getCover(),
							movieResults.get(i).getOriginalTitle(),
							movieResults.get(i).getReleasedate())
							);
				}

				if (isCancelled())
					return null;

				return "";
			} catch (Exception e) {}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (!isAdded() || result == null)
				return;

			hideProgressBar();
			if (searchText.getText().toString().length() > 0) {
				if (lv.getAdapter() == null) {
					mAdapter = new ListAdapter(getActivity());
					lv.setAdapter(mAdapter);
				} else {				
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	private String getLocaleShortcode() {
		String language = locale.toString();
		if (language.contains("_"))
			language = language.substring(0, language.indexOf("_"));
		return language;
	}

	static class ViewHolder {
		TextView title, orig_title, release;
		ImageView cover;
		LinearLayout layout;
	}

	public class ListAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ListAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mImageViewLayoutParams = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		public int getCount() {
			return results.size();
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_movie, parent, false);

				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.text);
				holder.orig_title = (TextView) convertView.findViewById(R.id.origTitle);
				holder.release = (TextView) convertView.findViewById(R.id.releasedate);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.layout = (LinearLayout) convertView.findViewById(R.id.cover_layout);

				// Check the height matches our calculated column width
				if (holder.layout.getLayoutParams().height != mItemHeight) {
					holder.layout.setLayoutParams(mImageViewLayoutParams);
				}

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.title.setText(results.get(position).getName());
			holder.orig_title.setText(results.get(position).getOriginalTitle());
			holder.release.setText(results.get(position).getRelease());

			mPicasso.load(results.get(position).getPic()).placeholder(R.drawable.gray).error(R.drawable.loading_image).config(mConfig).into(holder.cover);

			return convertView;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	public class Result {
		String name, id, pic, originaltitle, release;

		public Result(String name, String id, String pic, String originalTitle, String release) {
			this.name = name;
			this.id = id;
			this.pic = pic;
			this.originaltitle = originalTitle;
			this.release = release;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public String getPic() {
			return pic;
		}

		public String getOriginalTitle() {
			return originaltitle;
		}

		public String getRelease() {
			if (release.equals("null"))
				return getString(R.string.unknownYear);
			return release;
		}
	}

	private void showProgressBar() {
		lv.setVisibility(View.GONE);
		pbar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		lv.setVisibility(View.VISIBLE);
		pbar.setVisibility(View.GONE);
	}
}
