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

	private boolean isMovie = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.empty_layout);

		isMovie = getIntent().getExtras().getBoolean("isMovie", false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.areYouSure))
		.setTitle(getString(R.string.stringCancelUpdate))
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				if (isMovie)
					LocalBroadcastManager.getInstance(CancelLibraryUpdate.this).sendBroadcast(new Intent(MovieLibraryUpdate.STOP_MOVIE_LIBRARY_UPDATE));
				else
					LocalBroadcastManager.getInstance(CancelLibraryUpdate.this).sendBroadcast(new Intent(TvShowsLibraryUpdate.STOP_TVSHOW_LIBRARY_UPDATE));

				Toast.makeText(CancelLibraryUpdate.this, getString(R.string.stoppingUpdate), Toast.LENGTH_LONG).show();
				finish();
				return;
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