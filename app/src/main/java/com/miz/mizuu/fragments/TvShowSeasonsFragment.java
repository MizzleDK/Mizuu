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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.apis.trakt.Trakt;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.CoverItem;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.GridEpisode;
import com.miz.functions.GridSeason;
import com.miz.functions.IntentKeys;
import com.miz.functions.LibrarySectionAsyncTask;
import com.miz.functions.MizLib;
import com.miz.functions.TvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;
import com.miz.utils.IntentUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TvShowDatabaseUtils;
import com.miz.utils.TypefaceUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TvShowSeasonsFragment extends Fragment {

    private static final String SHOW_ID = "showId";
    private static final String SEASON = "season";
    private static final String EPISODE_COUNT = "episodeCount";

    private Set<Integer> mCheckedSeasons = new HashSet<Integer>();
    private List<GridSeason> mItems = new ArrayList<GridSeason>();
    private int mImageThumbSize, mImageThumbSpacing, mSelectedIndex;
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private ImageAdapter mAdapter;
    private String mShowId;
    private Picasso mPicasso;
    private Config mConfig;
    private boolean mContextualActionBarEnabled;
    private Bus mBus;
    private Context mContext;
    private int mToolbarColor;

    public static TvShowSeasonsFragment newInstance(String showId, int color) {
        TvShowSeasonsFragment frag = new TvShowSeasonsFragment();
        Bundle b = new Bundle();
        b.putString(SHOW_ID, showId);
        b.putInt(IntentKeys.TOOLBAR_COLOR, color);
        frag.setArguments(b);
        return frag;
    }

    public TvShowSeasonsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        mBus = MizuuApplication.getBus();
        mBus.register(this);

        mPicasso = MizuuApplication.getPicasso(mContext);
        mConfig = MizuuApplication.getBitmapConfig();

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mShowId = getArguments().getString(SHOW_ID);
        mToolbarColor = getArguments().getInt(IntentKeys.TOOLBAR_COLOR);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver,
                new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_SEASONS_OVERVIEW));
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadSeasons();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        mBus.unregister(this);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
    }

    @Subscribe
    public void refreshData(com.miz.mizuu.TvShowEpisode episode) {
        loadSeasons();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_grid_fragment, container, false);
    }

    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mAdapter = new ImageAdapter(mContext);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);

        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setEmptyView(v.findViewById(R.id.progress));
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setAdapter(mAdapter);
        mGridView.setColumnWidth(mImageThumbSize);

        // Calculate the total column width to set item heights by factor 1.5
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                mAdapter.setNumColumns(numColumns);
                            }
                        }
                    }
                });
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // Update the selected index variable
                mSelectedIndex = arg2;

                // Show the episode browser Activity for the given season
                getActivity().startActivityForResult(IntentUtils.getTvShowSeasonIntent(getActivity(), mShowId, mItems.get(arg2).getSeason(), mItems.get(arg2).getEpisodeCount(), mToolbarColor), 0);
            }
        });
        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mGridView.setItemChecked(position, true);
                return true;
            }
        });
        mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.seasons_contextual, menu);

                mContextualActionBarEnabled = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.watched:
                        changeWatchedStatus(true, new HashSet<Integer>(mCheckedSeasons));
                        break;
                    case R.id.unwatched:
                        changeWatchedStatus(false, new HashSet<Integer>(mCheckedSeasons));
                        break;
                    case R.id.remove:
                        removeSelectedSeasons(new HashSet<Integer>(mCheckedSeasons));
                        break;
                }

                mode.finish();

                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mContextualActionBarEnabled = false;
                mCheckedSeasons.clear();
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) {
                    mCheckedSeasons.add(mItems.get(position).getSeason());
                } else {
                    mCheckedSeasons.remove(mItems.get(position).getSeason());
                }

                int count = mCheckedSeasons.size();
                mode.setTitle(count + " " + getResources().getQuantityString(R.plurals.seasons_selected, count, count));

                // Nasty hack to update the selected items highlight...
                mAdapter.notifyDataSetChanged();
            }
        });

        // The layout has been created - let's load the data
        loadSeasons();
    }

    private void loadSeasons() {
        new SeasonLoader().execute();
    }

    private void changeWatchedStatus(boolean watched, final Set<Integer> selectedSeasons) {
        // Create and open database
        DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();

        // This ought to be done in the background, but performance is fairly decent
        // - roughly 0.7 seconds to update 600 entries on a Sony Xperia Tablet Z2
        for (int season : selectedSeasons) {
            db.setSeasonWatchStatus(mShowId, MizLib.addIndexZero(season), watched);
        }

        if (MizLib.isOnline(mContext) && Trakt.hasTraktAccount(mContext))
            syncWatchedStatusWithTrakt(selectedSeasons, watched);

        loadSeasons();
    }

    private void removeSelectedSeasons(final Set<Integer> selectedSeasons) {
        // Get the Activity Context
        final Context activityContext = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
        builder.setTitle(R.string.remove_selected_seasons);
        builder.setMessage(R.string.areYouSure);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Go through all seasons and remove the selected ones
                for (int season : selectedSeasons) {
                    TvShowDatabaseUtils.deleteSeason(activityContext, mShowId, season);
                }

                // Check if we've removed all TV show episodes
                if (MizuuApplication.getTvEpisodeDbAdapter().getEpisodeCount(mShowId) == 0) {

                    // Update the TV show library
                    LocalBroadcastUtils.updateTvShowLibrary(activityContext);

                    // Finish the Activity
                    getActivity().finish();
                } else {
                    // There's still episodes left, so re-load the TV show seasons
                    loadSeasons();
                }
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private class ImageAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private final Context mContext;
        private int mNumColumns = 0;

        public ImageAdapter(Context context) {
            mContext = context;
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mItems.size();
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
        public View getView(final int position, View convertView, ViewGroup container) {

            final GridSeason mSeason = mItems.get(position);
            final CoverItem holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.grid_season_cover, container, false);
                holder = new CoverItem();

                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.highlight = (ImageView) convertView.findViewById(R.id.highlight);
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

            // Android's GridView is pretty stupid regarding selectors
            // so we have to highlight the selected view manually - yuck!
            if (!mContextualActionBarEnabled) {
                holder.highlight.setVisibility(View.GONE);
            } else {
                if (mCheckedSeasons.contains(mSeason.getSeason())) {
                    holder.highlight.setVisibility(View.VISIBLE);
                } else {
                    holder.highlight.setVisibility(View.GONE);
                }
            }

            holder.text.setText(mSeason.getHeaderText());
            holder.subtext.setText(mSeason.getSubtitleText());

            mPicasso.load(mSeason.getCover()).error(R.drawable.loading_image).fit().config(mConfig).into(holder.cover);

            return convertView;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }

    private class SeasonLoader extends LibrarySectionAsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mItems.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {
            HashMap<String, EpisodeCounter> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(mShowId);

            File temp;
            for (String key : seasons.keySet()) {
                temp = FileUtils.getTvShowSeason(mContext, mShowId, key);
                mItems.add(new GridSeason(mContext, mShowId, Integer.valueOf(key), seasons.get(key).getEpisodeCount(), seasons.get(key).getWatchedCount(),
                        temp.exists() ? temp :
                                FileUtils.getTvShowThumb(mContext, mShowId)));
            }

            seasons.clear();

            Collections.sort(mItems);

            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void syncWatchedStatusWithTrakt(final Set<Integer> checkedSeasons, final boolean watched) {
        new com.miz.functions.AsyncTask<Void, Boolean, Boolean>() {

            private List<TvShowEpisode> mEpisodes = new ArrayList<>();

            @Override
            protected void onPreExecute() {
                DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();

                for (int season : checkedSeasons) {
                    List<GridEpisode> temp = db.getEpisodesInSeason(mContext, mShowId, season);
                    for (int i = 0; i < temp.size(); i++) {
                        mEpisodes.add(new TvShowEpisode(mShowId, temp.get(i).getEpisode(), temp.get(i).getSeason()));
                    }
                    temp.clear();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = Trakt.markEpisodeAsWatched(mShowId, mEpisodes, mContext, watched);
                if (!result) // Try again if it failed
                    result = Trakt.markEpisodeAsWatched(mShowId, mEpisodes, mContext, watched);

                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result)
                    Toast.makeText(mContext, R.string.sync_error, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }
}