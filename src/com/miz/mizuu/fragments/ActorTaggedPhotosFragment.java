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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.functions.CompleteActor;
import com.miz.functions.CoverItem;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.IntentUtils;
import com.squareup.picasso.Picasso;

public class ActorTaggedPhotosFragment extends Fragment {

	private Context mContext;
	private ImageAdapter mAdapter;
	private GridView mGridView;
	private Picasso mPicasso;
	private Config mConfig;
	private ProgressBar mProgressBar;
	private String mActorId;
	private CompleteActor mActor;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorTaggedPhotosFragment() {}
	
	public static ActorTaggedPhotosFragment newInstance(String actorId) { 
		ActorTaggedPhotosFragment pageFragment = new ActorTaggedPhotosFragment();
		Bundle bundle = new Bundle();
		bundle.putString("actorId", actorId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mContext = getActivity();
		
		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();
		
		mActorId = getArguments().getString("actorId");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.backdrop_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		mGridView = (GridView) v.findViewById(R.id.gridView);

		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				startActivity(IntentUtils.getActorTaggedPhotoIntent(mContext, mActor.getTaggedPhotos(), arg2));
			}
		});

		new TaggedPhotoLoader(mContext, mActorId).execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
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
			return mActor.getTaggedPhotos().size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			CoverItem holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_landscape_photo, container, false);
				holder = new CoverItem();
				
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}
			
			holder.cover.setImageResource(R.color.card_background_dark);

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(mActor.getTaggedPhotos().get(position)).placeholder(R.color.card_background_dark).error(R.drawable.nobackdrop).config(mConfig).into(holder.cover);

			return convertView;
		}
	}

	private class TaggedPhotoLoader extends AsyncTask<Void, Void, Void> {

		private final Context mContext;
		private final String mActorId;

		public TaggedPhotoLoader(Context context, String actorId) {
			mContext = context;
			mActorId = actorId;
		}

		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			mActor = TMDbMovieService.getInstance(mContext).getCompleteActorDetails(mActorId);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				mProgressBar.setVisibility(View.GONE);
				
				mAdapter = new ImageAdapter(getActivity());
				mGridView.setAdapter(mAdapter);
			}
		}
	}
}