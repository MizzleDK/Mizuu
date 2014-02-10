package com.miz.mizuu;

import java.io.IOException;
import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.MoveFilesService;
import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;

public class SplashScreen extends MizActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!isDeviceCompatible()) {
			Toast.makeText(this, getString(R.string.deviceNotSupported), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (MizLib.oldDataFolderExists()) {	
			Intent moveFiles = new Intent(this, MoveFilesService.class);
			startService(moveFiles);
			finish();
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
			getApplicationContext().startService(new Intent(getApplicationContext(), MovieLibraryUpdate.class));

		if (settings.getInt(ScheduledUpdatesFragment.SHOWS_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), TvShowsLibraryUpdate.class));

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		if (startup.equals("0")) { // Welcome
			i.setClass(getApplicationContext(), Welcome.class);
		} else {
			i.setClass(getApplicationContext(), Main.class);
		}

		startActivity(i);
		
		finish();
		return;
	}

	private boolean isDeviceCompatible() {
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL)
			return false;

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1)
			return false;

		return true;
	}
}