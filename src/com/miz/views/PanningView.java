package com.miz.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by f.laurent on 25/07/13.
 * Modified by Michell Bak on 18/07/14 to support centerCrop ScaleType in certain cases.
 */
public class PanningView extends ImageView {

	private final PanningViewAttacher mAttacher;

	private int mPanningDurationInMs = PanningViewAttacher.DEFAULT_PANNING_DURATION_IN_MS;

	public PanningView(Context context) {
		this(context, null);
	}

	public PanningView(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public PanningView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
		super.setScaleType(ScaleType.MATRIX);
		mAttacher = new PanningViewAttacher(this, mPanningDurationInMs);
	}

	@Override
	// setImageBitmap calls through to this method
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		stopUpdateStartIfNecessary();
	}

	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);
		stopUpdateStartIfNecessary();
	}

	@Override
	public void setImageURI(Uri uri) {
		super.setImageURI(uri);
		stopUpdateStartIfNecessary();
	}

	private void stopUpdateStartIfNecessary() {
		if (null != mAttacher) {
			boolean wasPanning = mAttacher.isPanning();
			mAttacher.stopPanning();
			mAttacher.update();
			if(wasPanning) {
				mAttacher.startPanning();
			}
		}
	}

	@Override
	public void setScaleType(ScaleType scaleType) {
		if (scaleType == ScaleType.CENTER_CROP) {
			super.setScaleType(scaleType);
			stopPanning();
		} else
			throw new UnsupportedOperationException("only matrix scaleType is supported");
	}

	@Override
	protected void onDetachedFromWindow() {
		mAttacher.cleanup();
		super.onDetachedFromWindow();
	}

	public void startPanning() {
		mAttacher.startPanning();
	}

	public void stopPanning() {
		mAttacher.stopPanning();
	}
}