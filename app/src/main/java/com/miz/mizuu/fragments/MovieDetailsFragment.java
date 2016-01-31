/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use mContext file except in compliance with the License.
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
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import com.miz.abstractclasses.MovieApiService;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.Actor;
import com.miz.functions.BlurTransformation;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.PaletteLoader;
import com.miz.functions.PreferenceKeys;
import com.miz.functions.SimpleAnimatorListener;
import com.miz.mizuu.EditMovie;
import com.miz.mizuu.IdentifyMovie;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCoverFanartBrowser;
import com.miz.mizuu.R;
import com.miz.remoteplayback.RemotePlayback;
import com.miz.service.DeleteFile;
import com.miz.service.MakeAvailableOffline;
import com.miz.utils.IntentUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.MovieDatabaseUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.ALWAYS_DELETE_FILE;
import static com.miz.functions.PreferenceKeys.CHROMECAST_BETA_SUPPORT;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.SHOW_FILE_LOCATION;

public class MovieDetailsFragment extends Fragment {

    private Activity mContext;
    private Movie mMovie;
    private DbAdapterMovies mDatabase;
    private TextView mTitle, mPlot, mSrc, mGenre, mRuntime, mReleaseDate, mRating, mTagline, mCertification;
    private View mDetailsArea;
    private ObservableScrollView mScrollView;
    private HorizontalCardLayout mActorsLayout;
    private boolean mIgnorePrefixes, mShowFileLocation;
    private ImageView mBackground, mCover;
    private Picasso mPicasso;
    private Typeface mLight, mMediumItalic, mMedium, mBold, mCondensedRegular;
    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor = 0;
    private long mVideoPlaybackStarted, mVideoPlaybackEnded;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private PaletteLoader mPaletteLoader;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public MovieDetailsFragment() {}

