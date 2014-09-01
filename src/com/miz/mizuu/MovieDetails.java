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

package com.miz.mizuu;

import static com.miz.functions.PreferenceKeys.ALWAYS_DELETE_FILE;
import static com.miz.functions.PreferenceKeys.IGNORED_FILES_ENABLED;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.REMOVE_MOVIES_FROM_WATCHLIST;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.ActionBarSpinner;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.fragments.ActorBrowserFragment;
import com.miz.mizuu.fragments.MovieDetailsFragment;
import com.miz.remoteplayback.RemotePlayback;
import com.miz.service.DeleteFile;
import com.miz.service.MakeAvailableOffline;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.WidgetUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

@SuppressLint("InflateParams")
public class MovieDetails extends MizActivity implements OnNavigationListener {

	private ViewPager mViewPager;
	private Movie mMovie;
	private DbAdapterMovies mDatabase;
	private boolean mIgnorePrefixes, mRemoveMoviesFromWatchlist, mIgnoreDeletedFiles;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdater;
	private ActionBar mActionBar;
	private String mMovieId, mYouTubeApiKey;
	private long mVideoPlaybackStarted, mVideoPlaybackEnded;
	private Context mContext;
	private Bus mBus;
	private Drawable mActionBarBackgroundDrawable;
	private ImageView mActionBarOverlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mBus = MizuuApplication.getBus();

		if (isFullscreen())
			setTheme(R.style.Mizuu_Theme_Transparent_NoBackGround_FullScreen);
		else
			setTheme(R.style.Mizuu_Theme_NoBackGround_Transparent);

