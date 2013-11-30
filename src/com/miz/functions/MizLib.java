package com.miz.functions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShow;
import com.miz.db.DbAdapterTvShowEpisode;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.Support;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowEpisode;
import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.UpdateMovieService;
import com.miz.service.UpdateShowsService;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.apache.OkApacheClient;

@SuppressLint("NewApi")
public class MizLib {

	public static final String tvdbLanguages = "en,sv,no,da,fi,nl,de,it,es,fr,pl,hu,el,tr,ru,he,ja,pt,zh,cs,sl,hr,ko";
	public static final String allFileTypes = ".3gp.aaf.mp4.ts.webm.m4v.mkv.divx.xvid.rec.avi.flv.f4v.moi.mpeg.mpg.mts.m2ts.ogv.rm.rmvb.mov.wmv.iso.vob.ifo.wtv.pyv";
	public static final String IMAGE_CACHE_DIR = "thumbs";
	public static final String TMDB_BASE_URL = "http://d3gtl9l2a4fn1j.cloudfront.net/t/p/";
	public static final String TMDB_API = "8f5f9f44983b8af692aae5f9974500f8";
	public static final String TVDBAPI = "1CB9725D261FAF38";
	public static final String YOUTUBE_API = "AIzaSyACKcfmngguy_PhREycetiispyMZ4fLPDY";
	public static final String TRAKT_API = "4f1093165b7b59c887526ce7abe365e8550c258d";
	public static final String CHARACTER_REGEX = "[^\\w\\s]";
	public static final String[] prefixes = new String[]{"the ", "a ", "an "};

	public static final int SECOND = 1000;
	public static final int MINUTE = 60 * SECOND;
	public static final int HOUR = 60 * MINUTE;
	public static final int DAY = 24 * HOUR;
	public static final int WEEK = 7 * DAY;

	public static String[] getPrefixes(Context c) {
		ArrayList<String> prefixesArray = new ArrayList<String>();
		String prefix = c.getString(R.string.prefixes);

		String[] split = prefix.split(",");
		int count = split.length;
		for (int i = 0; i < count; i++)
			prefixesArray.add(split[i]);

		count = prefixes.length;
		for (int i = 0; i < count; i++)
			prefixesArray.add(prefixes[i]);

		return prefixesArray.toArray(new String[]{});
	}

	public static boolean isVideoFile(String s) {
		String[] fileTypes = new String[]{".3gp",".aaf.","mp4",".ts",".webm",".m4v",".mkv",".divx",".xvid",".rec",".avi",".flv",".f4v",".moi",".mpeg",".mpg",".mts",".m2ts",".ogv",".rm",".rmvb",".mov",".wmv",".iso",".vob",".ifo",".wtv",".pyv"};
		int count = fileTypes.length;
		for (int i = 0; i < count; i++)
			if (s.endsWith(fileTypes[i]))
				return true;
		return false;
	}

