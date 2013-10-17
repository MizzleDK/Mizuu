package com.miz.functions;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jcifs.smb.SmbFile;

import android.content.Context;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

public class CifsImageDownloader extends BaseImageDownloader {

	public CifsImageDownloader(Context context) {
		super(context);
	}

	@Override
	protected InputStream getStreamFromOtherSource(String imageUri, Object extra) throws IOException {
		if (imageUri.contains("smb")) {
			String[] split = imageUri.split("<MiZ>");
			String smbPath = split[0].replace("MIZ_BG", ""), backup = split[1];
			
			SmbFile s = null;
			if (imageUri.contains("MIZ_BG")) {
				s = MizLib.getCustomCoverArt(smbPath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.BACKDROP);
			} else
				s = MizLib.getCustomCoverArt(smbPath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.COVER);
			if (s != null)
				return s.getInputStream();
			else {
				return new BufferedInputStream(new FileInputStream(backup), BUFFER_SIZE);
			}
		} else {
			if (imageUri.startsWith("/"))
				return new BufferedInputStream(new FileInputStream(imageUri), BUFFER_SIZE);
			return getStream(imageUri, extra);
		}
	}

}
