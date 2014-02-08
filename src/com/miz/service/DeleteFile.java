package com.miz.service;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

import jcifs.smb.SmbFile;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

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
					
					// Delete subtitles as well
					ArrayList<SmbFile> subs = new ArrayList<SmbFile>();

					String fileType = "";
					if (file.contains(".")) {
						fileType = file.substring(file.lastIndexOf("."));
					}

					int count = MizLib.subtitleFormats.length;
					for (int i = 0; i < count; i++) {
						subs.add(new SmbFile(
								MizLib.createSmbLoginString(
										URLEncoder.encode(source.getDomain(), "utf-8"),
										URLEncoder.encode(source.getUser(), "utf-8"),
										URLEncoder.encode(source.getPassword(), "utf-8"),
										file.replace(fileType, MizLib.subtitleFormats[i]),
										false
										)));
					}
					
					for (int i = 0; i < subs.size(); i++) {
						if (subs.get(i).exists())
							subs.get(i).delete();
					}
					
				} catch (Exception e) {
				}  // the file isn't available (either MalformedURLException or SmbException)
				showErrorMessage();
			}
		} else {
			boolean deleted;
			File f = new File(file);
			
			deleted = f.delete();
			if (!deleted)
				deleted = f.delete();
			
			if (!deleted)
				showErrorMessage();
			
			ArrayList<File> subs = new ArrayList<File>();

			String fileType = "";
			if (file.contains(".")) {
				fileType = file.substring(file.lastIndexOf("."));
			}

			int count = MizLib.subtitleFormats.length;
			for (int i = 0; i < count; i++) {
				subs.add(new File(file.replace(fileType, MizLib.subtitleFormats[i])));
			}
			
			for (int i = 0; i < subs.size(); i++) {
				if (subs.get(i).exists())
					subs.get(i).delete();
			}
		}
	}
	
	private void showErrorMessage() {
		Handler mHandler = new Handler();
		mHandler.post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), R.string.failedDeleting, Toast.LENGTH_LONG).show();              
			}
		});
	}
}