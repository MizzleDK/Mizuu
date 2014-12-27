package com.miz.utils;

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

import android.content.Context;
import android.graphics.Typeface;

import com.miz.mizuu.MizuuApplication;

public class TypefaceUtils {

    private TypefaceUtils() {} // No instantiation

    public static Typeface getRoboto(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-Regular.ttf");
    }

    public static Typeface getRobotoLight(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-Light.ttf");
    }

    public static Typeface getRobotoLightItalic(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-LightItalic.ttf");
    }

    public static Typeface getRobotoThin(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-Thin.ttf");
    }

    public static Typeface getRobotoMedium(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-Medium.ttf");
    }

    public static Typeface getRobotoMediumItalic(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-MediumItalic.ttf");
    }

    public static Typeface getRobotoBold(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-Bold.ttf");
    }

    public static Typeface getRobotoBoldItalic(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "Roboto-BoldItalic.ttf");
    }

    public static Typeface getRobotoCondensedRegular(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "RobotoCondensed-Regular.ttf");
    }

    public static Typeface getRobotoCondensedLight(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "RobotoCondensed-Light.ttf");
    }

    public static Typeface getRobotoCondensedLightItalic(Context context) {
        return MizuuApplication.getOrCreateTypeface(context, "RobotoCondensed-LightItalic.ttf");
    }
}
