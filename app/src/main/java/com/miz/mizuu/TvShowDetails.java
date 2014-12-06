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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.TvShowDetailsFragment;
import com.miz.utils.ViewUtils;

public class TvShowDetails extends MizActivity {

    private static String TAG = "TvShowDetailsFragment";
    private String mShowId;

    @Override
    protected int getLayoutResource() {
        return 0;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme
        setTheme(R.style.Mizuu_Theme_NoBackground);

        ViewUtils.setupWindowFlagsForStatusbarOverlay(getWindow(), true);

        setTitle(null);

        // Fetch the database ID of the TV show to view
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            mShowId = getIntent().getStringExtra(SearchManager.EXTRA_DATA_KEY);
        } else {
            mShowId = getIntent().getStringExtra("showId");
        }

        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, TvShowDetailsFragment.newInstance(mShowId), TAG);
            ft.commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                finish();
            }
        } else if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, getString(R.string.updatedTvShow), Toast.LENGTH_SHORT).show();

                // Create a new Intent with the Bundle
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), TvShowDetails.class);
                intent.putExtra("showId", mShowId);

                // Start the Intent for result
                startActivity(intent);

                finish();
            }
        }
    }
}