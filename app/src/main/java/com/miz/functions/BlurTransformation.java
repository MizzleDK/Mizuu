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

package com.miz.functions;

import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.squareup.picasso.Transformation;

public class BlurTransformation implements Transformation {

	private final Context mContext;
	private final String mKey;
	private final int mBlurRadius;
	
	public BlurTransformation(Context context, String key, int blurRadius) {
		mContext = context;
		mKey = key;
		mBlurRadius = blurRadius;
	}
	
	@Override
	public String key() {
		return mKey;
	}

	@Override
	public Bitmap transform(Bitmap source) {
		Bitmap blur;

        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PreferenceKeys.USE_FAST_BLUR, true))
            blur = MizLib.fastBlur(mContext, Bitmap.createScaledBitmap(source, source.getWidth() / 2, source.getHeight() / 2, true), mBlurRadius);
        else
            blur = MizLib.slowBlur(Bitmap.createScaledBitmap(source, source.getWidth() / 2, source.getHeight() / 2, true), mBlurRadius);

        source.recycle();
		
		return blur;
	}

}
