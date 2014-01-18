package com.miz.functions;

import com.miz.mizuu.R;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CoverItem implements Target {
	public TextView text, subtext;
	public ImageView cover;
	public RelativeLayout layout;

	@Override
	public void onBitmapFailed(Drawable arg0) {
		text.setVisibility(View.VISIBLE);
		cover.setAlpha(0f);
		cover.setImageResource(R.drawable.loading_image);
		cover.animate().alpha(1f).setDuration(200);
	}
	@Override
	public void onBitmapLoaded(Bitmap arg0, LoadedFrom arg1) {
		if (arg1 == LoadedFrom.DISK || arg1 == LoadedFrom.NETWORK)
			cover.setAlpha(0f);
		cover.setImageBitmap(arg0);
		if (arg1 == LoadedFrom.DISK || arg1 == LoadedFrom.NETWORK)
			cover.animate().alpha(1f).setDuration(250);
	}
	@Override
	public void onPrepareLoad(Drawable arg0) {}
}