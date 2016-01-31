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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.miz.db.DbAdapterMovies;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.MakeAvailableOffline;
import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;
import com.miz.utils.FileUtils;
import com.miz.utils.ViewUtils;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import static com.miz.functions.PreferenceKeys.INCLUDE_ADULT_CONTENT;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_MOVIE;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_TVSHOWS;
import static com.miz.functions.PreferenceKeys.TMDB_BASE_URL;
import static com.miz.functions.PreferenceKeys.TMDB_BASE_URL_TIME;
import static com.miz.functions.PreferenceKeys.TRAKT_USERNAME;

@SuppressLint("NewApi")
public class MizLib {

    public static final String TYPE = "type";
    public static final String MOVIE = "movie";
    public static final String TV_SHOW = "tvshow";
    public static final String FILESOURCE = "filesource";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String DOMAIN = "domain";
    public static final String SERVER = "server";
    public static final String SERIAL_NUMBER = "serial_number";

    public static final String allFileTypes = ".3gp.aaf.mp4.ts.webm.m4v.mkv.divx.xvid.rec.avi.flv.f4v.moi.mpeg.mpg.mts.m2ts.ogv.rm.rmvb.mov.wmv.iso.vob.ifo.wtv.pyv.ogm.img";
    public static final String IMAGE_CACHE_DIR = "thumbs";
    public static final String CHARACTER_REGEX = "[^\\w\\s]";
    public static final String[] prefixes = new String[]{"the ", "a ", "an "};

    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;
    public static final int WEEK = 7 * DAY;

    private MizLib() {} // No instantiation

    public static String getTmdbApiKey(Context context) {
        String key = context.getString(R.string.tmdb_api_key);
        if (TextUtils.isEmpty(key) || key.equals("add_your_own"))
            throw new RuntimeException("You need to add a TMDb API key!");
        return key;
    }

    public static String getTvdbApiKey(Context context) {
        String key = context.getString(R.string.tvdb_api_key);
        if (TextUtils.isEmpty(key) || key.equals("add_your_own"))
            throw new RuntimeException("You need to add a TVDb API key!");
        return key;
    }

    public static String[] getPrefixes(Context c) {
        ArrayList<String> prefixesArray = new ArrayList<String>();
        String prefix = c.getString(R.string.prefixes);

        String[] split = prefix.split(",");
        int count = split.length;
        prefixesArray.addAll(Arrays.asList(split).subList(0, count));

        count = prefixes.length;
        prefixesArray.addAll(Arrays.asList(prefixes).subList(0, count));

        return prefixesArray.toArray(new String[prefixesArray.size()]);
    }

