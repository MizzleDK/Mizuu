package com.miz.mizuu;

import java.util.List;
import java.util.Locale;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Support extends Activity {

	private Spinner subject;
	private EditText message;
	private TextView deviceDetails;

	@Override
	public void onCreate(Bundle b) {

		super.onCreate(b);

		setupDoneDiscard();

		setContentView(R.layout.contact_developer);

		subject = (Spinner) findViewById(R.id.subjectSpinner);
		message = (EditText) findViewById(R.id.traktUsername);
		deviceDetails = (TextView) findViewById(R.id.textView2);

		try {
			String versionType = "";
			if (getPackageName().equals("com.miz.mizuulite")) {
				versionType = " (lite)";
			}

			PackageManager manager = getPackageManager();
			PackageInfo packageInfo = manager.getPackageInfo(getPackageName(), 0);

			deviceDetails.setText(
					getString(R.string.contactDeviceModel) + ": " + Build.MODEL + " (" + Build.PRODUCT + ")\n" + // Model
							getString(R.string.contactDeviceManufacturer) + ": " + Build.MANUFACTURER + "\n" + // Manufacturer
							getString(R.string.contactDeviceCustomization) + ": " + Build.BRAND + "\n" + // Brand / customization
							getString(R.string.contactDeviceOS) + ": v" + Build.VERSION.RELEASE + " (" + Build.VERSION.INCREMENTAL + ")\n" + // OS
							"Mizuu version: " + packageInfo.versionName + versionType
					);
		} catch (Exception e) {
			Toast.makeText(this, getString(R.string.errorSomethingWentWrong), Toast.LENGTH_SHORT).show();
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
				final PackageManager pm = getPackageManager();
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
				startActivity(emailIntent);
				Toast.makeText(this, getString(R.string.launchingGmail), Toast.LENGTH_SHORT).show();
				finish();
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.failedGmailLaunch), Toast.LENGTH_SHORT).show();
			}
		} else {
			// Animation
			ObjectAnimator animation = ObjectAnimator.ofFloat(message, "translationX", -10f, -5f, 0f, 5f, 10f, 5f, 0f, -5f, -10f, -5f, 0f);
			animation.setDuration(250);
			animation.start();
		    
			Toast.makeText(this, getString(R.string.enterAMessage), Toast.LENGTH_SHORT).show();
		}
	}

	private void setupDoneDiscard() {
		// Inflate a "Done/Discard" custom action bar view.
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_send_discard, null);

		customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						send();
					}
				});
		customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});

		// Show the custom action bar view and hide the normal Home icon and title.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}
}