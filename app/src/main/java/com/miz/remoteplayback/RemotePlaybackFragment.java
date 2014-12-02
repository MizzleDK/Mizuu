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

package com.miz.remoteplayback;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemMetadata;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaSessionStatus;
import android.support.v7.media.RemotePlaybackClient;
import android.support.v7.media.RemotePlaybackClient.ItemActionCallback;
import android.support.v7.media.RemotePlaybackClient.SessionActionCallback;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.miz.apis.thetvdb.TheTVDbService;
import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.apis.tmdb.TMDbTvShowService;
import com.miz.mizuu.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Work in progress......
 * @author Michell
 *
 */
public class RemotePlaybackFragment extends Fragment {
	
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter mMediaRouter;
	private RemotePlaybackClient mRemotePlaybackClient;
	private boolean mPlaying, mPaused;
	private Menu mMenu;
	private ImageView mBackgroundImage;
	
	private String mVideoUrl, mTitle, mCoverUrl, mId, mType;

	public RemotePlaybackFragment() {} // Empty constructor
	
	public static RemotePlaybackFragment newInstance(String videoUrl, String coverUrl, String title, String id, String type) {
		RemotePlaybackFragment fragment = new RemotePlaybackFragment();
		Bundle args = new Bundle();
		args.putString("videoUrl", videoUrl);
		args.putString("coverUrl", coverUrl);
		args.putString("title", title);
        args.putString("id", id);
        args.putString("type", type);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		Bundle arguments = getArguments();
		mVideoUrl = arguments.getString("videoUrl");
		mCoverUrl = arguments.getString("coverUrl");
		mTitle = arguments.getString("title");
        mId = arguments.getString("id");
        mType = arguments.getString("type");
		
		mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK).build();

