package com.miz.functions;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

import jcifs.smb.SmbFile;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;

public class DeleteFile extends IntentService {

	public DeleteFile() {
		super("DeleteFile");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String file = intent.getExtras().getString("filepath");

		if (file.startsWith("smb://")) {
			boolean prefsDisableEthernetWiFiCheck = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefsDisableEthernetWiFiCheck", false);
			ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

			if (MizLib.isWifiConnected(this, prefsDisableEthernetWiFiCheck)) {
				FileSource source = null;

				for (int j = 0; j < filesources.size(); j++)
					if (file.contains(filesources.get(j).getFilepath())) {
						source = filesources.get(j);
						continue;
					}

				if (source == null)
					return;

				try {
					final SmbFile smbFile = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(source.getDomain(), "utf-8"),
									URLEncoder.encode(source.getUser(), "utf-8"),
									URLEncoder.encode(source.getPassword(), "utf-8"),
									file,
									false
									));

					if (smbFile.exists())
						smbFile.delete();
				} catch (Exception e) {
				}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
			}
		} else {
			File f = new File(file);
			if (!f.delete())
				f.delete();
		}
	}
}