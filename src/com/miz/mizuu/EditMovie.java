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

import java.util.Calendar;

import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapter;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.miz.functions.MovieVersion;

public class EditMovie extends MizActivity {

	private DbAdapter dbHelper;
	private Cursor cursor;
	private int rowID;
	private TextView release;
	private EditText title, tagline, plot, runtime, rating, genres;
	private Spinner certification;
	private String tmdbId;
	private String[] certifications = new String[]{"G", "PG", "PG-13", "R", "NC-17", "X", "Unknown"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupDoneDiscard();
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.editmovie);

		title = (EditText) findViewById(R.id.titleText);
		tagline = (EditText) findViewById(R.id.taglineText);
		plot = (EditText) findViewById(R.id.plotText);
		runtime = (EditText) findViewById(R.id.runtimeText);
		rating = (EditText) findViewById(R.id.ratingText);
		genres = (EditText) findViewById(R.id.genresText);
		release = (TextView) findViewById(R.id.releasedateText);
		certification = (Spinner) findViewById(R.id.certificationSpinner);

		rowID = getIntent().getExtras().getInt("rowId");
		tmdbId = getIntent().getExtras().getString("tmdbId");

		// Create and open database
		dbHelper = MizuuApplication.getMovieAdapter();

		cursor = dbHelper.fetchMovie(rowID);

		if (cursor.moveToFirst()) {

			// Set the title
			title.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)));

			// Set the plot
			plot.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)));

			// Set the tag line
			tagline.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)));
			if (tagline.getText().toString().equals("NOTAGLINE") || !(tagline.getText().toString().length() > 0))
				tagline.setText("");

			// Set the run time
			runtime.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)));

			// Set the rating
			try {
				rating.setText(
						String.valueOf(
								Float.valueOf(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)).trim()).floatValue()
								)
						);
			} catch (Exception e) { // In case it's not a valid float
				rating.setText("0.0");
			}

			// Set the certification
			boolean found = false;

			for (int i = 0; i < certifications.length; i++) {
				if (certifications[i].equals(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)))) {
					certification.setSelection(i);
					found = true;
					break;
				}
			}

			if (!found)
				certification.setSelection(certifications.length - 1);

			// Set the release date
			release.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)));

			// Set the genres
			genres.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)));
		}

		cursor.close();
	}

	private void setupDoneDiscard() {
		// Inflate a "Done/Discard" custom action bar view.
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_done_discard, null);

		customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						doneEditing();
					}
				});
		customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						cancelEditing();
					}
				});

		// Show the custom action bar view and hide the normal Home icon and title.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	private void doneEditing() {
		if (rating.getText().toString().isEmpty())
			rating.setText("0.0");

		if (dbHelper.hasMultipleVersions(tmdbId)) {
			MovieVersion[] versions = dbHelper.getRowIdsForMovie(tmdbId);
			for (int i = 0; i < versions.length; i++) {
				dbHelper.editUpdateMovie(versions[i].getRowId(), title.getText().toString(), plot.getText().toString(), rating.getText().toString(), tagline.getText().toString(),
						release.getText().toString(), certifications[certification.getSelectedItemPosition()], runtime.getText().toString(), genres.getText().toString(), "0", "0", String.valueOf(System.currentTimeMillis()));
			}
		} else {
			dbHelper.editUpdateMovie(rowID, title.getText().toString(), plot.getText().toString(), rating.getText().toString(), tagline.getText().toString(),
					release.getText().toString(), certifications[certification.getSelectedItemPosition()], runtime.getText().toString(), genres.getText().toString(), "0", "0", String.valueOf(System.currentTimeMillis()));
		}		

		setResult(4);
		finish();
	}

	public void cancelEditing() {
		finish();
	}

	public void showDatePicker(View v) {
		int year, month, day;

		try {
			year = Integer.valueOf(release.getText().toString().substring(0, release.getText().toString().indexOf("-")));
		} catch (Exception e) {
			year = Calendar.getInstance().get(Calendar.YEAR);
		}
		
		try {
			month = Integer.valueOf(release.getText().toString().substring(release.getText().toString().indexOf("-") + 1, release.getText().toString().lastIndexOf("-"))) - 1;
		} catch (Exception e) {
			month = Calendar.getInstance().get(Calendar.MONTH);
		}
		
		try {
			day = Integer.valueOf(release.getText().toString().substring(release.getText().toString().lastIndexOf("-") + 1));
		} catch (Exception e) {
			day = Calendar.getInstance().get(Calendar.DATE);
		}

		DatePickerDialog date = new DatePickerDialog(this,
				new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				String day = String.valueOf(dayOfMonth), month = String.valueOf(monthOfYear + 1);
				if (day.length() == 1) day = "0" + day;
				if (month.length() == 1) month = "0" + month;

				release.setText(year + "-" + month + "-" + day);
			}
		}, year, month, day);

		date.show();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}