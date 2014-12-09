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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.miz.abstractclasses.ApiService;
import com.miz.apis.thetvdb.TvShow;
import com.miz.base.MizActivity;
import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.service.IdentifyTvShowEpisodeService;
import com.miz.utils.TypefaceUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class IdentifyTvShowEpisodeFragment extends Fragment {

    private ListView mListView;
    private EditText mQuery;
    private Spinner mSpinner;
    private ProgressBar mProgress;
    private Picasso mPicasso;
    private Bitmap.Config mConfig;
    private String mShowId, mShowTitle, mLocale;
    private ArrayList<String> mFilepaths;
    private ListAdapter mAdapter;
    private LanguageAdapter mSpinnerAdapter;
    private TvShowSearch mTvShowSearch;
    private Toolbar mToolbar;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public IdentifyTvShowEpisodeFragment() {}

    public static IdentifyTvShowEpisodeFragment newInstance(ArrayList<String> filepaths, String showTitle, String showId) {
        IdentifyTvShowEpisodeFragment frag = new IdentifyTvShowEpisodeFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("filepaths", filepaths);
        args.putString("showTitle", showTitle);
        args.putString("showId", showId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // Hide the keyboard
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mFilepaths = getArguments().getStringArrayList("filepaths");
        mShowId = getArguments().getString("showId");
        mShowTitle = getArguments().getString("showTitle");

        mPicasso = MizuuApplication.getPicasso(getActivity());
        mConfig = MizuuApplication.getBitmapConfig();

        mAdapter = new ListAdapter(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.identify_movie_and_tv_show, container, false);
    }

    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);

        mListView = (ListView) v.findViewById(R.id.listView1);
        mQuery = (EditText) v.findViewById(R.id.editText1);
        mSpinner = (Spinner) v.findViewById(R.id.spinner1);
        mProgress = (ProgressBar) v.findViewById(R.id.progressBar1);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                updateShow(arg2);
            }
        });
        mListView.setEmptyView(v.findViewById(R.id.no_results));
        v.findViewById(R.id.no_results).setVisibility(View.GONE); // Manually make it gone to begin with

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mLocale = mSpinnerAdapter.getItem(position).getLanguage();
                searchForShows();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        mSpinnerAdapter = new LanguageAdapter();
        mSpinner.setAdapter(mSpinnerAdapter);
        String language = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LANGUAGE_PREFERENCE, "en");
        mSpinner.setSelection(mSpinnerAdapter.getIndexForLocale(language));

        mQuery.setText(mShowTitle);
        mQuery.setSelection(mQuery.length());
        mQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0)
                    searchForShows();
                else {
                    mTvShowSearch.cancel(true);
                    mAdapter.clearItems();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        mQuery.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_SEARCH)
                    searchForShows();
                return true;
            }
        });

        if (mTvShowSearch == null) {
            if (MizLib.isOnline(getActivity())) {
                mTvShowSearch = new TvShowSearch(getActivity(), mQuery.getText().toString());
            } else {
                Toast.makeText(getActivity(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchForShows() {
        if (MizLib.isOnline(getActivity())) {
            if (!mQuery.getText().toString().isEmpty()) {
                mTvShowSearch.cancel(true);
                mTvShowSearch = new TvShowSearch(getActivity(), mQuery.getText().toString());
            } else {
                mAdapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateShow(int id) {
        if (MizLib.isOnline(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.updatingShowInfo), Toast.LENGTH_LONG).show();

            Intent identifyService = new Intent(getActivity(), IdentifyTvShowEpisodeService.class);
            Bundle b = new Bundle();
            b.putString("oldShowId", mShowId);
            b.putString("newShowId", mAdapter.getItem(id).getId());
            b.putString("language", getSelectedLanguage());
            b.putStringArrayList("filepaths", mFilepaths);
            identifyService.putExtras(b);

            getActivity().startService(identifyService);

            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        } else
            Toast.makeText(getActivity(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
    }

    private String getSelectedLanguage() {
        return mLocale;
    }

    protected class TvShowSearch extends AsyncTask<Void, Boolean, Boolean> {

        private final Context mContext;
        private final String mQueryText;
        private ArrayList<Result> mResults = new ArrayList<Result>();

        public TvShowSearch(Context context, String query) {
            mContext = context;
            mQueryText = query;

            // Execute the AsyncTask
            execute();
        }

        @Override
        protected void onPreExecute() {
            showProgressBar();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (isCancelled())
                    return false;

                ApiService<TvShow> service = MizuuApplication.getTvShowService(mContext);
                List<TvShow> results = service.search(mQueryText, getSelectedLanguage());

                if (isCancelled())
                    return false;

                // TheTVDb always includes the English entry for a show,
                // so this can result in duplicates. We don't want that.
                HashSet<String> addedIds = new HashSet<String>();

                int count = results.size();
                for (int i = 0; i < count; i++) {
                    if (!addedIds.contains(results.get(i).getId())) {
                        mResults.add(new Result(
                                        results.get(i).getTitle(),
                                        results.get(i).getOriginalTitle(),
                                        results.get(i).getId(),
                                        results.get(i).getCoverUrl(),
                                        results.get(i).getDescription(),
                                        results.get(i).getFirstAired(),
                                        results.get(i).getRating())
                        );
                        addedIds.add(results.get(i).getId());
                    }
                }

                return !isCancelled();

            } catch (Exception e) {}

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result)
                return;

            hideProgressBar();

            if (mQuery.getText().toString().length() > 0) {
                if (mListView.getAdapter() == null) {
                    mAdapter = new ListAdapter(mContext);
                    mAdapter.setList(mResults);
                    mListView.setAdapter(mAdapter);
                } else {
                    mAdapter.setList(mResults);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    static class ViewHolder {
        TextView title, originalTitle, release, description;
        ImageView cover;
        LinearLayout layout;
    }

    public class ListAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private final Context mContext;
        private int mItemHeight = 0;
        private GridView.LayoutParams mImageViewLayoutParams;
        private ArrayList<Result> mItems = new ArrayList<Result>();

        public ListAdapter(Context context) {
            mContext = context;
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mImageViewLayoutParams = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        public void setList(ArrayList<Result> list) {
            mItems = new ArrayList<Result>(list);
        }

        public void clearItems() {
            mItems.clear();
        }

        public int getCount() {
            return mItems.size();
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_movie_and_tv_show, parent, false);

                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.movieTitle);

                holder.description = (TextView) convertView.findViewById(R.id.textView7);
                holder.description.setMaxLines(3);
                holder.description.setEllipsize(TruncateAt.END);

                holder.release = (TextView) convertView.findViewById(R.id.textReleaseDate);

                holder.originalTitle = (TextView) convertView.findViewById(R.id.originalTitle);
                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.layout = (LinearLayout) convertView.findViewById(R.id.cover_layout);

                holder.title.setTypeface(TypefaceUtils.getRobotoCondensedRegular(getActivity()));
                holder.release.setTypeface(TypefaceUtils.getRobotoLightItalic(getActivity()));
                holder.description.setTypeface(TypefaceUtils.getRobotoLightItalic(getActivity()));

                // Check the height matches our calculated column width
                if (holder.layout.getLayoutParams().height != mItemHeight) {
                    holder.layout.setLayoutParams(mImageViewLayoutParams);
                }

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Result result = getItem(position);

            holder.title.setText(result.getTitle());
            holder.release.setText(result.getRelease());

            holder.description.setVisibility(View.VISIBLE);
            if (!result.getDescription().isEmpty())
                holder.description.setText(result.getDescription());
            else {
                if (!mItems.get(position).getRating().equals("0.0")) {
                    try {
                        int rating = (int) (Double.parseDouble(mItems.get(position).getRating()) * 10);
                        holder.description.setText(Html.fromHtml(getString(R.string.detailsRating) + ": " + rating + "<small> %</small>"));
                    } catch (NumberFormatException e) {
                        holder.description.setVisibility(View.GONE);
                    }
                } else {
                    holder.description.setVisibility(View.GONE);
                }
            }

            if (result.hasOriginalTitle() && result.hasDifferentTitles()) {
                holder.originalTitle.setVisibility(View.VISIBLE);
                holder.originalTitle.setText(result.getOriginalTitle());
            } else {
                holder.originalTitle.setVisibility(View.GONE);
            }

            mPicasso.load(result.getImage()).placeholder(R.drawable.gray).error(R.drawable.loading_image).config(mConfig).into(holder.cover);

            return convertView;
        }

        @Override
        public Result getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    public class Result {

        private final String mTitle, mOriginalTitle, mId, mImage, mDescription, mRelease, mRating;

        public Result(String title, String originalTitle, String id, String image, String description, String release, String rating) {
            mTitle = title;
            mOriginalTitle = originalTitle;
            mId = id;
            mImage = image;
            mDescription = description;
            mRelease = MizLib.getPrettyDate(getActivity(), release);
            mRating = rating;
        }

        public String getTitle() {
            return mTitle;
        }

        public boolean hasDifferentTitles() {
            return !mTitle.equals(mOriginalTitle);
        }

        public boolean hasOriginalTitle() {
            return !mOriginalTitle.isEmpty();
        }

        public String getOriginalTitle() {
            return "(" + mOriginalTitle + ")";
        }

        public String getId() {
            return mId;
        }

        public String getImage() {
            return mImage;
        }

        public String getDescription() {
            return mDescription;
        }

        public String getRelease() {
            if (mRelease.equals("null"))
                return getString(R.string.unknownYear);
            return mRelease;
        }

        public String getRating() {
            return mRating;
        }
    }

    public class LanguageAdapter implements SpinnerAdapter {

        private ArrayList<Locale> mLocales = new ArrayList<Locale>();
        private final Locale[] mSystemLocales;
        private final LayoutInflater mInflater;

        public LanguageAdapter() {

            mInflater = LayoutInflater.from(getActivity());

            mSystemLocales = Locale.getAvailableLocales();
            String[] languageCodes = Locale.getISOLanguages();

            ArrayList<Locale> mTemp = new ArrayList<Locale>();
            for (String code : languageCodes) {
                if (code.length() == 2) { // We're only interested in two character codes
                    Locale l = new Locale(code);
                    if (hasLocale(l))
                        mTemp.add(l);
                }
            }

            Collections.sort(mTemp, new Comparator<Locale>() {
                @Override
                public int compare(Locale lhs, Locale rhs) {
                    return lhs.getDisplayLanguage(Locale.getDefault()).compareToIgnoreCase(rhs.getDisplayLanguage(Locale.getDefault()));
                }
            });

            mLocales = new ArrayList<Locale>(mTemp);
        }

        private boolean hasLocale(Locale l) {
            for (Locale locale : mSystemLocales)
                if (locale.equals(l))
                    return true;
            return false;
        }

        public int getIndexForLocale(String locale) {
            for (int i = 0; i < mLocales.size(); i++)
                if (mLocales.get(i).getLanguage().equalsIgnoreCase(locale))
                    return i;
            return 0;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {}

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {}

        @Override
        public int getCount() {
            return mLocales.size();
        }

        @Override
        public Locale getItem(int position) {
            return mLocales.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            ((TextView) convertView).setText(mLocales.get(position).getDisplayLanguage(Locale.getDefault()));

            return convertView;
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
        public boolean isEmpty() {
            return mLocales.size() == 0;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            ((TextView) convertView).setText(mLocales.get(position).getDisplayLanguage(Locale.getDefault()));

            return convertView;
        }

    }

    private void showProgressBar() {
        mListView.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        mListView.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);
    }
}