package com.miz.mizuu.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class ActorBiographyFragment extends Fragment {

	private String mName, mBio, mBirth, mBirthday, mImage;
	private TextView mActorName, mActorBio, mActorBirth, mActorBirthday;
	private ImageView mActorImage, mActorImageBackground;
	private ProgressBar pbar;
	private Bitmap image;
	private Typeface tf;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBiographyFragment() {}

	public static ActorBiographyFragment newInstance(String actorId) { 
		ActorBiographyFragment pageFragment = new ActorBiographyFragment();
		Bundle bundle = new Bundle();
		bundle.putString("actorId", actorId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Thin.ttf");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actor_bio, container, false);
	}
	
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		if (!MizLib.runsInPortraitMode(getActivity()))
			MizLib.addActionBarMargin(getActivity(), v.findViewById(R.id.linearLayout1));
		
		pbar = (ProgressBar) v.findViewById(R.id.progress);
		mActorBirthday = (TextView) v.findViewById(R.id.overviewMessage);
		mActorBirth = (TextView) v.findViewById(R.id.textView2);
		mActorName = (TextView) v.findViewById(R.id.textView3);
		mActorName.setTypeface(tf);
		mActorName.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		if (MizLib.runsOnTablet(getActivity()))
			mActorName.setTextSize(48f);
		mActorBio = (TextView) v.findViewById(R.id.textView4);
		mActorImage = (ImageView) v.findViewById(R.id.traktIcon);
		mActorImageBackground = (ImageView) v.findViewById(R.id.imageView2);
		
		if (!MizLib.runsInPortraitMode(getActivity())) {
			mActorImageBackground.setImageResource(R.drawable.bg);
		}

		if (image == null) // Check if the image has been retained
			new GetActorDetails().execute(getArguments().getString("actorId"));
		else {
			mActorName.setText(mName);
			mActorBio.setText(mBio);
			mActorBirth.setText(mBirth);
			mActorBirthday.setText(mBirthday);
			mActorImage.setImageBitmap(image);
			mActorImage.setVisibility(View.VISIBLE);
			new BlurImage().execute();
			pbar.setVisibility(View.GONE);
		}
	}

	protected class GetActorDetails extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httppost = new HttpGet("https://api.themoviedb.org/3/configuration?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String baseUrl = httpclient.execute(httppost, responseHandler);

				JSONObject jObject = new JSONObject(baseUrl);
				try { baseUrl = jObject.getJSONObject("images").getString("base_url");
				} catch (Exception e) { baseUrl = "http://cf2.imgobject.com/t/p/"; }

				httpclient = new DefaultHttpClient();
				httppost = new HttpGet("https://api.themoviedb.org/3/person/" + params[0] + "?api_key=" + MizLib.TMDB_API);
				httppost.setHeader("Accept", "application/json");
				responseHandler = new BasicResponseHandler();
				String html = httpclient.execute(httppost, responseHandler);

				jObject = new JSONObject(html);

				try {
					mName = jObject.getString("name");
				} catch (Exception e) {}

				try {
					mBio = jObject.getString("biography");
				} catch (Exception e) {
					mBio = "";
				}

				if (mBio.equals("null")) mBio = "";
				
				mBio = MizLib.removeWikipediaNotes(mBio);

				try {
					mBirth = jObject.getString("place_of_birth");
				} catch (Exception e) {
					mBirth = "";
				}

				if (mBirth.equals("null")) mBirth = "";

				try {
					mBirthday = jObject.getString("birthday");
				} catch (Exception e) {
					mBirthday = "";
				}

				if (mBirthday.equals("null")) mBirthday = "";

				try {
					mImage = baseUrl + "h632" + jObject.getString("profile_path");
				} catch (Exception e) {
					mImage = "";
				}

				if (!MizLib.isEmpty(mImage) && !mImage.contains("null"))
					image = BitmapFactory.decodeStream(new java.net.URL(mImage).openStream());
				else
					image = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.noactor);

			} catch (Exception e) { e.printStackTrace(); }

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);

				mActorName.setText(mName);

				if (!MizLib.isEmpty(mBio))
					mActorBio.setText(mBio);
				else
					mActorBio.setVisibility(View.GONE);

				if (!MizLib.isEmpty(mBirth))
					mActorBirth.setText(mBirth);
				else
					mActorBirth.setVisibility(View.GONE);

				if (!MizLib.isEmpty(mBirthday))
					mActorBirthday.setText(mBirthday);
				else
					mActorBirthday.setVisibility(View.GONE);

				mActorImage.setImageBitmap(image);
				mActorImage.setVisibility(View.VISIBLE);

				new BlurImage().execute();
			}
		}
	}

	protected class BlurImage extends AsyncTask<String, String, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... params) {
			if (image != null)
				return MizLib.fastblur(getActivity(), Bitmap.createScaledBitmap(image, image.getWidth() / 3, image.getHeight() / 3, false), 4);
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				mActorImageBackground.setImageBitmap(result);
				mActorImageBackground.setColorFilter(Color.parseColor("#AA181818"), android.graphics.PorterDuff.Mode.SRC_OVER);
			}
		}
	}
}