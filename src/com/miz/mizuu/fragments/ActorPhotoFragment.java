package com.miz.mizuu.fragments;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.miz.mizuu.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class ActorPhotoFragment extends Fragment {

	private String photoUrl;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;
	private ProgressBar pbar;

	public static ActorPhotoFragment newInstance(String url) {
		ActorPhotoFragment frag = new ActorPhotoFragment();
		Bundle b = new Bundle();
		b.putString("photo", url);
		frag.setArguments(b);
		return frag;
	}

	public ActorPhotoFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		

		setRetainInstance(true);

		photoUrl = getArguments().getString("photo");

		imageLoader = ImageLoader.getInstance();
		options = new DisplayImageOptions.Builder()
		.showImageOnFail(R.drawable.noactor)
		.cacheInMemory(true)
		.cacheOnDisc(true)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.photo, container, false);
	}
	
	@Override
	public void onPause() {
		super.onPause();

		imageLoader.stop();
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.progressBar1);
		ImageView img = (ImageView) v.findViewById(R.id.imageView1);
		imageLoader.displayImage(photoUrl, img, options);
		imageLoader.displayImage(photoUrl, img, options, new ImageLoadingListener() {
			@Override
			public void onLoadingStarted(String imageUri, View view) {}

			@Override
			public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
				pbar.setVisibility(View.GONE);
			}

			@Override
			public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
				pbar.setVisibility(View.GONE);
			}

			@Override
			public void onLoadingCancelled(String imageUri, View view) {
				pbar.setVisibility(View.GONE);
			}
		});
		img.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isAdded()) {
					ActionBar ab = getActivity().getActionBar();
					if (ab.isShowing()) {
						ab.hide();
					} else {
						ab.show();
					}
				}
			}
		});
	}

}