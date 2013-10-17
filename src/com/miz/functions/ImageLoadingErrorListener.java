package com.miz.functions;

import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class ImageLoadingErrorListener extends SimpleImageLoadingListener {

	private String text = "", fileUrl;
	private DisplayImageOptions options;

	public ImageLoadingErrorListener(String text, String fileUrl, DisplayImageOptions options) {
		this.text = text;
		this.fileUrl = fileUrl;
		this.options = options;
	}

	@Override
	public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
		if (!text.isEmpty())
			MizLib.setTextOfCoverView(text, view);

		if (!MizLib.isEmpty(fileUrl))
			ImageLoader.getInstance().displayImage(fileUrl, (ImageView) view, options);
	}
}