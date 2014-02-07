package com.miz.mizuu;

import com.miz.service.MovieLibraryUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class CancelMovieUpdate extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.empty_layout);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.areYouSure))
		.setTitle(getString(R.string.stringCancelUpdate))
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				LocalBroadcastManager.getInstance(CancelMovieUpdate.this).sendBroadcast(new Intent(MovieLibraryUpdate.STOP_MOVIE_LIBRARY_UPDATE));
				
				Toast.makeText(CancelMovieUpdate.this, getString(R.string.stringUpdateCancelled), Toast.LENGTH_LONG).show();
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