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

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.mizuu.R;

import java.util.ArrayList;
import java.util.Collections;

import static com.miz.functions.PreferenceKeys.IGNORED_FILENAME_TAGS;

public class CustomTagsFragment extends Fragment {

	private ArrayList<String> items = new ArrayList<String>();
	private String items_string;
	private ListView lv;
	private Button addItem;
	private TextView tagText;

	private SharedPreferences settings;

	public CustomTagsFragment() {}

	public static CustomTagsFragment newInstance() {
		return new CustomTagsFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		items_string = settings.getString(IGNORED_FILENAME_TAGS, "");

		if (!items_string.isEmpty()) {
			String[] split = items_string.split("<MiZ>");
            Collections.addAll(items, split);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.custom_tags_layout, container, false);

		lv = (ListView) v.findViewById(android.R.id.list);
		lv.setAdapter(new ListAdapter(getActivity()));

		tagText = (TextView) v.findViewById(R.id.search);

		addItem = (Button) v.findViewById(R.id.addTag);
		addItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				items_string += "<MiZ>" + tagText.getText();
				if (items_string.startsWith("<MiZ>"))
					items_string = items_string.substring(5);

				Editor editor = settings.edit();
				editor.putString(IGNORED_FILENAME_TAGS, items_string);
				editor.apply();

				items.clear();
				String[] split = items_string.split("<MiZ>");
                Collections.addAll(items, split);
				((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();

				tagText.setText("");
			}
		});

		return v;
	}

	static class ViewHolder {
		TextView folderName, fullPath;
		ImageView remove, icon;
	}

	public class ListAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private Context c;

		public ListAdapter(Context c) {
			this.c = c;
			inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return items.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.filesource_list, null);

				holder = new ViewHolder();
				holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.folderName.setTextAppearance(c, android.R.style.TextAppearance_Medium);
				holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
				holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.folderName.setText(items.get(position));
			holder.fullPath.setVisibility(View.GONE);
			holder.icon.setVisibility(View.GONE);

			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedTag(position);
				}
			});

			return convertView;
		}
	}

	private void removeSelectedTag(int position) {
		items.remove(position);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.size(); i++) {
            sb.append("<MiZ>");
            sb.append(items.get(i));
        }


		String result = sb.toString();
		if (result.startsWith("<MiZ>")) {
			result = result.substring(5);
		}

		items_string = result;

		Editor editor = settings.edit();
		editor.putString(IGNORED_FILENAME_TAGS, items_string);
		editor.apply();

		((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
	}
}