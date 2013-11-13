package com.miz.mizuu.fragments;

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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.service.TraktMoviesSyncService;
import com.miz.service.TraktTvShowsSyncService;

public class AccountsFragment extends Fragment {

	private SharedPreferences settings;
	private TextView traktUser, traktPass;
	private Button traktLogIn, traktRemoveAccount;
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

		traktUser = (TextView) v.findViewById(R.id.traktUsername);
		traktPass = (TextView) v.findViewById(R.id.traktPassword);

		traktLogIn = (Button) v.findViewById(R.id.traktLogIn);
		traktLogIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				traktLogin();
			}
		});
		traktRemoveAccount = (Button) v.findViewById(R.id.traktRemoveAccount);
		traktRemoveAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				traktRemove();
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
			traktLogIn.setEnabled(true);
			traktRemoveAccount.setEnabled(false);
			syncTrakt.setEnabled(false);
			syncTrakt.setChecked(false);
		} else {
			traktPass.setText("password");
			traktUser.setEnabled(false);
			traktPass.setEnabled(false);
			traktLogIn.setEnabled(false);
			traktRemoveAccount.setEnabled(true);
			syncTrakt.setEnabled(true);
		}
	}

	public void traktLogin() {	
		new Thread() {
			@Override
			public void run() {
				String username = traktUser.getText().toString().trim(), password = traktPass.getText().toString().trim();
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
				} finally {
					if (success) {
						Editor editor = settings.edit();

						editor.putString("traktUsername", username);
						editor.putString("traktPassword", MizLib.SHA1(password));
						editor.commit();

						if (isAdded())
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getActivity(), getString(R.string.loginSucceeded), Toast.LENGTH_LONG).show();

									traktUser.setEnabled(false);
									traktPass.setEnabled(false);
									syncTrakt.setEnabled(true);

									traktLogIn.setEnabled(false);
									traktRemoveAccount.setEnabled(true);

									startServices();
								}
							});
					} else {
						if (isAdded())
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getActivity(), getString(R.string.failedToLogin), Toast.LENGTH_LONG).show();
								}
							});
					}
				}
			}
		}.start();
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
		syncTrakt.setEnabled(false);
		syncTrakt.setChecked(false);

		traktLogIn.setEnabled(true);
		traktRemoveAccount.setEnabled(false);

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