	/**
	 * Converts the first character of a String to upper case.
	 * @param s (input String)
	 * @return Input String with first character in upper case.
	 */
	public static String toCapitalFirstChar(String s) {
		if (s == null) return s;
		else {
			if (!s.isEmpty()) return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1, s.length());
			else return s;
		}
	}

	/**
	 * Converts the first character of all words (separated by space)
	 * in the String to upper case.
	 * @param s (input String)
	 * @return Input string with first character of all words in upper case.
	 */
	public static String toCapitalWords(String s) {
		if (s == null) return s;
		else {
			if (!s.isEmpty()) {
				String result = "";
				String[] split = s.split("\\s");
				int count = split.length;
				for (int i = 0; i < count; i++) result += toCapitalFirstChar(split[i]) + " ";
				if (result.endsWith(" ")) result = result.substring(0, result.length() - 1);
				return result;
			} else return s;
		}
	}

	/**
	 * Adds spaces between capital characters.
	 * @param s (input String)
	 * @return Input string with spaces between capital characters.
	 */
	public static String addSpaceByCapital(String s) {
		if (s == null) return "";
		else {
			String result = "";
			if (!s.isEmpty()) {
				char[] chars = s.toCharArray();
				for (int i = 0; i < chars.length; i++)
					if (chars.length > (i+1))
						if (Character.isUpperCase(chars[i]) && (Character.isLowerCase(chars[i+1]) && !Character.isSpaceChar(chars[i+1])))
							result += " " + chars[i];
						else
							result += chars[i];
					else
						result += chars[i];
			}
			return result.trim();
		}
	}

	/**
	 * Returns any digits (numbers) in a String
	 * @param s (Input string)
	 * @return A string with any digits from the input string
	 */
	public static String getNumbersInString(String s) {
		if (s == null) return s;
		else {
			String result = "";
			if (!s.isEmpty()) {
				char[] charArray = s.toCharArray();
				int count = charArray.length;
				for (int i = 0; i < count; i++)
					if (Character.isDigit(charArray[i])) result += charArray[i];
			}
			return result;
		}
	}

	public static int getCharacterCountInString(String source, char c) {
		int result = 0;
		if (!isEmpty(source))
			for (int i = 0; i < source.length(); i++)
				if (source.charAt(i) == c)
					result++;
		return result;
	}

	/**
	 * Determines if the application is running on a tablet
	 * @param c - Context of the application
	 * @return True if running on a tablet, false if on a smartphone
	 */
	public static boolean runsOnTablet(Context c) {
		return (c.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Determines if the application is running on a xlarge tablet
	 * @param c - Context of the application
	 * @return True if running on a xlarge tablet, false if on a smaller device
	 */
	public static boolean isXlargeTablet(Context c) {
		return (c.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines if the device is in portrait mode
	 * @param c - Context of the application
	 * @return True if portrait mode, false if landscape mode
	 */
	public static boolean runsInPortraitMode(Context c) {
		if (c == null) return false;
		return (c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? true : false;
	}

	/**
	 * Determines if the device is currently connected to a network
	 * @param c - Context of the application
	 * @return True if connected to a network, else false
	 */
	public static boolean isOnline(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		int count = netInfo.length;
		for (int i = 0; i < count; i++)
			if (netInfo[i] != null && netInfo[i].isConnected()) return true;
		return false;
	}

	/**
	 * Determines if the device is currently connected to a WiFi or Ethernet network
	 * @param c - Context of the application
	 * @return True if connected to a network, else false
	 */
	public static boolean isWifiConnected(Context c, boolean disableCheck) {
		if (c!= null) {
			if (disableCheck)
				return isOnline(c);

			ConnectivityManager connManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo[] connections = connManager.getAllNetworkInfo();
			int count = connections.length;
			for (int i = 0; i < count; i++)
				if (connections[i]!= null && connections[i].getType() == ConnectivityManager.TYPE_WIFI && connections[i].isConnectedOrConnecting() ||
				connections[i]!= null &&  connections[i].getType() == ConnectivityManager.TYPE_ETHERNET && connections[i].isConnectedOrConnecting())
					return true;
		}
		return false;
	}

	/**
	 * Returns a custom theme background image as Bitmap.
	 * @param height
	 * @param width
	 * @return Bitmap with the background image
	 */
	public static Bitmap getCustomThemeBackground(int height, int width, String url) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.RGB_565;
			options.inDither = true;

			Bitmap bm = BitmapFactory.decodeFile(url, options);

			float scaleWidth = bm.getWidth() / ((float) width);
			float scaleHeight = bm.getHeight() / ((float) height);

			if (scaleWidth > scaleHeight) bm = Bitmap.createScaledBitmap(bm, (int)(bm.getWidth() / scaleHeight), (int)(bm.getHeight() / scaleHeight), true);
			else bm = Bitmap.createScaledBitmap(bm, (int)(bm.getWidth() / scaleWidth), (int)(bm.getHeight() / scaleWidth), true);

			bm = Bitmap.createBitmap(bm, (bm.getWidth() - width) / 2, (bm.getHeight() - height) / 2, width, height);
			return bm;
		} catch (Exception e) {
			return null;
		}
	}

	public static final long GB = 1000 * 1000 * 1000;
	public static final long MB = 1000 * 1000;
	public static final long KB = 1000;

	/**
	 * Returns the input file size as a string in either KB, MB or GB
	 * @param size (as bytes)
	 * @return Size as readable string
	 */
	public static String filesizeToString(long size) {
		if ((size / GB) >= 1) return substring(String.valueOf(((double) size / (double) GB)), 4) + " GB"; // GB
		else if ((size / MB) >= 1) return (size / MB) + " MB"; // MB
		else return (size / KB) + " KB"; // KB
	}

	/**
	 * Returns a string with a length trimmed to the specified max length
	 * @param s
	 * @param maxLength
	 * @return String with a length of the specified max length
	 */
	public static String substring(String s, int maxLength) {
		if (s.length() >= maxLength) return s.substring(0, maxLength);
		else return s;
	}

	/**
	 * Launches an intent to contact the developer (me!)
	 * @param context
	 */
	public static void contactDev(Context context) {
		context.startActivity(new Intent(Intent.ACTION_VIEW).setClass(context, Support.class));
	}

	/**
	 * Add a padding with a height of the ActionBar to the top of a given View
	 * @param c
	 * @param v
	 */
	public static void addActionBarPadding(Context c, View v) {
		int mActionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
		else
			mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

		v.setPadding(0, mActionBarHeight, 0, 0);
	}

	/**
	 * Add a margin with a height of the ActionBar to the top of a given View contained in a FrameLayout
	 * @param c
	 * @param v
	 */
	public static void addActionBarMargin(Context c, View v) {
		int mActionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
		else
			mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.setMargins(0, mActionBarHeight, 0, 0);
		v.setLayoutParams(params);
	}

	public static void addNavigationBarPadding(Context c, View v) {
		v.setPadding(0, 0, 0, getNavigationBarHeight(c));
	}

	public static void addNavigationBarMargin(Context c, View v) {
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.setMargins(0, 0, 0, getNavigationBarHeight(c));
		v.setLayoutParams(params);
	}

	public static boolean isGoogleTV(Context context) {
		return context.getPackageManager().hasSystemFeature("com.google.android.tv");
	}

	public static boolean isGoogleTV_720p(Context c) {
		return (isGoogleTV(c) && c.getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_TV);
	}

	public static boolean isGoogleTV_1080p(Context c) {
		return (isGoogleTV(c) && c.getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_XHIGH);
	}

	public static boolean hasFroyo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean hasGingerbread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	public static boolean hasHoneycombMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2;
	}

	public static boolean hasICS() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

	public static boolean hasJellyBeanMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	public static boolean hasJellyBeanMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}

	public static boolean hasKitKat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	public static int getThumbnailNotificationSize(Context c) {
		Resources r = c.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics());
	}

	public static Bitmap getNotificationImageThumbnail(Context c, String filepath) {
		int size = getThumbnailNotificationSize(c);

		try {
			Bitmap bm = decodeSampledBitmapFromFile(filepath, size, size);
			bm = Bitmap.createScaledBitmap(bm, size, (int) (size * 1.5), true);
			bm = Bitmap.createBitmap(bm, 0, 0, size, size);

			return bm;
		} catch (Exception e) {
			return decodeSampledBitmapFromResource(c.getResources(), R.drawable.refresh, size, size);
		}
	}

	public static int getLargeNotificationWidth(Context c) {
		Resources r = c.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, r.getDisplayMetrics());
	}

	public static int getMenuWidth(Context c) {
		Resources r = c.getResources();
		if (MizLib.runsOnTablet(c)) {
			return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, r.getDisplayMetrics());
		} else 
			return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, r.getDisplayMetrics());
	}

	public static int convertDpToPixels(Context c, int dp) {
		Resources r = c.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static int getActionBarHeight(Context c) {
		int mActionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
		else
			mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

		return mActionBarHeight;
	}

	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static int getGreatestNumber(int a, int b) {
		return (a > b) ? a : b;
	}

	public static int getLowestNumber(int a, int b) {
		return (a > b) ? b : a;
	}

	public static int getThumbnailSize(Context c) {
		final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int numColumns = (int) Math.floor(getGreatestNumber(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

		if (numColumns > 0) {
			final int columnWidth = (getGreatestNumber(size.x, size.y) / numColumns) - mImageThumbSpacing;

			if (columnWidth > 320)
				return 440;
			else if (columnWidth > 240)
				return 320;
			else if (columnWidth > 180)
				return 240;
		}

		return 180;
	}

	public static void resizeBitmapFileToCoverSize(Context c, String filepath) {
		final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int numColumns = (int) Math.floor(getGreatestNumber(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

		if (numColumns > 0) {
			final int columnWidth = (getGreatestNumber(size.x, size.y) / numColumns) - mImageThumbSpacing;

			int imageWidth = 0;

			if (columnWidth > 300)
				imageWidth = 500;
			else if (columnWidth > 240)
				imageWidth = 320;
			else if (columnWidth > 180)
				imageWidth = 240;
			else
				imageWidth = 180;

			if (new File(filepath).exists())
				try {
					Bitmap bm = decodeSampledBitmapFromFile(filepath, imageWidth, (int) (imageWidth * 1.5));
					FileOutputStream out = new FileOutputStream(filepath);
					bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
				} catch (Exception e) {}
		}
	}

	public static String getImageUrlSize(Context c) {
		final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int numColumns = (int) Math.floor(getGreatestNumber(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

		if (numColumns > 0) {
			final int columnWidth = (getGreatestNumber(size.x, size.y) / numColumns) - mImageThumbSpacing;

			if (columnWidth > 300)
				return "w500";
			else if (columnWidth > 185)
				return "w300";
		}

		return "w185";
	}

	public static String getBackdropUrlSize(Context c) {
		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int width = getLowestNumber(size.x, size.y);

		if (width > 1280)
			return "original";
		else if (width > 780)
			return "w1280";
		else
			return "w780";
	}

	public static String getBackdropThumbUrlSize(Context c) {
		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int width = getLowestNumber(size.x, size.y);

		if (width >= 1200)
			return "w780";
		else
			return "w300";
	}

	public static String getActorUrlSize(Context c) {
		final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display d = window.getDefaultDisplay();

		Point size = new Point();
		d.getSize(size);

		final int numColumns = (int) Math.floor(getGreatestNumber(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

		if (numColumns > 0) {
			final int columnWidth = (getGreatestNumber(size.x, size.y) / numColumns) - mImageThumbSpacing;

			if (columnWidth > 400)
				return "h632";
		}

		return "w185";
	}

	public static Intent getVideoIntent(String fileUrl, boolean useWildcard, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(Uri.fromFile(new File(fileUrl)), getMimeType(fileUrl, useWildcard, false));
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(Uri file, boolean useWildcard, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(file, getMimeType(file.getPath(), useWildcard, false));
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(String fileUrl, String mimeType, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(Uri.fromFile(new File(fileUrl)), mimeType);
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	public static Intent getVideoIntent(Uri file, String mimeType, Object videoObject) {
		Intent videoIntent = new Intent(Intent.ACTION_VIEW);
		videoIntent.setDataAndType(file, mimeType);
		videoIntent.putExtras(getVideoIntentBundle(videoObject));

		return videoIntent;
	}

	private static Bundle getVideoIntentBundle(Object videoObject) {
		Bundle b = new Bundle();
		String title = "";
		if (videoObject instanceof Movie) {
			title = ((Movie) videoObject).getTitle();
			b.putString("plot", ((Movie) videoObject).getPlot());
			b.putString("date", ((Movie) videoObject).getReleasedate());
			b.putDouble("rating", ((Movie) videoObject).getRawRating());
			b.putString("cover", ((Movie) videoObject).getThumbnail());
			b.putString("genres", ((Movie) videoObject).getGenres());
		} else if (videoObject instanceof TvShowEpisode) {
			title = ((TvShowEpisode) videoObject).getTitle();
			b.putString("plot", ((TvShowEpisode) videoObject).getDescription());
			b.putString("date", ((TvShowEpisode) videoObject).getReleasedate());
			b.putDouble("rating", ((TvShowEpisode) videoObject).getRawRating());
			b.putString("cover", ((TvShowEpisode) videoObject).getEpisodePhoto());
			b.putString("episode", ((TvShowEpisode) videoObject).getEpisode());
			b.putString("season", ((TvShowEpisode) videoObject).getSeason());
		} else {
			title = (String) videoObject;
		}
		b.putString("title", title);
		b.putString("forcename", title);
		b.putBoolean("forcedirect", true);
		return b;
	}

	public static boolean isEmpty(String string) {
		if (string == null)
			return true;
		return string.length() == 0;
	}

	public static String getMimeType(String url, boolean useWildcard, boolean useCorrectMkvMimeType) {
		if (useWildcard)
			return "video/*";

		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);

		if (extension != null) {
			if (useCorrectMkvMimeType && url.contains(".mkv"))
				return "video/x-matroska";

			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension);

			if (isEmpty(type))
				type = "video/*"; // No MIME type found, so use the video wildcard
		}

		return type;
	}

	public static boolean checkFileTypes(String file) {
		if (file.contains(".")) { // Must have a file type
			String type = file.substring(file.lastIndexOf("."));
			String[] filetypes = allFileTypes.split("\\.");
			int count = filetypes.length;
			for (int i = 0; i < count; i++)
				if (type.toLowerCase(Locale.ENGLISH).equals("." + filetypes[i]))
					return true;
		}
		return false;
	}

	/**
	 * Returns a blurred bitmap. It uses a fast blur algorithm.
	 * @param context
	 * @param sentBitmap
	 * @param radius
	 * @return
	 */
	public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {
		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012

		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		//Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
		sir[2] = (p & 0x0000ff);
		rbs = r1 - Math.abs(i);
		rsum += sir[0] * rbs;
		gsum += sir[1] * rbs;
		bsum += sir[2] * rbs;
		if (i > 0) {
			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];
		} else {
			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];
		}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
			sir[2] = (p & 0x0000ff);

			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];

			rsum += rinsum;
			gsum += ginsum;
			bsum += binsum;

			stackpointer = (stackpointer + 1) % div;
			sir = stack[(stackpointer) % div];

			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];

			rinsum -= sir[0];
			ginsum -= sir[1];
			binsum -= sir[2];

			yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				pix[yi] = 0xff000000 | (dv[rsum] << 16) | (dv[gsum] << 8)
						| dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		//Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);

		return (bitmap);
	}

	public static boolean downloadFile(String url, String savePath) {
		// the size of my buffer in bits
		int bufferSize = 8192;
		byte[] retVal = null;
		InputStream in = null;
		OutputStream fileos = null;
		OkHttpClient client = new OkHttpClient();
		HttpURLConnection urlConnection = null;

		try {
			urlConnection = client.open(new URL(url));
			fileos = new BufferedOutputStream(new FileOutputStream(savePath));

			in = new BufferedInputStream(urlConnection.getInputStream(), bufferSize);

			retVal = new byte[bufferSize];
			int length = 0;
			while((length = in.read(retVal)) > -1) {
				fileos.write(retVal, 0, length);
			}

		} catch(IOException e) {
			return false;
		} finally {
			if(fileos != null) {
				try {
					fileos.flush();
					fileos.close();
				} catch (IOException e) {}
			}
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return true;
	}

	public static JSONObject getJSONObject(String url) {
		try {
			// Hack to work around bug with secure HTTP connections: https://github.com/square/okhttp/issues/184
			if (url.startsWith("https")) {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet(url);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				return new JSONObject(html);
			} else {
				OkApacheClient httpclient = new OkApacheClient();
				HttpGet httppost = new HttpGet(url);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);
				
				return new JSONObject(html);
			}
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	public static String getStringFromJSONObject(JSONObject json, String name, String fallback) {
		try {
			String s = json.getString(name);
			if (s.equals("null"))
				return fallback;
			return s;
		} catch (Exception e) {
			return fallback;
		}
	}

	public static String removeIndexZero(String s) {
		if (!isEmpty(s))
			try {
				return String.valueOf(Integer.parseInt(s));
			} catch (NumberFormatException e) {}
		return s;
	}

	public static String addIndexZero(String s) {
		if (isEmpty(s))
			return "00";
		try {
			return String.format(Locale.ENGLISH, "%02d", Integer.parseInt(s));
		} catch (NumberFormatException e) {
			return "00";
		}
	}

	public static String addIndexZero(int number) {
		try {
			return String.format(Locale.ENGLISH, "%02d", number);
		} catch (Exception e) {
			return "00";
		}
	}

	public static String URLEncodeUTF8(String s) {
		return Uri.parse(s).toString();
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap); 
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public static final int TYPE_MOVIE = 0, TYPE_SHOWS = 1;

	public static ArrayList<FileSource> getFileSources(int type, boolean onlyNetworkSources) {

		ArrayList<FileSource> filesources = new ArrayList<FileSource>();
		DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();

		Cursor c = null;
		if (type == TYPE_MOVIE)
			c = dbHelperSources.fetchAllMovieSources();
		else
			c = dbHelperSources.fetchAllShowSources();

		while (c.moveToNext()) {
			if (onlyNetworkSources) {
				if (c.getInt(c.getColumnIndex(DbAdapterSources.KEY_IS_SMB)) == 1) {
					filesources.add(new FileSource(
							c.getLong(c.getColumnIndex(DbAdapterSources.KEY_ROWID)),
							c.getString(c.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
							c.getInt(c.getColumnIndex(DbAdapterSources.KEY_IS_SMB)),
							c.getString(c.getColumnIndex(DbAdapterSources.KEY_USER)),
							c.getString(c.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
							c.getString(c.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
							c.getString(c.getColumnIndex(DbAdapterSources.KEY_TYPE))
							));
				}
			} else {
				filesources.add(new FileSource(
						c.getLong(c.getColumnIndex(DbAdapterSources.KEY_ROWID)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
						c.getInt(c.getColumnIndex(DbAdapterSources.KEY_IS_SMB)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_USER)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
						c.getString(c.getColumnIndex(DbAdapterSources.KEY_TYPE))
						));
			}
		}
		c.close();

		return filesources;
	}

	public static NtlmPasswordAuthentication getAuthFromFilesource(FileSource source) {
		NtlmPasswordAuthentication auth;

		if (source == null) {
			auth = NtlmPasswordAuthentication.ANONYMOUS;
		} else {
			if (source.getDomain().isEmpty() && source.getUser().isEmpty() && source.getPassword().isEmpty()) {
				auth = NtlmPasswordAuthentication.ANONYMOUS;
			} else {
				auth = new NtlmPasswordAuthentication(source.getDomain(), source.getUser(), source.getPassword());
			}
		}
		return auth;
	}

	public static NtlmPasswordAuthentication getAuthFromFilepath(int type, String filepath) {

		ArrayList<FileSource> filesources = MizLib.getFileSources(type, true);	
		FileSource source = null;

		for (int i = 0; i < filesources.size(); i++) {
			if (filepath.contains(filesources.get(i).getFilepath())) {
				source = filesources.get(i);
				continue;
			}
		}

		return getAuthFromFilesource(source);
	}

	public static int COVER = 1, BACKDROP = 2;
	public static SmbFile getCustomCoverArt(String filepath, NtlmPasswordAuthentication auth, int type) throws MalformedURLException, UnsupportedEncodingException, SmbException {

		String parentPath = filepath.substring(0, filepath.lastIndexOf("/"));
		if (!parentPath.endsWith("/"))
			parentPath += "/";

		String filename = filepath.substring(0, filepath.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();

		String[] list = MizuuApplication.getCifsFilesList(parentPath);
		if (list == null) {
			SmbFile s = new SmbFile(createSmbLoginString(URLEncoder.encode(auth.getDomain(), "utf-8"),
					URLEncoder.encode(auth.getUsername(), "utf-8"),
					URLEncoder.encode(auth.getPassword(), "utf-8"),
					parentPath,
					false));

			list = s.list();
			s = null;
			MizuuApplication.putCifsFilesList(parentPath, list);
		}

		String name = "", absolutePath = "", customCoverArt = "";

		if (type == COVER) {
			for (int i = 0; i < list.length; i++) {
				name = list[i];
				absolutePath = parentPath + list[i];
				if (name.equalsIgnoreCase("poster.jpg") ||
						name.equalsIgnoreCase("poster.jpeg") ||
						name.equalsIgnoreCase("poster.tbn") ||
						name.equalsIgnoreCase("folder.jpg") ||
						name.equalsIgnoreCase("folder.jpeg") ||
						name.equalsIgnoreCase("folder.tbn") ||
						name.equalsIgnoreCase("cover.jpg") ||
						name.equalsIgnoreCase("cover.jpeg") ||
						name.equalsIgnoreCase("cover.tbn") ||
						absolutePath.equalsIgnoreCase(filename + ".jpg") ||
						absolutePath.equalsIgnoreCase(filename + ".jpeg") ||
						absolutePath.equalsIgnoreCase(filename + ".tbn")) {
					customCoverArt = absolutePath;
					continue;
				}
			}
		} else {
			for (int i = 0; i < list.length; i++) {
				name = list[i];
				absolutePath = parentPath + list[i];
				if (name.equalsIgnoreCase("fanart.jpeg") ||
						name.equalsIgnoreCase("fanart.tbn") ||
						name.equalsIgnoreCase("banner.jpg") ||
						name.equalsIgnoreCase("banner.jpeg") ||
						name.equalsIgnoreCase("banner.tbn") ||
						name.equalsIgnoreCase("backdrop.jpg") ||
						name.equalsIgnoreCase("backdrop.jpeg") ||
						name.equalsIgnoreCase("backdrop.tbn") ||
						absolutePath.equalsIgnoreCase(filename + "-fanart.jpg") ||
						absolutePath.equalsIgnoreCase(filename + "-fanart.jpeg") ||
						absolutePath.equalsIgnoreCase(filename + "-fanart.tbn") ||
						absolutePath.equalsIgnoreCase(filename + "-banner.jpg") ||
						absolutePath.equalsIgnoreCase(filename + "-banner.jpeg") ||
						absolutePath.equalsIgnoreCase(filename + "-banner.tbn") ||
						absolutePath.equalsIgnoreCase(filename + "-backdrop.jpg") ||
						absolutePath.equalsIgnoreCase(filename + "-backdrop.jpeg") ||
						absolutePath.equalsIgnoreCase(filename + "-backdrop.tbn")) {
					customCoverArt = absolutePath;
					continue;
				}
			}
		}

		list = null;

		if (!customCoverArt.isEmpty())
			return new SmbFile(createSmbLoginString(URLEncoder.encode(auth.getDomain(), "utf-8"),
					URLEncoder.encode(auth.getUsername(), "utf-8"),
					URLEncoder.encode(auth.getPassword(), "utf-8"),
					customCoverArt,
					false));

		return null;
	}

	private static String[] subtitleFormats = new String[]{".srt", ".sub", ".ssa", ".ssf", ".smi", ".txt", ".usf", ".ass", ".stp",};

	public static List<SmbFile> getSubtitleFiles(String filepath, NtlmPasswordAuthentication auth) throws MalformedURLException, UnsupportedEncodingException {
		ArrayList<SmbFile> subs = new ArrayList<SmbFile>();

		String fileType = "";
		if (filepath.contains(".")) {
			fileType = filepath.substring(filepath.lastIndexOf("."));
		}

		int count = subtitleFormats.length;
		for (int i = 0; i < count; i++) {
			subs.add(new SmbFile(createSmbLoginString(URLEncoder.encode(auth.getDomain(), "utf-8"),
					URLEncoder.encode(auth.getUsername(), "utf-8"),
					URLEncoder.encode(auth.getPassword(), "utf-8"),
					filepath.replace(fileType, subtitleFormats[i]),
					false)));
		}

		return subs;
	}

	public static List<SmbFile> getDVDFiles(String filepath, NtlmPasswordAuthentication auth) throws MalformedURLException {
		ArrayList<SmbFile> subs = new ArrayList<SmbFile>();

		try {
			String s = filepath.substring(0, filepath.lastIndexOf("/"));
			SmbFile dir = new SmbFile(createSmbLoginString(
					URLEncoder.encode(auth.getDomain(), "utf-8"),
					URLEncoder.encode(auth.getUsername(), "utf-8"),
					URLEncoder.encode(auth.getPassword(), "utf-8"),
					s,
					true));
			SmbFile[] listFiles = dir.listFiles();
			int count = listFiles.length;
			for (int i = 0; i < count; i++) {
				if (!listFiles[i].getName().equalsIgnoreCase("video_ts.ifo"))
					subs.add(listFiles[i]);
			}
		} catch (Exception e) {}

		return subs;
	}

	/**
	 * A bit of a hack to properly delete files / folders from the OS
	 * @param f
	 * @return
	 */
	public static boolean deleteFile(File f) {
		return f.delete();
	}

	public static String createSmbLoginString(String domain, String user, String password, String server, boolean isFolder) {
		// Create the string to fit the following syntax: smb://[[[domain;]username[:password]@]server[:port]/		
		StringBuilder sb = new StringBuilder();

		sb.append("smb://");

		// Only add domain, username and password details if the username isn't empty
		if (!user.isEmpty()) {
			// Add the domain details
			if (!domain.isEmpty())
				sb.append(domain + ";");

			// Add username
			sb.append(user);

			// Add password
			if (!password.isEmpty())
				sb.append(":" + password);

			sb.append("@");
		}

		sb.append(server.replace("smb://", ""));

		if (isFolder)
			if (!server.endsWith("/"))
				sb.append("/");

		return sb.toString();
	}

	public static void deleteShow(Context c, TvShow thisShow, boolean showToast) {
		// Create and open database
		DbAdapterTvShow dbHelper = MizuuApplication.getTvDbAdapter();
		boolean deleted = dbHelper.deleteShow(thisShow.getId());

		DbAdapterTvShowEpisode db = MizuuApplication.getTvEpisodeDbAdapter();
		deleted = deleted && db.deleteAllEpisodes(thisShow.getId());

		if (deleted) {
			try {
				// Delete cover art image
				File coverArt = new File(thisShow.getCoverPhoto());
				if (coverArt.exists() && coverArt.getAbsolutePath().contains("com.miz.mizuu")) {
					MizLib.deleteFile(coverArt);
				}

				// Delete thumbnail image
				File thumbnail = new File(thisShow.getThumbnail());
				if (thumbnail.exists() && thumbnail.getAbsolutePath().contains("com.miz.mizuu")) {
					MizLib.deleteFile(thumbnail);
				}

				// Delete backdrop image
				File backdrop = new File(thisShow.getBackdrop());
				if (backdrop.exists() && backdrop.getAbsolutePath().contains("com.miz.mizuu")) {
					MizLib.deleteFile(backdrop);
				}

				// Delete episode images
				File dataFolder = getTvShowEpisodeFolder(c);
				File[] listFiles = dataFolder.listFiles();
				if (listFiles != null) {
					int count = listFiles.length;
					for (int i = 0; i < count; i++)
						if (listFiles[i].getName().startsWith(thisShow.getId() + "_S"))
							MizLib.deleteFile(listFiles[i]);
				}
			} catch (NullPointerException e) {} // No file to delete
		} else {
			if (showToast)
				Toast.makeText(c, c.getString(R.string.failedToRemoveShow), Toast.LENGTH_SHORT).show();
		}
	}

	public static String getYouTubeId(String url) {
		String pattern = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";

		Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = compiledPattern.matcher(url);

		if (matcher.find(1))
			return getYTId(matcher.group(1));

		if (matcher.find(0))
			return getYTId(matcher.group(0));

		return url;
	}

	private static String getYTId(String url) {
		String result = url;

		if (result.contains("v=")) {
			result = result.substring(result.indexOf("v=") + 2);

			if (result.contains("&")) {
				result = result.substring(0, result.indexOf("&"));
			}
		}

		if (result.contains("youtu.be/")) {
			result = result.substring(result.indexOf("youtu.be/") + 9);

			if (result.contains("&")) {
				result = result.substring(0, result.indexOf("&"));
			}
		}

		return result;
	}

	private static String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
		int count = data.length;
		for (int i = 0; i < count; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String SHA1(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			byte[] sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (Exception e) {
			return "";
		}
	}

	public static boolean checkInMovieTrakt(Movie movie, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		// Cancel the current check in to allow this one
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = new HttpPost("http://api.trakt.tv/movie/cancelcheckin/" + MizLib.TRAKT_API);
		httppost.setHeader("Content-type", "application/json");

		try {

			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);

			StringEntity se = new StringEntity(holder.toString());
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httppost.setEntity(se);

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

		} catch (Exception e) {}

		// Check in with the movie
		httpclient = new OkApacheClient();
		httppost = new HttpPost("http://api.trakt.tv/movie/checkin/" + MizLib.TRAKT_API);

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("username", username));
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("imdb_id", movie.getImdbId()));
			nameValuePairs.add(new BasicNameValuePair("tmdb_id", movie.getTmdbId()));
			nameValuePairs.add(new BasicNameValuePair("title", movie.getTitle()));
			nameValuePairs.add(new BasicNameValuePair("year", movie.getReleaseYear().replace("(", "").replace(")", "")));
			nameValuePairs.add(new BasicNameValuePair("app_version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName));
			nameValuePairs.add(new BasicNameValuePair("app_date", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean checkInEpisodeTrakt(TvShowEpisode episode, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		// Cancel the current check in to allow this one
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = new HttpPost("http://api.trakt.tv/show/cancelcheckin/" + MizLib.TRAKT_API);
		httppost.setHeader("Content-type", "application/json");

		try {

			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);

			StringEntity se = new StringEntity(holder.toString());
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httppost.setEntity(se);

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

		} catch (Exception e) {}

		// Check in with the movie
		httpclient = new OkApacheClient();
		httppost = new HttpPost("http://api.trakt.tv/show/checkin/" + MizLib.TRAKT_API);

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("username", username));
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("tvdb_id", episode.getShowId()));
			nameValuePairs.add(new BasicNameValuePair("title", ""));
			nameValuePairs.add(new BasicNameValuePair("year", ""));
			nameValuePairs.add(new BasicNameValuePair("season", episode.getSeason()));
			nameValuePairs.add(new BasicNameValuePair("episode", episode.getEpisode()));
			nameValuePairs.add(new BasicNameValuePair("app_version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName));
			nameValuePairs.add(new BasicNameValuePair("app_date", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean markMovieAsWatched(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		if (movies.size() == 0)
			return false;

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = null;
		if (movies.get(0).hasWatched())
			httppost = new HttpPost("http://api.trakt.tv/movie/seen/" + MizLib.TRAKT_API);
		else
			httppost = new HttpPost("http://api.trakt.tv/movie/unseen/" + MizLib.TRAKT_API);

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				if (movies.get(i).hasWatched()) {
					jsonMovie.put("plays", "1");
					jsonMovie.put("last_played", String.valueOf(System.currentTimeMillis() / 1000L));
				}
				array.put(jsonMovie);
			}
			json.put("movies", array);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean markEpisodeAsWatched(List<TvShowEpisode> episodes, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		if (episodes.size() == 0)
			return false;

		// Mark episode as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = null;

		if (episodes.get(0).hasWatched())
			httppost = new HttpPost("http://api.trakt.tv/show/episode/seen/" + MizLib.TRAKT_API);
		else
			httppost = new HttpPost("http://api.trakt.tv/show/episode/unseen/" + MizLib.TRAKT_API);

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);
			json.put("imdb_id", "");
			json.put("tvdb_id", episodes.get(0).getShowId());
			json.put("title", "");
			json.put("year", "");

			JSONArray array = new JSONArray();
			int count = episodes.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("season", episodes.get(i).getSeason());
				jsonMovie.put("episode", episodes.get(i).getEpisode());
				array.put(jsonMovie);
			}
			json.put("episodes", array);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean movieWatchlist(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		if (movies.size() == 0)
			return false;

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = null;
		if (movies.get(0).toWatch())
			httppost = new HttpPost("http://api.trakt.tv/movie/watchlist/" + MizLib.TRAKT_API);
		else
			httppost = new HttpPost("http://api.trakt.tv/movie/unwatchlist/" + MizLib.TRAKT_API);

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				array.put(jsonMovie);
			}
			json.put("movies", array);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean movieFavorite(List<Movie> movies, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		if (movies.size() == 0)
			return false;

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = new HttpPost("http://api.trakt.tv/rate/movies/" + MizLib.TRAKT_API);

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = movies.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonMovie = new JSONObject();
				jsonMovie.put("imdb_id", movies.get(i).getImdbId());
				jsonMovie.put("tmdb_id", movies.get(i).getTmdbId());
				jsonMovie.put("year", movies.get(i).getReleaseYear());
				jsonMovie.put("title", movies.get(i).getTitle());
				jsonMovie.put("rating", movies.get(i).isFavourite() ? "love" : "unrate");
				array.put(jsonMovie);
			}
			json.put("movies", array);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static boolean hasTraktAccount(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		return true;
	}

	public static boolean tvShowFavorite(List<TvShow> shows, Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return false;

		if (shows.size() == 0)
			return false;

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = new HttpPost("http://api.trakt.tv/rate/shows/" + MizLib.TRAKT_API);

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			JSONArray array = new JSONArray();
			int count = shows.size();
			for (int i = 0; i < count; i++) {
				JSONObject jsonShow = new JSONObject();
				jsonShow.put("tvdb_id", shows.get(i).getId());
				jsonShow.put("title", shows.get(i).getTitle());
				jsonShow.put("year", "");
				jsonShow.put("rating", shows.get(i).isFavorite() ? "love" : "unrate");
				array.put(jsonShow);
			}
			json.put("shows", array);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			httpclient.execute(httppost, responseHandler);

			return true;
		} catch (Exception e) {}

		return false;
	}

	public static int WATCHED = 1, RATINGS = 2, WATCHLIST = 3;
	public static JSONArray getTraktMovieLibrary(Context c, int type) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return new JSONArray();

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = null;
		if (type == WATCHED) {
			httppost = new HttpPost("http://api.trakt.tv/user/library/movies/watched.json/" + MizLib.TRAKT_API + "/" + username);
		} else if (type == RATINGS) {
			httppost = new HttpPost("http://api.trakt.tv/user/ratings/movies.json/" + MizLib.TRAKT_API + "/" + username + "/love");
		} else if (type == WATCHLIST) {
			httppost = new HttpPost("http://api.trakt.tv/user/watchlist/movies.json/" + MizLib.TRAKT_API + "/" + username);
		}

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(httppost, responseHandler);

			return new JSONArray(result);
		} catch (Exception e) {}

		return new JSONArray();
	}

	public static JSONArray getTraktTvShowLibrary(Context c, int type) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return new JSONArray();

		// Mark as seen / unseen
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = null;
		if (type == WATCHED) {
			httppost = new HttpPost("http://api.trakt.tv/user/library/shows/watched.json/" + MizLib.TRAKT_API + "/" + username);
		} else if (type == RATINGS) {
			httppost = new HttpPost("http://api.trakt.tv/user/ratings/shows.json/" + MizLib.TRAKT_API + "/" + username + "/love");
		}

		try {
			JSONObject json = new JSONObject();
			json.put("username", username);
			json.put("password", password);

			httppost.setEntity(new StringEntity(json.toString()));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(httppost, responseHandler);

			return new JSONArray(result);
		} catch (Exception e) {}

		return new JSONArray();
	}

	public static boolean isMovieLibraryBeingUpdated(Context c) {
		ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
		int count = services.size();
		for (int i = 0; i < count; i++) {
			if (UpdateMovieService.class.getName().equals(services.get(i).service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTvShowLibraryBeingUpdated(Context c) {
		ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
		int count = services.size();
		for (int i = 0; i < count; i++) {
			if (UpdateShowsService.class.getName().equals(services.get(i).service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public static final int HEIGHT = 100, WIDTH = 110;

	public static int getDisplaySize(Context c, int type) {
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		Point size = new Point();
		display.getSize(size);

		return type == HEIGHT ?
				size.y : size.x;
	}

	public static int getFileSizeLimit(Context c) {
		String limit = PreferenceManager.getDefaultSharedPreferences(c).getString("prefsIgnoreFilesSize", c.getString(R.string.smallFilesOption_1));
		if (limit.equals(c.getString(R.string.smallFilesOption_1))) {
			return 0;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_2))) {
			return 50 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_3))) {
			return 100 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_4))) {
			return 150 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_5))) {
			return 200 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_6))) {
			return 250 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_7))) {
			return 300 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_8))) {
			return 350 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_9))) {
			return 400 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_10))) {
			return 450 * 1024 * 1024;
		} else if (limit.equals(c.getString(R.string.smallFilesOption_11))) {
			return 500 * 1024 * 1024;
		}
		return 50 * 1024 * 1024;
	}

	public static boolean findAndUpdateSpinner(Object root, int position) {
		if (root instanceof android.widget.Spinner) {
			// Found the Spinner
			Spinner spinner = (Spinner) root;
			spinner.setSelection(position);
			return true;
		} else if (root instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) root;
			if (group.getId() != android.R.id.content) {
				// Found a container that isn't the container holding our screen layout
				for (int i = 0; i < group.getChildCount(); i++) {
					if (findAndUpdateSpinner(group.getChildAt(i), position)) {
						return true; // Found and done searching the View tree
					}
				}
			}
		}

		return false; // Nothing found
	}

	public static File getOldDataFolder() {
		File dataFolder = new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu");
		return dataFolder;
	}

	public static boolean oldDataFolderExists() {
		return getOldDataFolder().exists() && getOldDataFolder().list() != null;
	}

	public static File getDataFolder(Context c) {
		File f = c.getExternalFilesDir(null);
		f.mkdirs();
		return f;
	}

	public static File getMovieThumbFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/movie-thumbs");
		f.mkdirs();
		return f;
	}

	public static File getMovieBackdropFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/movie-backdrops");
		f.mkdirs();
		return f;
	}

	public static File getTvShowThumbFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/tvshows-thumbs");
		f.mkdirs();
		return f;
	}

	public static File getTvShowBackdropFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/tvshows-backdrops");
		f.mkdirs();
		return f;
	}

	public static File getTvShowEpisodeFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/tvshows-episodes");
		f.mkdirs();
		return f;
	}

	public static File getCacheFolder(Context c) {
		File f = new File(c.getExternalFilesDir(null) + "/app_cache");
		f.mkdirs();
		return f;
	}

	public static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public static void moveFile(File src, File dst) throws IOException {
		copyFile(src, dst);
		src.delete();
	}

	public static void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			File[] listFiles = fileOrDirectory.listFiles();
			if (listFiles != null) {
				int count = listFiles.length;
				for (int i = 0; i < count; i++)
					deleteRecursive(listFiles[i]);
			}
		}

		fileOrDirectory.delete();
	}

	public static String getTraktUserName(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		return username;
	}

	public static JSONArray getTraktCalendar(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String username = settings.getString("traktUsername", "").trim();
		String password = settings.getString("traktPassword", "");

		if (username.isEmpty() || password.isEmpty())
			return new JSONArray();

		// Cancel the current check in to allow this one
		OkApacheClient httpclient = new OkApacheClient();
		HttpPost httppost = new HttpPost("http://api.trakt.tv/user/calendar/shows.json/" + MizLib.TRAKT_API + "/" + username);
		httppost.setHeader("Content-type", "application/json");

		try {

			JSONObject holder = new JSONObject();
			holder.put("username", username);
			holder.put("password", password);

			StringEntity se = new StringEntity(holder.toString());
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httppost.setEntity(se);

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(httppost, responseHandler);

			return new JSONArray(result);
		} catch (Exception e) {}

		return new JSONArray();
	}

	public static String removeWikipediaNotes(String original) {
		original = original.replaceAll("(?i)from wikipedia, the free encyclopedia.", "").replaceAll("(?i)from wikipedia, the free encyclopedia", "");
		if (original.contains("Description above from the Wikipedia")) {
			original = original.substring(0, original.lastIndexOf("Description above from the Wikipedia"));
		}
		original = original.trim();

		return original;
	}

	public static String getParentFolder(String filepath) {
		try {
			String pathWithoutEnding = filepath.substring(0, filepath.lastIndexOf("/"));
			return pathWithoutEnding;
		} catch (Exception e) {
			return "";
		}
	}

	public static void scheduleMovieUpdate(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		// Check if scheduled updates are enabled, and schedule the next update if this is the case
		if (settings.getInt(ScheduledUpdatesFragment.MOVIE_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED) > ScheduledUpdatesFragment.AT_LAUNCH) {
			ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.MOVIES, context);
			long duration = MizLib.HOUR * 6;
			switch (settings.getInt(ScheduledUpdatesFragment.MOVIE_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED)) {
			case ScheduledUpdatesFragment.EVERY_12_HOURS:
				duration = MizLib.HOUR * 12;
				break;
			case ScheduledUpdatesFragment.EVERY_DAY:
				duration = MizLib.DAY;
				break;
			case ScheduledUpdatesFragment.EVERY_WEEK:
				duration = MizLib.WEEK;
				break;
			}
			ScheduledUpdatesAlarmManager.startUpdate(ScheduledUpdatesAlarmManager.MOVIES, context, duration);
		}
	}

	public static void scheduleShowsUpdate(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		// Check if scheduled updates are enabled, and schedule the next update if this is the case
		if (settings.getInt(ScheduledUpdatesFragment.SHOWS_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED) > ScheduledUpdatesFragment.AT_LAUNCH) {
			ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.SHOWS, context);
			long duration = MizLib.HOUR * 6;
			switch (settings.getInt(ScheduledUpdatesFragment.SHOWS_UPDATE_PREF, ScheduledUpdatesFragment.NOT_ENABLED)) {
			case ScheduledUpdatesFragment.EVERY_12_HOURS:
				duration = MizLib.HOUR * 12;
				break;
			case ScheduledUpdatesFragment.EVERY_DAY:
				duration = MizLib.DAY;
				break;
			case ScheduledUpdatesFragment.EVERY_WEEK:
				duration = MizLib.WEEK;
				break;
			}
			ScheduledUpdatesAlarmManager.startUpdate(ScheduledUpdatesAlarmManager.SHOWS, context, duration);
		}
	}

	public static int flatPosition(ExpandableListView list, int groupPosition, int childPosition) {
		try {
			return list.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isImdbInstalled(Context c) {
		PackageManager pm = c.getPackageManager();
		try {
			pm.getPackageInfo("com.imdb.mobile", PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	public static void setTextOfCoverView(String text, View view) {
		if (view.getParent() instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view.getParent();
			if (group.getId() != android.R.id.content) {
				// Found a container that isn't the container holding our screen layout
				for (int i = 0; i < group.getChildCount(); i++) {
					if (group.getChildAt(i).getId() == R.id.text) {
						TextView tv = (TextView) group.getChildAt(i);
						tv.setText(text);
						tv.setVisibility(View.VISIBLE);
						continue;
					}
				}
			}
		}
	}

	public static int countOccurrences(String haystack, char needle) {
		int count = 0;
		char[] array = haystack.toCharArray();
		for (int i = 0; i < array.length; i++) {
			if (array[i] == needle) {
				++count;
			}
		}
		return count;
	}

	public static DecryptedMovie decryptMovie(String filepath, String customTags) {
		DecryptedMovie mMovie = new DecryptedMovie();
		mMovie.setFilepath(filepath);
		mMovie.setFileNameYear(decryptYear(mMovie.getFileName()));
		mMovie.setDecryptedFileName(decryptName(mMovie.getFileName(), customTags));
		if (mMovie.hasParentName()) {
			mMovie.setParentNameYear(decryptYear(mMovie.getParentName()));
			mMovie.setDecryptedParentName(decryptName(mMovie.getParentName(), customTags));
		}

		return mMovie;
	}

	public static String decryptName(String input, String customTags) {
		String output = getNameFromFilename(input);
		output = fixAbbreviations(output);

		// Used to remove [SET {title}] from the beginning of filenames
		if (output.startsWith("[") && output.contains("]")) {
			String after = "";

			if (output.matches("(?i)^\\[SET .*\\].*?")) {
				try {
					after = output.substring(output.indexOf("]") + 1, output.length());
				} catch (IndexOutOfBoundsException e) {}
			}

			if (!after.isEmpty())
				output = after;
		}

		output = output.replaceAll(WAREZ_PATTERN + "|\\)|\\(|\\[|\\]|\\{|\\}|\\'|\\<|\\>|\\-", "");

		// Improved support for French titles that start with C' or L'
		if (output.matches("(?i)^(c|l)(\\_|\\.)\\w.*?")) {
			StringBuilder sb = new StringBuilder(output);
			sb.replace(1, 2, "'");
			output = sb.toString();
		}

		if (!customTags.isEmpty()) {
			String[] custom = customTags.split("<MiZ>");
			int count = custom.length;
			for (int i = 0; i < count; i++)
				try {
					output = output.replaceAll("(?i)" + custom[i], "");
				} catch (Exception e) {}
		}

		output = output.replaceAll("\\s\\-\\s|\\.|\\,|\\_", " "); // Remove separators
		output = output.trim().replaceAll("(?i)(part)$", ""); // Remove "part" in the end of the string

		return output.replaceAll(" +", " ").trim(); // replaceAll() needed to remove all instances of multiple spaces
	}

	private final static String YEAR_PATTERN = "(19|20)[0-9][0-9]";
	private final static String WAREZ_PATTERN = "(?i)(dvdscreener|dvdscreen|dvdscr|dvdrip|dvd5|dvd|xvid|divx|m\\-480p|m\\-576p|m\\-720p|m\\-864p|m\\-900p|m\\-1080p|m480p|m576p|m720p|m864p|m900p|m1080p|480p|576p|720p|864p|900p|1080p|1080i|720i|mhd|brrip|bdrip|brscreener|brscreen|brscr|aac|x264|bluray|dts|screener|hdtv|ac3|repack|2\\.1|5\\.1|ac3_6|7\\.1|h264|hdrip|ntsc|proper|readnfo|rerip|vcd|scvd|pdtv|sdtv|sample|tvrip|tvr|extended.editions?|directors cut|tfe|unrated|\\(.*?torrent.*?\\)|\\[.*?\\]|\\(.*?\\)|\\{.*?\\}|part[0-9]|cd[0-9])";
	private final static String ABBREVIATION_PATTERN = "(?<=(^|[.])[\\S&&\\D])[.](?=[\\S&&\\D]([.]|$))";

	public static String getNameFromFilename(String input) {
		int lastIndex = 0;

		Pattern searchPattern = Pattern.compile(YEAR_PATTERN);
		Matcher searchMatcher = searchPattern.matcher(input);

		while (searchMatcher.find())
			lastIndex = searchMatcher.end();

		if (lastIndex > 0)
			try {
				return input.substring(0, lastIndex - 4);
			} catch (IndexOutOfBoundsException e) {}

		return input;
	}

	public static String fixAbbreviations(String input) {
		return input.replaceAll(ABBREVIATION_PATTERN, "");
	}

	public static String decryptYear(String input) {
		String result = "";
		Pattern searchPattern = Pattern.compile(YEAR_PATTERN);
		Matcher searchMatcher = searchPattern.matcher(input);

		while (searchMatcher.find()) {
			try {
				int lastIndex = searchMatcher.end();
				result = input.substring(lastIndex - 4, lastIndex);
			} catch (IndexOutOfBoundsException e) {}
		}

		return result;
	}

	public static DecryptedShowEpisode decryptEpisode(String filepath, String customTags) {
		DecryptedShowEpisode mEpisode = new DecryptedShowEpisode();
		mEpisode.setFilepath(filepath);
		mEpisode.setFileNameYear(decryptYear(mEpisode.getFileName()));
		mEpisode.setDecryptedFileName(decryptName(getStringBeforeSeasonAndEpisode(mEpisode.getFileName()), customTags));
		mEpisode.setEpisode(getEpisodeOrSeasonFromFileName(mEpisode.getFileName(), EPISODE));
		mEpisode.setSeason(getEpisodeOrSeasonFromFileName(mEpisode.getFileName(), SEASON));

		if (mEpisode.hasSeasonsFolder()) { // There's a season folder in the file structure - let's overwrite the season number with the one in the folder name
			mEpisode.setSeason(getSeasonFromFolderName(mEpisode.getSeasonFolder()));
		}

		if (mEpisode.hasParentName()) {
			mEpisode.setParentNameYear(decryptYear(mEpisode.getParentName()));
			mEpisode.setDecryptedParentName(decryptName(getStringBeforeSeason(mEpisode.getParentName()), customTags));
		}

		return mEpisode;
	}

	public static boolean folderNameContainsSeasonNumber(String folderName) {

		// S##
		Pattern searchPattern = Pattern.compile("(?i)s(\\d){1,2}");
		Matcher searchMatcher = searchPattern.matcher(folderName);

		if (searchMatcher.find())
			return true;

		// Season ##
		searchPattern = Pattern.compile("(?i)season((\\s)+)(\\d){1,2}");
		searchMatcher = searchPattern.matcher(folderName);

		if (searchMatcher.find())
			return true;

		// ##
		searchPattern = Pattern.compile("(\\d){1,2}");
		searchMatcher = searchPattern.matcher(folderName);

		if (searchMatcher.find())
			return true;

		return false;
	}

	public static int SEASON = 0, EPISODE = 1;

	public static int getEpisodeOrSeasonFromFileName(String input, int type) {
		int episode = 0, season = 0;

		// ##e##
		Pattern searchPattern = Pattern.compile("(?i)(\\d){1,2}e(\\d){1,3}");
		Matcher searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				episode = Integer.valueOf(result.split("(?i)e")[1].trim());
				season = Integer.valueOf(result.split("(?i)e")[0].trim());

				if (type == SEASON)
					return season;
				return episode;
			} catch (IndexOutOfBoundsException e) {}
		}

		// ##x##
		searchPattern = Pattern.compile("(?i)(\\d){1,2}x(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				episode = Integer.valueOf(result.split("(?i)x")[1].trim());
				season = Integer.valueOf(result.split("(?i)x")[0].trim());

				if (type == SEASON)
					return season;
				return episode;
			} catch (IndexOutOfBoundsException e) {}
		}

		// season ## episode ##
		searchPattern = Pattern.compile("(?i)season((\\s)+)(\\d){1,2}((\\s)+)episode((\\s)+)(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				episode = Integer.valueOf(result.split("(?i)episode")[1].trim());
				season = Integer.valueOf(result.split("(?i)episode")[0].replaceAll("(?i)season", "").trim());

				if (type == SEASON)
					return season;
				return episode;
			} catch (IndexOutOfBoundsException e) {}
		}

		// episode ##
		searchPattern = Pattern.compile("(?i)episode((\\s)+)(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				episode = Integer.valueOf(result.split("(?i)episode")[1].trim());
				// No information found about the season number, return 0

				if (type == SEASON)
					return season;
				return episode;
			} catch (IndexOutOfBoundsException e) {}
		}

		// #####
		searchPattern = Pattern.compile("(\\d){1,5}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());

				switch (result.length()) {
				case 1: // # (episode)
					episode = Integer.valueOf(result);
					// No information found about the season number, return 0
					break;
				case 2: // ## (episode)
					episode = Integer.valueOf(result);
					// No information found about the season number, return 0
					break;
				case 3: // ### (season [1], episode [2])
					episode = Integer.valueOf(result.substring(1, 3));
					season = Integer.valueOf(result.substring(0, 1));
					break;
				case 4: // #### (season [2], episode [2])
					episode = Integer.valueOf(result.substring(2, 4));
					season = Integer.valueOf(result.substring(0, 2));
					break;
				case 5: // ##### (season [2], episode [3])
					episode = Integer.valueOf(result.substring(2, 5));
					season = Integer.valueOf(result.substring(0, 2));
					break;
				}

				if (type == SEASON)
					return season;
				return episode;
			} catch (IndexOutOfBoundsException e) {}
		}

		return episode;
	}

	public static String getStringBeforeSeasonAndEpisode(String input) {
		String result = input;

		// s##e##
		Pattern searchPattern = Pattern.compile("(?i)s(\\d){1,2}e(\\d){1,3}");
		Matcher searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// ##e##
		searchPattern = Pattern.compile("(?i)(\\d){1,2}e(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// s##x##
		searchPattern = Pattern.compile("(?i)s(\\d){1,2}x(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// ##x##
		searchPattern = Pattern.compile("(?i)(\\d){1,2}x(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// season ## episode ##
		searchPattern = Pattern.compile("(?i)season((\\s)+)(\\d){1,2}((\\s)+)episode((\\s)+)(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// episode ##
		searchPattern = Pattern.compile("(?i)episode((\\s)+)(\\d){1,3}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// e#####
		searchPattern = Pattern.compile("(?i)e(\\d){1,5}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// #####
		searchPattern = Pattern.compile("(\\d){1,5}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		return result;
	}

	public static String getStringBeforeSeason(String input) {
		String result = input;

		// s##
		Pattern searchPattern = Pattern.compile("(?i)s(\\d){1,2}");
		Matcher searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		// season ## episode ##
		searchPattern = Pattern.compile("(?i)season((\\s)+)(\\d){1,2}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				result = input.substring(0, searchMatcher.start());
				return result;
			} catch (IndexOutOfBoundsException e) {}
		}

		return result;
	}

	public static int getSeasonFromFolderName(String input) {
		int season = 0;

		// S##
		Pattern searchPattern = Pattern.compile("(?i)s(\\d){1,2}");
		Matcher searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				season = Integer.valueOf(result.replaceAll("(?i)s", "").trim());

				return season;
			} catch (IndexOutOfBoundsException e) {}
		}

		// season ##
		searchPattern = Pattern.compile("(?i)season((\\s)+)(\\d){1,2}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());
				season = Integer.valueOf(result.replaceAll("(?i)season", "").trim());

				return season;
			} catch (IndexOutOfBoundsException e) {}
		}

		// ##
		searchPattern = Pattern.compile("(\\d){1,2}");
		searchMatcher = searchPattern.matcher(input);

		if (searchMatcher.find()) {
			try {
				String result = input.substring(searchMatcher.start(), searchMatcher.end());

				switch (result.length()) {
				case 1: // # (episode)
					season = Integer.valueOf(result);
					// No information found about the season number, return 0
					break;
				case 2: // ## (episode)
					season = Integer.valueOf(result);
					// No information found about the season number, return 0
					break;
				}

				return season;
			} catch (IndexOutOfBoundsException e) {}
		}

		return season;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 *
	 * @param filename The full path of the file to decode
	 * @param reqWidth The requested width of the resulting bitmap
	 * @param reqHeight The requested height of the resulting bitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
	 *         that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeFile(filename, options);
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested width and height.
	 *
	 * @param res The resources object containing the image data
	 * @param resId The resource id of the image data
	 * @param reqWidth The requested width of the resulting bitmap
	 * @param reqHeight The requested height of the resulting bitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
	 *         that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
	 * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
	 * the closest inSampleSize that will result in the final decoded bitmap having a width and
	 * height equal to or larger than the requested width and height. This implementation does not
	 * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
	 * results in a larger bitmap which isn't as useful for caching purposes.
	 *
	 * @param options An options object with out* params already populated (run through a decode*
	 *            method with inJustDecodeBounds==true
	 * @param reqWidth The requested width of the resulting bitmap
	 * @param reqHeight The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee a final image
			// with both dimensions larger than or equal to the requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger inSampleSize).

			final float totalPixels = width * height;

			// Anything more than 2x the requested pixels we'll sample down further
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}

	@SuppressWarnings("deprecation")
	public static int getFreeMemory() {
		StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
		if (hasJellyBeanMR2())
			return (int) (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
		else
			return statFs.getAvailableBlocks() * statFs.getBlockSize();
	}

	private static String[] MEDIA_APPS = new String[]{"com.imdb.mobile", "com.google.android.youtube", "com.ted.android", "com.google.android.videos", "se.mtg.freetv.tv3_dk", "tv.twitch.android.viewer",
		"com.netflix.mediaclient", "com.gotv.crackle.handset", "net.flixster.android", "com.google.tv.alf", "com.viki.android", "com.mobitv.client.mobitv", "com.hulu.plus.jp", "com.hulu.plus",
		"com.mobitv.client.tv", "air.com.vudu.air.DownloaderTablet", "com.hbo.android.app", "com.HBO", "bbc.iplayer.android", "air.uk.co.bbc.android.mediaplayer", "com.rhythmnewmedia.tvdotcom",
		"com.cnettv.app", "com.xfinity.playnow"};

	public static boolean isMediaApp(ApplicationInfo ai) {
		for (int i = 0; i < MEDIA_APPS.length; i++)
			if (MEDIA_APPS[i].equals(ai.packageName))
				return true;
		return false;
	}

	private static int mRuntimeInMinutes;
	public static String getRuntimeInMinutesOrHours(String runtime, String hour, String minute) {
		mRuntimeInMinutes = Integer.valueOf(runtime);
		if (mRuntimeInMinutes >= 60) {
			return (mRuntimeInMinutes / 60) + hour;
		}
		return mRuntimeInMinutes + minute;
	}

	public static int getPartNumberFromFilepath(String filepath) {
		if (filepath.matches(".*part[1-9].*"))
			filepath = filepath.substring(filepath.lastIndexOf("part") + 4, filepath.length());
		else if (filepath.matches(".*cd[1-9].*"))
			filepath = filepath.substring(filepath.lastIndexOf("cd") + 2, filepath.length());

		filepath = filepath.substring(0, 1);

		try {
			return Integer.valueOf(filepath);
		} catch (NumberFormatException nfe) { return 0; }
	}

	public static List<String> getSplitParts(String filepath, NtlmPasswordAuthentication auth) throws MalformedURLException, UnsupportedEncodingException, SmbException {
		ArrayList<String> parts = new ArrayList<String>();

		String fileType = "";
		if (filepath.contains(".")) {
			fileType = filepath.substring(filepath.lastIndexOf("."));
		}

		if (filepath.matches(".*part[1-9].*"))
			filepath = filepath.substring(0, filepath.lastIndexOf("part") + 4);
		else if (filepath.matches(".*cd[1-9].*"))
			filepath = filepath.substring(0, filepath.lastIndexOf("cd") + 2);

		if (auth == null) { // Check if it's a local file
			File temp;
			for (int i = 1; i < 10; i++) {
				temp = new File(filepath + i + fileType);
				if (temp.exists())
					parts.add(temp.getAbsolutePath());
			}
		} else { // It's a network file
			SmbFile temp;
			for (int i = 1; i < 10; i++) {
				temp = new SmbFile(createSmbLoginString(URLEncoder.encode(auth.getDomain(), "utf-8"),
						URLEncoder.encode(auth.getUsername(), "utf-8"),
						URLEncoder.encode(auth.getPassword(), "utf-8"),
						filepath + i + fileType,
						false));
				if (temp.exists())
					parts.add(temp.getPath());
			}
		}

		return parts;
	}

	public static String transformSmbPath(String smbPath) {
		if (smbPath.contains("smb") && smbPath.contains("@"))
			return "smb://" + smbPath.substring(smbPath.indexOf("@") + 1);
		return smbPath.replace("/smb:/", "smb://");
	}

	public static int getNavigationBarHeight(Context context) {
		Resources resources = context.getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			return resources.getDimensionPixelSize(resourceId);
		}
		return 0;
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
				.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static String getLatestBackdropPath(Context c) {
		File latestMovie = lastFileModified(getMovieBackdropFolder(c));
		File latestShow = lastFileModified(getTvShowBackdropFolder(c));
		if (latestMovie != null && latestShow != null) {
			if (latestMovie.lastModified() > latestShow.lastModified())
				return latestMovie.getAbsolutePath();
			return latestShow.getAbsolutePath();
		} else if (latestMovie != null) {
			return latestMovie.getAbsolutePath();
		} else if (latestShow != null) {
			return latestShow.getAbsolutePath();
		}
		return "";
	}

	public static File lastFileModified(File dir) {
		File[] files = dir.listFiles(new FileFilter() {			
			public boolean accept(File file) {
				return file.isFile();
			}
		});

		if (files != null) {
			long lastMod = Long.MIN_VALUE;
			File choice = null;
			for (int i = 0; i < files.length; i++) {
				if (files[i].lastModified() > lastMod) {
					choice = files[i];
					lastMod = files[i].lastModified();
				}
			}
			return choice;
		}
		return null;
	}
}