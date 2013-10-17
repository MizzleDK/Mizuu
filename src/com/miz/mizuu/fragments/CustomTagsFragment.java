package com.miz.mizuu.fragments;

import java.util.ArrayList;

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
		items_string = settings.getString("ignoredTags", "");

		if (!items_string.isEmpty())
			for (String s : items_string.split("<MiZ>"))
				items.add(s);
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
				editor.putString("ignoredTags", items_string);
				editor.commit();
				
				items.clear();
				for (String s : items_string.split("<MiZ>"))
					items.add(s);
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
		for (String item : items) {
			sb.append("<MiZ>" + item);
		}
		
		String result = sb.toString();
		if (result.startsWith("<MiZ>")) {
			result = result.substring(5);
		}
		
		items_string = result;
		
		Editor editor = settings.edit();
		editor.putString("ignoredTags", items_string);
		editor.commit();
		
		((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
	}
}