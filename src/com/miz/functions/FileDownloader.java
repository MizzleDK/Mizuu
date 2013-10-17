package com.miz.functions;

import java.net.URLEncoder;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import android.app.IntentService;
import android.content.Intent;

public class FileDownloader extends IntentService {

	public static final String FILE_TYPE = "filetype", FILE_PATH = "filepath";
	private String networkFilepath = "";
	private SmbFile file = null;
	private int type = 0;
	
	public FileDownloader() {
		super("FileDownloader");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		reset();
		
		type = intent.getExtras().getInt(FILE_TYPE);
		networkFilepath = intent.getExtras().getString(FILE_PATH);
		
		int freeMemory = MizLib.getFreeMemory();
		final NtlmPasswordAuthentication auth = MizLib.getAuthFromFilepath(type, networkFilepath);
		
		try {
			file = new SmbFile(
					MizLib.createSmbLoginString(
							URLEncoder.encode(auth.getDomain(), "utf-8"),
							URLEncoder.encode(auth.getUsername(), "utf-8"),
							URLEncoder.encode(auth.getPassword(), "utf-8"),
							networkFilepath,
							false
							));
			
			// Check if the device has enough storage memory available
			if (file.length() > (freeMemory * 1.1)) {
				
				// Copy file...
				
			} else {
				
				// Show "not enough storage space" error
				
			}
		} catch (Exception ignored) {}
	}
	
	private void reset() {
		type = 0;
		networkFilepath = "";
		file = null;
	}

}