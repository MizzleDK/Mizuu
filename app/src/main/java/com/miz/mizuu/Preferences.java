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

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;

import com.miz.functions.MizLib;

import java.lang.reflect.Field;
import java.util.List;

public class Preferences extends PreferenceActivity {

    private View mBreadcrumb;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.Mizuu_Theme_Preference);

        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        if (MizLib.hasLollipop()) {
            // Lollipop has some strange colors for the breadcrumb title,
            // so we're fixing it using reflection
            mBreadcrumb = findViewById(android.R.id.title);
            if (mBreadcrumb == null) {
                // Single pane layout
                return;
            }

            try {
                final Field titleColor = mBreadcrumb.getClass().getDeclaredField("mTextColor");
                titleColor.setAccessible(true);
                titleColor.setInt(mBreadcrumb, Color.WHITE);
            } catch (final Exception ignored) {}
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}