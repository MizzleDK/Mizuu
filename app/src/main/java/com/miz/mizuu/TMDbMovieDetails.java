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

package com.miz.mizuu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.TmdbMovieDetailsFragment;
import com.miz.utils.ViewUtils;

public class TMDbMovieDetails extends MizActivity {

    private static String TAG = "TmdbMovieDetailsFragment";
    private String mMovieId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme
        setTheme(R.style.Mizuu_Theme_NoBackground);

        ViewUtils.setupWindowFlagsForStatusbarOverlay(getWindow(), true);

        setTitle(null);

        mMovieId = getIntent().getExtras().getString("tmdbId");

        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, TmdbMovieDetailsFragment.newInstance(mMovieId), TAG);
            ft.commit();
        }
    }

    @Override
    protected int getLayoutResource() {
        return 0;
    }
}