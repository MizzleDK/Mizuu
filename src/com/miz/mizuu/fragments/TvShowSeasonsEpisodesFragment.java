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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.miz.functions.GridSeason;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class TvShowSeasonsEpisodesFragment extends Fragment {

	private static final String SHOW_ID = "showId";

	private String mShowId;
	private boolean mDualPane;
	private Bus mBus;
	private FragmentManager mFragmentManager;

	public static TvShowSeasonsEpisodesFragment newInstance(String showId) {
		TvShowSeasonsEpisodesFragment frag = new TvShowSeasonsEpisodesFragment();
		Bundle b = new Bundle();
		b.putString(SHOW_ID, showId);
		frag.setArguments(b);		
		return frag;
	}

	public TvShowSeasonsEpisodesFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mShowId = getArguments().getString(SHOW_ID);

		// Use a Bus instance to allow for communication between fragments
		mBus = MizuuApplication.getBus();
		mBus.register(this);

		mFragmentManager = getChildFragmentManager();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mBus.unregister(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		return inflater.inflate(R.layout.show_episodes_layout, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mDualPane = v.findViewById(R.id.episode) == null ? false : true;
		
		v.findViewById(R.id.container).setBackgroundResource(MizuuApplication.getBackgroundColorResource(getActivity()));

		MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		FragmentTransaction ft = mFragmentManager.beginTransaction();
		ft.replace(R.id.seasons, TvShowSeasonsFragment.newInstance(mShowId, mDualPane));
		ft.commit();
	}

	@Subscribe
	public void seasonSelected(GridSeason season) {		
		if (mDualPane) {
			FragmentTransaction ft = mFragmentManager.beginTransaction();
			ft.replace(R.id.episode, TvShowEpisodesFragment.newInstance(mShowId, season.getSeason()));
			ft.commitAllowingStateLoss();
		}
	}
}