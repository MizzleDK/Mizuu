package com.miz.mizuu;

import java.util.HashMap;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.functions.CifsImageDownloader;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.L;

public class MizuuApplication extends Application implements OnSharedPreferenceChangeListener {

	private static boolean mLoadWhileScrolling;
	private static PauseOnScrollListener mPauseOnScrollListener;
	private static DbAdapterTvShow dbTvShow;
	private static DbAdapterTvShowEpisode dbTvShowEpisode;
	private static DbAdapterSources dbSources;
	private static DbAdapter db;
	private static HashMap<String, String[]> map = new HashMap<String, String[]>();

	@Override
	public void onCreate() {
		super.onCreate();
		
		jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false");

		if (!(0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)))
			Crashlytics.start(this);

		initImageLoader(getApplicationContext());

		// Database setup
		dbTvShow = new DbAdapterTvShow(this);
		dbTvShowEpisode = new DbAdapterTvShowEpisode(this);
		dbSources = new DbAdapterSources(this);
		db = new DbAdapter(this);

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		mLoadWhileScrolling = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefsLoadImagesWhileScrolling", false);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		dbTvShow.close();
		dbTvShowEpisode.close();
		dbSources.close();
		db.close();

		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	public static void initImageLoader(Context context) {

		if (!(0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)))
			L.disableLogging();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
		.threadPoolSize(4)
		.threadPriority(Thread.NORM_PRIORITY)
		.denyCacheImageMultipleSizesInMemory()
		.memoryCacheSizePercentage(20)
		.imageDownloader(new CifsImageDownloader(context))
		.discCacheFileNameGenerator(new Md5FileNameGenerator())
		.tasksProcessingOrder(QueueProcessingType.FIFO)
		.build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	public static DisplayImageOptions getDefaultCoverLoadingOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.gray)
		.showImageOnFail(R.drawable.loading_image)
		.cacheInMemory(true)
		.showImageForEmptyUri(R.drawable.loading_image)
		.cacheOnDisc(false)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		return options;
	}

	public static DisplayImageOptions getDefaultActorLoadingOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.gray)
		.showImageOnFail(R.drawable.noactor)
		.showImageForEmptyUri(R.drawable.noactor)
		.cacheInMemory(true)
		.cacheOnDisc(false)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		return options;
	}

	public static DisplayImageOptions getDefaultBackdropLoadingOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.gray)
		.showImageOnFail(R.drawable.nobackdrop)
		.showImageForEmptyUri(R.drawable.nobackdrop)
		.cacheInMemory(true)
		.cacheOnDisc(false)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		return options;
	}

	public static DisplayImageOptions getBackdropLoadingOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showImageOnFail(R.drawable.bg)
		.cacheInMemory(true)
		.showImageForEmptyUri(R.drawable.bg)
		.cacheOnDisc(false)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		return options;
	}

	public static DbAdapterTvShow getTvDbAdapter() {
		return dbTvShow;
	}

	public static DbAdapterTvShowEpisode getTvEpisodeDbAdapter() {
		return dbTvShowEpisode;
	}

	public static DbAdapterSources getSourcesAdapter() {
		return dbSources;
	}

	public static DbAdapter getMovieAdapter() {
		return db;
	}

	public static String[] getCifsFilesList(String parentPath) {
		return map.get(parentPath);
	}

	public static void putCifsFilesList(String parentPath, String[] list) {
		if (!map.containsKey(parentPath))
			map.put(parentPath, list);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("prefsLoadImagesWhileScrolling")) {
			mLoadWhileScrolling = sharedPreferences.getBoolean("prefsLoadImagesWhileScrolling", false);
		}
	}

	public static PauseOnScrollListener getPauseOnScrollListener(ImageLoader imageLoader) {
		if (mLoadWhileScrolling)
			mPauseOnScrollListener = new PauseOnScrollListener(imageLoader, false, false);
		else
			mPauseOnScrollListener = new PauseOnScrollListener(imageLoader, true, true);

		return mPauseOnScrollListener;
	}
}