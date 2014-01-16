package com.miz.mizuu.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.miz.functions.MizLib;
import com.miz.mizuu.EpisodeDetails;
import com.miz.mizuu.R;

public class ShowEpisodesFragment extends Fragment {

	private String showId;
	private boolean mDualPane;
	private FragmentManager fm;

	public static ShowEpisodesFragment newInstance(String showId) {
		ShowEpisodesFragment frag = new ShowEpisodesFragment();
		Bundle b = new Bundle();
		b.putString("showId", showId);
		frag.setArguments(b);
		return frag;
	}

	public ShowEpisodesFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showId = getArguments().getString("showId");
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-episode-selected"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isAdded())
				if (mDualPane) {
					FragmentTransaction ft = fm.beginTransaction();
					ft.replace(R.id.episode, ShowEpisodeDetailsFragment.newInstance(intent.getExtras().getString("rowId")));
					ft.commitAllowingStateLoss();
				} else {
					Intent i = new Intent();
					i.setClass(getActivity(), EpisodeDetails.class);
					i.putExtra("showId", intent.getExtras().getString("showId"));
					i.putExtra("rowId", intent.getExtras().getString("rowId"));
					startActivity(i);
				}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		return inflater.inflate(R.layout.show_episodes_layout, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mDualPane = v.findViewById(R.id.episode) == null ? false : true;

		MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));
		if (!MizLib.runsInPortraitMode(getActivity()) && !MizLib.runsOnTablet(getActivity()))
			v.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#101010"));

		fm = getChildFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.seasons, ShowSeasonsFragment.newInstance(showId, !MizLib.runsInPortraitMode(getActivity())));
		ft.commit();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}
}