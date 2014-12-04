package com.miz.mizuu.fragments;/*
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

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

import java.util.ArrayList;

public class IgnoredTvShowFilesFragment extends Fragment {

    private ListView mListView;

    public IgnoredTvShowFilesFragment() {} // Empty constructor as per the documentation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.list, container, false);
        mListView = (ListView) v.findViewById(android.R.id.list);
        mListView.setBackgroundResource(0);
        mListView.setAdapter(new TvShowAdapter(getActivity()));

        return v;
    }

    static class ViewHolder {
        TextView folderName, fullPath;
        ImageView remove, icon;
    }

    public class TvShowAdapter extends BaseAdapter {

        private ArrayList<String> mEpisodes = new ArrayList<String>();
        private LayoutInflater inflater;
        private Context mContext;

        public TvShowAdapter(Context context) {
            mContext = context;
            inflater = LayoutInflater.from(context);

            notifyDataSetChanged();
        }

        public int getCount() {
            return mEpisodes.size();
        }

        public String getItem(int position) {
            return mEpisodes.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            final String filepath = getItem(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.filesource_list, parent, false);

                holder = new ViewHolder();
                holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
                holder.folderName.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
                holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
                holder.fullPath.setVisibility(View.GONE);
                holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
                holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);
                holder.icon.setVisibility(View.GONE);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String temp = filepath.contains("<MiZ>") ? filepath.split("<MiZ>")[1] : filepath;

            holder.folderName.setText(MizLib.transformSmbPath(temp));
            holder.remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeSelectedEpisode(getItem(position));
                }
            });

            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {

            mEpisodes.clear();

            Cursor cursor = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllIgnoredFilepaths();
            if (cursor != null)
                try {
                    while (cursor.moveToNext()) {
                        mEpisodes.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));
                    }
                } catch (Exception e) {
                } finally {
                    cursor.close();
                }

            super.notifyDataSetChanged();
        }
    }

    private void removeSelectedEpisode(String filepath) {
        MizuuApplication.getTvShowEpisodeMappingsDbAdapter().deleteFilepath(filepath);

        mListView.setAdapter(new TvShowAdapter(getActivity()));
    }
}
