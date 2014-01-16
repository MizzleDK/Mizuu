package com.miz.mizuu.fragments;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.miz.functions.AspectRatioImageViewCover;
import com.miz.functions.MizLib;
import com.miz.functions.TMDb;
import com.miz.functions.TMDbMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class TmdbMovieDetailsFragment extends Fragment {

	private String movieId;
	private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private AspectRatioImageViewCover cover;
	private ImageView background;
	private Typeface tf;
	private TMDb tmdb;
	private TMDbMovie thisMovie;
	private View movieDetailsLayout, progressBar;
	private FrameLayout container;
	private boolean isRetained = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public TmdbMovieDetailsFragment() {}

	public static TmdbMovieDetailsFragment newInstance(String movieId) { 
		TmdbMovieDetailsFragment pageFragment = new TmdbMovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}
	
	public static TmdbMovieDetailsFragment newInstance(String movieId, String json) { 
		TmdbMovieDetailsFragment pageFragment = new TmdbMovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("movieId", movieId);
		bundle.putString("json", json);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		tmdb = new TMDb(getActivity());

		// Get the database ID of the movie in question
		movieId = getArguments().getString("movieId");

		tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Thin.ttf");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.movie_details, container, false);

		movieDetailsLayout = v.findViewById(R.id.movieDetailsLayout);
		progressBar = v.findViewById(R.id.progressBar1);

		this.container = (FrameLayout) v.findViewById(R.id.container);
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
		cover.setImageResource(R.drawable.loading_image);

		// Get rid of these...
		v.findViewById(R.id.imageView7).setVisibility(View.GONE); // File
		v.findViewById(R.id.textView3).setVisibility(View.GONE); // File
		v.findViewById(R.id.imageView2).setVisibility(View.GONE); // Play button

		if (!isRetained) { // Nothing has been retained - load the data
			setLoading(true);
			new MovieLoader().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, getArguments().getString("json"));
			isRetained = true;
		} else {
			setupFields();
		}

		return v;
	}

	private class MovieLoader extends AsyncTask<String, Object, Object> {
		@Override
		protected Object doInBackground(String... params) {
			thisMovie = tmdb.getMovie(movieId, params[0], "en");
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			setupFields();
		}
	}

	private void setupFields() {
		if (isAdded() && thisMovie != null) {
			// Set the movie title
			textTitle.setVisibility(View.VISIBLE);
			textTitle.setText(thisMovie.getTitle());
			textTitle.setTypeface(tf);
			textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			// Set the movie plot
			textPlot.setText(thisMovie.getPlot());

			// Set movie tag line
			if (thisMovie.getTagline().equals("NOTAGLINE") || thisMovie.getTagline().isEmpty())
				textTagline.setVisibility(TextView.GONE);
			else
				textTagline.setText(thisMovie.getTagline());

			// Set the movie genre
			if (!MizLib.isEmpty(thisMovie.getGenres())) {
				textGenre.setText(thisMovie.getGenres());
			} else {
				textGenre.setText(R.string.stringNA);
			}

			// Set the movie runtime
			try {
				int hours = Integer.parseInt(thisMovie.getRuntime()) / 60;
				int minutes = Integer.parseInt(thisMovie.getRuntime()) % 60;
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
				if (!MizLib.isEmpty(thisMovie.getRuntime())) {
					textRuntime.setText(thisMovie.getRuntime());
				} else {
					textRuntime.setText(R.string.stringNA);
				}
			}

			// Set the movie release date
			if (!MizLib.isEmpty(thisMovie.getReleasedate())) {
				try {
					String[] date = thisMovie.getReleasedate().split("-");
					Calendar cal = Calendar.getInstance();
					cal.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));

					textReleaseDate.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(cal.getTime()));
				} catch (Exception e) { // Fall back if something goes wrong
					textReleaseDate.setText(thisMovie.getReleasedate());
				}
			} else {
				textReleaseDate.setText(R.string.stringNA);
			}

			// Set the movie rating
			if (!thisMovie.getRating().equals("0.0")) {
				thisMovie.setRating(thisMovie.getRating() + "/10");
				textRating.setText(Html.fromHtml(thisMovie.getRating().replace("/", "<small> / ") + "</small>"));
			} else {
				textRating.setText(R.string.stringNA);
			}

			// Set the movie certification
			if (!MizLib.isEmpty(thisMovie.getCertification())) {
				textCertification.setText(thisMovie.getCertification());
			} else {
				textCertification.setText(R.string.stringNA);
			}

			setLoading(false);
			
			ImageLoader.getInstance().displayImage(thisMovie.getCover(), cover, MizuuApplication.getDefaultCoverLoadingOptions());
			ImageLoader.getInstance().displayImage(thisMovie.getBackdrop(), background, MizuuApplication.getBackdropLoadingOptions(), new SimpleImageLoadingListener() {
				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
					ImageLoader.getInstance().displayImage(thisMovie.getCover(), background, MizuuApplication.getDefaultCoverLoadingOptions());
				}
			});
		}
	}

	private void setLoading(boolean isLoading) {
		if (isLoading) {
			progressBar.setVisibility(View.VISIBLE);
			movieDetailsLayout.setVisibility(View.GONE);
			container.setBackgroundResource(R.drawable.bg);
		} else {
			progressBar.setVisibility(View.GONE);
			movieDetailsLayout.setVisibility(View.VISIBLE);
			container.setBackgroundResource(0);
		}
	}
}
