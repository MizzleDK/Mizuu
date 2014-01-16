package com.miz.mizuu.fragments;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.miz.db.DbAdapterTvShow;
import com.miz.functions.AspectRatioImageViewCover;
import com.miz.functions.ImageLoad;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.R;

public class ShowDetailsFragment extends Fragment {

	private DbAdapterTvShow dbHelper;
	private TvShow thisShow;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private AspectRatioImageViewCover cover;
	private ImageView background;
	private Typeface tf;
	private boolean ignorePrefixes;

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

		tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Thin.ttf");

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

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-show-backdrop-change"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isAdded())
				getActivity().recreate();
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
	
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		background = (ImageView) v.findViewById(R.id.imageBackground);
		textTitle = (TextView) v.findViewById(R.id.movieTitle);
		textPlot = (TextView) v.findViewById(R.id.textView2);
		textGenre = (TextView) v.findViewById(R.id.textView7);
		textRuntime = (TextView) v.findViewById(R.id.textView9);
		textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
		textRating = (TextView) v.findViewById(R.id.textView12);
		textTagline = (TextView) v.findViewById(R.id.textView6);
		textCertification = (TextView) v.findViewById(R.id.textView11);
		cover = (AspectRatioImageViewCover) v.findViewById(R.id.traktIcon);

		// Set the show title
		textTitle.setVisibility(View.VISIBLE);
		textTitle.setText(thisShow.getTitle());
		textTitle.setTypeface(tf);
		textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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
		try {
			int hours = Integer.parseInt(thisShow.getRuntime()) / 60;
			int minutes = Integer.parseInt(thisShow.getRuntime()) % 60;
			String hours_string = hours + " " + getResources().getQuantityString(R.plurals.hour, hours, hours);
			String minutes_string = minutes + " " + getResources().getQuantityString(R.plurals.minute, minutes, minutes);
			if (hours > 0) {
				if (minutes == 0)
					textRuntime.setText(hours_string);
				else
					textRuntime.setText(hours_string + " " + minutes_string);
			} else {
				textRuntime.setText(minutes_string);
			}
		} catch (Exception e) { // Fall back if something goes wrong
			if (!MizLib.isEmpty(thisShow.getRuntime())) {
				textRuntime.setText(thisShow.getRuntime() + " " + getString(R.string.minutes));
			} else {
				textRuntime.setText(R.string.stringNA);
			}
		}

		// Set the genre first aired date
		try {
			String[] date = thisShow.getFirstAirdate().split("-");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));

			textReleaseDate.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(cal.getTime()));
		} catch (Exception e) { // Fall back if something goes wrong
			if (!MizLib.isEmpty(thisShow.getFirstAirdate())) {
				textReleaseDate.setText(thisShow.getFirstAirdate());
			} else {
				textReleaseDate.setText(R.string.stringNA);
				
			}
		}

		// Set the show rating		
		if (!thisShow.getRating().equals("0.0/10")) {
			if (thisShow.getRating().contains("/")) {
				textRating.setText(Html.fromHtml(thisShow.getRating().replace("/", "<small> / ") + "</small>"));
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
		if (!MizLib.runsInPortraitMode(getActivity())) {
			ImageLoad posterLoader = new ImageLoad();
			posterLoader.setFileUrl(thisShow.getCoverPhoto());
			posterLoader.setOnFailResource(R.drawable.loading_image);
			posterLoader.setImageView(cover);
			posterLoader.setDuration(0);
			posterLoader.execute();

			ImageLoad backdropLoader = new ImageLoad();
			backdropLoader.setFileUrl(thisShow.getBackdrop());
			backdropLoader.setImageView(background);
			backdropLoader.setOnFailResource(R.drawable.bg);
			backdropLoader.setDuration(0);
			backdropLoader.execute();
		} else {
			ImageLoad backdropLoader = new ImageLoad();
			backdropLoader.setFileUrl(thisShow.getBackdrop());
			backdropLoader.setImageView(background);
			backdropLoader.setOnFailResource(R.drawable.bg);
			backdropLoader.setDuration(0);
			backdropLoader.execute();
		}
	}
}