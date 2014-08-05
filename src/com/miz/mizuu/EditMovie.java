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

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapter;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditMovie extends MizActivity {

	private DbAdapter mDatabaseHelper;
	private Cursor mCursor;
	private TextView mRelease;
	private EditText mTitle, mTagline, mPlot, mRuntime, mRating, mGenres;
	private Spinner mCertification;
	private String mTmdbId, mToWatch, mHasWatched;
	private String[] mCertifications = new String[]{"G", "PG", "PG-13", "R", "NC-17", "X", "Unknown"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.editmovie);

		mTitle = (EditText) findViewById(R.id.titleText);
		mTagline = (EditText) findViewById(R.id.taglineText);
		mPlot = (EditText) findViewById(R.id.plotText);
		mRuntime = (EditText) findViewById(R.id.runtimeText);
		mRating = (EditText) findViewById(R.id.ratingText);
		mGenres = (EditText) findViewById(R.id.genresText);
		mRelease = (TextView) findViewById(R.id.releasedateText);
		mCertification = (Spinner) findViewById(R.id.certificationSpinner);

		mTmdbId = getIntent().getExtras().getString("tmdbId");

		// Create and open database
		mDatabaseHelper = MizuuApplication.getMovieAdapter();

		mCursor = mDatabaseHelper.fetchMovie(mTmdbId);

		if (mCursor.moveToFirst()) {

			// Watchlist
			mToWatch = mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_TO_WATCH));

			// Has watched
			mHasWatched = mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED));

			// Set the title
			mTitle.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_TITLE)));

			// Set the plot
			mPlot.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_PLOT)));

			// Set the tag line
			mTagline.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_TAGLINE)));
			if (mTagline.getText().toString().equals("NOTAGLINE") || !(mTagline.getText().toString().length() > 0))
				mTagline.setText("");

			// Set the run time
			mRuntime.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_RUNTIME)));

			// Set the rating
			try {
				mRating.setText(
						String.valueOf(
								Float.valueOf(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_RATING)).trim()).floatValue()
								)
						);
			} catch (Exception e) { // In case it's not a valid float
				mRating.setText("0.0");
			}

			// Set the certification
			boolean found = false;

			for (int i = 0; i < mCertifications.length; i++) {
				if (mCertifications[i].equals(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)))) {
					mCertification.setSelection(i);
					found = true;
					break;
				}
			}

			if (!found)
				mCertification.setSelection(mCertifications.length - 1);

			// Set the release date
			mRelease.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)));

			// Set the genres
			mGenres.setText(mCursor.getString(mCursor.getColumnIndex(DbAdapter.KEY_GENRES)));
		}

		mCursor.close();
	}

	private void doneEditing() {
		if (mRating.getText().toString().isEmpty())
			mRating.setText("0.0");

		mDatabaseHelper.editUpdateMovie(mTmdbId, mTitle.getText().toString(), mPlot.getText().toString(), mRating.getText().toString(),
				mTagline.getText().toString(), mRelease.getText().toString(), mCertifications[mCertification.getSelectedItemPosition()],
				mRuntime.getText().toString(), mGenres.getText().toString(), mToWatch, mHasWatched, String.valueOf(System.currentTimeMillis()));

		setResult(4);
		finish();
	}

	public void cancelEditing() {
		finish();
	}

	public void showDatePicker(View v) {
		int year, month, day;

		String release = mRelease.getText().toString();

		try {
			year = Integer.valueOf(release.substring(0, release.indexOf("-")));
		} catch (Exception e) {
			year = Calendar.getInstance().get(Calendar.YEAR);
		}

		try {
			month = Integer.valueOf(release.substring(release.indexOf("-") + 1, release.lastIndexOf("-"))) - 1;
		} catch (Exception e) {
			month = Calendar.getInstance().get(Calendar.MONTH);
		}

		try {
			day = Integer.valueOf(release.substring(release.lastIndexOf("-") + 1));
		} catch (Exception e) {
			day = Calendar.getInstance().get(Calendar.DATE);
		}

		DatePickerDialog date = new DatePickerDialog(this,
				new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				String day = String.valueOf(dayOfMonth), month = String.valueOf(monthOfYear + 1);
				if (day.length() == 1) day = "0" + day;
				if (month.length() == 1) month = "0" + month;

				mRelease.setText(year + "-" + month + "-" + day);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.editmovie, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.cancel_editing:
			cancelEditing();
			return true;
		case R.id.done_editing:
			doneEditing();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}