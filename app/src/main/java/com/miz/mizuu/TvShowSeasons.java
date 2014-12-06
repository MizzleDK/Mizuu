package com.miz.mizuu;/*
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.miz.base.MizActivity;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.TvShowSeasonsFragment;
import com.miz.utils.ViewUtils;

public class TvShowSeasons extends MizActivity {

    private static String TAG = "TvShowDetailsFragment";
    private String mShowId;
    private int mToolbarColor;

    @Override
    protected int getLayoutResource() {
        return R.layout.empty_layout_with_toolbar;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Mizuu_Theme_NoBackground);

        if (MizLib.isPortrait(this)) {
            getWindow().setBackgroundDrawableResource(R.drawable.bg);
        }

        setTitle(getIntent().getStringExtra("showTitle"));
        mShowId = getIntent().getStringExtra("showId");
        mToolbarColor = getIntent().getExtras().getInt(IntentKeys.TOOLBAR_COLOR);

        int count = MizuuApplication.getTvEpisodeDbAdapter().getSeasonCount(mShowId);
        getSupportActionBar().setSubtitle(count + " " + getResources().getQuantityString(R.plurals.seasons, count, count));

        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.content, TvShowSeasonsFragment.newInstance(mShowId, mToolbarColor), TAG);
            ft.commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.setToolbarAndStatusBarColor(getSupportActionBar(), getWindow(), mToolbarColor);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
