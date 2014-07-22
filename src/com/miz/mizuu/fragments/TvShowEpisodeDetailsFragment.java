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

import static com.miz.functions.PreferenceKeys.DISABLE_ETHERNET_WIFI_CHECK;
import static com.miz.functions.PreferenceKeys.IGNORED_FILES_ENABLED;
import static com.miz.functions.PreferenceKeys.IGNORE_VIDEO_FILE_TYPE;
import static com.miz.functions.PreferenceKeys.ALWAYS_DELETE_FILE;
import static com.miz.functions.PreferenceKeys.BUFFER_SIZE;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.BlurTransformation;
import com.miz.functions.MizLib;
import com.miz.mizuu.IdentifyTvShow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisode;
import com.miz.service.DeleteFile;
import com.miz.service.MakeAvailableOffline;
import com.miz.smbstreamer.Streamer;
import com.miz.views.ObservableScrollView;
import com.miz.views.PanningView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class TvShowEpisodeDetailsFragment extends Fragment {

	private TvShowEpisode mEpisode;
	private ImageView mBackdrop, mEpisodePhoto;
	private TextView mTitle, mDescription, mFileSource, mAirDate, mRating, mDirector, mWriter, mGuestStars, mSeasonEpisodeNumber;
	private Picasso mPicasso;
	private Typeface mLight, mLightItalic, mMedium;
	private DbAdapterTvShowEpisode mDatabaseHelper;
	private long mVideoPlaybackStarted, mVideoPlaybackEnded;
	private boolean mVideoWildcard, mDisableEthernetWiFiCheck, mIgnoreDeletedFiles;
	private Bus mBus;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public TvShowEpisodeDetailsFragment() {}

	public static TvShowEpisodeDetailsFragment newInstance(String showId, int season, int episode) { 
		TvShowEpisodeDetailsFragment pageFragment = new TvShowEpisodeDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("showId", showId);
		bundle.putInt("season", season);
		bundle.putInt("episode", episode);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		mBus = MizuuApplication.getBus();

		mVideoWildcard = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORE_VIDEO_FILE_TYPE, false);
		mDisableEthernetWiFiCheck = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(DISABLE_ETHERNET_WIFI_CHECK, false);
		mIgnoreDeletedFiles = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_FILES_ENABLED, false);

		mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");
		mLightItalic = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-LightItalic.ttf");
		mMedium = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Medium.ttf");

		mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();

		if (!getArguments().getString("showId").isEmpty() && getArguments().getInt("season") >= 0 && getArguments().getInt("episode") > 0) {
			Cursor cursor = mDatabaseHelper.getEpisode(getArguments().getString("showId"), getArguments().getInt("season"), getArguments().getInt("episode"));
			if (cursor.moveToFirst()) {
				mEpisode = new TvShowEpisode(getActivity(),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_DIRECTOR)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_WRITER)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_GUESTSTARS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EXTRA_1))
						);
			}
			cursor.close();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_episode_details, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mBackdrop = (ImageView) view.findViewById(R.id.imageBackground);
		mEpisodePhoto = (ImageView) view.findViewById(R.id.episodePhoto);

		mTitle = (TextView) view.findViewById(R.id.movieTitle);
		mSeasonEpisodeNumber = (TextView) view.findViewById(R.id.textView7);
		mDescription = (TextView) view.findViewById(R.id.textView2);
		mFileSource = (TextView) view.findViewById(R.id.textView3);
		mAirDate = (TextView) view.findViewById(R.id.textReleaseDate);
		mRating = (TextView) view.findViewById(R.id.textView12);
		mDirector = (TextView) view.findViewById(R.id.director);
		mWriter = (TextView) view.findViewById(R.id.writer);
		mGuestStars = (TextView) view.findViewById(R.id.guest_stars);	

		if (MizLib.isPortrait(getActivity())) {
			final boolean fullscreen = MizuuApplication.isFullscreen(getActivity());
			final int height = fullscreen ? MizLib.getActionBarHeight(getActivity()) : MizLib.getActionBarAndStatusBarHeight(getActivity());

			ObservableScrollView sv = (ObservableScrollView) view.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = mEpisodePhoto.getHeight() - height;
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);

					// We only want to update the ActionBar if it would actually make a change (times 1.2 to handle fast flings)
					if (t <= headerHeight * 1.2) {
						mBus.post(Integer.valueOf(newAlpha));

						// Such parallax, much wow
						mEpisodePhoto.setPadding(0, (int) (t / 1.5), 0, 0);
					}
				}
			});
		} else {
			if (!MizuuApplication.isFullscreen(getActivity()))
				MizLib.addActionBarAndStatusBarMargin(getActivity(), view.findViewById(R.id.episode_relative_layout));
			else
				MizLib.addActionBarMargin(getActivity(), view.findViewById(R.id.episode_relative_layout));
		}

		// Set the episode title
		mTitle.setVisibility(View.VISIBLE);
		mTitle.setText(mEpisode.getTitle());
		mTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		mTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		mDescription.setTypeface(mLight);
		mFileSource.setTypeface(mLight);
		mDirector.setTypeface(mLight);
		mWriter.setTypeface(mLight);
		mGuestStars.setTypeface(mLight);

		if (MizLib.isPortrait(getActivity())) {
			mAirDate.setTypeface(mMedium);
			mRating.setTypeface(mMedium);
			mSeasonEpisodeNumber.setTypeface(mLightItalic);		
			mSeasonEpisodeNumber.setText(getString(R.string.showSeason) + " " + mEpisode.getSeason() + ", " + getString(R.string.showEpisode) + " " + mEpisode.getEpisode());
		} else {
			mAirDate.setTypeface(mLight);
			mRating.setTypeface(mLight);
		}

		mDescription.setBackgroundResource(R.drawable.selectable_background);
		mDescription.setMaxLines(getActivity().getResources().getInteger(R.integer.episode_details_max_lines));
		mDescription.setTag(true); // true = collapsed
		mDescription.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((Boolean) mDescription.getTag())) {
					mDescription.setMaxLines(1000);
					mDescription.setTag(false);
				} else {
					mDescription.setMaxLines(getActivity().getResources().getInteger(R.integer.episode_details_max_lines));
					mDescription.setTag(true);
				}
			}
		});
		mDescription.setEllipsize(TextUtils.TruncateAt.END);
		mDescription.setFocusable(true);
		mDescription.setText(mEpisode.getDescription());

		mFileSource.setText(mEpisode.getFilepath());

		// Set the episode air date
		mAirDate.setText(MizLib.getPrettyDatePrecise(getActivity(), mEpisode.getReleasedate()));

		// Set the movie rating
		if (!mEpisode.getRating().equals("0.0/10")) {
			if (mEpisode.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(mEpisode.getRating().substring(0, mEpisode.getRating().indexOf("/"))) * 10);
					mRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					mRating.setText(Html.fromHtml(mEpisode.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				mRating.setText(mEpisode.getRating());
			}
		} else {
			mRating.setText(R.string.stringNA);
		}

		if (MizLib.isEmpty(mEpisode.getDirector()) || mEpisode.getDirector().equals(getString(R.string.stringNA))) {
			mDirector.setVisibility(View.GONE);
		} else {
			mDirector.setText(mEpisode.getDirector());
		}

		if (MizLib.isEmpty(mEpisode.getWriter()) || mEpisode.getWriter().equals(getString(R.string.stringNA))) {
			mWriter.setVisibility(View.GONE);
		} else {
			mWriter.setText(mEpisode.getWriter());
		}

		if (MizLib.isEmpty(mEpisode.getGuestStars()) || mEpisode.getGuestStars().equals(getString(R.string.stringNA))) {
			mGuestStars.setVisibility(View.GONE);
		} else {
			mGuestStars.setText(mEpisode.getGuestStars());
		}

		mPicasso.load(mEpisode.getEpisodePhoto()).placeholder(R.drawable.bg).config(MizuuApplication.getBitmapConfig()).into(mEpisodePhoto, new Callback() {
			@Override
			public void onError() {
				if (!isAdded())
					return;
				int width = getActivity().getResources().getDimensionPixelSize(R.dimen.episode_details_background_overlay_width);
				int height = getActivity().getResources().getDimensionPixelSize(R.dimen.episode_details_background_overlay_height);
				mPicasso.load(mEpisode.getTvShowBackdrop()).placeholder(R.drawable.bg).error(R.drawable.nobackdrop).resize(width, height).config(MizuuApplication.getBitmapConfig()).into(mEpisodePhoto, new Callback() {
					@Override
					public void onError() {
						if (!isAdded())
							return;
						setPanning(false);
					}

					@Override
					public void onSuccess() {
						if (!isAdded())
							return;
						setPanning(true);
					}
				});
			}

			@Override
			public void onSuccess() {
				if (!isAdded())
					return;
				setPanning(true);
			}
		});

		if (!MizLib.isPortrait(getActivity()))
			mPicasso.load(mEpisode.getEpisodePhoto()).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(getActivity().getApplicationContext(), mEpisode.getEpisodePhoto().getAbsolutePath() + "-blur", 4)).config(MizuuApplication.getBitmapConfig()).into(mBackdrop, new Callback() {
				@Override public void onError() {
					mPicasso.load(mEpisode.getTvShowBackdrop()).placeholder(R.drawable.bg).error(R.drawable.nobackdrop).transform(new BlurTransformation(getActivity().getApplicationContext(), mEpisode.getTvShowBackdrop().getAbsolutePath() + "-blur", 4)).config(MizuuApplication.getBitmapConfig()).config(MizuuApplication.getBitmapConfig()).into(mBackdrop, new Callback() {
						@Override
						public void onError() {}

						@Override
						public void onSuccess() {
							if (!isAdded())
								return;
							mBackdrop.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
						}
					});
				}
				
				@Override
				public void onSuccess() {
					if (!isAdded())
						return;
					mBackdrop.setColorFilter(Color.parseColor("#aa181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
				}
			});
	}

	private void setPanning(boolean successful) {
		if (!MizLib.isPortrait(getActivity()))
			return;

		if (successful) {
			((PanningView) mEpisodePhoto).startPanning();
		} else {
			((PanningView) mEpisodePhoto).setScaleType(ScaleType.CENTER_CROP);
		}
	}

	private void play() {
		if (mEpisode.hasOfflineCopy()) {
			playEpisode(mEpisode.getOfflineCopyUri(), false);
		} else {
			playEpisode(mEpisode.getFilepath(), mEpisode.isNetworkFile());
		}
	}

	private void playEpisode(String filepath, boolean isNetworkFile) {
		mVideoPlaybackStarted = System.currentTimeMillis();
		if (isNetworkFile) {
			playNetworkFile();
		} else {
			try { // Attempt to launch intent based on the MIME type
				getActivity().startActivity(MizLib.getVideoIntent(filepath, mVideoWildcard, mEpisode));
				checkIn();
			} catch (Exception e) {
				try { // Attempt to launch intent based on wildcard MIME type
					getActivity().startActivity(MizLib.getVideoIntent(filepath, "video/*", mEpisode));
					checkIn();
				} catch (Exception e2) {
					Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	public void onResume() {
		super.onResume();

		mBus.register(getActivity());

		mVideoPlaybackEnded = System.currentTimeMillis();

		if (mVideoPlaybackStarted > 0 && mVideoPlaybackEnded - mVideoPlaybackStarted > (1000 * 60 * 5)) {
			if (!mEpisode.hasWatched())
				watched(false); // Mark it as watched
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_details, menu);

		try {
			if (mEpisode.hasWatched()) {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsUnwatched);
			} else {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsWatched);
			}

			if (mEpisode.isNetworkFile() || mEpisode.isUpnpFile()) {
				menu.findItem(R.id.watchOffline).setVisible(true);

				if (mEpisode.hasOfflineCopy())
					menu.findItem(R.id.watchOffline).setTitle(R.string.removeOfflineCopy);
				else
					menu.findItem(R.id.watchOffline).setTitle(R.string.watchOffline);
			}
		} catch (Exception e) {}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.play_video:
			play();
			break;
		case R.id.menuDeleteEpisode:
			deleteEpisode();
			break;
		case R.id.watched:
			watched(true);
			break;
		case R.id.identify:
			identifyEpisode();
			break;
		case R.id.watchOffline:
			watchOffline(item);
			break;
		}
		return false;
	}

	public void watchOffline(MenuItem m) {
		if (mEpisode.hasOfflineCopy()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(getString(R.string.areYouSure))
			.setTitle(getString(R.string.removeOfflineCopy))
			.setCancelable(false)
			.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {	
					boolean success = mEpisode.getOfflineCopyFile().delete();
					if (!success)
						mEpisode.getOfflineCopyFile().delete();
					getActivity().invalidateOptionsMenu();
					return;
				}
			})
			.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.create().show();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(getString(R.string.downloadOfflineCopy))
			.setTitle(getString(R.string.watchOffline))
			.setCancelable(false)
			.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (MizLib.isLocalCopyBeingDownloaded(getActivity()))
						Toast.makeText(getActivity(), R.string.addedToDownloadQueue, Toast.LENGTH_SHORT).show();

					Intent i = new Intent(getActivity(), MakeAvailableOffline.class);
					i.putExtra(MakeAvailableOffline.FILEPATH, mEpisode.getFilepath());
					i.putExtra(MakeAvailableOffline.TYPE, MizLib.TYPE_SHOWS);
					i.putExtra("thumb", mEpisode.getThumbnail());
					i.putExtra("backdrop", mEpisode.getEpisodePhoto());
					getActivity().startService(i);
					return;
				}
			})
			.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.create().show();
		}
	}

	private void identifyEpisode() {
		Intent i = new Intent();
		i.setClass(getActivity(), IdentifyTvShow.class);
		i.putExtra("rowId", mEpisode.getRowId());
		i.putExtra("files", new String[]{mEpisode.getFullFilepath()});
		i.putExtra("isShow", false);
		startActivity(i);
	}

	private void deleteEpisode() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		View dialogLayout = getActivity().getLayoutInflater().inflate(R.layout.delete_file_dialog_layout, null);
		final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);
		cb.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ALWAYS_DELETE_FILE, true));

		builder.setTitle(getString(R.string.removeEpisode) + " S" + mEpisode.getSeason() + "E" + mEpisode.getEpisode())
		.setView(dialogLayout)
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				// Create and open database
				mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();
				boolean deleted = false;
				if (mIgnoreDeletedFiles)
					deleted = mDatabaseHelper.ignoreEpisode(mEpisode.getRowId());
				else
					deleted = mDatabaseHelper.deleteEpisode(mEpisode.getRowId());

				if (deleted) {
					try {
						// Delete episode images
						File episodePhoto = MizLib.getTvShowEpisode(getActivity(), mEpisode.getShowId(), mEpisode.getSeason(), mEpisode.getEpisode());
						if (episodePhoto.exists()) {
							MizLib.deleteFile(episodePhoto);
						}
					} catch (NullPointerException e) {} // No file to delete

					if (mDatabaseHelper.getEpisodeCount(mEpisode.getShowId()) == 0) { // No more episodes for this show
						DbAdapterTvShow dbShow = MizuuApplication.getTvDbAdapter();
						boolean deletedShow = dbShow.deleteShow(mEpisode.getShowId());

						if (deletedShow) {
							MizLib.deleteFile(MizLib.getTvShowThumb(getActivity(), mEpisode.getShowId()));
							MizLib.deleteFile(MizLib.getTvShowBackdrop(getActivity(), mEpisode.getShowId()));
						}
					}

					if (cb.isChecked()) {
						Intent deleteIntent = new Intent(getActivity(), DeleteFile.class);
						deleteIntent.putExtra("filepath", mEpisode.getFilepath());
						getActivity().startService(deleteIntent);
					}

					notifyDatasetChanges();
					getActivity().finish();
					return;
				} else {
					Toast.makeText(getActivity(), getString(R.string.failedToRemoveEpisode), Toast.LENGTH_SHORT).show();
				}
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.show();
	}

	private void watched(boolean showToast) {
		// Create and open database
		mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();

		mEpisode.setHasWatched(!mEpisode.hasWatched()); // Reverse the hasWatched boolean

		if (mDatabaseHelper.updateSingleItem(Long.valueOf(mEpisode.getRowId()), DbAdapter.KEY_HAS_WATCHED, mEpisode.getHasWatched())) {
			getActivity().invalidateOptionsMenu();

			if (showToast)
				if (mEpisode.hasWatched()) {
					Toast.makeText(getActivity(), getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(), getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
				}
		} else {
			if (showToast)
				Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
		}

		mBus.post(mEpisode);

		new Thread() {
			@Override
			public void run() {
				ArrayList<com.miz.functions.TvShowEpisode> episode = new ArrayList<com.miz.functions.TvShowEpisode>();
				episode.add(new com.miz.functions.TvShowEpisode(mEpisode.getShowId(), Integer.valueOf(mEpisode.getEpisode()), Integer.valueOf(mEpisode.getSeason())));
				Trakt.markEpisodeAsWatched(mEpisode.getShowId(), episode, getActivity(), false);
			}
		}.start();
	}

	private void notifyDatasetChanges() {
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-shows-update"));
	}

	private void checkIn() {
		new Thread() {
			@Override
			public void run() {
				Trakt.performEpisodeCheckin(mEpisode, getActivity());
			}
		}.start();
	}

	private void playNetworkFile() {
		if (!MizLib.isWifiConnected(getActivity(), mDisableEthernetWiFiCheck)) {
			Toast.makeText(getActivity(), getString(R.string.noConnection), Toast.LENGTH_LONG).show();
			return;
		}

		int bufferSize;
		String buff = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(BUFFER_SIZE, getString(R.string._16kb));
		if (buff.equals(getString(R.string._16kb)))
			bufferSize = 8192 * 2; // This appears to be the limit for most video players
		else bufferSize = 8192;

		final Streamer s = Streamer.getInstance();
		if (s != null)
			s.setBufferSize(bufferSize);
		else {
			Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
			return;
		}

		final NtlmPasswordAuthentication auth = MizLib.getAuthFromFilepath(MizLib.TYPE_SHOWS, mEpisode.getFilepath());

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(auth.getDomain(), "utf-8"),
									URLEncoder.encode(auth.getUsername(), "utf-8"),
									URLEncoder.encode(auth.getPassword(), "utf-8"),
									mEpisode.getFilepath(),
									false
									));

					s.setStreamSrc(file, MizLib.getSubtitleFiles(mEpisode.getFilepath(), auth)); //the second argument can be a list of subtitle files
					getActivity().runOnUiThread(new Runnable(){
						public void run(){
							try{
								Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(mEpisode.getFilepath()).getPath())).getEncodedPath());	
								startActivity(MizLib.getVideoIntent(uri, mVideoWildcard, mEpisode));
								checkIn();
							} catch (Exception e) {
								try { // Attempt to launch intent based on wildcard MIME type
									Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(mEpisode.getFilepath()).getPath())).getEncodedPath());	
									startActivity(MizLib.getVideoIntent(uri, "video/*", mEpisode));
									checkIn();
								} catch (Exception e2) {
									Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
								}
							}
						}
					});
				}
				catch (MalformedURLException e) {}
				catch (UnsupportedEncodingException e1) {}
			}
		}.start();
	}
}