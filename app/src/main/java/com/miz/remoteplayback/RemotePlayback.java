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

package com.miz.remoteplayback;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.miz.base.MizActivity;
import com.miz.mizuu.R;

public class RemotePlayback extends MizActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get Intent extras
		Bundle extras = getIntent().getExtras();
		final String videoUrl = extras.getString("videoUrl");
		final String coverUrl = extras.getString("coverUrl");
		final String title = extras.getString("title");
        final String id = extras.getString("id");
        final String type = extras.getString("type", "movie");

		final FragmentManager fm = getSupportFragmentManager();
		
		if (fm.findFragmentById(android.R.id.content) == null) {
			fm.beginTransaction().add(android.R.id.content, RemotePlaybackFragment.newInstance(videoUrl, coverUrl, title, id, type)).commit();
		}
	}

    @Override
    protected int getLayoutResource() {
        return R.layout.empty_layout_with_toolbar;
    }

}