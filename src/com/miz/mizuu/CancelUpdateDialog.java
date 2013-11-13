package com.miz.mizuu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.miz.service.UpdateMovieService;
import com.miz.service.UpdateShowsService;

import android.view.View;
import android.widget.Toast;

public class CancelUpdateDialog extends Activity {

	public static final int MOVIE = 1, TVSHOWS = 2;
	private int type = MOVIE;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		type = getIntent().getExtras().getInt("type", MOVIE);
		
		setTitle(R.string.stringCancelUpdate);
		setContentView(R.layout.cancel_update);
	}
	
	public void ok(View v) {
		if (type == MOVIE) {
			stopService(new Intent(this, UpdateMovieService.class));
		} else {
			stopService(new Intent(this, UpdateShowsService.class));
		}
		Toast.makeText(this, getString(R.string.stringUpdateCancelled), Toast.LENGTH_LONG).show();
		finish();
	}
	
	public void cancel(View v) {
		finish();
	}
}