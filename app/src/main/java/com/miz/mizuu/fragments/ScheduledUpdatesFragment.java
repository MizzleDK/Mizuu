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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.miz.functions.MizLib;
import com.miz.functions.ScheduledUpdatesAlarmManager;
import com.miz.mizuu.R;

import static com.miz.functions.PreferenceKeys.NEXT_SCHEDULED_MOVIE_UPDATE;
import static com.miz.functions.PreferenceKeys.NEXT_SCHEDULED_TVSHOWS_UPDATE;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_MOVIE;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_TVSHOWS;

public class ScheduledUpdatesFragment extends Fragment {

	public static final int NOT_ENABLED = 0, AT_LAUNCH = 1, EVERY_HOUR = 2, EVERY_2_HOURS = 3, EVERY_4_HOURS = 4, EVERY_6_HOURS = 5, EVERY_12_HOURS = 6, EVERY_DAY = 7, EVERY_WEEK = 8;
	private int movieUpdateType = NOT_ENABLED, showsUpdateType = NOT_ENABLED;
	private SharedPreferences settings;
	private Editor editor;
	private CheckBox movies, shows;
	private Spinner moviesSpinner, showsSpinner;
	private TextView nextMovieUpdate, nextShowsUpdate;
	private boolean ignoreMovies = true, ignoreShows = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.scheduled_updates, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		movies = (CheckBox) v.findViewById(R.id.moviesCheckBox);
		shows = (CheckBox) v.findViewById(R.id.showsCheckBox);

		moviesSpinner = (Spinner) v.findViewById(R.id.moviesSpinner);
		showsSpinner = (Spinner) v.findViewById(R.id.showsSpinner);

		nextMovieUpdate = (TextView) v.findViewById(R.id.nextMovieUpdate);
		nextShowsUpdate = (TextView) v.findViewById(R.id.nextShowsUpdate);

		movieUpdateType = settings.getInt(SCHEDULED_UPDATES_MOVIE, NOT_ENABLED);
		showsUpdateType = settings.getInt(SCHEDULED_UPDATES_TVSHOWS, NOT_ENABLED);

		if (movieUpdateType == NOT_ENABLED) {
			movies.setChecked(false);
			moviesSpinner.setEnabled(false);
			setOnCheckedListenerMovie();
		} else {
			movies.setChecked(true);
			moviesSpinner.setEnabled(true);
			moviesSpinner.setSelection(movieUpdateType - 1);
			setOnCheckedListenerMovie();
		}

		if (showsUpdateType == NOT_ENABLED) {
			shows.setChecked(false);
			showsSpinner.setEnabled(false);
			setOnCheckedListenerShows();
		} else {
			shows.setChecked(true);
			showsSpinner.setEnabled(true);
			showsSpinner.setSelection(showsUpdateType - 1);
			setOnCheckedListenerShows();
		}

		updateNextMovieUpdateText();
		updateNextShowsUpdateText();

