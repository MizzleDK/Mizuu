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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;

public class CancelLibraryUpdate extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final boolean isMovie = getIntent().getExtras().getBoolean("isMovie");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.areYouSure))
		.setTitle(getString(R.string.stringCancelUpdate))
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				if (isMovie) {
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(MovieLibraryUpdate.STOP_MOVIE_LIBRARY_UPDATE));
				} else {
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(TvShowsLibraryUpdate.STOP_TVSHOW_LIBRARY_UPDATE));
				}
				
				Toast.makeText(CancelLibraryUpdate.this, getString(R.string.stoppingUpdate), Toast.LENGTH_LONG).show();
				finish();
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		})
		.create().show();
	}
}