package com.miz.functions;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.PaletteAsyncListener;
import android.support.v7.graphics.PaletteItem;
import android.view.View;

import com.miz.mizuu.MizuuApplication;
import com.squareup.picasso.Transformation;

public class PaletteTransformation implements Transformation {

	private String mKey;
	private View[] mViews;
	private boolean mHasSetBackground = false;

	public PaletteTransformation(String key, View... views) {
		mKey = key;
		mViews = views;

		Palette p = MizuuApplication.getPalette(mKey);
		if (p != null) {
			setBackgroundColor(p, false);
		}
	}

	@Override
	public String key() {
		return mKey;
	}

	@Override
	public Bitmap transform(Bitmap arg0) {
		if (!mHasSetBackground)
			Palette.generateAsync(arg0, new PaletteAsyncListener() {
				@Override
				public void onGenerated(Palette arg0) {
					// Cache the Palette
					MizuuApplication.addToPaletteCache(mKey, arg0);

					setBackgroundColor(arg0, true);
				}
			});

		return arg0;
	}

	private void setBackgroundColor(Palette p, boolean animate) {
		if (mViews == null || mHasSetBackground)
			return;

		PaletteItem pi = p.getDarkVibrantColor();

		if (pi == null)
			pi = p.getDarkMutedColor();

		if (pi == null)
			pi = p.getVibrantColor();

		if (pi != null) {
			if (animate) {
				for (View v : mViews) {
					if (v != null) {
						ObjectAnimator backgroundColorAnimator = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), 0xFF666666, pi.getRgb());
						backgroundColorAnimator.setDuration(500);
						backgroundColorAnimator.start();
					}
				}
			} else {
				for (View v : mViews)
					if (v != null)
						v.setBackgroundColor(pi.getRgb());
			}
		}

		mHasSetBackground = true;
	}
}