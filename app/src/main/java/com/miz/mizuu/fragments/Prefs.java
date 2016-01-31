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

package com.miz.mizuu.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.miz.mizuu.R;
import com.miz.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class Prefs extends PreferenceFragment {

	private Preference mLanguagePref, mCopyDatabase;
	private Locale[] mSystemLocales;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int res=getActivity().getResources().getIdentifier(getArguments().getString("resource"), "xml", getActivity().getPackageName());
		addPreferencesFromResource(res);

        mCopyDatabase = getPreferenceScreen().findPreference("prefsCopyDatabase");
        if (mCopyDatabase != null)
            mCopyDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String path = FileUtils.copyDatabase(getActivity());

                    if (!TextUtils.isEmpty(path)) {
                        Toast.makeText(getActivity(), getString(R.string.database_copied) + "\n(" + path + ")", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
            });

		mLanguagePref = getPreferenceScreen().findPreference(LANGUAGE_PREFERENCE);
		if (mLanguagePref != null)
			mLanguagePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {

					mSystemLocales = Locale.getAvailableLocales();
					String[] languageCodes = Locale.getISOLanguages();

					final ArrayList<Locale> mTemp = new ArrayList<Locale>();
					for (String code : languageCodes) {
						if (code.length() == 2) { // We're only interested in two character codes
							Locale l = new Locale(code);
							if (hasLocale(l))
								mTemp.add(l);
						}
					}

					Collections.sort(mTemp, new Comparator<Locale>() {
						@Override
						public int compare(Locale lhs, Locale rhs) {
							return lhs.getDisplayLanguage(Locale.getDefault()).compareToIgnoreCase(rhs.getDisplayLanguage(Locale.getDefault()));
						}
					});

					String[] items = new String[mTemp.size()];
					for (int i = 0; i < mTemp.size(); i++)
						items[i] = mTemp.get(i).getDisplayLanguage(Locale.getDefault());
					
					final String[] codes = new String[mTemp.size()];
					for (int i = 0; i < mTemp.size(); i++)
						codes[i] = mTemp.get(i).getLanguage();
					
					mTemp.clear();
					
					int checkedItem = getIndexForLocale(codes, PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LANGUAGE_PREFERENCE, "en"));
					if (checkedItem == -1)
						checkedItem = getIndexForLocale(codes, "en"); // "en" by default
					
					AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
					bldr.setTitle(R.string.set_pref_language_title);
					bldr.setSingleChoiceItems(items, checkedItem, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							savePreference(LANGUAGE_PREFERENCE, codes[which]);
							dialog.dismiss();
						}
					});
					bldr.show();

					return true;
				}
			});
	}
	
	private void savePreference(String key, String value) {
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(key, value).commit();
	}
	
	private boolean hasLocale(Locale l) {
		for (Locale locale : mSystemLocales)
			if (locale.equals(l))
				return true;
		return false;
	}
	
	public int getIndexForLocale(String[] languages, String locale) {
		for (int i = 0; i < languages.length; i++)
			if (languages[i].equalsIgnoreCase(locale))
				return i;
		return -1;
	}
}