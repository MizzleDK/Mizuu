package com.miz.functions;

import com.miz.mizuu.R;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CoverItem implements Target {

	public TextView text, subtext;
	public ImageView cover;
	public RelativeLayout layout;
	private int animationDuration = 200;

	@Override
	public void onBitmapFailed(Drawable arg0) {
		cover.setImageResource(R.drawable.loading_image);
		ObjectAnimator.ofFloat(cover, "alpha", 0f, 1f).setDuration(animationDuration).start();
	}
	@Override
	public void onBitmapLoaded(Bitmap arg0, LoadedFrom arg1) {
		cover.setImageBitmap(arg0);
		ObjectAnimator.ofFloat(cover, "alpha", 0f, 1f).setDuration(animationDuration).start();
	}
	@Override
	public void onPrepareLoad(Drawable arg0) {}
}