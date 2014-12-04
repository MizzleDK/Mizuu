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

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.miz.apis.trakt.Trakt;
import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.service.TraktMoviesSyncService;
import com.miz.service.TraktTvShowsSyncService;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.File;

import static com.miz.functions.PreferenceKeys.SYNC_WITH_TRAKT;
import static com.miz.functions.PreferenceKeys.TRAKT_FULL_NAME;
import static com.miz.functions.PreferenceKeys.TRAKT_PASSWORD;
import static com.miz.functions.PreferenceKeys.TRAKT_USERNAME;

public class AccountsFragment extends Fragment {

	private SharedPreferences settings;
	private EditText traktUser, traktPass;
	private Button traktLogIn, traktRemoveAccount, traktSyncNow;
	private CheckBox syncTrakt;
	private String mTraktApiKey;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mTraktApiKey = Trakt.getApiKey(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.account_layout, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		traktUser = (EditText) v.findViewById(R.id.traktUsername);
		traktUser.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		traktPass = (EditText) v.findViewById(R.id.traktPassword);
		traktPass.setTypeface(Typeface.DEFAULT);
		traktPass.setTransformationMethod(new PasswordTransformationMethod());

		traktLogIn = (Button) v.findViewById(R.id.traktLogIn);
		traktLogIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new TraktLogin().execute();
			}
		});
		traktRemoveAccount = (Button) v.findViewById(R.id.traktRemoveAccount);
		traktRemoveAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				traktRemove();
			}
		});
		traktSyncNow = (Button) v.findViewById(R.id.traktSyncNow);
		traktSyncNow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().startService(new Intent(getActivity(), TraktMoviesSyncService.class));
				getActivity().startService(new Intent(getActivity(), TraktTvShowsSyncService.class));
			}
		});

		syncTrakt = (CheckBox) v.findViewById(R.id.syncTrakt);
		syncTrakt.setChecked(settings.getBoolean(SYNC_WITH_TRAKT, true));
		syncTrakt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor editor = settings.edit();
				editor.putBoolean(SYNC_WITH_TRAKT, isChecked);
				editor.apply();
			}
		});

		traktUser.setText(settings.getString(TRAKT_USERNAME, ""));
		if (TextUtils.isEmpty(settings.getString(TRAKT_PASSWORD, ""))) {
			traktPass.setText("");
			traktUser.setEnabled(true);
			traktPass.setEnabled(true);
			traktLogIn.setVisibility(View.VISIBLE);
			traktSyncNow.setVisibility(View.GONE);
			traktRemoveAccount.setVisibility(View.GONE);
			syncTrakt.setVisibility(View.GONE);
			syncTrakt.setChecked(false);
		} else {
			traktPass.setText("password");
			traktUser.setEnabled(false);
			traktPass.setEnabled(false);
			traktLogIn.setVisibility(View.GONE);
			traktSyncNow.setVisibility(View.VISIBLE);
			traktRemoveAccount.setVisibility(View.VISIBLE);
			syncTrakt.setVisibility(View.VISIBLE);;
		}
	}

	private class TraktLogin extends AsyncTask<Void, Void, Boolean> {

		private String username, password;

		@Override
		protected void onPreExecute() {
			traktLogIn.setText(R.string.authenticating);
			traktLogIn.setEnabled(false);
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			username = traktUser.getText().toString().trim();
			password = traktPass.getText().toString().trim();

			boolean success = false;
			
			try {
				
				Request request = MizLib.getTraktAuthenticationRequest("http://api.trakt.tv/account/test/" + mTraktApiKey, username, MizLib.SHA1(password));
				Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
				JSONObject jObject = new JSONObject(response.body().string());
				
				if (response.isSuccessful()) {
					String status = jObject.getString("status");
					success = status.equals("success");
				}
			} catch (Exception e) {
				success = false;
			}

			if (success) {

				Editor editor = settings.edit();
				editor.putString(TRAKT_USERNAME, username);
				editor.putString(TRAKT_PASSWORD, MizLib.SHA1(password));
				editor.apply();

				try {
					Request request = MizLib.getTraktAuthenticationRequest("http://api.trakt.tv/user/profile.json/" + mTraktApiKey + "/" + username, username, MizLib.SHA1(password));
					Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
					JSONObject jObject = new JSONObject(response.body().string());
					
					if (response.isSuccessful()) {
						String name = jObject.getString("full_name");
						if (TextUtils.isEmpty(name) || name.equals("null"))
							name = jObject.getString("username");
						String avatar = jObject.getString("avatar");

						editor.putString(TRAKT_FULL_NAME, name);
						editor.apply();

						if (isAdded() && (avatar.contains("gravatar") || (avatar.contains("trakt") && !avatar.contains("avatar-large.jpg"))))
							MizLib.downloadFile(avatar, new File(MizuuApplication.getCacheFolder(getActivity()), "avatar.jpg").getAbsolutePath());
					}
				} catch (Exception e) {
					success = false;
				}
			}

			return success;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				if (isAdded()) {
					Toast.makeText(getActivity(), getString(R.string.loginSucceeded), Toast.LENGTH_LONG).show();

					traktUser.setEnabled(false);
					traktPass.setEnabled(false);
					syncTrakt.setVisibility(View.VISIBLE);;
                    syncTrakt.setChecked(true);

					traktLogIn.setVisibility(View.GONE);
					traktSyncNow.setVisibility(View.VISIBLE);
					traktRemoveAccount.setVisibility(View.VISIBLE);

					startServices();
				}
			} else {
				if (isAdded()) {
					Toast.makeText(getActivity(), getString(R.string.failedToLogin), Toast.LENGTH_LONG).show();
					traktLogIn.setText(R.string.logIn);
					traktLogIn.setEnabled(true);
				}
			}
		}

	}

	public void traktRemove() {
		Editor editor = settings.edit();

		editor.putString(TRAKT_USERNAME, "");
		editor.putString(TRAKT_PASSWORD, "");
		editor.putString(TRAKT_FULL_NAME, "");
		editor.apply();
		
		new File(MizuuApplication.getCacheFolder(getActivity()), "avatar.jpg").delete();

		traktUser.setText("");
		traktUser.setEnabled(true);
		traktPass.setText("");
		traktPass.setEnabled(true);
		syncTrakt.setVisibility(View.GONE);
		syncTrakt.setChecked(false);

		traktLogIn.setVisibility(View.VISIBLE);
		traktSyncNow.setVisibility(View.GONE);
		traktRemoveAccount.setVisibility(View.GONE);

		Toast.makeText(getActivity(), getString(R.string.removedAccount), Toast.LENGTH_LONG).show();
	}

	private void startServices() {
		if (isAdded() && syncTrakt.isChecked()) {
			Intent movies = new Intent(getActivity(), TraktMoviesSyncService.class);
			getActivity().startService(movies);

			Intent shows = new Intent(getActivity(), TraktTvShowsSyncService.class);
			getActivity().startService(shows);
		}
	}
}