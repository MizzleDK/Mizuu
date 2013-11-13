package com.miz.mizuu.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MainMovies;
import com.miz.mizuu.MainTvShows;
import com.miz.mizuu.MainWatchlist;
import com.miz.mizuu.MainWeb;
import com.miz.mizuu.MovieDiscovery;
import com.miz.mizuu.R;

public class MenuListFragment extends Fragment {

	private ListView list;
	private int mNumMovies, mNumShows, mNumWatchlist, type;
	private Typeface tf;
	private DbAdapter dbHelper;
	private DbAdapterTvShow dbHelperTv;

	public static MenuListFragment newInstance(int type) {
		MenuListFragment frag = new MenuListFragment();
		Bundle b = new Bundle();
		b.putInt("type", type);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		dbHelper = MizuuApplication.getMovieAdapter();
		dbHelperTv = MizuuApplication.getTvDbAdapter();

		type = getArguments().getInt("type");
		tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Thin.ttf");

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-update"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-shows-update"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateLibraryCounts();
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		updateLibraryCounts();
	}

	@Override
	public void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);

		super.onDestroy();
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.list, null);

		list = (ListView) v.findViewById(android.R.id.list);
		list.setAdapter(new MenuAdapter());
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				switch (arg2) {
				case 1:
					i.setClass(getActivity(), MainMovies.class);
					break;
				case 2:
					i.setClass(getActivity(), MainTvShows.class);
					break;
				case 3:
					i.setClass(getActivity(), MainWatchlist.class);
					break;
				case 5:
					i.setClass(getActivity(), MovieDiscovery.class);
					break;
				case 6:
					i.setClass(getActivity(), MainWeb.class);
					break;
				default:
					break;
				}

				if (!(arg2 == 0 || arg2 == 4)) {
					getActivity().startActivity(i);
					getActivity().overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
				}
			}
		});

		return v;
	}

	private void updateLibraryCounts() {
		new Thread() {
			@Override
			public void run() {
				try {					
					mNumMovies = dbHelper.count();
					mNumWatchlist = dbHelper.countWatchlist();
					mNumShows = dbHelperTv.count();

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
						}
					});
				} catch (Exception e) {} // Problemer med at kontakte databasen
			}
		}.start();
	}

	public class MenuAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return 7;
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
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0 || position == 4)
				return 0;
			else
				return 1;
		}
		
		@Override
		public boolean isEnabled(int position) {
			if (position == 0 || position == 4)
				return false;
			else
				return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == 0 || position == 4) {
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.row_header, null);
				if (position == 0) {
					((TextView) convertView.findViewById(R.id.options)).setText(getString(R.string.stringLocal));
				} else {
					((TextView) convertView.findViewById(R.id.options)).setText(getString(R.string.stringDiscover));
				}
				return convertView;
			}

			convertView = LayoutInflater.from(getActivity()).inflate(R.layout.row, null);
			ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
			TextView title = (TextView) convertView.findViewById(R.id.row_title);
			if (MizLib.runsOnTablet(getActivity()))
				title.setTextSize(26f);
			TextView description = (TextView) convertView.findViewById(R.id.local_movie_count);

			title.setTypeface(tf);
			title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			switch (position) {
			case 1:
				icon.setImageResource(R.drawable.ic_action_movie);
				title.setText(getString(R.string.chooserMovies));
				description.setText(String.valueOf(mNumMovies));
				break;
			case 2:
				icon.setImageResource(R.drawable.ic_action_tv);
				title.setText(getString(R.string.chooserTVShows));
				description.setText(String.valueOf(mNumShows));
				break;
			case 3:
				icon.setImageResource(R.drawable.ic_action_list_2);
				title.setText(getString(R.string.chooserWatchList));
				description.setText(String.valueOf(mNumWatchlist));
				break;
			case 5:
				icon.setImageResource(R.drawable.ic_action_movie);
				title.setText(getString(R.string.chooserMovies));
				description.setVisibility(View.GONE);
				break;
			case 6:
				icon.setImageResource(R.drawable.ic_action_globe);
				title.setText(getString(R.string.chooserWebVideos));
				description.setVisibility(View.GONE);
				break;
			}

			if (type == position)
				convertView.setBackgroundColor(Color.parseColor("#50000000"));

			return convertView;
		}	
	}
}