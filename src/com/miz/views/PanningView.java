/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	private boolean mDetached = false;

	public PanningView(Context context) {
		this(context, null);
	}

	public PanningView(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public PanningView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
		super.setScaleType(ScaleType.MATRIX);
		mAttacher = new PanningViewAttacher(this);
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		if (!mDetached) {
			super.setImageDrawable(drawable);
			stopUpdateStartIfNecessary();
		}
	}

	@Override
	public void setImageResource(int resId) {
		if (!mDetached) {
			super.setImageResource(resId);
			stopUpdateStartIfNecessary();
		}
	}

	@Override
	public void setImageURI(Uri uri) {
		if (!mDetached) {
			super.setImageURI(uri);
			stopUpdateStartIfNecessary();
		}
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
			stopPanning();
			super.setScaleType(scaleType);
		} else
			throw new UnsupportedOperationException("only matrix scaleType is supported");
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mDetached = true;
		mAttacher.cleanup();
	}

	public void startPanning() {
		mAttacher.startPanning();
	}

	public void stopPanning() {
		mAttacher.stopPanning();
	}
}