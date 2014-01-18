package com.miz.functions;

import java.io.FileInputStream;
import java.io.IOException;

import jcifs.smb.SmbFile;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.Downloader;

public class PicassoDownloader implements Downloader {

	private Context mContext;
	
	public PicassoDownloader(Context context) {
		mContext = context;
	}
	
	@Override
	public Response load(Uri uri, boolean localCacheOnly) throws IOException {
		String imageUri = uri.toString();
		
		if (imageUri.startsWith("smb")) {
			String[] split = imageUri.split("<MiZ>");
			String smbPath = split[0].replace("MIZ_BG", ""), backup = split[1];
			
			SmbFile s = null;
			if (imageUri.contains("MIZ_BG")) {
				s = MizLib.getCustomCoverArt(smbPath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.BACKDROP);
			} else
				s = MizLib.getCustomCoverArt(smbPath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, imageUri), MizLib.COVER);
			
			if (s != null)
				return new Response(s.getInputStream(), localCacheOnly);
			else {
				ContentResolver contentResolver = mContext.getContentResolver();
				return new Response(contentResolver.openInputStream(Uri.parse(backup)), localCacheOnly);
			}
			
		} else {			
			return new Response(new FileInputStream(imageUri), localCacheOnly);
		}
	}

}
