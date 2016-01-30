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

public class MenuItem {
	
	public static final int HEADER = 0, SEPARATOR = 1000, SEPARATOR_EXTRA_PADDING = 1100, SUB_HEADER = 1500, SECTION = 2000, SETTINGS_AREA = 4000;
	
	private int mType, mCount, mFragment, mIcon;
	private String mTitle;

    public MenuItem(int type) {
        mType = type;
    }

	public MenuItem(String title, int type, int icon) {
		mTitle = title;
		mType = type;
		mIcon = icon;
	}
	
	public MenuItem(String title, int count, int type, int fragment, int icon) {
		mTitle = title;
		mCount = count;
		mType = type;
		mFragment = fragment;
        mIcon = icon;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getCount() {
		return mCount;
	}
	
	public int getType() {
		return mType;
	}
	
	public int getFragment() {
		return mFragment;
	}
	
	public int getIcon() {
		return mIcon;
	}
}