		if (MizLib.isPortrait(this)) {
			getWindow().setBackgroundDrawableResource(R.drawable.bg);
		}

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.viewpager);

		mActionBarOverlay = (ImageView) findViewById(R.id.actionbar_overlay);
		mActionBarOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, isFullscreen() ? MizLib.getActionBarHeight(this) : MizLib.getActionBarAndStatusBarHeight(this)));

		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdater == null)
			mSpinnerAdater = new ActionBarSpinner(this, mSpinnerItems);

		setTitle(null);

		mIgnorePrefixes = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IGNORED_TITLE_PREFIXES, false);
		mRemoveMoviesFromWatchlist = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(REMOVE_MOVIES_FROM_WATCHLIST, true);
		mIgnoreDeletedFiles = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IGNORED_FILES_ENABLED, false);

		mYouTubeApiKey = MizLib.getYouTubeApiKey(this);

		// Fetch the database ID of the movie to view
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			mMovieId = getIntent().getStringExtra(SearchManager.EXTRA_DATA_KEY);
		} else {
			mMovieId = getIntent().getExtras().getString("tmdbId");
		}

		// Set up database and open it
		mDatabase = MizuuApplication.getMovieAdapter();

		Cursor cursor = mDatabase.fetchMovie(mMovieId);
		try {
			mMovie = new Movie(this,
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
		} catch (Exception e) {
			System.out.println("EXCEPTION: " + e);
			finish();
			return;
		} finally {
			cursor.close();
		}

		if (mMovie != null) {
			mViewPager = (ViewPager) findViewById(R.id.awesomepager);
			mViewPager.setAdapter(new MovieDetailsAdapter(getSupportFragmentManager()));
			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
				@Override
				public void onPageSelected(int position) {
					mActionBar.setSelectedNavigationItem(position);

					updateActionBarDrawable(1, false, false);
				}
			});

			if (savedInstanceState != null) {
				mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
			}

			setupSpinnerItems();
		} else {
			Toast.makeText(this, getString(R.string.errorSomethingWentWrong) + " (movie ID: " + mMovieId + ")", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
	}

	@Subscribe
	public void onScrollChanged(Integer newAlpha) {
		updateActionBarDrawable(newAlpha, true, false);
	}

	private void updateActionBarDrawable(int newAlpha, boolean setBackground, boolean showActionBar) {
		if (mViewPager.getCurrentItem() == 0) { // Details page
			mActionBarOverlay.setVisibility(View.VISIBLE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				if (newAlpha == 0) {
					mActionBar.hide();
					mActionBarOverlay.setVisibility(View.GONE);
				} else
					mActionBar.show();

			if (setBackground) {
				mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
				mActionBarOverlay.setImageDrawable(mActionBarBackgroundDrawable);
			}
		} else { // Actors page
			mActionBarOverlay.setVisibility(View.GONE);

			if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
				mActionBar.show();
		}

		if (showActionBar) {
			mActionBar.show();
		}
	}

	public void onResume() {
		super.onResume();

		mBus.register(this);
		updateActionBarDrawable(1, true, true);

		mVideoPlaybackEnded = System.currentTimeMillis();

		if (mVideoPlaybackStarted > 0 && mVideoPlaybackEnded - mVideoPlaybackStarted > (1000 * 60 * 5)) {
			if (!mMovie.hasWatched())
				watched(false); // Mark it as watched
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}

	private void setupSpinnerItems() {
		mSpinnerItems.clear();
		mSpinnerItems.add(new SpinnerItem(mMovie.getTitle(), getString(R.string.overview)));
		mSpinnerItems.add(new SpinnerItem(mMovie.getTitle(), getString(R.string.detailsActors)));

		mActionBar.setListNavigationCallbacks(mSpinnerAdater, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.movie_details, menu);

		if (MizLib.isTablet(mContext)) {
			menu.findItem(R.id.movie_fav).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.watch_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}

		try {
			if (mMovie.isFavourite()) {
				menu.findItem(R.id.movie_fav).setIcon(R.drawable.fav);
				menu.findItem(R.id.movie_fav).setTitle(R.string.menuFavouriteTitleRemove);
			} else {
				menu.findItem(R.id.movie_fav).setIcon(R.drawable.ic_action_star_0);
				menu.findItem(R.id.movie_fav).setTitle(R.string.menuFavouriteTitle);
			}

			if (mMovie.toWatch()) {
				menu.findItem(R.id.watch_list).setIcon(R.drawable.watchlist_remove);
				menu.findItem(R.id.watch_list).setTitle(R.string.removeFromWatchlist);
			} else {
				menu.findItem(R.id.watch_list).setIcon(R.drawable.watchlist_add);
				menu.findItem(R.id.watch_list).setTitle(R.string.watchLater);
			}

			if (mMovie.hasWatched()) {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsUnwatched);
			} else {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsWatched);
			}

			for (Filepath path : mMovie.getFilepaths()) {
				if (path.isNetworkFile()) {

					// Set the menu item visibility
					menu.findItem(R.id.watchOffline).setVisible(true);

					if (mMovie.hasOfflineCopy(path))
						// There's already an offline copy, so let's allow the user to remove it
						menu.findItem(R.id.watchOffline).setTitle(R.string.removeOfflineCopy);
					else
						// There's no offline copy, so let the user download one
						menu.findItem(R.id.watchOffline).setTitle(R.string.watchOffline);

					break;
				}
			}
			
			if (!MizLib.isValidTmdbId(mMovie.getTmdbId()))
				menu.findItem(R.id.change_cover).setVisible(false);

		} catch (Exception e) {} // This can happen if mMovie is null for whatever reason

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (getIntent().getExtras().getBoolean("isFromWidget")) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				i.putExtra("startup", String.valueOf(Main.MOVIES));
				i.setClass(getApplicationContext(), Main.class);
				startActivity(i);
			}

			finish();
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
		case R.id.play_video:
			playMovie();
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
			watchTrailer();
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

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	public void deleteMovie() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View dialogLayout = getLayoutInflater().inflate(R.layout.delete_file_dialog_layout, null);
		final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);
		
		
		if (mMovie.getFilepaths().size() == 1 && mMovie.getFilepaths().get(0).getType() == FileSource.UPNP)
			cb.setEnabled(false);
		else
			cb.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ALWAYS_DELETE_FILE, false));

		builder.setTitle(getString(R.string.removeMovie))
		.setView(dialogLayout)
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				boolean deleted = true;
				if (mIgnoreDeletedFiles) {
					deleted = mDatabase.ignoreMovie(mMovie.getTmdbId());
				} else {
					deleted = mDatabase.deleteMovie(mMovie.getTmdbId());
				}

				if (deleted) {
					if (cb.isChecked()) {
						for (Filepath path : mMovie.getFilepaths()) {
							Intent deleteIntent = new Intent(getApplicationContext(), DeleteFile.class);
							deleteIntent.putExtra("filepath", path.getFilepath());
							getApplicationContext().startService(deleteIntent);
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
					finish();
					return;
				} else {
					Toast.makeText(getApplicationContext(), getString(R.string.failedToRemoveMovie), Toast.LENGTH_SHORT).show();
				}
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
			startActivityForResult(getIdentifyIntent(mMovie.getFilepaths().get(0).getFullFilepath()), 0);
		} else {
			MizLib.showSelectFileDialog(MovieDetails.this, mMovie.getFilepaths(), new Dialog.OnClickListener() {
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
		Intent intent = new Intent(MovieDetails.this, IdentifyMovie.class);
		intent.putExtra("fileName", filepath);
		return intent;
	}

	public void shareMovie(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, "http://www.imdb.com/title/" + mMovie.getImdbId());
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public void favAction() {
		mMovie.setFavourite(!mMovie.isFavourite()); // Reverse the favourite boolean

		boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_FAVOURITE, mMovie.getFavourite());

		if (success) {
			invalidateOptionsMenu();

			if (mMovie.isFavourite()) {
				Toast.makeText(this, getString(R.string.addedToFavs), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getString(R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
				setResult(2); // Favorite removed
			}

			notifyDatasetChanges();

		} else Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> movie = new ArrayList<Movie>();
				movie.add(mMovie);
				Trakt.movieFavorite(movie, getApplicationContext());
			}
		}.start();
	}

	private void watched(boolean showToast) {
		mMovie.setHasWatched(!mMovie.hasWatched()); // Reverse the hasWatched boolean

		boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_HAS_WATCHED, mMovie.getHasWatched());

		if (success) {
			invalidateOptionsMenu();

			if (showToast)
				if (mMovie.hasWatched()) {
					Toast.makeText(this, getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
				}

			notifyDatasetChanges();

		} else Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

		if (mRemoveMoviesFromWatchlist)
			removeFromWatchlist();

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
				watchedMovies.add(mMovie);
				Trakt.markMovieAsWatched(watchedMovies, getApplicationContext());
			}
		}.start();
	}

	public void watchList() {
		mMovie.setToWatch(!mMovie.toWatch()); // Reverse the toWatch boolean

		boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_TO_WATCH, mMovie.getToWatch());

		if (success) {
			invalidateOptionsMenu();

			if (mMovie.toWatch()) {
				Toast.makeText(this, getString(R.string.addedToWatchList), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getString(R.string.removedFromWatchList), Toast.LENGTH_SHORT).show();
			}

			notifyDatasetChanges();

		} else Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> watchlist = new ArrayList<Movie>();
				watchlist.add(mMovie);
				Trakt.movieWatchlist(watchlist, getApplicationContext());
			}
		}.start();
	}

	public void removeFromWatchlist() {
		mMovie.setToWatch(false); // Remove it

		boolean success = mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_TO_WATCH, mMovie.getToWatch());

		if (success) {
			invalidateOptionsMenu();
			notifyDatasetChanges();
		}

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> watchlist = new ArrayList<Movie>();
				watchlist.add(mMovie);
				Trakt.movieWatchlist(watchlist, getApplicationContext());
			}
		}.start();
	}

	public void watchTrailer() {
		
		String localTrailer = "";
		for (Filepath path : mMovie.getFilepaths()) {
			if (path.getType() == FileSource.FILE) {
				localTrailer = path.getFullFilepath();
				break;
			}
		}
		
		localTrailer = mMovie.getLocalTrailer(localTrailer);
		
		if (!TextUtils.isEmpty(localTrailer)) {
			try { // Attempt to launch intent based on the MIME type
				startActivity(MizLib.getVideoIntent(localTrailer, false, mMovie.getTitle() + " " + getString(R.string.detailsTrailer)));
			} catch (Exception e) {
				try { // Attempt to launch intent based on wildcard MIME type
					startActivity(MizLib.getVideoIntent(localTrailer, "video/*", mMovie.getTitle() + " " + getString(R.string.detailsTrailer)));
				} catch (Exception e2) {
					Toast.makeText(this, getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
				}
			}
		} else {
			if (!TextUtils.isEmpty(mMovie.getTrailer())) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getApplicationContext()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(this, mYouTubeApiKey, MizLib.getYouTubeId(mMovie.getTrailer()), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(mMovie.getTrailer()));
					startActivity(intent);
				}
			} else {
				Toast.makeText(this, getString(R.string.searching), Toast.LENGTH_SHORT).show();
				new TmdbTrailerSearch().execute(mMovie.getTmdbId());
			}
		}
	}

	public void watchOffline() {

		if (mMovie.getFilepaths().size() == 1) {
			watchOffline(mMovie.getFilepaths().get(0));
		} else {
			MizLib.showSelectFileDialog(MovieDetails.this, mMovie.getFilepaths(), new Dialog.OnClickListener() {
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
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.areYouSure))
			.setTitle(getString(R.string.removeOfflineCopy))
			.setCancelable(false)
			.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {	
					boolean success = mMovie.getOfflineCopyFile(path).delete();
					if (!success)
						mMovie.getOfflineCopyFile(path).delete();
					invalidateOptionsMenu();
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
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.downloadOfflineCopy))
			.setTitle(getString(R.string.watchOffline))
			.setCancelable(false)
			.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (MizLib.isLocalCopyBeingDownloaded(MovieDetails.this))
						Toast.makeText(getApplicationContext(), R.string.addedToDownloadQueue, Toast.LENGTH_SHORT).show();

					Intent i = new Intent(MovieDetails.this, MakeAvailableOffline.class);
					i.putExtra(MakeAvailableOffline.FILEPATH, path.getFilepath());
					i.putExtra(MakeAvailableOffline.TYPE, MizLib.TYPE_MOVIE);
					i.putExtra("thumb", mMovie.getThumbnail().getAbsolutePath());
					i.putExtra("backdrop", mMovie.getBackdrop().getAbsolutePath());
					startService(i);
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

	private class TmdbTrailerSearch extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				JSONObject jObject = MizLib.getJSONObject(mContext, "https://api.themoviedb.org/3/movie/" + params[0] + "/trailers?api_key=" + MizLib.getTmdbApiKey(getApplicationContext()));
				JSONArray trailers = jObject.getJSONArray("youtube");

				if (trailers.length() > 0)
					return trailers.getJSONObject(0).getString("source");
				else
					return null;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getApplicationContext()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(MovieDetails.this, mYouTubeApiKey, MizLib.getYouTubeId(result), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(result));
				}
			} else {
				new YoutubeTrailerSearch().execute(mMovie.getTitle());
			}
		}
	}

	private class YoutubeTrailerSearch extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				JSONObject jObject = MizLib.getJSONObject(mContext, "https://gdata.youtube.com/feeds/api/videos?q=" + params[0] + "&max-results=20&alt=json&v=2");
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");

				for (int i = 0; i < aitems.length(); i++) {
					JSONObject item0 = aitems.getJSONObject(i);

					// Check if the video can be embedded or viewed on a mobile device
					boolean embedding = false, mobile = false;
					JSONArray access = item0.getJSONArray("yt$accessControl");

					for (int j = 0; j < access.length(); j++) {
						if (access.getJSONObject(i).getString("action").equals("embed"))
							embedding = access.getJSONObject(i).getString("permission").equals("allowed") ? true : false;

						if (access.getJSONObject(i).getString("action").equals("syndicate"))
							mobile = access.getJSONObject(i).getString("permission").equals("allowed") ? true : false;
					}

					// Add the video ID if it's accessible from a mobile device
					if (embedding || mobile) {
						JSONObject id = item0.getJSONObject("id");
						String fullYTlink = id.getString("$t");
						return fullYTlink.substring(fullYTlink.lastIndexOf("video:") + 6);
					}
				}
			} catch (Exception e) {}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				if (YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getApplicationContext()).equals(YouTubeInitializationResult.SUCCESS)) {
					Intent intent = YouTubeStandalonePlayer.createVideoIntent(MovieDetails.this, mYouTubeApiKey, MizLib.getYouTubeId(result), 0, false, true);
					startActivity(intent);
				} else {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(result));
				}
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
			}
		}
	}

	public void searchCover() {
		if (MizLib.isOnline(getApplicationContext())) { // Make sure that the device is connected to the web
			Intent intent = new Intent();
			intent.putExtra("tmdbId", mMovie.getTmdbId());
			intent.putExtra("collectionId", mMovie.getCollectionId());
			intent.setClass(this, MovieCoverFanartBrowser.class);
			startActivity(intent); // Start the intent for result
		} else {
			// No movie ID / Internet connection
			Toast.makeText(this, getString(R.string.coverSearchFailed), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				finish();
			}
		}
		
		if (resultCode == 1)
			WidgetUtils.updateMovieWidgets(mContext);

		if (resultCode == 2 || resultCode == 4) {
			if (resultCode == 4) // The movie data has been edited
				Toast.makeText(this, getString(R.string.updatedMovie), Toast.LENGTH_SHORT).show();

			// Create a new Intent with the Bundle
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), MovieDetails.class);
			intent.putExtra("tmdbId", mMovieId);

			// Start the Intent for result
			startActivity(intent);

			finish();
			return;
		}
	}

	private void notifyDatasetChanges() {
		LocalBroadcastUtils.updateMovieLibrary(mContext);
	}

	public void editMovie() {
		Intent intent = new Intent(this, EditMovie.class);
		intent.putExtra("movieId", mMovie.getTmdbId());
		startActivityForResult(intent, 1);
	}

	private class MovieDetailsAdapter extends FragmentPagerAdapter {

		public MovieDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			switch (index) {
			case 0:
				return MovieDetailsFragment.newInstance(mMovieId);
			case 1:
				return ActorBrowserFragment.newInstance(mMovie.getTmdbId());
			}
			return null;
		}  

		@Override  
		public int getCount() {
			if (!MizLib.isValidTmdbId(mMovie.getTmdbId()))
				return 1;
			return 2;
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition);

		return true;
	}

	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			playMovie();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void checkIn() {
		new Thread() {
			@Override
			public void run() {
				Trakt.performMovieCheckin(mMovie, getApplicationContext());
			}
		}.start();
	}

	private void playMovie() {
		ArrayList<Filepath> paths = mMovie.getFilepaths();
		if (paths.size() == 1) {
			Filepath path = paths.get(0);
			if (mMovie.hasOfflineCopy(path)) {
				boolean playbackStarted = VideoUtils.playVideo(MovieDetails.this, mMovie.getOfflineCopyUri(path), false, mMovie);
				if (playbackStarted) {
					mVideoPlaybackStarted = System.currentTimeMillis();
					checkIn();
				}
			} else {
				playMovie(paths.get(0).getFilepath(), paths.get(0).isNetworkFile(), paths.get(0).getType() == FileSource.UPNP);
			}
		} else {
			boolean hasOfflineCopy = false;
			for (Filepath path : paths) {
				if (mMovie.hasOfflineCopy(path)) {
					boolean playbackStarted = VideoUtils.playVideo(MovieDetails.this, mMovie.getOfflineCopyUri(path), false, mMovie);
					if (playbackStarted) {
						mVideoPlaybackStarted = System.currentTimeMillis();
						checkIn();
					}

					hasOfflineCopy = true;
					break;
				}
			}

			if (!hasOfflineCopy) {
				MizLib.showSelectFileDialog(MovieDetails.this, mMovie.getFilepaths(), new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Filepath path = mMovie.getFilepaths().get(which);
						playMovie(path.getFilepath(), path.isNetworkFile(), path.getType() == FileSource.UPNP);
					}
				});
			}
		}
	}

	private void playMovie(final String filepath, final boolean isNetworkFile, boolean isUpnpFile) {
		if (isUpnpFile) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MovieDetails.this);
			builder.setTitle(R.string.where_to_play);
			builder.setItems(R.array.upnp_play_dialog, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) { // Play remotely
						Intent remotePlayback = new Intent(MovieDetails.this, RemotePlayback.class);
						remotePlayback.putExtra("videoUrl", filepath);
						remotePlayback.putExtra("coverUrl", "");
						remotePlayback.putExtra("title", mMovie.getTitle());
						startActivity(remotePlayback);
					} else {
						playMovie(filepath, isNetworkFile);
					}
					
					dialog.dismiss();
				}
			});
			builder.show();
		} else {
			playMovie(filepath, isNetworkFile);
		}
	}
	
	private void playMovie(String filepath, boolean isNetworkFile) {
		if (filepath.toLowerCase(Locale.getDefault()).matches(".*(cd1|part1).*")) {
			new GetSplitFiles(filepath, isNetworkFile).execute();
		} else {
			mVideoPlaybackStarted = System.currentTimeMillis();
			boolean playbackStarted = VideoUtils.playVideo(this, filepath, isNetworkFile, mMovie);
			if (playbackStarted)
				checkIn();
		}
	}

	private class GetSplitFiles extends AsyncTask<String, Void, List<SplitFile>> {

		private ProgressDialog progress;
		private String orig_filepath;
		private boolean isNetworkFile;

		public GetSplitFiles(String filepath, boolean isNetworkFile) {
			this.orig_filepath = filepath;
			this.isNetworkFile = isNetworkFile;
		}

		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(getApplicationContext());
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
				if (isNetworkFile)
					temp = MizLib.getSplitParts(orig_filepath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, orig_filepath));
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
				AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
				builder.setTitle(getString(R.string.playPart));
				builder.setAdapter(new SplitAdapter(getApplicationContext(), result), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						boolean playbackStarted = VideoUtils.playVideo(MovieDetails.this, result.get(which).getFilepath(), isNetworkFile, mMovie);
						if (playbackStarted)
							checkIn();
					}});
				builder.show();
			} else if (result.size() == 1) {
				boolean playbackStarted = VideoUtils.playVideo(MovieDetails.this, result.get(0).getFilepath(), isNetworkFile, mMovie);
				if (playbackStarted)
					checkIn();
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
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