    public static MovieDetailsFragment newInstance(String tmdbId) {
        MovieDetailsFragment pageFragment = new MovieDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("tmdbId", tmdbId);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mContext = getActivity();

        mIgnorePrefixes = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(IGNORED_TITLE_PREFIXES, false);
        mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(SHOW_FILE_LOCATION, true);

        mLight = TypefaceUtils.getRobotoLight(mContext);
        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        mPicasso = MizuuApplication.getPicassoDetailsView(mContext);

        mDatabase = MizuuApplication.getMovieAdapter();

        Cursor cursor = mDatabase.fetchMovie(getArguments().getString("tmdbId"));
        try {
            mMovie = new Movie(mContext,
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TITLE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_PLOT)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TAGLINE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_IMDB_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RATING)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RELEASEDATE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_CERTIFICATION)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RUNTIME)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TRAILER)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_GENRES)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_FAVOURITE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_ACTORS)),
                    MizuuApplication.getCollectionsAdapter().getCollection(cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID))),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TO_WATCH)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_HAS_WATCHED)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_DATE_ADDED)),
                    mIgnorePrefixes
            );
        } catch (Exception e) {} finally {
            if (cursor != null) {
                cursor.close();
            } else { // Cursor is null, yikes!
                getActivity().finish();
                return;
            }
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
    }

    @Override
    public void onResume() {
        super.onResume();

        mVideoPlaybackEnded = System.currentTimeMillis();

        if (mVideoPlaybackStarted > 0 && mVideoPlaybackEnded - mVideoPlaybackStarted > (1000 * 60 * 5)) {
            if (!mMovie.hasWatched())
                watched(false); // Mark it as watched
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadImages();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This needs to be re-initialized here and not in onCreate()
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mDetailsArea = view.findViewById(R.id.details_area);
        mBackground = (ImageView) view.findViewById(R.id.imageBackground);
        mTitle = (TextView) view.findViewById(R.id.movieTitle);
        mPlot = (TextView) view.findViewById(R.id.textView2);
        mSrc = (TextView) view.findViewById(R.id.textView3);
        mGenre = (TextView) view.findViewById(R.id.textView7);
        mRuntime = (TextView) view.findViewById(R.id.textView9);
        mReleaseDate = (TextView) view.findViewById(R.id.textReleaseDate);
        mRating = (TextView) view.findViewById(R.id.textView12);
        mTagline = (TextView) view.findViewById(R.id.textView6);
        mCertification = (TextView) view.findViewById(R.id.textView11);
        mCover = (ImageView) view.findViewById(R.id.traktIcon);
        mActorsLayout = (HorizontalCardLayout) view.findViewById(R.id.horizontal_card_layout);
        mScrollView = (ObservableScrollView) view.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtils.animateFabJump(v, new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        playMovie();
                    }
                });
            }
        });
        if (MizLib.isTablet(mContext))
            mFab.setType(FloatingActionButton.TYPE_NORMAL);

        final int height = MizLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                ViewUtils.handleOnScrollChangedEvent(
                        getActivity(), view, mBackground, mMovie.getTitle(),
                        height, t, mToolbar, mToolbarColor);

            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, view,
                        mBackground, mScrollView, this);
            }
        });

        // Set the movie title
        mTitle.setVisibility(View.VISIBLE);
        mTitle.setText(mMovie.getTitle());
        mTitle.setTypeface(mCondensedRegular);

        mPlot.setTypeface(mCondensedRegular);

        mRuntime.setTypeface(mMedium);
        mCertification.setTypeface(mMedium);
        mRating.setTypeface(mMedium);

        // Set the movie plot
        mPlot.setBackgroundResource(R.drawable.selectable_background);
        if (!mMovie.getTagline().isEmpty())
            mPlot.setMaxLines(mContext.getResources().getInteger(R.integer.movie_details_max_lines));
        else
            mPlot.setMaxLines(mContext.getResources().getInteger(R.integer.show_details_max_lines));
        mPlot.setTag(true); // true = collapsed
        mPlot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Boolean) mPlot.getTag())) {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(mPlot, 50); // It seems highly unlikely that there would every be more than 50 lines

                    // Reverse the tag
                    mPlot.setTag(false);
                } else {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(mPlot, mMovie.getTagline().isEmpty() ?
                            getResources().getInteger(R.integer.show_details_max_lines) : getResources().getInteger(R.integer.movie_details_max_lines));

                    // Reverse the tag
                    mPlot.setTag(true);
                }
            }
        });
        mPlot.setEllipsize(TextUtils.TruncateAt.END);
        mPlot.setFocusable(true);
        if (MizLib.isTablet(mContext))
            mPlot.setLineSpacing(0, 1.15f);
        mPlot.setText(mMovie.getPlot());

        // Set the movie file source
        mSrc.setTypeface(mCondensedRegular);
        if (mShowFileLocation) {
            mSrc.setText(mMovie.getAllFilepaths());
        } else {
            mSrc.setVisibility(View.GONE);
        }

        // Set movie tag line
        mTagline.setTypeface(mBold);
        if (mMovie.getTagline().isEmpty())
            mTagline.setVisibility(TextView.GONE);
        else
            mTagline.setText(mMovie.getTagline());

        // Set the movie genre
        mGenre.setTypeface(mMediumItalic);
        if (!TextUtils.isEmpty(mMovie.getGenres())) {
            mGenre.setText(mMovie.getGenres());
        } else {
            mGenre.setVisibility(View.GONE);
        }

        // Set the movie runtime
        mRuntime.setText(MizLib.getPrettyRuntime(mContext, Integer.parseInt(mMovie.getRuntime())));

        // Set the movie release date
        mReleaseDate.setTypeface(mMedium);
        mReleaseDate.setText(MizLib.getPrettyDate(mContext, mMovie.getReleasedate()));

        // Set the movie rating
        if (!mMovie.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mMovie.getRating()) * 10);
                mRating.setText(Html.fromHtml(+ rating + "<small> %</small>"));
            } catch (NumberFormatException e) {
                mRating.setText(mMovie.getRating());
            }
        } else {
            mRating.setText(R.string.stringNA);
        }

        // Set the movie certification
        if (!TextUtils.isEmpty(mMovie.getCertification())) {
            mCertification.setText(mMovie.getCertification());
        } else {
            mCertification.setText(R.string.stringNA);
        }

        mActorsLayout.setTitle(R.string.detailsActors);
        mActorsLayout.setSeeMoreVisibility(true);
        mActorsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mActorsLayout.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mActorsLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mActorsLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                            loadActors(numColumns);
                            MizLib.removeViewTreeObserver(mActorsLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mActorsLayout.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getActorBrowserMovies(mContext, mMovie.getTitle(), mMovie.getTmdbId(), mToolbarColor));
            }
        });

        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mMovie.getTitle(), mToolbarColor);

        loadImages();
    }

    private void loadActors(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<Actor> mActors;

            @Override
            protected Void doInBackground(Void... params) {
                MovieApiService service = MizuuApplication.getMovieService(mContext);
                mActors = service.getActors(mMovie.getTmdbId());

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mActorsLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
            }
        }.execute();
    }

    private void loadImages() {
        // Cover image
        mPicasso.load(mMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(mCover, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.fromFile(mMovie.getThumbnail()), new PaletteLoader.OnPaletteLoadedCallback() {
                        @Override
                        public void onPaletteLoaded(int swatchColor) {
                            mToolbarColor = swatchColor;
                        }
                    });

                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mActorsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    mPaletteLoader.execute();
                } else {
                    // Clear old views after configuration change
                    mPaletteLoader.clearViews();

                    // Add views after configuration change
                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mActorsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    // Re-color the views
                    mPaletteLoader.colorViews();
                }
            }

            @Override
            public void onError() {}
        });

        if (!MizLib.isPortrait(mContext)) {
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PreferenceKeys.BLUR_BACKDROPS, false)) {
                mPicasso.load(mMovie.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).transform(new BlurTransformation(mContext, mMovie.getBackdrop().getAbsolutePath(), 8)).into(mBackground);
            } else {
                mPicasso.load(mMovie.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(mBackground);
            }
        } else {
            mPicasso.load(mMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(mBackground, new Callback() {
                @Override
                public void onError() {
                    if (!isAdded())
                        return;
                    mPicasso.load(mMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(mBackground);
                }

                @Override
                public void onSuccess() {}
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.movie_details, menu);

        // If this is a tablet, we have more room to display icons
        if (MizLib.isTablet(mContext)) {
            menu.findItem(R.id.movie_fav).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.watch_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(CHROMECAST_BETA_SUPPORT, false)) {

            boolean add = false;
            for (Filepath path : mMovie.getFilepaths()) {
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

                        for (Filepath path : mMovie.getFilepaths()) {
                            if (path.isNetworkFile()) {
                                networkFiles.add(path);
                            }
                        }

                        MizLib.showSelectFileDialog(mContext, networkFiles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent i = new Intent(mContext, RemotePlayback.class);
                                i.putExtra("coverUrl", "");
                                i.putExtra("title", mMovie.getTitle());
                                i.putExtra("id", mMovie.getTmdbId());
                                i.putExtra("type", "movie");

                                if (networkFiles.get(which).getType() == FileSource.SMB) {
                                    String url = VideoUtils.startSmbServer(getActivity(), networkFiles.get(which).getFilepath(), mMovie);
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

        // Favourite
        menu.findItem(R.id.movie_fav).setIcon(mMovie.isFavourite() ?
                R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                .setTitle(mMovie.isFavourite() ?
                        R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);

        // Watchlist
        menu.findItem(R.id.watch_list).setIcon(mMovie.toWatch() ?
                R.drawable.ic_video_collection_white_24dp : R.drawable.ic_queue_white_24dp)
                .setTitle(mMovie.toWatch() ?
                        R.string.removeFromWatchlist : R.string.watchLater);

        // Watched / unwatched
        menu.findItem(R.id.watched).setTitle(mMovie.hasWatched() ?
                R.string.stringMarkAsUnwatched : R.string.stringMarkAsWatched);

        // Only allow the user to browse artwork if it's a valid TMDb movie
        menu.findItem(R.id.change_cover).setVisible(MizLib.isValidTmdbId(mMovie.getTmdbId()));

        // Go through filepaths and find a network file for offline watching
        for (Filepath path : mMovie.getFilepaths()) {
            if (path.isNetworkFile()) {
                menu.findItem(R.id.watchOffline)
                        .setVisible(true)
                        .setTitle(mMovie.hasOfflineCopy(path) ?
                                R.string.removeOfflineCopy : R.string.watchOffline);

                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getActivity().getIntent().getExtras().getBoolean("isFromWidget")) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("startup", String.valueOf(Main.MOVIES));
                    i.setClass(mContext, Main.class);
                    startActivity(i);
                }

                getActivity().finish();
                return true;
            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "http://www.imdb.com/title/" + mMovie.getImdbId());
                startActivity(intent);
                return true;
            case R.id.imdb:
                Intent imdbIntent = new Intent(Intent.ACTION_VIEW);
                imdbIntent.setData(Uri.parse("http://www.imdb.com/title/" + mMovie.getImdbId()));
                startActivity(imdbIntent);
                return true;
            case R.id.tmdb:
                Intent tmdbIntent = new Intent(Intent.ACTION_VIEW);
                tmdbIntent.setData(Uri.parse("http://www.themoviedb.org/movie/" + mMovie.getTmdbId()));
                startActivity(tmdbIntent);
                return true;
            case R.id.watchOffline:
                watchOffline();
                return true;
            case R.id.change_cover:
                searchCover();
                return true;
            case R.id.editMovie:
                editMovie();
                return true;
            case R.id.identify:
                identifyMovie();
                return true;
            case R.id.watched:
                watched(true);
                return true;
            case R.id.trailer:
                VideoUtils.playTrailer(getActivity(), mMovie);
                return true;
            case R.id.watch_list:
                watchList();
                return true;
            case R.id.movie_fav:
                favAction();
                return true;
            case R.id.delete_movie:
                deleteMovie();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("InflateParams")
    public void deleteMovie() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        View dialogLayout = LayoutInflater.from(mContext).inflate(R.layout.delete_file_dialog_layout, null);
        final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);


        if (mMovie.getFilepaths().size() == 1 && mMovie.getFilepaths().get(0).getType() == FileSource.UPNP)
            cb.setEnabled(false);
        else
            cb.setChecked(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ALWAYS_DELETE_FILE, false));

        builder.setTitle(getString(R.string.removeMovie))
                .setView(dialogLayout)
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        MovieDatabaseUtils.deleteMovie(getActivity(), mMovie.getTmdbId());

                        if (cb.isChecked()) {
                            for (Filepath path : mMovie.getFilepaths()) {
                                Intent deleteIntent = new Intent(mContext, DeleteFile.class);
                                deleteIntent.putExtra("filepath", path.getFilepath());
                                mContext.startService(deleteIntent);
                            }
                        }

                        boolean movieExists = mDatabase.movieExists(mMovie.getTmdbId());

                        // We only want to delete movie images, if there are no other versions of the same movie
                        if (!movieExists) {
                            try { // Delete cover art image
                                File coverArt = mMovie.getPoster();
                                if (coverArt.exists() && coverArt.getAbsolutePath().contains("com.miz.mizuu")) {
                                    MizLib.deleteFile(coverArt);
                                }
                            } catch (NullPointerException e) {} // No file to delete

                            try { // Delete thumbnail image
                                File thumbnail = mMovie.getThumbnail();
                                if (thumbnail.exists() && thumbnail.getAbsolutePath().contains("com.miz.mizuu")) {
                                    MizLib.deleteFile(thumbnail);
                                }
                            } catch (NullPointerException e) {} // No file to delete

                            try { // Delete backdrop image
                                File backdrop = mMovie.getBackdrop();
                                if (backdrop.exists() && backdrop.getAbsolutePath().contains("com.miz.mizuu")) {
                                    MizLib.deleteFile(backdrop);
                                }
                            } catch (NullPointerException e) {} // No file to delete
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
                .create().show();
    }

    public void identifyMovie() {
        if (mMovie.getFilepaths().size() == 1) {
            getActivity().startActivityForResult(getIdentifyIntent(mMovie.getFilepaths().get(0).getFullFilepath()), 0);
        } else {
            MizLib.showSelectFileDialog(mContext, mMovie.getFilepaths(), new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(getIdentifyIntent(mMovie.getFilepaths().get(which).getFullFilepath()));

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
        }
    }

    private Intent getIdentifyIntent(String filepath) {
        Intent intent = new Intent(mContext, IdentifyMovie.class);
        intent.putExtra("fileName", filepath);
        intent.putExtra("currentMovieId", mMovie.getTmdbId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        return intent;
    }

    public void favAction() {
        mMovie.setFavourite(!mMovie.isFavourite()); // Reverse the favourite boolean

        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_FAVOURITE, mMovie.getFavourite());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (mMovie.isFavourite()) {
                Toast.makeText(mContext, getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
                getActivity().setResult(2); // Favorite removed
            }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> movie = new ArrayList<Movie>();
                movie.add(mMovie);
                Trakt.movieFavorite(movie, mContext);
            }
        }.start();
    }

    private void watched(boolean showToast) {
        mMovie.setHasWatched(!mMovie.hasWatched()); // Reverse the hasWatched boolean

        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_HAS_WATCHED, mMovie.getHasWatched());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (showToast)
                if (mMovie.hasWatched()) {
                    Toast.makeText(mContext, getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
                }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        // Remove from watchlist when watched
        removeFromWatchlist();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
                watchedMovies.add(mMovie);
                Trakt.markMovieAsWatched(watchedMovies, mContext);
            }
        }.start();
    }

    public void watchList() {
        mMovie.setToWatch(!mMovie.toWatch()); // Reverse the toWatch boolean

        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_TO_WATCH, mMovie.getToWatch());

        if (success) {
            getActivity().invalidateOptionsMenu();

            if (mMovie.toWatch()) {
                Toast.makeText(mContext, getString(R.string.addedToWatchList), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, getString(R.string.removedFromWatchList), Toast.LENGTH_SHORT).show();
            }

            notifyDatasetChanges();

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> watchlist = new ArrayList<Movie>();
                watchlist.add(mMovie);
                Trakt.movieWatchlist(watchlist, mContext);
            }
        }.start();
    }

    public void removeFromWatchlist() {
        mMovie.setToWatch(false); // Remove it

        boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_TO_WATCH, mMovie.getToWatch());

        if (success) {
            getActivity().invalidateOptionsMenu();
            notifyDatasetChanges();
        }

        new Thread() {
            @Override
            public void run() {
                ArrayList<Movie> watchlist = new ArrayList<Movie>();
                watchlist.add(mMovie);
                Trakt.movieWatchlist(watchlist, mContext);
            }
        }.start();
    }

    public void watchOffline() {
        if (mMovie.getFilepaths().size() == 1) {
            watchOffline(mMovie.getFilepaths().get(0));
        } else {
            MizLib.showSelectFileDialog(mContext, mMovie.getFilepaths(), new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    watchOffline(mMovie.getFilepaths().get(which));

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
        }
    }

    public void watchOffline(final Filepath path) {
        if (mMovie.hasOfflineCopy(path)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(getString(R.string.areYouSure))
                    .setTitle(getString(R.string.removeOfflineCopy))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            boolean success = mMovie.getOfflineCopyFile(path).delete();
                            if (!success)
                                mMovie.getOfflineCopyFile(path).delete();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(getString(R.string.downloadOfflineCopy))
                    .setTitle(getString(R.string.watchOffline))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (MizLib.isLocalCopyBeingDownloaded(mContext))
                                Toast.makeText(mContext, R.string.addedToDownloadQueue, Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(mContext, MakeAvailableOffline.class);
                            i.putExtra(MakeAvailableOffline.FILEPATH, path.getFilepath());
                            i.putExtra(MakeAvailableOffline.TYPE, MizLib.TYPE_MOVIE);
                            i.putExtra("thumb", mMovie.getThumbnail().getAbsolutePath());
                            i.putExtra("backdrop", mMovie.getBackdrop().getAbsolutePath());
                            mContext.startService(i);
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

    public void searchCover() {
        if (MizLib.isOnline(mContext)) { // Make sure that the device is connected to the web
            Intent intent = new Intent(mContext, MovieCoverFanartBrowser.class);
            intent.putExtra("tmdbId", mMovie.getTmdbId());
            intent.putExtra("collectionId", mMovie.getCollectionId());
            intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
            startActivity(intent); // Start the intent for result
        } else {
            // No movie ID / Internet connection
            Toast.makeText(mContext, getString(R.string.coverSearchFailed), Toast.LENGTH_LONG).show();
        }
    }

    private void notifyDatasetChanges() {
        LocalBroadcastUtils.updateMovieLibrary(mContext);
    }

    private void checkIn() {
        new Thread() {
            @Override
            public void run() {
                Trakt.performMovieCheckin(mMovie, getActivity());
            }
        }.start();
    }

    private void playMovie() {
        ArrayList<Filepath> paths = mMovie.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (mMovie.hasOfflineCopy(path)) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), mMovie.getOfflineCopyUri(path), FileSource.FILE, mMovie);
                if (playbackStarted) {
                    mVideoPlaybackStarted = System.currentTimeMillis();
                    checkIn();
                }
            } else {
                playMovie(paths.get(0).getFilepath(), paths.get(0).getType());
            }
        } else {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (mMovie.hasOfflineCopy(path)) {
                    boolean playbackStarted = VideoUtils.playVideo(getActivity(), mMovie.getOfflineCopyUri(path), FileSource.FILE, mMovie);
                    if (playbackStarted) {
                        mVideoPlaybackStarted = System.currentTimeMillis();
                        checkIn();
                    }

                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                MizLib.showSelectFileDialog(mContext, mMovie.getFilepaths(), new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Filepath path = mMovie.getFilepaths().get(which);
                        playMovie(path.getFilepath(), path.getType());
                    }
                });
            }
        }
    }

    private void playMovie(String filepath, int filetype) {
        if (filepath.toLowerCase(Locale.getDefault()).matches(".*(cd1|part1).*")) {
            new GetSplitFiles(filepath, filetype).execute();
        } else {
            mVideoPlaybackStarted = System.currentTimeMillis();
            boolean playbackStarted = VideoUtils.playVideo(getActivity(), filepath, filetype, mMovie);
            if (playbackStarted)
                checkIn();
        }
    }

    public void editMovie() {
        Intent intent = new Intent(mContext, EditMovie.class);
        intent.putExtra("movieId", mMovie.getTmdbId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        getActivity().startActivityForResult(intent, 1);
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playMovie();
        }
    }

    private class GetSplitFiles extends AsyncTask<String, Void, List<SplitFile>> {

        private ProgressDialog progress;
        private String orig_filepath;
        private int fileType;

        public GetSplitFiles(String filepath, int filetype) {
            this.orig_filepath = filepath;
            fileType = filetype;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setIndeterminate(true);
            progress.setTitle(getString(R.string.loading_movie_parts));
            progress.setMessage(getString(R.string.few_moments));
            progress.show();
        }

        @Override
        protected List<SplitFile> doInBackground(String... params) {
            List<SplitFile> parts = new ArrayList<SplitFile>();
            List<String> temp;

            try {
                if (fileType == FileSource.SMB)
                    temp = MizLib.getSplitParts(orig_filepath, MizLib.getLoginFromFilepath(MizLib.TYPE_MOVIE, orig_filepath));
                else
                    temp = MizLib.getSplitParts(orig_filepath, null);

                for (int i = 0; i < temp.size(); i++)
                    parts.add(new SplitFile(temp.get(i)));

            } catch (Exception e) {}

            return parts;
        }

        @Override
        protected void onPostExecute(final List<SplitFile> result) {
            progress.dismiss();

            if (result.size() > 0)
                mVideoPlaybackStarted = System.currentTimeMillis();

            if (result.size() > 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(getString(R.string.playPart));
                builder.setAdapter(new SplitAdapter(mContext, result), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        boolean playbackStarted = VideoUtils.playVideo(getActivity(), result.get(which).getFilepath(), fileType, mMovie);
                        if (playbackStarted)
                            checkIn();
                    }});
                builder.show();
            } else if (result.size() == 1) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), result.get(0).getFilepath(), fileType, mMovie);
                if (playbackStarted)
                    checkIn();
            } else {
                Toast.makeText(mContext, getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SplitAdapter implements android.widget.ListAdapter {

        private List<SplitFile> mFiles;
        private Context mContext;
        private LayoutInflater inflater;

        public SplitAdapter(Context context, List<SplitFile> files) {
            mContext = context;
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFiles = files;
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = inflater.inflate(R.layout.split_file_item, parent, false);

            // Don't care about the ViewHolder pattern here
            ((TextView) convertView.findViewById(R.id.title)).setText(getString(R.string.part) + " " + mFiles.get(position).getPartNumber());
            ((TextView) convertView.findViewById(R.id.description)).setText(mFiles.get(position).getUserFilepath());

            return convertView;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return mFiles.isEmpty();
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {}

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {}

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

    }

    private class SplitFile {

        String filepath;

        public SplitFile(String filepath) {
            this.filepath = filepath;
        }

        public String getFilepath() {
            return filepath;
        }

        public String getUserFilepath() {
            return MizLib.transformSmbPath(filepath);
        }

        public int getPartNumber() {
            return MizLib.getPartNumberFromFilepath(getUserFilepath());
        }
    }
}