    public static boolean isVideoFile(String s) {
        String[] fileTypes = new String[]{".3gp",".aaf.","mp4",".ts",".webm",".m4v",".mkv",".divx",".xvid",".rec",".avi",".flv",".f4v",".moi",".mpeg",".mpg",".mts",".m2ts",".ogv",".rm",".rmvb",".mov",".wmv",".iso",".vob",".ifo",".wtv",".pyv",".ogm",".img"};
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
        if (TextUtils.isEmpty(s))
            return "";

        return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1, s.length());
    }

    /**
     * Converts the first character of all words (separated by space)
     * in the String to upper case.
     * @param s (input String)
     * @return Input string with first character of all words in upper case.
     */
    public static String toCapitalWords(String s) {
        if (TextUtils.isEmpty(s))
            return "";

        StringBuilder result = new StringBuilder();
        String[] split = s.split("\\s");
        int count = split.length;
        for (int i = 0; i < count; i++)
            result.append(toCapitalFirstChar(split[i])).append(" ");
        return result.toString().trim();
    }

    /**
     * Adds spaces between capital characters.
     * @param s (input String)
     * @return Input string with spaces between capital characters.
     */
    public static String addSpaceByCapital(String s) {
        if (TextUtils.isEmpty(s))
            return "";

        StringBuilder result = new StringBuilder();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++)
            if (chars.length > (i+1))
                if (Character.isUpperCase(chars[i]) && (Character.isLowerCase(chars[i+1]) && !Character.isSpaceChar(chars[i+1])))
                    result.append(" ").append(chars[i]);
                else
                    result.append(chars[i]);
            else
                result.append(chars[i]);
        return result.toString().trim();
    }

    /**
     * Returns any digits (numbers) in a String
     * @param s (Input string)
     * @return A string with any digits from the input string
     */
    public static String getNumbersInString(String s) {
        if (TextUtils.isEmpty(s))
            return "";

        StringBuilder result = new StringBuilder();
        char[] charArray = s.toCharArray();
        int count = charArray.length;
        for (int i = 0; i < count; i++)
            if (Character.isDigit(charArray[i]))
                result.append(charArray[i]);

        return result.toString();
    }

    public static int getCharacterCountInString(String source, char c) {
        int result = 0;
        if (!TextUtils.isEmpty(source))
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
    public static boolean isTablet(Context c) {
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
    public static boolean isPortrait(Context c) {
        return c != null && (c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
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
    public static boolean isWifiConnected(Context c) {
        if (c!= null) {
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
     * Add a padding with a combined height of the ActionBar and Status bar to the top of a given View
     * @param c
     * @param v
     */
    public static void addActionBarAndStatusBarPadding(Context c, View v) {
        int mActionBarHeight = 0, mStatusBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        if (hasKitKat())
            mStatusBarHeight = convertDpToPixels(c, 25);

        v.setPadding(0, mActionBarHeight + mStatusBarHeight, 0, 0);
    }

    /**
     * Add a padding with a height of the ActionBar to the bottom of a given View
     * @param c
     * @param v
     */
    public static void addActionBarPaddingBottom(Context c, View v) {
        int mActionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        v.setPadding(0, 0, 0, mActionBarHeight);
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

    /**
     * Add a margin with a combined height of the ActionBar and Status bar to the top of a given View contained in a FrameLayout
     * @param c
     * @param v
     */
    public static void addActionBarAndStatusBarMargin(Context c, View v) {
        int mActionBarHeight = 0, mStatusBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        if (hasKitKat())
            mStatusBarHeight = convertDpToPixels(c, 25);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.setMargins(0, mActionBarHeight + mStatusBarHeight, 0, 0);

        v.setLayoutParams(params);
    }

    /**
     * Add a margin with a combined height of the ActionBar and Status bar to the top of a given View contained in a FrameLayout
     * @param c
     * @param v
     */
    public static void addActionBarAndStatusBarMargin(Context c, View v, FrameLayout.LayoutParams layoutParams) {
        int mActionBarHeight = 0, mStatusBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        if (hasKitKat())
            mStatusBarHeight = convertDpToPixels(c, 25);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.setMargins(0, mActionBarHeight + mStatusBarHeight, 0, 0);
        params.gravity = layoutParams.gravity;
        v.setLayoutParams(params);
    }

    /**
     * Add a margin with a height of the ActionBar to the top of a given View contained in a FrameLayout
     * @param c
     * @param v
     */
    public static void addActionBarMarginBottom(Context c, View v) {
        int mActionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            mActionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, mActionBarHeight);
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

    public static boolean hasICSMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
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

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static int getThumbnailNotificationSize(Context c) {
        Resources r = c.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics());
    }

    public static int getLargeNotificationWidth(Context c) {
        Resources r = c.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, r.getDisplayMetrics());
    }

    public static int convertDpToPixels(Context c, int dp) {
        Resources r = c.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static int getActionBarHeight(Context c) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, c.getResources().getDisplayMetrics());
        else
            actionBarHeight = 0; // No ActionBar style (pre-Honeycomb or ActionBar not in theme)

        return actionBarHeight;
    }

    public static int getActionBarAndStatusBarHeight(Context c) {
        int actionBarHeight = getActionBarHeight(c);
        int statusBarHeight = ViewUtils.getStatusBarHeight(c);

        // We're only interested in returning the combined
        // height, if we're running on KitKat or above
        return hasKitKat() ?
                actionBarHeight + statusBarHeight : actionBarHeight;
    }

    public static String md5(final String s) {
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

    public static int getThumbnailSize(Context c) {
        final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d = window.getDefaultDisplay();

        Point size = new Point();
        d.getSize(size);

        final int numColumns = (int) Math.floor(Math.max(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

        if (numColumns > 0) {
            final int columnWidth = (Math.max(size.x, size.y) / numColumns) - mImageThumbSpacing;

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

        final int numColumns = (int) Math.floor(Math.max(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

        if (numColumns > 0) {
            final int columnWidth = (Math.max(size.x, size.y) / numColumns) - mImageThumbSpacing;

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
                    bm = Bitmap.createScaledBitmap(bm, imageWidth, (int) (imageWidth * 1.5), true);
                    FileOutputStream out = new FileOutputStream(filepath);
                    bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.close();
                    bm.recycle();
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

        final int numColumns = (int) Math.floor(Math.max(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

        if (numColumns > 0) {
            final int columnWidth = (Math.max(size.x, size.y) / numColumns) - mImageThumbSpacing;

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

        final int width = Math.max(size.x, size.y);

        if (width > 1280 && isTablet(c)) // We only want to download full size images on tablets, as these are the only devices where you can see the difference
            return "original";
        else if (width > 780)
            return "w1280";
        return "w780";
    }

    public static String getBackdropThumbUrlSize(Context c) {
        WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d = window.getDefaultDisplay();

        Point size = new Point();
        d.getSize(size);

        final int width = Math.min(size.x, size.y);

        if (width >= 780)
            return "w780";
        if (width >= 400)
            return "w500";
        return "w300";
    }

    public static String getActorUrlSize(Context c) {
        final int mImageThumbSize = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        final int mImageThumbSpacing = c.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        WindowManager window = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d = window.getDefaultDisplay();

        Point size = new Point();
        d.getSize(size);

        final int numColumns = (int) Math.floor(Math.max(size.x, size.y) / (mImageThumbSize + mImageThumbSpacing));

        if (numColumns > 0) {
            final int columnWidth = (Math.max(size.x, size.y) / numColumns) - mImageThumbSpacing;

            if (columnWidth > 400)
                return "h632";

            if (columnWidth >= 300)
                return "w300";
        }

        return "w185";
    }

    public static boolean checkFileTypes(String file) {
        // We don't want to include files that start with ._ or .DS_Store
        if (file.matches("(?i).*[/][\\.](?:_|DS_Store).*[\\.].*$"))
            return false;

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

    public static String removeExtension(String filepath) {
        final int lastPeriodPos = filepath.lastIndexOf('.');
        if (lastPeriodPos <= 0) {
            return filepath;
        } else {
            // Remove the last period and everything after it
            return filepath.substring(0, lastPeriodPos);
        }
    }

    public static String convertToGenericNfo(String filepath) {
        final int lastPeriodPos = filepath.lastIndexOf('/');
        if (lastPeriodPos <= 0) {
            return filepath;
        } else {
            // Remove the last period and everything after it
            return filepath.substring(0, lastPeriodPos) + "/movie.nfo";
        }
    }

    /**
     * Returns a blurred bitmap. It uses a RenderScript to blur the bitmap very fast.
     * @param context
     * @param originalBitmap
     * @param radius
     * @return
     */
    public static Bitmap fastBlur(Context context, Bitmap originalBitmap, int radius) {
        final RenderScript rs = RenderScript.create(context);

        final Allocation input = Allocation.createFromBitmap(rs, originalBitmap);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(originalBitmap);

        return originalBitmap;
    }

    /**
     * Stack BlurUtil v1.0 from
     http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
     Java Author: Mario Klingemann <mario at quasimondo.com>
     http://incubator.quasimondo.com
     created Feburary 29, 2004
     Android port : Yahel Bouaziz <yahel at kayenko.com>
     http://www.kayenko.com
     ported april 5th, 2012
     This is a compromise between Gaussian BlurUtil and Box blur
     It creates much better looking blurs than Box BlurUtil, but is
     7x faster than my Gaussian BlurUtil implementation.
     I called it Stack BlurUtil because this describes best how this
     filter works internally: it creates a kind of moving stack
     of colors whilst scanning through the image. Thereby it
     just has to add one new block of color to the right side
     of the stack and remove the leftmost color. The remaining
     colors on the topmost layer of the stack are either added on
     or reduced by one, depending on if they are on the right or
     on the left side of the stack.
     If you are using this algorithm in your code please add
     the following line:
     Stack BlurUtil Algorithm by Mario Klingemann <mario@quasimondo.com>
     */
    public static Bitmap slowBlur(Bitmap originalBitmap, int radius) {
        if (radius < 1) {
            return (null);
        }

        int w = originalBitmap.getWidth();
        int h = originalBitmap.getHeight();

        int[] pix = new int[w * h];
        originalBitmap.getPixels(pix, 0, w, 0, 0, w, h);

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
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

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
        originalBitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return originalBitmap;
    }

    public static boolean downloadFile(String url, String savePath) {
        if (TextUtils.isEmpty(url))
            return false;

        InputStream in = null;
        OutputStream fileos = null;

        try {
            int bufferSize = 8192;
            byte[] retVal = null;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = MizuuApplication.getOkHttpClient().newCall(request).execute();
            if (!response.isSuccessful())
                return false;

            fileos = new BufferedOutputStream(new FileOutputStream(savePath));
            in = new BufferedInputStream(response.body().byteStream(), bufferSize);

            retVal = new byte[bufferSize];
            int length = 0;
            while((length = in.read(retVal)) > -1) {
                fileos.write(retVal, 0, length);
            }
        } catch(Exception e) {
            // The download failed, so let's delete whatever was downloaded
            deleteFile(new File(savePath));

            return false;
        } finally {
            if (fileos != null) {
                try {
                    fileos.flush();
                    fileos.close();
                } catch (IOException e) {}
            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }

        return true;
    }

    public static JSONObject getJSONObject(Context context, String url) {
        final OkHttpClient client = MizuuApplication.getOkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();

            if (response.code() >= 429) {
                // HTTP error 429 and above means that we've exceeded the query limit
                // for TMDb. Sleep for 5 seconds and try again.
                Thread.sleep(5000);
                response = client.newCall(request).execute();
            }
            return new JSONObject(response.body().string());
        } catch (Exception e) { // IOException and JSONException
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

    public static int getInteger(String number) {
        try {
            return Integer.valueOf(number);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static int getInteger(double number) {
        try {
            return (int) number;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String removeIndexZero(String s) {
        if (!TextUtils.isEmpty(s))
            try {
                return String.valueOf(Integer.parseInt(s));
            } catch (NumberFormatException e) {}
        return s;
    }

    public static String addIndexZero(String s) {
        if (TextUtils.isEmpty(s))
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

    public static final int TYPE_MOVIE = 0, TYPE_SHOWS = 1;

    public static ArrayList<FileSource> getFileSources(int type, boolean onlyNetworkSources) {

        ArrayList<FileSource> filesources = new ArrayList<FileSource>();
        DbAdapterSources dbHelperSources = MizuuApplication.getSourcesAdapter();

        Cursor c = null;
        if (type == TYPE_MOVIE)
            c = dbHelperSources.fetchAllMovieSources();
        else
            c = dbHelperSources.fetchAllShowSources();

        ColumnIndexCache cache = new ColumnIndexCache();

        try {
            while (c.moveToNext()) {
                if (onlyNetworkSources) {
                    if (c.getInt(cache.getColumnIndex(c, DbAdapterSources.KEY_FILESOURCE_TYPE)) == FileSource.SMB) {
                        filesources.add(new FileSource(
                                c.getLong(cache.getColumnIndex(c, DbAdapterSources.KEY_ROWID)),
                                c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_FILEPATH)),
                                c.getInt(cache.getColumnIndex(c, DbAdapterSources.KEY_FILESOURCE_TYPE)),
                                c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_USER)),
                                c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_PASSWORD)),
                                c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_DOMAIN)),
                                c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_TYPE))
                        ));
                    }
                } else {
                    filesources.add(new FileSource(
                            c.getLong(cache.getColumnIndex(c, DbAdapterSources.KEY_ROWID)),
                            c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_FILEPATH)),
                            c.getInt(cache.getColumnIndex(c, DbAdapterSources.KEY_FILESOURCE_TYPE)),
                            c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_USER)),
                            c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_PASSWORD)),
                            c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_DOMAIN)),
                            c.getString(cache.getColumnIndex(c, DbAdapterSources.KEY_TYPE))
                    ));
                }
            }
        } catch (Exception e) {
        } finally {
            c.close();
            cache.clear();
        }

        return filesources;
    }

    public static SmbLogin getLoginFromFilesource(FileSource source) {
        if (source == null ||
                TextUtils.isEmpty(source.getDomain()) &&
                        TextUtils.isEmpty(source.getUser()) &&
                        TextUtils.isEmpty(source.getPassword())) {
            return new SmbLogin();
        } else {
            return new SmbLogin(source.getDomain(), source.getUser(), source.getPassword());
        }
    }

    public static SmbLogin getLoginFromFilepath(int type, String filepath) {

        ArrayList<FileSource> filesources = MizLib.getFileSources(type, true);
        FileSource source = null;

        for (int i = 0; i < filesources.size(); i++) {
            if (filepath.contains(filesources.get(i).getFilepath())) {
                source = filesources.get(i);
                break;
            }
        }

        return getLoginFromFilesource(source);
    }

    public static int COVER = 1, BACKDROP = 2;
    public static SmbFile getCustomCoverArt(String filepath, SmbLogin auth, int type) throws MalformedURLException, UnsupportedEncodingException, SmbException {
        String parentPath = filepath.substring(0, filepath.lastIndexOf("/"));
        if (!parentPath.endsWith("/"))
            parentPath += "/";

        String filename = filepath.substring(0, filepath.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();

        String[] list = MizuuApplication.getCifsFilesList(parentPath);

        if (list == null) {
            SmbFile s = new SmbFile(createSmbLoginString(
                    auth.getDomain(),
                    auth.getUsername(),
                    auth.getPassword(),
                    parentPath,
                    false));

            try {
                list = s.list();

                MizuuApplication.putCifsFilesList(parentPath, list);
            } catch (Exception e) {
                return null;
            }
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
                        absolutePath.equalsIgnoreCase(filename + "-poster.jpg") ||
                        absolutePath.equalsIgnoreCase(filename + "-poster.jpeg") ||
                        absolutePath.equalsIgnoreCase(filename + "-poster.tbn") ||
                        absolutePath.equalsIgnoreCase(filename + "-folder.jpg") ||
                        absolutePath.equalsIgnoreCase(filename + "-folder.jpeg") ||
                        absolutePath.equalsIgnoreCase(filename + "-folder.tbn") ||
                        absolutePath.equalsIgnoreCase(filename + "-cover.jpg") ||
                        absolutePath.equalsIgnoreCase(filename + "-cover.jpeg") ||
                        absolutePath.equalsIgnoreCase(filename + "-cover.tbn") ||
                        absolutePath.equalsIgnoreCase(filename + ".jpg") ||
                        absolutePath.equalsIgnoreCase(filename + ".jpeg") ||
                        absolutePath.equalsIgnoreCase(filename + ".tbn")) {
                    customCoverArt = absolutePath;
                    break;
                }
            }
        } else {
            for (int i = 0; i < list.length; i++) {
                name = list[i];
                absolutePath = parentPath + list[i];
                if (name.equalsIgnoreCase("fanart.jpg") ||
                        name.equalsIgnoreCase("fanart.jpeg") ||
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
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(customCoverArt))
            return new SmbFile(createSmbLoginString(
                    auth.getDomain(),
                    auth.getUsername(),
                    auth.getPassword(),
                    customCoverArt,
                    false));

        return null;
    }

    public static String[] subtitleFormats = new String[]{".srt", ".sub", ".ssa", ".ssf", ".smi", ".txt", ".usf", ".ass", ".stp", ".idx", ".aqt", ".cvd", ".dks", ".jss", ".mpl", ".pjs", ".psb", ".rt", ".svcd", ".usf"};

    public static boolean isSubtitleFile(String s) {
        int count = subtitleFormats.length;
        for (int i = 0; i < count; i++)
            if (s.endsWith(subtitleFormats[i]))
                return true;
        return false;
    }

    public static List<SmbFile> getSubtitleFiles(String filepath, SmbLogin auth) throws MalformedURLException, UnsupportedEncodingException {
        ArrayList<SmbFile> subs = new ArrayList<SmbFile>();

        String fileType = "";
        if (filepath.contains(".")) {
            fileType = filepath.substring(filepath.lastIndexOf("."));
        }

        int count = subtitleFormats.length;
        for (int i = 0; i < count; i++) {
            subs.add(new SmbFile(createSmbLoginString(
                    auth.getDomain(),
                    auth.getUsername(),
                    auth.getPassword(),
                    filepath.replace(fileType, subtitleFormats[i]),
                    false)));
        }

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
        StringBuilder sb = new StringBuilder("smb://");

        try {
            user = URLEncoder.encode(user, "utf-8");
        } catch (UnsupportedEncodingException e) {}
        try {
            password = URLEncoder.encode(password, "utf-8");
        } catch (UnsupportedEncodingException e) {}
        try {
            domain = URLEncoder.encode(domain, "utf-8");
        } catch (UnsupportedEncodingException e) {}

        user = user.replace("+", "%20");
        password = password.replace("+", "%20");
        domain = domain.replace("+", "%20");
        server = server.replace("smb://", "");

        // Only add domain, username and password details if the username isn't empty
        if (!TextUtils.isEmpty(user)) {
            // Add the domain details
            if (!TextUtils.isEmpty(domain))
                sb.append(domain).append(";");

            // Add username
            sb.append(user);

            // Add password
            if (!TextUtils.isEmpty(password))
                sb.append(":").append(password);

            sb.append("@");
        }

        sb.append(server);

        if (isFolder)
            if (!server.endsWith("/"))
                sb.append("/");

        return sb.toString();
    }

    public static void deleteShow(Context c, TvShow thisShow, boolean showToast) {
        // Create and open database
        DbAdapterTvShows dbHelper = MizuuApplication.getTvDbAdapter();
        boolean deleted = dbHelper.deleteShow(thisShow.getId());

        DbAdapterTvShowEpisodes db = MizuuApplication.getTvEpisodeDbAdapter();
        deleted = deleted && db.deleteAllEpisodes(thisShow.getId());

        if (deleted) {
            try {
                // Delete cover art image
                File coverArt = thisShow.getCoverPhoto();
                if (coverArt.exists() && coverArt.getAbsolutePath().contains("com.miz.mizuu")) {
                    MizLib.deleteFile(coverArt);
                }

                // Delete thumbnail image
                File thumbnail = thisShow.getThumbnail();
                if (thumbnail.exists() && thumbnail.getAbsolutePath().contains("com.miz.mizuu")) {
                    MizLib.deleteFile(thumbnail);
                }

                // Delete backdrop image
                File backdrop = new File(thisShow.getBackdrop());
                if (backdrop.exists() && backdrop.getAbsolutePath().contains("com.miz.mizuu")) {
                    MizLib.deleteFile(backdrop);
                }

                // Delete episode images
                File dataFolder = MizuuApplication.getTvShowEpisodeFolder(c);
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

    public static Request getJsonPostRequest(String url, JSONObject holder) {
        return new Request.Builder()
                .url(url)
                .addHeader("Content-type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), holder.toString()))
                .build();
    }

    public static Request getTraktAuthenticationRequest(String url, String username, String password) throws JSONException {
        JSONObject holder = new JSONObject();
        holder.put("username", username);
        holder.put("password", password);

        return new Request.Builder()
                .url(url)
                .addHeader("Content-type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), holder.toString()))
                .build();
    }

    public static boolean isMovieLibraryBeingUpdated(Context c) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        int count = services.size();
        for (int i = 0; i < count; i++) {
            if (MovieLibraryUpdate.class.getName().equals(services.get(i).service.getClassName())) {
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
            if (TvShowsLibraryUpdate.class.getName().equals(services.get(i).service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLocalCopyBeingDownloaded(Context c) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        int count = services.size();
        for (int i = 0; i < count; i++) {
            if (MakeAvailableOffline.class.getName().equals(services.get(i).service.getClassName())) {
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
        return 25 * 1024 * 1024; // 25 MB
    }

    public static String getTraktUserName(Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return settings.getString(TRAKT_USERNAME, "").trim();
    }

    public static String removeWikipediaNotes(String original) {
        original = original.trim().replaceAll("(?i)from wikipedia, the free encyclopedia.", "").replaceAll("(?i)from wikipedia, the free encyclopedia", "");
        original = original.replaceAll("(?m)^[ \t]*\r?\n", "");
        if (original.contains("Description above from the Wikipedia")) {
            original = original.substring(0, original.lastIndexOf("Description above from the Wikipedia"));
        }

        return original.trim();
    }

    public static String getParentFolder(String filepath) {
        try {
            return filepath.substring(0, filepath.lastIndexOf("/"));
        } catch (Exception e) {
            return "";
        }
    }

    public static void scheduleMovieUpdate(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        // Check if scheduled updates are enabled, and schedule the next update if this is the case
        if (settings.getInt(SCHEDULED_UPDATES_MOVIE, ScheduledUpdatesFragment.NOT_ENABLED) > ScheduledUpdatesFragment.AT_LAUNCH) {
            ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.MOVIES, context);
            long duration = MizLib.HOUR;
            switch (settings.getInt(SCHEDULED_UPDATES_MOVIE, ScheduledUpdatesFragment.NOT_ENABLED)) {
                case ScheduledUpdatesFragment.EVERY_2_HOURS:
                    duration = MizLib.HOUR * 2;
                    break;
                case ScheduledUpdatesFragment.EVERY_4_HOURS:
                    duration = MizLib.HOUR * 4;
                    break;
                case ScheduledUpdatesFragment.EVERY_6_HOURS:
                    duration = MizLib.HOUR * 6;
                    break;
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
        if (settings.getInt(SCHEDULED_UPDATES_TVSHOWS, ScheduledUpdatesFragment.NOT_ENABLED) > ScheduledUpdatesFragment.AT_LAUNCH) {
            ScheduledUpdatesAlarmManager.cancelUpdate(ScheduledUpdatesAlarmManager.SHOWS, context);
            long duration = MizLib.HOUR;
            switch (settings.getInt(SCHEDULED_UPDATES_TVSHOWS, ScheduledUpdatesFragment.NOT_ENABLED)) {
                case ScheduledUpdatesFragment.EVERY_2_HOURS:
                    duration = MizLib.HOUR * 2;
                    break;
                case ScheduledUpdatesFragment.EVERY_4_HOURS:
                    duration = MizLib.HOUR * 4;
                    break;
                case ScheduledUpdatesFragment.EVERY_6_HOURS:
                    duration = MizLib.HOUR * 6;
                    break;
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

    public static String decryptImdbId(String filename) {
        Pattern p = Pattern.compile("(tt\\d{7})");
        Matcher m = p.matcher(filename);
        if (m.find())
            return m.group(1);
        return null;
    }

    public static String decryptName(String input, String customTags) {
        if (TextUtils.isEmpty(input))
            return "";

        String output = getNameFromFilename(input);
        output = fixAbbreviations(output);

        // Used to remove [SET {title}] from the beginning of filenames
        if (output.startsWith("[") && output.contains("]")) {
            String after = "";

            if (output.matches("(?i)^\\[SET .*\\].*?")) {
                try {
                    after = output.substring(output.indexOf("]") + 1, output.length());
                } catch (Exception e) {}
            }

            if (!TextUtils.isEmpty(after))
                output = after;
        }

        output = output.replaceAll(WAREZ_PATTERN + "|\\)|\\(|\\[|\\]|\\{|\\}|\\'|\\<|\\>|\\-", "");

        // Improved support for French titles that start with C' or L'
        if (output.matches("(?i)^(c|l)(\\_|\\.)\\w.*?")) {
            StringBuilder sb = new StringBuilder(output);
            sb.replace(1, 2, "'");
            output = sb.toString();
        }

        if (!TextUtils.isEmpty(customTags)) {
            String[] custom = customTags.split("<MiZ>");
            int count = custom.length;
            for (int i = 0; i < count; i++)
                try {
                    output = output.replaceAll("(?i)" + custom[i], "");
                } catch (Exception e) {}
        }

        output = output.replaceAll("\\s\\-\\s|\\.|\\,|\\_", " "); // Remove separators
        output = output.trim().replaceAll("(?i)(part)$", ""); // Remove "part" in the end of the string
        output = output.trim().replaceAll("(?i)(?:s|season[ ._-]*)\\d{1,4}.*", ""); // Remove "season####" in the end of the string

        return output.replaceAll(" +", " ").trim(); // replaceAll() needed to remove all instances of multiple spaces
    }

    private final static String YEAR_PATTERN = "(18|19|20)[0-9][0-9]";
    private final static String WAREZ_PATTERN = "(?i)(dvdscreener|dvdscreen|dvdscr|dvdrip|dvd5|dvd|xvid|divx|m\\-480p|m\\-576p|m\\-720p|m\\-864p|m\\-900p|m\\-1080p|m480p|m576p|m720p|m864p|m900p|m1080p|480p|576p|720p|864p|900p|1080p|1080i|720i|mhd|brrip|bdrip|brscreener|brscreen|brscr|aac|x264|bluray|dts|screener|hdtv|ac3|repack|2\\.1|5\\.1|ac3_6|7\\.1|h264|hdrip|ntsc|proper|readnfo|rerip|subbed|vcd|scvd|pdtv|sdtv|hqts|hdcam|multisubs|650mb|700mb|750mb|webdl|web-dl|bts|korrip|webrip|korsub|1link|sample|tvrip|tvr|extended.editions?|directors cut|tfe|unrated|\\(.*?torrent.*?\\)|\\[.*?\\]|\\(.*?\\)|\\{.*?\\}|part[0-9]|cd[0-9])";
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
            } catch (Exception e) {}

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
            } catch (Exception e) {}
        }

        return result;
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
        options.inMutable = true;
        options.inPreferredConfig = Config.RGB_565;

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
    public static long getFreeMemory() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        if (hasJellyBeanMR2())
            return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        else
            return stat.getAvailableBlocks() * stat.getBlockSize();
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

    public static List<String> getSplitParts(String filepath, SmbLogin auth) throws MalformedURLException, UnsupportedEncodingException, SmbException {
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
                temp = new SmbFile(createSmbLoginString(
                        auth.getDomain(),
                        auth.getUsername(),
                        auth.getPassword(),
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

    public static int getNavigationBarWidth(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_width", "dimen", "android");
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

    public static String getRandomBackdropPath(Context c) {
        ArrayList<File> files = new ArrayList<File>();

        File[] f = MizuuApplication.getMovieBackdropFolder(c).listFiles();
        if (f != null)
            Collections.addAll(files, f);

        f = MizuuApplication.getTvShowBackdropFolder(c).listFiles();
        if (f != null)
            Collections.addAll(files, f);

        if (files.size() > 0) {
            Random rndm = new Random();
            return files.get(rndm.nextInt(files.size())).getAbsolutePath();
        }

        return "";
    }

    public static boolean isValidFilename(String name) {
        return !(name.startsWith(".") && MizLib.getCharacterCountInString(name, '.') == 1) && !name.startsWith("._");
    }

    public static boolean exists(String URLName){
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con =
                    (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(10000);
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if the device uses navigation controls as the primary navigation from a number of factors.
     * @param context Application Context
     * @return True if the device uses navigation controls, false otherwise.
     */
    public static boolean usesNavigationControl(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration.navigation == Configuration.NAVIGATION_NONAV) {
            return false;
        } else if (configuration.touchscreen == Configuration.TOUCHSCREEN_FINGER) {
            return false;
        } else if (configuration.navigation == Configuration.NAVIGATION_DPAD) {
            return true;
        } else if (configuration.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) {
            return true;
        } else if (configuration.touchscreen == Configuration.TOUCHSCREEN_UNDEFINED) {
            return true;
        } else if (configuration.navigationHidden == Configuration.NAVIGATIONHIDDEN_YES) {
            return true;
        } else if (configuration.uiMode == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        return false;
    }

    public static int getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

    public static String getPrettyTime(Context context, int minutes) {
        if (minutes == 0)
            return context.getString(R.string.stringNA);;
        try {
            int hours = (minutes / 60);
            minutes = (minutes % 60);
            String hours_string = hours + " " + context.getResources().getQuantityString(R.plurals.hour, hours, hours);
            String minutes_string = minutes + " " + context.getResources().getQuantityString(R.plurals.minute, minutes, minutes);
            if (hours > 0) {
                if (minutes == 0)
                    return hours_string;
                else
                    return hours_string + " " + minutes_string;
            } else {
                return minutes_string;
            }
        } catch (Exception e) { // Fall back if something goes wrong
            if (minutes > 0)
                return String.valueOf(minutes);
            return context.getString(R.string.stringNA);
        }
    }

    public static String getPrettyRuntime(Context context, int minutes) {
        if (minutes == 0) {
            return context.getString(R.string.stringNA);
        }

        int hours = (minutes / 60);
        minutes = (minutes % 60);

        if (hours > 0) {
            if (minutes == 0) {
                return hours + " " + context.getResources().getQuantityString(R.plurals.hour, hours, hours);
            } else {
                return hours + " " + context.getResources().getQuantityString(R.plurals.hour_short, hours, hours) + " " + minutes + " " + context.getResources().getQuantityString(R.plurals.minute_short, minutes, minutes);
            }
        } else {
            return minutes + " " + context.getResources().getQuantityString(R.plurals.minute, minutes, minutes);
        }
    }

    public static String getPrettyDate(Context context, String date) {
        if (!TextUtils.isEmpty(date)) {
            try {
                String[] dateArray = date.split("-");
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]));

                return MizLib.toCapitalFirstChar(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " + cal.get(Calendar.YEAR));
            } catch (Exception e) { // Fall back if something goes wrong
                return date;
            }
        } else {
            return context.getString(R.string.stringNA);
        }
    }

    public static String getPrettyDatePrecise(Context context, String date) {
        if (!TextUtils.isEmpty(date)) {
            try {
                String[] dateArray = date.split("-");
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]));

                return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(cal.getTime());
            } catch (Exception e) { // Fall back if something goes wrong
                return date;
            }
        } else {
            return context.getString(R.string.stringNA);
        }
    }

    public static String getPrettyDate(Context context, long millis) {
        if (millis > 0) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);

                return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(cal.getTime());
            } catch (Exception e) { // Fall back if something goes wrong
                return String.valueOf(millis);
            }
        } else {
            return context.getString(R.string.stringNA);
        }
    }

    public static Comparator<WebMovie> getWebMovieDateComparator() {
        return new Comparator<WebMovie>() {
            @Override
            public int compare(WebMovie o1, WebMovie o2) {
                // Dates are always presented as YYYY-MM-DD, so removing
                // the hyphens will easily provide a great way of sorting.

                int firstDate = 0, secondDate = 0;
                String first = "", second = "";

                if (o1.getRawDate() != null)
                    first = o1.getRawDate().replace("-", "");

                if (!TextUtils.isEmpty(first))
                    firstDate = Integer.valueOf(first);

                if (o2.getRawDate() != null)
                    second = o2.getRawDate().replace("-", "");

                if (!TextUtils.isEmpty(second))
                    secondDate = Integer.valueOf(second);

                // This part is reversed to get the highest numbers first
                if (firstDate < secondDate)
                    return 1; // First date is lower than second date - put it second
                else if (firstDate > secondDate)
                    return -1; // First date is greater than second date - put it first

                return 0; // They're equal
            }
        };
    }

    private static String[] mAdultKeywords = new String[]{"adult", "sex", "porn", "explicit", "penis", "vagina", "asshole",
            "blowjob", "cock", "fuck", "dildo", "kamasutra", "masturbat", "squirt", "slutty", "cum", "cunt"};

    public static boolean isAdultContent(Context context, String title) {
        // Check if the user has enabled adult content - if so, nothing should
        // be blocked and the method should return false regardless of the title
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(INCLUDE_ADULT_CONTENT, false))
            return false;

        String lowerCase = title.toLowerCase(Locale.getDefault());

        // Run through the keywords and check
        for (int i = 0; i < mAdultKeywords.length; i++)
            if (lowerCase.contains(mAdultKeywords[i]))
                return true;

        // Certain titles include "XXX" (all caps), so test this against the normal-case title as a last check
        return title.contains("XXX");
    }

    public static boolean isNumber(String runtime) {
        return TextUtils.isDigitsOnly(runtime);
    }

    public static boolean isValidTmdbId(String id) {
        return !TextUtils.isEmpty(id) && !id.equals(DbAdapterMovies.UNIDENTIFIED_ID) && isNumber(id);
    }

    /**
     * Helper method to remove a ViewTreeObserver correctly, i.e.
     * avoiding the deprecated method on API level 16+.
     * @param vto
     * @param victim
     */
    @SuppressWarnings("deprecation")
    public static void removeViewTreeObserver(ViewTreeObserver vto, OnGlobalLayoutListener victim) {
        if (MizLib.hasJellyBean()) {
            vto.removeOnGlobalLayoutListener(victim);
        } else {
            vto.removeGlobalOnLayoutListener(victim);
        }
    }

    public static String getTmdbImageBaseUrl(Context context) {
        long time = PreferenceManager.getDefaultSharedPreferences(context).getLong(TMDB_BASE_URL_TIME, 0);
        long currentTime = System.currentTimeMillis();

        // We store the TMDb base URL for 24 hours
        if (((currentTime - time) < DAY && PreferenceManager.getDefaultSharedPreferences(context).contains(TMDB_BASE_URL)) |
                Looper.getMainLooper().getThread() == Thread.currentThread()) {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(TMDB_BASE_URL, "");
        }

        try {
            JSONObject configuration = getJSONObject(context, "https://api.themoviedb.org/3/configuration?api_key=" + getTmdbApiKey(context));
            String baseUrl = configuration.getJSONObject("images").getString("secure_base_url");

            Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(TMDB_BASE_URL, baseUrl);
            editor.putLong(TMDB_BASE_URL_TIME, System.currentTimeMillis());
            editor.commit();

            return baseUrl;
        } catch (JSONException e) {
            return null;
        }
    }

    public static void showSelectFileDialog(Context context, ArrayList<Filepath> paths, final Dialog.OnClickListener listener) {
        String[] items = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++)
            items[i] = paths.get(i).getFilepath();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.selectFile));
        builder.setItems(items, listener);
        builder.show();
    }

    public static String getFilenameWithoutExtension(String filename) {
        try {
            return filename.substring(0, filename.lastIndexOf("."));
        } catch (IndexOutOfBoundsException e) {
            return filename;
        }
    }

    public static void copyDatabase(Context context) {
        try {
            FileUtils.copyFile(context.getDatabasePath("mizuu_data"), new File(Environment.getExternalStorageDirectory(), "mizuu_data.db"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}