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

import com.miz.mizuu.R;

public class WebMovie {

	private String mTitle, mId, mUrl, mDate;
	private Context mContext;
	private boolean mInLibrary;

	public WebMovie(Context context, String title, String id, String url, String date) {
		mContext = context;
		mTitle = title;
		mId = id;
		mUrl = url;
		mDate = date;
	}

	public String getTitle() { return mTitle; }
	public String getId() { return mId; }
	public String getUrl() { return mUrl; }
	public String getRawDate() { return mDate; }
	public boolean isInLibrary() { return mInLibrary; }

	public String getDate() {
		if (mDate.equals("null")) 
			return mContext.getString(R.string.stringNA);
		return mDate;
	}
	
	public String getSubtitle() {
		return MizLib.getPrettyDate(mContext, getDate()) + (isInLibrary() ? " (" + mContext.getString(R.string.inLibrary) + ")" : "");
	}
	
	public void setInLibrary(boolean inLibrary) {
		mInLibrary = inLibrary;
	}
}