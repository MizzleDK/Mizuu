package com.miz.functions;

import android.graphics.Bitmap;

public interface TvShowLibraryUpdateCallback {
	void onTvShowAdded(String showId, String title, Bitmap cover, Bitmap backdrop, int count);
	void onEpisodeAdded(String showId, String title, Bitmap cover, Bitmap photo);
}
