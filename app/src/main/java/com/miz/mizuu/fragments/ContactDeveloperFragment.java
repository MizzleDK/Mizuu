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

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.utils.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ContactDeveloperFragment extends Fragment {

	private Spinner subject;
	private EditText message;
	private TextView deviceDetails;
	private Button send;
    private CheckBox mCheckBox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.support, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		if (!MizLib.isTablet(getActivity()))
			getActivity().setTitle(R.string.menuAboutContact);

        mCheckBox = (CheckBox) v.findViewById(R.id.includeDatabase);
		subject = (Spinner) v.findViewById(R.id.subjectSpinner);
		message = (EditText) v.findViewById(R.id.traktUsername);
		deviceDetails = (TextView) v.findViewById(R.id.textView2);
		send = (Button) v.findViewById(R.id.send);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});

		try {
			PackageManager manager = getActivity().getPackageManager();
			PackageInfo packageInfo = manager.getPackageInfo(getActivity().getPackageName(), 0);

			deviceDetails.setText(
					getString(R.string.contactDeviceModel) + ": " + Build.MODEL + " (" + Build.PRODUCT + ")\n" + // Model
							getString(R.string.contactDeviceManufacturer) + ": " + Build.MANUFACTURER + "\n" + // Manufacturer
							getString(R.string.contactDeviceCustomization) + ": " + Build.BRAND + "\n" + // Brand / customization
							getString(R.string.contactDeviceOS) + ": v" + Build.VERSION.RELEASE + " (" + Build.VERSION.INCREMENTAL + ")\n" + // OS
							"Mizuu version: " + packageInfo.versionName
					);
		} catch (Exception e) {
			Toast.makeText(getActivity(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_SHORT).show();
		}
	}

	private void send() {
		if (!message.getText().toString().isEmpty() && message.getText().toString().length() > 5) {
			try {
				Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				String[] recipients = new String[]{"michell.bak@gmail.com"};
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Mizuu support");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						"Subject: " + subject.getSelectedItem().toString() +
						"\n\nMessage:\n" + message.getText().toString() + // Enter description
						"\n\n" + deviceDetails.getText().toString()); // Mizuu version
				emailIntent.setType("plain/text"); // This is an incorrect MIME, but Gmail is one of the only apps that responds to it
				final PackageManager pm = getActivity().getPackageManager();
				final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
				ResolveInfo best = null;
				int count = matches.size();
				for (int i = 0; i < count; i++) {
					if (matches.get(i).activityInfo.packageName.endsWith(".gm") ||
							matches.get(i).activityInfo.name.toLowerCase(Locale.ENGLISH).contains("gmail")) best = matches.get(i);
				}
				if (best != null)
					emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
				emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (mCheckBox.isChecked()) {
                    String path = FileUtils.copyDatabase(getActivity());

                    if (!TextUtils.isEmpty(path))
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                }

				startActivity(emailIntent);
				Toast.makeText(getActivity(), getString(R.string.launchingGmail), Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(getActivity(), getString(R.string.failedGmailLaunch), Toast.LENGTH_SHORT).show();
			}
		} else {
			// Animation
            try {
                ObjectAnimator animation = ObjectAnimator.ofFloat(message, "translationX", -10f, -5f, 0f, 5f, 10f, 5f, 0f, -5f, -10f, -5f, 0f);
                animation.setDuration(250);
                animation.start();
            } catch (Exception e) {
                // Some devices crash at runtime when using the ObjectAnimator
            }

			Toast.makeText(getActivity(), getString(R.string.enterAMessage), Toast.LENGTH_SHORT).show();
		}
	}
}