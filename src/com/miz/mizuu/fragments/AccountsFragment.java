package com.miz.mizuu.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
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

import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.service.TraktMoviesSyncService;
import com.miz.service.TraktTvShowsSyncService;

public class AccountsFragment extends Fragment {

	private SharedPreferences settings;
	private EditText traktUser, traktPass;
	private Button traktLogIn, traktRemoveAccount, traktSyncNow;
	private CheckBox syncTrakt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
		syncTrakt.setChecked(settings.getBoolean("syncLibrariesWithTrakt", true));
		syncTrakt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor editor = settings.edit();
				editor.putBoolean("syncLibrariesWithTrakt", isChecked);
				editor.commit();
			}
		});

		traktUser.setText(settings.getString("traktUsername", ""));
		if (settings.getString("traktPassword", "").isEmpty()) {
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

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://api.trakt.tv/account/test/" + MizLib.TRAKT_API);

			try {
				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("username", username));
				nameValuePairs.add(new BasicNameValuePair("password", MizLib.SHA1(password)));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(html);

				String status = jObject.getString("status");
				success = status.equals("success");

			} catch (Exception e) {
				success = false;
			}

			if (success) {

				Editor editor = settings.edit();
				editor.putString("traktUsername", username);
				editor.putString("traktPassword", MizLib.SHA1(password));
				editor.commit();

				httpclient = new DefaultHttpClient();
				httppost = new HttpPost("http://api.trakt.tv/user/profile.json/" + MizLib.TRAKT_API + "/" + username);

				try {
					// Add your data
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("username", username));
					nameValuePairs.add(new BasicNameValuePair("password", MizLib.SHA1(password)));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					String html = httpclient.execute(httppost, responseHandler);

					JSONObject jObject = new JSONObject(html);

					String name = jObject.getString("full_name");
					if (name.equals("null") || name.isEmpty())
						name = jObject.getString("username");
					String avatar = jObject.getString("avatar");

					editor.putString("traktFullName", name);
					editor.commit();

					if (isAdded() && (avatar.contains("gravatar") || (avatar.contains("trakt") && !avatar.contains("avatar-large.jpg"))))
						MizLib.downloadFile(avatar, new File(MizLib.getCacheFolder(getActivity()), "avatar.jpg").getAbsolutePath());

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

		editor.putString("traktUsername", "");
		editor.putString("traktPassword", "");
		editor.commit();

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