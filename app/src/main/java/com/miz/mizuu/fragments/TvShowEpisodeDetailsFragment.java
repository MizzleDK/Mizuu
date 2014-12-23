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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.BlurTransformation;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteLoader;
import com.miz.functions.SimpleAnimatorListener;
import com.miz.mizuu.EditTvShowEpisode;
import com.miz.mizuu.IdentifyTvShowEpisode;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisode;
import com.miz.remoteplayback.RemotePlayback;
import com.miz.service.DeleteFile;
import com.miz.service.MakeAvailableOffline;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TvShowDatabaseUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import static com.miz.functions.PreferenceKeys.ALWAYS_DELETE_FILE;
import static com.miz.functions.PreferenceKeys.CHROMECAST_BETA_SUPPORT;
import static com.miz.functions.PreferenceKeys.SHOW_FILE_LOCATION;

@SuppressLint("InflateParams") public class TvShowEpisodeDetailsFragment extends Fragment {

    private Activity mContext;
    private TvShowEpisode mEpisode;
    private ImageView mBackdrop, mEpisodePhoto;
    private TextView mTitle, mDescription, mFileSource, mAirDate, mRating, mDirector, mWriter, mGuestStars, mSeasonEpisodeNumber;
    private View mDetailsArea;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mCondensedRegular;
    private DbAdapterTvShowEpisodes mDatabaseHelper;
    private long mVideoPlaybackStarted, mVideoPlaybackEnded;
    private boolean mShowFileLocation;
    private Bus mBus;
    private int mToolbarColor = 0;
    private FloatingActionButton mFab;
    private PaletteLoader mPaletteLoader;
    private ObservableScrollView mScrollView;

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

        mContext = getActivity();

        mBus = MizuuApplication.getBus();

        mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SHOW_FILE_LOCATION, true);

        mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver,
                new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_EPISODE_DETAILS_OVERVIEW));

        loadEpisode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadEpisode();
            loadData();
        }
    };

    private void loadEpisode() {
        if (!getArguments().getString("showId").isEmpty() && getArguments().getInt("season") >= 0 && getArguments().getInt("episode") >= 0) {
            Cursor cursor = mDatabaseHelper.getEpisode(getArguments().getString("showId"), getArguments().getInt("season"), getArguments().getInt("episode"));

            if (cursor.moveToFirst()) {
                mEpisode = new TvShowEpisode(getActivity(),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                );

                mEpisode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                ));
            }
            cursor.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.episode_details, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBackdrop = (ImageView) view.findViewById(R.id.imageBackground);
        mEpisodePhoto = (ImageView) view.findViewById(R.id.episodePhoto);
        mDetailsArea = view.findViewById(R.id.details_area);

        mTitle = (TextView) view.findViewById(R.id.movieTitle);
        mSeasonEpisodeNumber = (TextView) view.findViewById(R.id.textView7);
        mDescription = (TextView) view.findViewById(R.id.textView2);
        mFileSource = (TextView) view.findViewById(R.id.textView3);
        mAirDate = (TextView) view.findViewById(R.id.textReleaseDate);
        mRating = (TextView) view.findViewById(R.id.textView12);
        mDirector = (TextView) view.findViewById(R.id.director);
        mWriter = (TextView) view.findViewById(R.id.writer);
        mGuestStars = (TextView) view.findViewById(R.id.guest_stars);
        mScrollView = (ObservableScrollView) view.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtils.animateFabJump(v, new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        play();
                    }
                });
            }
        });
        if (MizLib.isTablet(mContext))
            mFab.setType(FloatingActionButton.TYPE_NORMAL);

        final int height = MizLib.getActionBarAndStatusBarHeight(getActivity());

        mScrollView = (ObservableScrollView) view.findViewById(R.id.observableScrollView);
        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                final int headerHeight = mEpisodePhoto.getHeight() - height;
                final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
                final int newAlpha = (int) (ratio * 255);

                mBus.post(new BusToolbarColorObject(mToolbarColor, newAlpha));

                if (MizLib.isPortrait(mContext)) {
                    // Such parallax, much wow
                    mEpisodePhoto.setPadding(0, (int) (t / 1.5), 0, 0);
                }
            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, view,
                        mBackdrop, mScrollView, this);
            }
        });

        loadData();

        mPicasso.load(mEpisode.getEpisodePhoto()).placeholder(R.drawable.bg).config(MizuuApplication.getBitmapConfig()).into(mEpisodePhoto, new Callback() {
            @Override
            public void onError() {
                if (!isAdded())
                    return;
                int width = getActivity().getResources().getDimensionPixelSize(R.dimen.episode_details_background_overlay_width);
                int height = getActivity().getResources().getDimensionPixelSize(R.dimen.episode_details_background_overlay_height);
                mPicasso.load(mEpisode.getTvShowBackdrop()).placeholder(R.drawable.bg).error(R.drawable.nobackdrop).resize(width, height).config(MizuuApplication.getBitmapConfig()).into(mEpisodePhoto);
            }

            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.fromFile(mEpisode.getEpisodePhoto()), new PaletteLoader.OnPaletteLoadedCallback() {
                        @Override
                        public void onPaletteLoaded(int swatchColor) {
                            mToolbarColor = swatchColor;
                        }
                    });

                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.setFab(mFab);

                    mPaletteLoader.execute();
                } else {
                    // Clear old views after configuration change
                    mPaletteLoader.clearViews();

                    // Add views after configuration change
                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.setFab(mFab);

                    // Re-color the views
                    mPaletteLoader.colorViews();
                }
            }
        });

        if (!MizLib.isPortrait(getActivity()))
            mPicasso.load(mEpisode.getEpisodePhoto()).placeholder(R.drawable.bg).error(R.drawable.bg).transform(new BlurTransformation(getActivity().getApplicationContext(), mEpisode.getEpisodePhoto().getAbsolutePath() + "-blur", 4)).into(mBackdrop, new Callback() {
                @Override public void onError() {
                    if (!isAdded())
                        return;

                    mPicasso.load(mEpisode.getTvShowBackdrop()).placeholder(R.drawable.bg).error(R.drawable.nobackdrop).transform(new BlurTransformation(getActivity().getApplicationContext(), mEpisode.getTvShowBackdrop().getAbsolutePath() + "-blur", 4)).into(mBackdrop, new Callback() {
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

    private void loadData() {
        // Set the episode title
        mTitle.setVisibility(View.VISIBLE);
        mTitle.setText(mEpisode.getTitle());
        mTitle.setTypeface(mCondensedRegular);

        mDescription.setTypeface(mCondensedRegular);
        mFileSource.setTypeface(mCondensedRegular);
        mDirector.setTypeface(mCondensedRegular);
        mWriter.setTypeface(mCondensedRegular);
        mGuestStars.setTypeface(mCondensedRegular);

        mAirDate.setTypeface(mMedium);
        mRating.setTypeface(mMedium);
        mSeasonEpisodeNumber.setTypeface(mMediumItalic);
        mSeasonEpisodeNumber.setText(getString(R.string.showSeason) + " " + mEpisode.getSeason() + ", " + getString(R.string.showEpisode) + " " + mEpisode.getEpisode());

        // Set the movie plot
        if (!MizLib.isPortrait(getActivity())) {
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
        } else {
            if (MizLib.isTablet(getActivity()))
                mDescription.setLineSpacing(0, 1.15f);
        }
        mDescription.setText(mEpisode.getDescription());

        if (mShowFileLocation) {
            mFileSource.setText(mEpisode.getAllFilepaths());
        } else {
            mFileSource.setVisibility(View.GONE);
        }

        // Set the episode air date
        mAirDate.setText(MizLib.getPrettyDatePrecise(getActivity(), mEpisode.getReleasedate()));

        // Set the movie rating
        if (!mEpisode.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mEpisode.getRating()) * 10);
                mRating.setText(Html.fromHtml(rating + "<small> %</small>"));
            } catch (NumberFormatException e) {
                mRating.setText(mEpisode.getRating());
            }
        } else {
            mRating.setText(R.string.stringNA);
        }

        if (TextUtils.isEmpty(mEpisode.getDirector()) || mEpisode.getDirector().equals(getString(R.string.stringNA))) {
            mDirector.setVisibility(View.GONE);
        } else {
            mDirector.setText(mEpisode.getDirector());
        }

        if (TextUtils.isEmpty(mEpisode.getWriter()) || mEpisode.getWriter().equals(getString(R.string.stringNA))) {
            mWriter.setVisibility(View.GONE);
        } else {
            mWriter.setText(mEpisode.getWriter());
        }

        if (TextUtils.isEmpty(mEpisode.getGuestStars()) || mEpisode.getGuestStars().equals(getString(R.string.stringNA))) {
            mGuestStars.setVisibility(View.GONE);
        } else {
            mGuestStars.setText(mEpisode.getGuestStars());
        }
    }

    private void play() {
        ArrayList<Filepath> paths = mEpisode.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (mEpisode.hasOfflineCopy(path)) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), mEpisode.getOfflineCopyUri(path), FileSource.FILE, mEpisode);
                if (playbackStarted) {
                    mVideoPlaybackStarted = System.currentTimeMillis();
                    checkIn();
                }
            } else {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), mEpisode);
                if (playbackStarted) {
                    mVideoPlaybackStarted = System.currentTimeMillis();
                    checkIn();
                }
            }
        } else {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (mEpisode.hasOfflineCopy(path)) {
                    boolean playbackStarted = VideoUtils.playVideo(getActivity(), mEpisode.getOfflineCopyUri(path), FileSource.FILE, mEpisode);
                    if (playbackStarted) {
                        mVideoPlaybackStarted = System.currentTimeMillis();
                        checkIn();
                    }

                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                MizLib.showSelectFileDialog(getActivity(), mEpisode.getFilepaths(), new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Filepath path = mEpisode.getFilepaths().get(which);
                        boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), mEpisode);
                        if (playbackStarted) {
                            mVideoPlaybackStarted = System.currentTimeMillis();
                            checkIn();
                        }
                    }
                });
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

        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(CHROMECAST_BETA_SUPPORT, false)) {

            boolean add = false;
            for (Filepath path : mEpisode.getFilepaths()) {
                if (path.isNetworkFile()) {
                    add = true;
                    break;
                }
            }

            if (add) {
                menu.add("Remote play").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final ArrayList<Filepath> networkFiles = new ArrayList<Filepath>();

                        for (Filepath path : mEpisode.getFilepaths()) {
                            if (path.isNetworkFile()) {
                                networkFiles.add(path);
                            }
                        }

                        MizLib.showSelectFileDialog(getActivity(), networkFiles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String showName = MizuuApplication.getTvDbAdapter().getShowTitle(mEpisode.getShowId());

                                Intent i = new Intent(getActivity(), RemotePlayback.class);
                                i.putExtra("coverUrl", "");
                                i.putExtra("title", showName + " (S" + MizLib.addIndexZero(mEpisode.getSeason()) + "E" + MizLib.addIndexZero(mEpisode.getEpisode()) + "): " + mEpisode.getTitle());
                                i.putExtra("id", mEpisode.getShowId());
                                i.putExtra("type", "tv");

                                if (networkFiles.get(which).getType() == FileSource.SMB) {
                                    String url = VideoUtils.startSmbServer(getActivity(), networkFiles.get(which).getFilepath(), mEpisode);
                                    i.putExtra("videoUrl", url);
                                } else {
                                    i.putExtra("videoUrl", networkFiles.get(which).getFilepath());
                                }

                                startActivity(i);
                            }
                        });

                        return false;
                    }
                });
            }
        }

        try {
            if (mEpisode.hasWatched()) {
                menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsUnwatched);
            } else {
                menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsWatched);
            }

            for (Filepath path : mEpisode.getFilepaths()) {
                if (path.isNetworkFile()) {

                    // Set the menu item visibility
                    menu.findItem(R.id.watchOffline).setVisible(true);

                    if (mEpisode.hasOfflineCopy(path))
                        // There's already an offline copy, so let's allow the user to remove it
                        menu.findItem(R.id.watchOffline).setTitle(R.string.removeOfflineCopy);
                    else
                        // There's no offline copy, so let the user download one
                        menu.findItem(R.id.watchOffline).setTitle(R.string.watchOffline);

                    break;
                }
            }
        } catch (Exception e) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
                watchOffline();
                break;
            case R.id.editTvShowEpisode:
                editEpisode();
                break;
        }
        return false;
    }

    private void editEpisode() {
        Intent intent = new Intent(getActivity(), EditTvShowEpisode.class);
        intent.putExtra("showId", mEpisode.getShowId());
        intent.putExtra("season", MizLib.getInteger(mEpisode.getSeason()));
        intent.putExtra("episode", MizLib.getInteger(mEpisode.getEpisode()));
        startActivityForResult(intent, 0);
    }

    public void watchOffline() {

        if (mEpisode.getFilepaths().size() == 1) {
            watchOffline(mEpisode.getFilepaths().get(0));
        } else {
            MizLib.showSelectFileDialog(getActivity(), mEpisode.getFilepaths(), new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    watchOffline(mEpisode.getFilepaths().get(which));

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
        }
    }

    private void watchOffline(final Filepath path) {
        if (mEpisode.hasOfflineCopy(path)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.areYouSure))
                    .setTitle(getString(R.string.removeOfflineCopy))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            boolean success = mEpisode.getOfflineCopyFile(path).delete();
                            if (!success)
                                mEpisode.getOfflineCopyFile(path).delete();
                            getActivity().invalidateOptionsMenu();
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
                            i.putExtra(MakeAvailableOffline.FILEPATH, path.getFilepath());
                            i.putExtra(MakeAvailableOffline.TYPE, MizLib.TYPE_SHOWS);
                            i.putExtra("thumb", mEpisode.getThumbnail().getAbsolutePath());
                            i.putExtra("backdrop", mEpisode.getEpisodePhoto().getAbsolutePath());
                            getActivity().startService(i);
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
        if (mEpisode.getFilepaths().size() == 1) {
            getActivity().startActivityForResult(getIdentifyIntent(mEpisode.getFilepaths().get(0).getFullFilepath()), 0);

        } else {
            MizLib.showSelectFileDialog(getActivity(), mEpisode.getFilepaths(), new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().startActivityForResult(getIdentifyIntent(mEpisode.getFilepaths().get(which).getFullFilepath()), 0);

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
        }
    }

    private Intent getIdentifyIntent(String filepath) {
        Intent i = new Intent(getActivity(), IdentifyTvShowEpisode.class);
        ArrayList<String> filepaths = new ArrayList<String>();
        filepaths.add(filepath);
        i.putExtra("filepaths", filepaths);
        i.putExtra("showId", mEpisode.getShowId());
        i.putExtra("showTitle", MizuuApplication.getTvDbAdapter().getShowTitle(mEpisode.getShowId()));
        return i;
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
                        TvShowDatabaseUtils.deleteEpisode(mContext, mEpisode.getShowId(),
                                MizLib.getInteger(mEpisode.getSeason()), MizLib.getInteger(mEpisode.getEpisode()));

                        if (cb.isChecked()) {
                            for (Filepath path : mEpisode.getFilepaths()) {
                                Intent deleteIntent = new Intent(getActivity(), DeleteFile.class);
                                deleteIntent.putExtra("filepath", path.getFilepath());
                                getActivity().startService(deleteIntent);
                            }
                        }

                        if (MizuuApplication.getTvEpisodeDbAdapter().getEpisodeCount(mEpisode.getShowId()) == 0) {
                            // The show has been deleted! Let's show the TV show library overview

                            Intent i = new Intent(mContext, Main.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra("startup", String.valueOf(Main.SHOWS));
                            startActivity(i);
                        } else {
                            LocalBroadcastUtils.updateTvShowSeasonsOverview(mContext);
                            LocalBroadcastUtils.updateTvShowEpisodesOverview(mContext);
                        }

                        notifyDatasetChanges();
                        getActivity().finish();
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

        if (mDatabaseHelper.setEpisodeWatchStatus(mEpisode.getShowId(), mEpisode.getSeason(), mEpisode.getEpisode(), mEpisode.hasWatched())) {
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
        LocalBroadcastUtils.updateTvShowLibrary(getActivity());
    }

    private void checkIn() {
        new Thread() {
            @Override
            public void run() {
                Trakt.performEpisodeCheckin(mEpisode, getActivity());
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                loadEpisode();
                loadData();
            }
        }
    }

    public class BusToolbarColorObject {

        private final int mToolbarColor, mAlpha;

        public BusToolbarColorObject(int toolbarColor, int alpha) {
            mToolbarColor = toolbarColor;
            mAlpha = alpha;
        }

        public int getToolbarColor() {
            return mToolbarColor;
        }

        public int getAlpha() {
            return mAlpha;
        }
    }
}