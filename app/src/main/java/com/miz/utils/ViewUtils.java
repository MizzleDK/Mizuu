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

package com.miz.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.miz.functions.Actor;
import com.miz.functions.GridSeason;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.squareup.picasso.Picasso;

import java.util.List;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;

public class ViewUtils {

    private ViewUtils() {} // No instantiation

    /**
     * Returns a actor card with name, character, image and click listener.
     * @param context
     * @param picasso
     * @param actor
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupActorCard(final Activity context, Picasso picasso, final Actor actor) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(actor.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(actor.getName());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(actor.getCharacter());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorIntent(context, actor));
            }
        });

        return v;
    }

    /**
     * Returns a movie card with title, release date, image and click listener.
     * @param context
     * @param picasso
     * @param movie
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupMovieCard(final Activity context, Picasso picasso, final WebMovie movie) {
        final View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(movie.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(movie.getTitle());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(movie.getSubtitle());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(context, v.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(context, IntentUtils.getTmdbMovieDetails(context, movie), options.toBundle());
            }
        });

        return v;
    }

    /**
     * Returns a TV show card with title, release date, image and click listener.
     * @param context
     * @param picasso
     * @param show
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupTvShowCard(final Context context, Picasso picasso, final WebMovie show) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(show.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(show.getTitle());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(show.getSubtitle());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getTmdbTvShowLink(context, show));
            }
        });

        return v;
    }

    /**
     * Returns a TV show season card with title, release date, image and click listener.
     * @param context
     * @param picasso
     * @param season
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupTvShowSeasonCard(final Activity context, Picasso picasso, final GridSeason season, final int toolbarColor) {
        final View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(season.getCover()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(
            MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(season.getHeaderText());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(season.getSimpleSubtitleText());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivityForResult(IntentUtils.getTvShowSeasonIntent(context, season.getShowId(), season.getSeason(), season.getEpisodeCount(), toolbarColor), 0);
            }
        });

        return v;
    }

    /**
     * Returns a photo card with image and click listener.
     * @param context
     * @param picasso
     * @param url
     * @param items
     * @param index
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupPhotoCard(final Context context, Picasso picasso, final String url, final List<String> items, final int index) {
        final View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_no_text, null);

        // Load image
        picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorPhotoIntent(context, items, index));
            }
        });

        return v;
    }

    /**
     * Returns a photo card with image and click listener.
     * @param context
     * @param picasso
     * @param url
     * @param items
     * @param index
     * @return
     */
    @SuppressLint("InflateParams")
    public static View setupTaggedPhotoCard(final Context context, Picasso picasso, final String url, final List<String> items, final int index) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_landscape_no_text, null);

        // Load image
        picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorTaggedPhotoIntent(context, items, index));
            }
        });

        return v;
    }

    /**
     * Animates the transition when changing the maxLines
     * attribute of a TextView.
     * @param text
     * @param maxLines
     */
    public static void animateTextViewMaxLines(TextView text, int maxLines) {
        try {
            ObjectAnimator animation = ObjectAnimator.ofInt(text, "maxLines", maxLines);
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setDuration(200);
            animation.start();
        } catch (Exception e) {
            // Some devices crash at runtime when using the ObjectAnimator
            text.setMaxLines(maxLines);
        }
    }

    public static void animateFabJump(View fab, Animator.AnimatorListener listener) {
        try {
            ObjectAnimator animation = ObjectAnimator.ofFloat(fab, "translationY", -10f, -5f, 0f, 5f, 10f, 5f, 0f, -5f, -10f, -5f, 0f);
            animation.setDuration(350);
            animation.addListener(listener);
            animation.start();
        } catch (Exception e) {
            // Some devices crash at runtime when using the ObjectAnimator
        }
    }

    private static int defaultTitleTextColor = -1;

    /**
     * Update the Toolbar background color and title.
     * @param activity
     * @param toolbar
     * @param alpha
     * @param title
     * @param color
     */
    public static void updateToolbarBackground(Activity activity, Toolbar toolbar,
                                               int alpha, String title, int color) {
        if (defaultTitleTextColor == -1) {
            int[] textColorAttr = new int[]{R.attr.actionMenuTextColor};
            TypedValue typedValue = new TypedValue();
            int indexOfAttrTextColor = 0;
            TypedArray a = activity.obtainStyledAttributes(typedValue.data, textColorAttr);
            defaultTitleTextColor = a.getColor(indexOfAttrTextColor, -1);
            a.recycle();
        }
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(adjustAlpha(defaultTitleTextColor, alpha));
        int toolbarColor = adjustAlpha(color, alpha);
        if (MizLib.hasJellyBean()) {
            int topColor = darkenColor(color, alpha / 255f);
            topColor = adjustAlpha(topColor, Math.max(125, alpha));
            int[] colors = {topColor, toolbarColor};
            toolbar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors));
        } else {
            toolbar.setBackgroundColor(toolbarColor);
        }
    }

    public static int adjustAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int darkenColor(int color, float factor) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(hsv);
    }

    public static boolean isTranslucentDecorAvailable(Context context) {
        int id = context.getResources().getIdentifier("config_enableTranslucentDecor", "bool", "android");
        return (id == 0) && context.getResources().getBoolean(id);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavigationDrawerWidth(Context context) {
        int drawerWidth;

        // The navigation drawer should have a width equal to
        // the screen width minus the Toolbar height - at least
        // on mobile devices. Tablets are accounted for below.
        int toolbarHeight = MizLib.getActionBarHeight(context);

        // Get the display size
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);

        // Get the smallest number
        int smallestDisplayWidth = Math.min(size.x, size.y);

        // Calculate the drawer width
        drawerWidth = smallestDisplayWidth - toolbarHeight;

        // Make sure that the calculated drawer width
        // isn't greater than the max width, i.e.
        // 5 times the standard increment (56dp for
        // mobile or 64dp for tablets).
        int maxWidth = MizLib.convertDpToPixels(context, 5 * (MizLib.isTablet(context) ? 64 : 56));

        if (drawerWidth > maxWidth)
            drawerWidth = maxWidth;

        return drawerWidth;
    }

    /**
     * Since Toolbar needs to be bigger than the default height in some cases,
     * this method will change the height to include the status bar on Kitkat and newer.
     * @param context
     * @param toolbar
     */
    public static void setProperToolbarSize(Context context, Toolbar toolbar) {
        toolbar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                MizLib.hasKitKat() ? MizLib.getActionBarAndStatusBarHeight(context) : MizLib.getActionBarHeight(context)));
    }

    public static void setupWindowFlagsForStatusbarOverlay(Window window, boolean setBackgroundResource) {

        if (MizLib.isKitKat()) {
            // If we're running on KitKat, we want to enable
            // the translucent status bar
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (MizLib.hasKitKat()) {
            // If we're running on KitKat or above, we want to show
            // the background image beneath the status bar as well.
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // Make the status bar color transparent to begin with
        if (MizLib.hasLollipop())
            window.setStatusBarColor(Color.TRANSPARENT);

        // If requested, set a background resource on the Window object
        if (setBackgroundResource)
            window.setBackgroundDrawableResource(R.drawable.bg);
    }

    public static void setLayoutParamsForDetailsEmptyView(Context context, View layout, ImageView background, ObservableScrollView scrollView, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (!MizLib.isPortrait(context)) {
            // Let's set the size of the empty view on the scroll container
            View empty = layout.findViewById(R.id.empty_view);

            if (empty == null)
                return;

            // First, we get the height of the background image, since that
            // fills the available screen estate in its entirety
            int fullHeight = background.getHeight();

            // Then we get the content height - this is how much of the content
            // will be shown on the screen at a minimum
            int contentHeight = context.getResources().getDimensionPixelSize(R.dimen.content_details_main_height);

            // Finally we set the empty view to fill the width and have a height
            // that fills the gap between the full height and content height
            empty.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullHeight - contentHeight));
        }

        // Remove the ViewTreeObserver when we're done :-)
        MizLib.removeViewTreeObserver(scrollView.getViewTreeObserver(), listener);
    }

    public static void handleOnScrollChangedEvent(Activity activity, View layout, View background, String title, int height, int t, Toolbar toolbar, int toolbarColor) {
        final int headerHeight = (MizLib.isPortrait(activity) ? background.getHeight() : layout.findViewById(R.id.empty_view).getHeight()) - height;
        final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
        final int newAlpha = (int) (ratio * 255);

        // Update the Toolbar
        ViewUtils.updateToolbarBackground(activity, toolbar, newAlpha, title, toolbarColor);

        if (MizLib.isPortrait(activity)) {
            // Such parallax, much wow
            background.setPadding(0, (int) (t / 1.5), 0, 0);
        }

        // Update scroll background in landscape mode
        if (!MizLib.isPortrait(activity)) {
            View v = layout.findViewById(R.id.background_view);
            View v2 = layout.findViewById(R.id.background_view_more);
            final int backgroundAlpha = (int) (ratio * 15) + 240;

            v.setBackgroundColor(ViewUtils.adjustAlpha(Color.parseColor("#303030"), backgroundAlpha));
            v2.setBackgroundColor(ViewUtils.adjustAlpha(Color.parseColor("#303030"), backgroundAlpha));
        }
    }

    public static void setToolbarAndStatusBarColor(android.support.v7.app.ActionBar actionBar, Window window, int color) {
        if (color != 0) {
            actionBar.setBackgroundDrawable(new ColorDrawable(color));
            if (MizLib.hasLollipop())
                window.setStatusBarColor(color);
        }
    }

    public static int getGridViewThumbSize(Context context) {
        int thumbSize = 0;

        String thumbnailSize = PreferenceManager.getDefaultSharedPreferences(context).getString(GRID_ITEM_SIZE, context.getString(R.string.normal));
        if (thumbnailSize.equals(context.getString(R.string.large)))
            thumbSize = (int) (context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
        else if (thumbnailSize.equals(context.getString(R.string.normal)))
            thumbSize = (context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1);
        else
            thumbSize = (int) (context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);

        return thumbSize;
    }
}
