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

package com.miz.base;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.miz.functions.MizLib;
import com.miz.mizuu.R;

public abstract class MizActivity extends ActionBarActivity {

    public Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getLayoutResource() > 0) {
            setContentView(getLayoutResource());

            mToolbar = (Toolbar) findViewById(R.id.toolbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
            }
        }
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        try {
            if (MizLib.hasLollipop())
                toolbar.setElevation(1f);

            super.setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // Samsung pls...
        }
    }

    protected abstract int getLayoutResource();
}
