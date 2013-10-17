package com.miz.mizuu;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	Preference pref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int res=getActivity().getResources().getIdentifier(getArguments().getString("resource"), "xml", getActivity().getPackageName());
		addPreferencesFromResource(res);

		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		pref = getPreferenceScreen().findPreference("prefsIgnoredFiles");
		if (pref != null)
			pref.setEnabled(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnoredFilesEnabled", false));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("prefsIgnoredFilesEnabled")) {
			if (pref != null)
				pref.setEnabled(sharedPreferences.getBoolean("prefsIgnoredFilesEnabled", false));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}
}