        new CoverLoader().execute();
	}

	@Override
	public void onAttach(Activity host) {
		super.onAttach(host);

		mMediaRouter = MediaRouter.getInstance(host);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.remote_playback, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mBackgroundImage = (ImageView) view.findViewById(R.id.background_image);
	}

	@Override
	public void onResume() {
		super.onResume();

		mMediaRouter.addCallback(mMediaRouteSelector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	public void onPause() {
		super.onPause();
		
		mMediaRouter.removeCallback(mCallback);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		disconnect();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		mMenu = menu;
		inflater.inflate(R.menu.remote_playback, menu);

		updateMenu();

		MenuItem item = menu.findItem(R.id.route_provider);
		MediaRouteActionProvider provider= (MediaRouteActionProvider) MenuItemCompat.getActionProvider(item);

		if (provider != null)
			provider.setRouteSelector(mMediaRouteSelector);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.play:
			if (mPlaying && mPaused) {
				resume();
			} else {
				play();
			}
			return true;
		case R.id.stop:
			stop();
			return true;
		case R.id.pause:
			pause();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenu() {
		if (mMenu != null) {
			mMenu.findItem(R.id.stop).setVisible(mRemotePlaybackClient != null && mPlaying);
			mMenu.findItem(R.id.pause).setVisible(mRemotePlaybackClient != null && mPlaying && !mPaused);
			mMenu.findItem(R.id.play).setVisible(mRemotePlaybackClient != null && (!mPlaying || mPaused));
		}
	}

	private void play() {
		ItemActionCallback playCallback = new ItemActionCallback() {
			@Override
			public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus,
					String itemId, MediaItemStatus itemStatus) {
				mPlaying = true;
				updateMenu();
			}

			@Override
			public void onError(String error, int code, Bundle data) {}
		};
		
		/**
		 * WEB CONTENT
		 */
		Bundle meta = new Bundle();
		meta.putString(MediaItemMetadata.KEY_TITLE, mTitle);
		meta.putString(MediaItemMetadata.KEY_ARTWORK_URI, mCoverUrl);

		mRemotePlaybackClient.play(Uri.parse(mVideoUrl), "video/*", meta, 0, new Bundle(), playCallback);
	}

	private void pause() {
		PauseCallback pauseCallback = new PauseCallback();

		mRemotePlaybackClient.pause(null, pauseCallback);
		postCallback(pauseCallback);
	}

	private void resume() {
		ResumeCallback resumeCallback = new ResumeCallback();

		mRemotePlaybackClient.resume(null, resumeCallback);
		postCallback(resumeCallback);
	}

	private void stop() {
		StopCallback stopCallback = new StopCallback();

		mRemotePlaybackClient.stop(null, stopCallback);
		postCallback(stopCallback);
	}

	private void connect(MediaRouter.RouteInfo route) {
		mRemotePlaybackClient = new RemotePlaybackClient(getActivity().getApplication(), route);

		if (mRemotePlaybackClient.isRemotePlaybackSupported()) {
			if (mRemotePlaybackClient.isSessionManagementSupported()) {
				mRemotePlaybackClient.startSession(null, new SessionActionCallback() {
					@Override
					public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
						updateMenu();
					}

					@Override
					public void onError(String error, int code, Bundle data) {}
				});
			}
			else {
				getActivity().supportInvalidateOptionsMenu();
			}
		}
		else {
			mRemotePlaybackClient = null;
		}
	}

	private void disconnect() {
		mPlaying = false;
		mPaused = false;

		if (mRemotePlaybackClient != null) {
			EndSessionCallback endCallback = new EndSessionCallback();

			if (mRemotePlaybackClient.isSessionManagementSupported()) {
				mRemotePlaybackClient.endSession(null, endCallback);
			}

			postCallback(endCallback);
		}
	}

	private MediaRouter.Callback mCallback = new MediaRouter.Callback() {
		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
			connect(route);
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
			disconnect();
		}
	};

	abstract class RunnableSessionActionCallback extends SessionActionCallback implements Runnable {
		
		private boolean hasRun = false;
		
		abstract protected void doWork();

		@Override
		public void onResult(Bundle data, String sessionId, MediaSessionStatus sessionStatus) {
			removeCallbacks(this);
			run();
		}

		@Override
		public void run() {
			if (!hasRun) {
				hasRun = true;
				doWork();
			}
		}
	}

	private class PauseCallback extends RunnableSessionActionCallback {
		@Override
		protected void doWork() {
			mPaused = true;
			updateMenu();
		}
	}

	private class ResumeCallback extends RunnableSessionActionCallback {
		@Override
		protected void doWork() {
			mPaused = false;
			updateMenu();
		}
	}

	private class StopCallback extends RunnableSessionActionCallback {
		@Override
		protected void doWork() {
			mPlaying = false;
			mPaused = false;
			updateMenu();
		}
	}

	private class EndSessionCallback extends RunnableSessionActionCallback {
		@Override
		protected void doWork() {
			mRemotePlaybackClient.release();
			mRemotePlaybackClient = null;

			if (getActivity() != null) {
				updateMenu();
			}
		}
	}
	
	private void postCallback(RunnableSessionActionCallback callback) {
		mBackgroundImage.postDelayed(callback, 750);
	}
	
	private void removeCallbacks(RunnableSessionActionCallback callback) {
		mBackgroundImage.removeCallbacks(callback);
	}

    private class CoverLoader extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {

            List<String> covers = new ArrayList<String>();

            if (mType.equals("movie")) {
                TMDbMovieService service = TMDbMovieService.getInstance(getActivity());
                covers = service.getCovers(mId);
            } else {
                if (mId.startsWith("tmdb_")) {
                    TMDbTvShowService service = TMDbTvShowService.getInstance(getActivity());
                    covers = service.getCovers(mId.replace("tmdb_", ""));
                } else {
                    TheTVDbService service = TheTVDbService.getInstance(getActivity());
                    covers = service.getCovers(mId);
                }
            }

            if (covers.size() > 0)
                return covers.get(0);
            return null;
        }

        protected void onPostExecute(String result) {
            if (!TextUtils.isEmpty(result))
                mCoverUrl = result;
        }
    }
}