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

import android.net.Uri;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;

/**
 * Changes the 'file://' Uri to '/' in order to make use of
 * the custom Downloader.
 * @author Michell
 *
 */
public class FileRequestTransformer implements Picasso.RequestTransformer {
	@Override
	public Request transformRequest(Request arg0) {
        if (arg0.resourceId > 0)
            return arg0;

		return arg0.buildUpon().setUri(Uri.parse(arg0.uri.toString().replace("file://", "/"))).build();
	}
}