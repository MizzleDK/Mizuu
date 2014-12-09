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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.miz.apis.tmdb.TMDbTvShowService;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.Actor;
import com.miz.functions.BlurTransformation;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.GridSeason;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteLoader;
import com.miz.functions.PreferenceKeys;
import com.miz.functions.SimpleAnimatorListener;
import com.miz.mizuu.EditTvShow;
import com.miz.mizuu.IdentifyTvShow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.ShowCoverFanartBrowser;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowEpisode;
import com.miz.utils.FileUtils;
import com.miz.utils.IntentUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;

public class TvShowDetailsFragment extends Fragment {

    private Activity mContext;
    private DbAdapterTvShows dbHelper;
    private TvShow thisShow;
    private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textCertification;
    private ImageView background, cover;
    private ObservableScrollView mScrollView;
    private View mDetailsArea;
    private boolean ignorePrefixes;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mBold, mCondensedRegular;
    private Bus mBus;
    private HorizontalCardLayout mSeasonsLayout, mActorsLayout;
    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor = 0;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private PaletteLoader mPaletteLoader;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public TvShowDetailsFragment() {}

    public static TvShowDetailsFragment newInstance(String showId) {
        TvShowDetailsFragment pageFragment = new TvShowDetailsFragment();
        Bundle b = new Bundle();
        b.putString("showId", showId);
        pageFragment.setArguments(b);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mContext = getActivity();

        mBus = MizuuApplication.getBus();

        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(IGNORED_TITLE_PREFIXES, false);

        // Create and open database
        dbHelper = MizuuApplication.getTvDbAdapter();

        Cursor cursor = dbHelper.getShow(getArguments().getString("showId"));
        try {
            if (cursor.moveToFirst()) {
                thisShow = new TvShow(getActivity(),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_GENRES)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ACTORS)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RUNTIME)),
                        ignorePrefixes,
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
                        MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)))
                );
            }
        } catch (Exception e) {
        } finally {
            cursor.close();
        }

        mPicasso = MizuuApplication.getPicassoDetailsView(getActivity());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadImages();
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        mBus.register(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
    }

    public void onViewCreated(final View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This needs to be re-initialized here and not in onCreate()
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mDetailsArea = v.findViewById(R.id.details_area);

        background = (ImageView) v.findViewById(R.id.imageBackground);
        textTitle = (TextView) v.findViewById(R.id.movieTitle);
        textPlot = (TextView) v.findViewById(R.id.textView2);
        textGenre = (TextView) v.findViewById(R.id.textView7);
        textRuntime = (TextView) v.findViewById(R.id.textView9);
        textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
        textRating = (TextView) v.findViewById(R.id.textView12);
        textCertification = (TextView) v.findViewById(R.id.textView11);
        cover = (ImageView) v.findViewById(R.id.traktIcon);
        mSeasonsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout);
        mActorsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra);
        mActorsLayout.setVisibility(View.VISIBLE);
        mScrollView = (ObservableScrollView) v.findViewById(R.id.observableScrollView);
        mFab = (FloatingActionButton) v.findViewById(R.id.fab);

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtils.animateFabJump(v, new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        playFirstEpisode();
                    }
                });
            }
        });
        if (MizLib.isTablet(mContext))
            mFab.setType(FloatingActionButton.TYPE_NORMAL);

        // Get rid of these...
        v.findViewById(R.id.textView3).setVisibility(View.GONE); // File
        v.findViewById(R.id.textView6).setVisibility(View.GONE); // Tagline

        final int height = MizLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                ViewUtils.handleOnScrollChangedEvent(
                        getActivity(), v, background, thisShow.getTitle(),
                        height, t, mToolbar, mToolbarColor);
            }
        });
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, v,
                        background, mScrollView, this);
            }
        });

        // Set the show title
        textTitle.setVisibility(View.VISIBLE);
        textTitle.setText(thisShow.getTitle());
        textTitle.setTypeface(mCondensedRegular);

        textPlot.setTypeface(mCondensedRegular);
        textRuntime.setTypeface(mMedium);
        textRating.setTypeface(mMedium);
        textCertification.setTypeface(mMedium);

        textRuntime.setTypeface(mMedium);
        textCertification.setTypeface(mMedium);
        textRating.setTypeface(mMedium);

        // Set the show plot
        textPlot.setBackgroundResource(R.drawable.selectable_background);
        textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
        textPlot.setTag(true); // true = collapsed
        textPlot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Boolean) textPlot.getTag())) {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(textPlot, 50); // It seems highly unlikely that there would every be more than 50 lines

                    // Reverse the tag
                    textPlot.setTag(false);
                } else {
                    // Animate
                    ViewUtils.animateTextViewMaxLines(textPlot, getResources().getInteger(R.integer.show_details_max_lines));

                    // Reverse the tag
                    textPlot.setTag(true);
                }
            }
        });
        textPlot.setEllipsize(TextUtils.TruncateAt.END);
        textPlot.setFocusable(true);

        if (MizLib.isTablet(getActivity()))
            textPlot.setLineSpacing(0, 1.15f);

        textPlot.setText(thisShow.getDescription());

        // Set the show genres
        textGenre.setTypeface(mMediumItalic);
        if (!TextUtils.isEmpty(thisShow.getGenres())) {
            textGenre.setText(thisShow.getGenres());
        } else {
            textGenre.setVisibility(View.GONE);
        }

        // Set the show runtime
        textRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(thisShow.getRuntime())));

        // Set the show release date
        textReleaseDate.setTypeface(mMedium);
        textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisShow.getFirstAirdate()));

        // Set the show rating
        if (!thisShow.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(thisShow.getRating()) * 10);
                textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
            } catch (NumberFormatException e) {
                textRating.setText(thisShow.getRating());
            }
        } else {
            textRating.setText(R.string.stringNA);
        }

        // Set the show certification
        if (!TextUtils.isEmpty(thisShow.getCertification())) {
            textCertification.setText(thisShow.getCertification());
        } else {
            textCertification.setText(R.string.stringNA);
        }

        mSeasonsLayout.setTitle(R.string.seasons);
        mSeasonsLayout.setSeeMoreVisibility(true);
        mSeasonsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mActorsLayout.getWidth() > 0) {
                            final int numColumns = (int) Math.floor(mActorsLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            mImageThumbSize = (mActorsLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                            loadSeasons(numColumns);
                            MizLib.removeViewTreeObserver(mSeasonsLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mSeasonsLayout.setSeeMoreOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(IntentUtils.getTvShowSeasonsIntent(mContext, thisShow.getTitle(), thisShow.getId(), mToolbarColor));
            }
        });

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
                startActivity(IntentUtils.getActorBrowserTvShows(mContext, thisShow.getTitle(), thisShow.getId(), mToolbarColor));
            }
        });

        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, thisShow.getTitle(), mToolbarColor);

        loadImages();
    }

    private void loadImages() {
        mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.parse(thisShow.getCoverPhoto().toString()), new PaletteLoader.OnPaletteLoadedCallback() {
                        @Override
                        public void onPaletteLoaded(int swatchColor) {
                            mToolbarColor = swatchColor;
	                        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, thisShow.getTitle(), mToolbarColor);
                        }
                    });

                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mActorsLayout.getSeeMoreView());
                    mPaletteLoader.addView(mSeasonsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    mPaletteLoader.execute();
                } else {
                    // Clear old views after configuration change
                    mPaletteLoader.clearViews();

                    // Add views after configuration change
                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mActorsLayout.getSeeMoreView());
                    mPaletteLoader.addView(mSeasonsLayout.getSeeMoreView());
                    mPaletteLoader.setFab(mFab);

                    // Re-color the views
                    mPaletteLoader.colorViews();
                }
            }

            @Override
            public void onError() {
            }
        });

        if (!MizLib.isPortrait(getActivity())) {
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PreferenceKeys.BLUR_BACKDROPS, false)) {
                mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).transform(new BlurTransformation(mContext, thisShow.getBackdrop(), 8)).into(background);
            } else {
                mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
            }
        } else {
            mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
                @Override
                public void onError() {
                    if (!isAdded())
                        return;

                    mPicasso.load(thisShow.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
                }

                @Override
                public void onSuccess() {}
            });
        }
    }

    private void loadActors(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<Actor> mActors;

            @Override
            protected Void doInBackground(Void... params) {
                TMDbTvShowService service = TMDbTvShowService.getInstance(mContext);
                mActors = service.getActors(thisShow.getId());

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mActorsLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mActors, HorizontalCardLayout.ACTORS, mToolbarColor);
            }
        }.execute();
    }

    private void loadSeasons(final int capacity) {
        // Show ProgressBar
        new AsyncTask<Void, Void, Void>() {
            private List<GridSeason> mSeasons = new ArrayList<GridSeason>();

            @Override
            protected Void doInBackground(Void... params) {

                HashMap<String, EpisodeCounter> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(thisShow.getId());

                for (String key : seasons.keySet()) {
                    File temp = FileUtils.getTvShowSeason(mContext, thisShow.getId(), key);
                    mSeasons.add(new GridSeason(mContext, thisShow.getId(), Integer.valueOf(key), seasons.get(key).getEpisodeCount(), seasons.get(key).getWatchedCount(),
                            temp.exists() ? temp :
                                    FileUtils.getTvShowThumb(mContext, thisShow.getId())));
                }

                seasons.clear();

                Collections.sort(mSeasons);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mSeasonsLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, mSeasons, HorizontalCardLayout.SEASONS, mToolbarColor);
                mSeasonsLayout.setSeeMoreVisibility(true);
            }
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tv_show_details, menu);

        // If this is a tablet, we have more room to display icons
        if (MizLib.isTablet(mContext))
            menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        // Favourite
        menu.findItem(R.id.show_fav).setIcon(thisShow.isFavorite() ?
                R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                .setTitle(thisShow.isFavorite() ?
                        R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_fav:
                favAction();
                break;
            case R.id.menuDeleteShow:
                deleteShow();
                break;
            case R.id.change_cover:
                searchCover();
                break;
            case R.id.identify_show:
                identifyShow();
                break;
            case R.id.editTvShow:
                editTvShow();
                break;
            case R.id.openInBrowser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                if (thisShow.getIdType() == TvShow.TMDB) {
                    browserIntent.setData(Uri.parse("https://www.themoviedb.org/tv/" + thisShow.getIdWithoutHack()));
                } else {
                    browserIntent.setData(Uri.parse("http://thetvdb.com/?tab=series&id=" + thisShow.getId()));
                }
                startActivity(browserIntent);
                break;
            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, ((thisShow.getIdType() == TvShow.THETVDB) ? "http://thetvdb.com/?tab=series&id=" : "http://www.themoviedb.org/tv/") + thisShow.getIdWithoutHack());
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void identifyShow() {
        ArrayList<String> files = new ArrayList<String>();

        Cursor cursor = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllFilepaths(thisShow.getId());
        while (cursor.moveToNext())
            files.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));

        cursor.close();

        Intent i = new Intent();
        i.setClass(mContext, IdentifyTvShow.class);
        i.putExtra("showTitle", thisShow.getTitle());
        i.putExtra("showId", thisShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(i, 0);
    }

    private void editTvShow() {
        Intent intent = new Intent(mContext, EditTvShow.class);
        intent.putExtra("showId", thisShow.getId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(intent, 1);
    }

    public void favAction() {
        // Create and open database
        thisShow.setFavorite(!thisShow.isFavorite()); // Reverse the favourite boolean

        if (dbHelper.updateShowSingleItem(thisShow.getId(), DbAdapterTvShows.KEY_SHOW_FAVOURITE, thisShow.getFavorite())) {
            getActivity().invalidateOptionsMenu();

            Toast.makeText(mContext, getString(thisShow.isFavorite() ? R.string.addedToFavs : R.string.removedFromFavs), Toast.LENGTH_SHORT).show();

            LocalBroadcastUtils.updateTvShowLibrary(mContext);

        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread() {
            @Override
            public void run() {
                ArrayList<TvShow> show = new ArrayList<TvShow>();
                show.add(thisShow);
                Trakt.tvShowFavorite(show, getActivity().getApplicationContext());
            }
        }.start();
    }

    private void searchCover() {
        Intent i = new Intent();
        i.setClass(mContext, ShowCoverFanartBrowser.class);
        i.putExtra("id", thisShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivity(i);
    }

    private void deleteShow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(getString(R.string.areYouSure))
                .setTitle(getString(R.string.removeShow))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MizLib.deleteShow(getActivity().getApplicationContext(), thisShow, true);
                        LocalBroadcastUtils.updateTvShowLibrary(getActivity().getApplicationContext());
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

    private void playFirstEpisode() {

        DbAdapterTvShowEpisodes dbAdapter = MizuuApplication.getTvEpisodeDbAdapter();
        Cursor cursor = dbAdapter.getEpisodes(thisShow.getId());
        TvShowEpisode episode = null;

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {

                    // We want to avoid specials
                    if (MizLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON))) > 0) {

                        // Set the initial episode as a fallback if all episodes have been watched
                        if (episode == null) {
                            episode = new TvShowEpisode(getActivity(),
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

                            episode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));
                        }

                        // Check if the episode has been watched - if not, add
                        // it as our episode to watch, and break the while loop
                        if (cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)).equals("0")) {
                            episode = new TvShowEpisode(getActivity(),
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

                            episode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));

                            break;
                        }
                    }

                }
            } catch (Exception e) {} finally {
                cursor.close();
            }

            if (episode != null) {
                play(episode);
                Toast.makeText(mContext, String.format(mContext.getString(R.string.playing_season_episode),
                        thisShow.getTitle(), episode.getSeason(), episode.getEpisode(), episode.getTitle()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, R.string.no_episodes_to_play, Toast.LENGTH_SHORT).show();
            }
        };

    }

    private void play(final TvShowEpisode episode) {
        ArrayList<Filepath> paths = episode.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (episode.hasOfflineCopy(path)) {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode);
                if (playbackStarted) {
                    checkIn(episode);
                }
            } else {
                boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode);
                if (playbackStarted) {
                    checkIn(episode);
                }
            }
        } else {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (episode.hasOfflineCopy(path)) {
                    boolean playbackStarted = VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode);
                    if (playbackStarted) {
                        checkIn(episode);
                    }

                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                MizLib.showSelectFileDialog(getActivity(), episode.getFilepaths(), new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Filepath path = episode.getFilepaths().get(which);
                        boolean playbackStarted = VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode);
                        if (playbackStarted) {
                            checkIn(episode);
                        }
                    }
                });
            }
        }
    }

    private void checkIn(final TvShowEpisode episode) {
        new Thread() {
            @Override
            public void run() {
                Trakt.performEpisodeCheckin(episode, getActivity());
            }
        }.start();
    }
}