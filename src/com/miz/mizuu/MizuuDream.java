package com.miz.mizuu;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

@SuppressLint("NewApi")
public class MizuuDream extends DreamService implements ViewFactory {

	private DbAdapter dbHelper;
	private DbAdapterTvShow dbHelperTv;
	private ArrayList<Backdrop> backdrops = new ArrayList<Backdrop>();
	private long interval = 10000;
	private int index = 0;
	private boolean isRunning = false;
	private TextView title;

	@Override
	public void onDreamingStopped() {
		isRunning = false;
		backdrops.clear();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		isRunning = true;

		// Exit dream upon user touch
		setInteractive(false);
		// Hide system UI
		setFullscreen(true);
		// Set the dream layout
		setContentView(R.layout.welcome);

		findViewById(R.id.list).setVisibility(View.GONE);
		
		title = (TextView) findViewById(R.id.title);
		if (MizLib.runsOnTablet(this))
			title.setTextSize(26f);

		dbHelper = MizuuApplication.getMovieAdapter();
		dbHelperTv = MizuuApplication.getTvDbAdapter();

		Cursor cursor = dbHelper.fetchAllMovies(DbAdapter.KEY_TITLE + " ASC", false, false);
		while (cursor.moveToNext()) {
			try {
				backdrops.add(new Backdrop(
						new File(MizLib.getMovieBackdropFolder(this),
								cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)) + "_bg.jpg").getAbsolutePath(),
								cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE))
						));
			} catch (NullPointerException e) {}
		}
		cursor = dbHelperTv.getAllShows();
		while (cursor.moveToNext()) {
			try {
				backdrops.add(new Backdrop(
						new File(MizLib.getTvShowBackdropFolder(this),
								cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)) + "_tvbg.jpg").getAbsolutePath(),
								cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE))
						));
			} catch (NullPointerException e) {}
		}
		cursor.close();

		Collections.shuffle(backdrops, new Random(System.nanoTime()));

		if (backdrops.size() > 0) {
			title.setText(backdrops.get(index).getTitle());
			startAnimatedBackground();
		}
	}

	private void startAnimatedBackground() {
		Animation aniIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		aniIn.setDuration(800);
		Animation aniOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		aniOut.setDuration(800);

		final ImageSwitcher imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher1);
		imageSwitcher.setInAnimation(aniIn);
		imageSwitcher.setOutAnimation(aniOut);
		imageSwitcher.setFactory(this);
		imageSwitcher.setImageURI(Uri.parse(backdrops.get(index).getPath()));

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (isRunning) {
					index++;
					index = index % backdrops.size();
					imageSwitcher.setImageURI(Uri.parse(backdrops.get(index).getPath()));
					
					final Animation aniIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
					Animation aniOut = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
					aniOut.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationEnd(Animation animation) {
							title.setText(backdrops.get(index).getTitle());
							title.startAnimation(aniIn);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {}

						@Override
						public void onAnimationStart(Animation animation) {}
					});

					title.startAnimation(aniOut);
					
					handler.postDelayed(this, interval);
				}
			}
		};
		handler.postDelayed(runnable, interval);

	}

	@Override
	public View makeView() {
		ImageView imageView = new ImageView(this);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return imageView;
	}

	private class Backdrop {
		String path, title;

		public Backdrop(String path, String title) {
			this.path = path;
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public String getPath() {
			return path;
		}
	}
}