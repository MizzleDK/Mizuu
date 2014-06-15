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

import java.io.File;

import android.content.Context;

public class DbEpisode {

	private Context context;
	private String filepath, rowId, showId, episodeCoverPath;

	public DbEpisode(Context context, String filepath, String rowId, String showId, String episodeCoverPath) {
		this.context = context;
		this.filepath = filepath;
		this.rowId = rowId;
		this.showId = showId;
		this.episodeCoverPath = episodeCoverPath;
	}

	public String getFilepath() {
		if (filepath.contains("smb") && filepath.contains("@"))
			return "smb://" + filepath.substring(filepath.indexOf("@") + 1);
		return filepath.replace("/smb:/", "smb://");
	}

	public String getRowId() {
		return rowId;
	}

	public String getShowId() {
		return showId;
	}

	public String getEpisodeCoverPath() {
		return new File(MizLib.getTvShowEpisodeFolder(context), episodeCoverPath).getAbsolutePath();
	}

	public String getThumbnail() {
		return MizLib.getTvShowThumb(context, getShowId()).getAbsolutePath();
	}

	public String getBackdrop() {
		return MizLib.getTvShowBackdrop(context, getShowId()).getAbsolutePath();
	}

	public boolean isNetworkFile() {
		return getFilepath().contains("smb:/");
	}
	
	public boolean isUpnpFile() {
		return getFilepath().startsWith("http://");
	}
	
	public boolean isUnidentified() {
		return getShowId().equals("invalid");
	}
}