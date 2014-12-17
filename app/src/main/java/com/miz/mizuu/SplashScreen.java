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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.MoveFilesService;
import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;
import com.miz.utils.FileUtils;

import java.io.IOException;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.ROOT_ACCESS;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_MOVIE;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_TVSHOWS;
import static com.miz.functions.PreferenceKeys.STARTUP_SELECTION;
import static com.miz.functions.PreferenceKeys.USE_ENGLISH_LANGUAGE;

public class SplashScreen extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!isDeviceCompatible()) {
			Toast.makeText(this, getString(R.string.deviceNotSupported), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (FileUtils.oldDataFolderExists()) {	
			Intent moveFiles = new Intent(this, MoveFilesService.class);
			startService(moveFiles);
			finish();
			return;
		}

		// Initialize the PreferenceManager variable and preference variable(s)
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String startup = settings.getString(STARTUP_SELECTION, "1");

		if (settings.getBoolean(USE_ENGLISH_LANGUAGE, false)) {
			Locale locale = new Locale("en");
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		boolean rootPref = settings.getBoolean(ROOT_ACCESS, false);
		if (rootPref) { // Request root access if preference is enabled
			try {
				Runtime.getRuntime().exec("su");
			} catch (IOException e) {}
		}

		if (settings.getInt(SCHEDULED_UPDATES_MOVIE, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), MovieLibraryUpdate.class));

		if (settings.getInt(SCHEDULED_UPDATES_TVSHOWS, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), TvShowsLibraryUpdate.class));

		Intent i = new Intent();
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		if (startup.equals("0")) { // Welcome
			i.setClass(getApplicationContext(), Welcome.class);
		} else {
			i.setClass(getApplicationContext(), Main.class);
		}

		startActivity(i);
		
		finish();
	}

	private boolean isDeviceCompatible() {
        return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) != Configuration.SCREENLAYOUT_SIZE_SMALL && MizLib.hasICSMR1();
    }
}