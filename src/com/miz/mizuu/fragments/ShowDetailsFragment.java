package com.miz.mizuu.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MizLib;
import com.miz.functions.ObservableScrollView;
import com.miz.functions.ObservableScrollView.OnScrollChangedListener;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ShowDetailsFragment extends Fragment {

	private DbAdapterTvShow dbHelper;
	private TvShow thisShow;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private ImageView background, cover;
	private boolean ignorePrefixes;
	private Picasso mPicasso;
	private Typeface mLight;
	private Drawable mActionBarBackgroundDrawable;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ShowDetailsFragment() {}

	public static ShowDetailsFragment newInstance(String showId) {
		ShowDetailsFragment pageFragment = new ShowDetailsFragment();
		Bundle b = new Bundle();
		b.putString("showId", showId);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		
		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnorePrefixesInTitles", false);

		// Create and open database
		dbHelper = MizuuApplication.getTvDbAdapter();

		Cursor cursor = dbHelper.getShow(getArguments().getString("showId"));
		while (cursor.moveToNext()) {
			thisShow = new TvShow(getActivity(),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_GENRES)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_CERTIFICATION)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_FIRST_AIRDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_RUNTIME)),
					ignorePrefixes,
					cursor.getString(cursor.getColumnIndex(DbAdapterTvShow.KEY_SHOW_EXTRA1))
					);
		}

		cursor.close();
		
		mPicasso = MizuuApplication.getPicasso(getActivity());

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-backdrop-change"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadImages();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.show_details, container, false);
	}
	
	public void onViewCreated(final View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		if (MizLib.isPortrait(getActivity())) {
			mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x00000000, 0xaa000000});
			getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);

			ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = v.findViewById(R.id.imageBackground).getHeight() - getActivity().getActionBar().getHeight();
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);
					mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "080808"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "080808") : 0xaa080808});
					getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
				}
			});
		}
		
		background = (ImageView) v.findViewById(R.id.imageBackground);
		textTitle = (TextView) v.findViewById(R.id.movieTitle);
		textPlot = (TextView) v.findViewById(R.id.textView2);
		textGenre = (TextView) v.findViewById(R.id.textView7);
		textRuntime = (TextView) v.findViewById(R.id.textView9);
		textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
		textRating = (TextView) v.findViewById(R.id.textView12);
		textTagline = (TextView) v.findViewById(R.id.textView6);
		textCertification = (TextView) v.findViewById(R.id.textView11);
		cover = (ImageView) v.findViewById(R.id.traktIcon);

		// Set the show title
		textTitle.setVisibility(View.VISIBLE);
		textTitle.setText(thisShow.getTitle());
		textTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		textPlot.setTypeface(mLight);
		textGenre.setTypeface(mLight);
		textRuntime.setTypeface(mLight);
		textReleaseDate.setTypeface(mLight);
		textRating.setTypeface(mLight);
		textCertification.setTypeface(mLight);
		
		// Set the show plot
		textPlot.setText(thisShow.getDescription());

		textTagline.setVisibility(TextView.GONE);

		// Set the show genre
		if (!MizLib.isEmpty(thisShow.getGenres())) {
			textGenre.setText(thisShow.getGenres());
		} else {
			textGenre.setText(R.string.stringNA);
		}

		// Set the show runtime
		textRuntime.setText(MizLib.getPrettyTime(getActivity(), Integer.parseInt(thisShow.getRuntime())));

		// Set the first aired date
		textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisShow.getFirstAirdate()));

		// Set the show rating
		if (!thisShow.getRating().equals("0.0/10")) {
			if (thisShow.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(thisShow.getRating().substring(0, thisShow.getRating().indexOf("/"))) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(Html.fromHtml(thisShow.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				textRating.setText(thisShow.getRating());
			}
		} else {
			textRating.setText(R.string.stringNA);
		}

		// Set the show certification
		if (!MizLib.isEmpty(thisShow.getCertification())) {
			textCertification.setText(thisShow.getCertification());
		} else {
			textCertification.setText(R.string.stringNA);
		}

		loadImages();
	}

	private void loadImages() {
		if (!MizLib.isPortrait(getActivity())) {
			mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
			mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
		} else {
			mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
			mPicasso.load(thisShow.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
				@Override
				public void onError() {
					mPicasso.load(thisShow.getCoverPhoto()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
				}

				@Override
				public void onSuccess() {}					
			});
		}
	}
}