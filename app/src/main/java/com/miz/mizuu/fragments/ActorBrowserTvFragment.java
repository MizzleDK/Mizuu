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

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.apis.tmdb.TMDbTvShowService;
import com.miz.functions.Actor;
import com.miz.functions.CoverItem;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.miz.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ActorBrowserTvFragment extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private List<Actor> mActors = new ArrayList<Actor>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private Picasso mPicasso;
	private Config mConfig;
	private boolean mLoaded = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBrowserTvFragment() {}

	public static ActorBrowserTvFragment newInstance(String showId) { 
		ActorBrowserTvFragment pageFragment = new ActorBrowserTvFragment();
		Bundle bundle = new Bundle();
		bundle.putString("showId", showId);
		pageFragment.setArguments(bundle);		
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);	
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
		
		new GetActorDetails(getActivity(), getArguments().getString("showId")).execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mActors.size() > 0)
			mProgressBar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mAdapter = new ImageAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(
								mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
						if (numColumns > 0) {
							mGridView.setNumColumns(numColumns);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), arg1.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(getActivity(), IntentUtils.getActorIntent(getActivity(), mActors.get(arg2)), options.toBundle());
			}
		});
		
		TextView title = (TextView) v.findViewById(R.id.empty_library_title);
		title.setText(R.string.no_actors);
		
		TextView description = (TextView) v.findViewById(R.id.empty_library_description);
		description.setText(R.string.no_actors_description);
		
		mGridView.setEmptyView(v.findViewById(R.id.empty_library_layout));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mActors.size();
		}
		
		@Override
		public boolean isEmpty() {
			return getCount() == 0 && mLoaded;
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
			
			final Actor actor = mActors.get(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_cover_two_line, container, false);
				holder = new CoverItem();

				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.text.setSingleLine(true);
				holder.subtext = (TextView) convertView.findViewById(R.id.sub_text);
				holder.subtext.setSingleLine(true);

				holder.text.setTypeface(TypefaceUtils.getRobotoMedium(mContext));

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			holder.cover.setImageResource(R.color.card_background_dark);

			holder.text.setText(actor.getName());
			holder.subtext.setText(actor.getCharacter());

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			if (!actor.getUrl().endsWith("null"))
				mPicasso.load(actor.getUrl()).error(R.drawable.noactor).config(mConfig).into(holder);
			else
				holder.cover.setImageResource(R.drawable.noactor);

			return convertView;
		}
	}

	protected class GetActorDetails extends AsyncTask<String, String, String> {

		private String mId;
		private Context mContext;

		public GetActorDetails(Context context, String id) {
			mContext = context;
			mId = id;
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				TMDbTvShowService service = TMDbTvShowService.getInstance(mContext);
				mActors = service.getActors(mId);
				
				mLoaded = true;
			} catch (Exception e) {} // If the fragment is no longer attached to the Activity

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}