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

import android.support.v4.app.Fragment;

public class SpinnerItem {
	
	private Fragment mFragment;
	private String mTitle, mSubtitle;
	
	public SpinnerItem(String title, String subtitle) {
		mTitle = title;
		mSubtitle = subtitle;
	}
	
	public SpinnerItem(String title, String subtitle, Fragment fragment) {
		mTitle = title;
		mSubtitle = subtitle;
		mFragment = fragment;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getSubtitle() {
		return mSubtitle;
	}
	
	public Fragment getFragment() {
		return mFragment;
	}
}