package com.miz.mizuu;

import java.io.IOException;
import java.util.Locale;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.MoveFilesService;
import com.miz.service.UpdateMovieService;
import com.miz.service.UpdateShowsService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.miz.base.MizActivity;
import android.widget.Toast;

public class SplashScreen extends MizActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!isDeviceCompatible()) {
			Toast.makeText(this, getString(R.string.deviceNotSupported), Toast.LENGTH_LONG).show();
			finish();
		}

		if (MizLib.oldDataFolderExists()) {	
			Intent moveFiles = new Intent(this, MoveFilesService.class);
			startService(moveFiles);

			return;
		}

		// Initialize the PreferenceManager variable and preference variable(s)
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String startup = settings.getString("prefsStartup", "1");

		if (settings.getBoolean("prefsUseEnglishLanguage", false)) {
			Locale locale = new Locale("en");
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		boolean rootPref = settings.getBoolean("prefsRootAccess", false);
		if (rootPref) { // Request root access if preference is enabled
			try {
				Runtime.getRuntime().exec("su");
			} catch (IOException e) {}
		}

		if (settings.getInt(ScheduledUpdatesFragment.MOVIE_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), UpdateMovieService.class));

		if (settings.getInt(ScheduledUpdatesFragment.SHOWS_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), UpdateShowsService.class));

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		if (startup.equals("0")) { // Welcome
			i.setClass(getApplicationContext(), Welcome.class);
		} else if (startup.equals("1")) {
			i.setClass(getApplicationContext(), MainMovies.class);
		} else if (startup.equals("2")) {
			i.setClass(getApplicationContext(), MainTvShows.class);
		} else if (startup.equals("3")) {
			i.setClass(getApplicationContext(), MainWatchlist.class);
		} else if (startup.equals("4")) {
			i.setClass(getApplicationContext(), MovieDiscovery.class);
		} else {	
			i.setClass(getApplicationContext(), MainWeb.class);
		}

		startActivity(i);
		
		finish();
	}

	private boolean isDeviceCompatible() {
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL)
			return false;

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1)
			return false;

		return true;
	}
}