		movies.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				moviesSpinner.setEnabled(isChecked);
				saveMovieSchedule(!isChecked);
			}
		});

		shows.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				showsSpinner.setEnabled(isChecked);
				saveShowsSchedule(!isChecked);
			}
		});
	}

	private void setOnCheckedListenerMovie() {
		moviesSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (!ignoreMovies) {
					if (moviesSpinner.isEnabled())
						saveMovieSchedule(false);
				} else {
					ignoreMovies = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}

	private void setOnCheckedListenerShows() {
		showsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (!ignoreShows) {
					if (showsSpinner.isEnabled())
						saveShowsSchedule(false);
				} else {
					ignoreShows = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}

	private void saveMovieSchedule(boolean isDisabled) {
		editor = settings.edit();
		editor.putInt(SCHEDULED_UPDATES_MOVIE, isDisabled ? NOT_ENABLED : moviesSpinner.getSelectedItemPosition() + 1);
		editor.apply();

		if (isAdded()) {
			if ((moviesSpinner.getSelectedItemPosition() + 1) > AT_LAUNCH) {
				MizLib.scheduleMovieUpdate(getActivity());
			} else {
				ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.MOVIES, getActivity());
			}

			updateNextMovieUpdateText();
		}
	}

	private void saveShowsSchedule(boolean isDisabled) {
		editor = settings.edit();
		editor.putInt(SCHEDULED_UPDATES_TVSHOWS, isDisabled ? NOT_ENABLED : showsSpinner.getSelectedItemPosition() + 1);
		editor.apply();

		if (isAdded()) {
			if ((showsSpinner.getSelectedItemPosition() + 1) > AT_LAUNCH) {
				MizLib.scheduleShowsUpdate(getActivity());
			} else {
				ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.SHOWS, getActivity());
			}

			updateNextShowsUpdateText();
		}
	}

	private void updateNextMovieUpdateText() {
		if (movies.isChecked() && moviesSpinner.getSelectedItemPosition() > 0) {
			nextMovieUpdate.setVisibility(View.VISIBLE);

			long nextUpdate = settings.getLong(NEXT_SCHEDULED_MOVIE_UPDATE, 0);
			if (nextUpdate > 0) {
				long timeUntilNext = (nextUpdate - System.currentTimeMillis()) / 1000 / 60;

				long minutes = timeUntilNext % 60;
				timeUntilNext /= 60;
				long hours = timeUntilNext % 24;
				timeUntilNext /= 24;
				long days = timeUntilNext;

				String minutes_string = "", hours_string = "", days_string = "";

				if (minutes > 0)
					minutes_string = minutes + " " + getResources().getQuantityString(R.plurals.minute, (int) minutes, (int) minutes);

				if (hours > 0)
					hours_string = hours + " " + getResources().getQuantityString(R.plurals.hour, (int) hours, (int) hours);

				if (days > 0)
					days_string = days + " " + getResources().getQuantityString(R.plurals.day, (int) days, (int) days);

				nextMovieUpdate.setText(getString(R.string.nextUpdateIn) + " " + ((days > 0) ? days_string + ", " : "") + ((hours > 0) ? hours_string + ", " : "") + ((minutes > 0) ? minutes_string : ""));
			} else {
				nextMovieUpdate.setVisibility(View.GONE);
			}
		} else {
			nextMovieUpdate.setVisibility(View.GONE);
		}
	}

	private void updateNextShowsUpdateText() {
		if (shows.isChecked() && showsSpinner.getSelectedItemPosition() > 0) {
			nextShowsUpdate.setVisibility(View.VISIBLE);

			long nextUpdate = settings.getLong(NEXT_SCHEDULED_TVSHOWS_UPDATE, 0);
			if (nextUpdate > 0) {
				long timeUntilNext = (nextUpdate - System.currentTimeMillis()) / 1000 / 60;

				long minutes = timeUntilNext % 60;
				timeUntilNext /= 60;
				long hours = timeUntilNext % 24;
				timeUntilNext /= 24;
				long days = timeUntilNext;

				String minutes_string = "", hours_string = "", days_string = "";

				if (minutes > 0)
					minutes_string = minutes + " " + getResources().getQuantityString(R.plurals.minute, (int) minutes, (int) minutes);

				if (hours > 0)
					hours_string = hours + " " + getResources().getQuantityString(R.plurals.hour, (int) hours, (int) hours);

				if (days > 0)
					days_string = days + " " + getResources().getQuantityString(R.plurals.day, (int) days, (int) days);

				nextShowsUpdate.setText(getString(R.string.nextUpdateIn) + " " + ((days > 0) ? days_string + ", " : "") + ((hours > 0) ? hours_string + ", " : "") + ((minutes > 0) ? minutes_string : ""));
			} else {
				nextShowsUpdate.setVisibility(View.GONE);
			}
		} else {
			nextShowsUpdate.setVisibility(View.GONE);
		}
	}
}