package com.miz.functions;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.miz.mizuu.R;

public class ImageLoad extends AsyncTask<Object, String, Bitmap> {

	private String file = "";
	private float startAlpha = 0f;
	private float endAlpha = 1.0f;
	private int duration = 500, width = 0, height = 0, onFail;
	private ImageView img;

	public void setImageView(ImageView img) {
		this.img = img;
	}

	public void setFileUrl(String fileUrl) {
		file = fileUrl;
	}

	public void setDuration(int time) {
		duration = time;
	}

	public void setStartAlpha(float startAlpha) {
		this.startAlpha = startAlpha;
	}

	public void setEndAlpha(float endAlpha) {
		this.endAlpha = endAlpha;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public void setOnFailResource(int failResource) {
		this.onFail = failResource;
	}

	@Override
	protected Bitmap doInBackground(Object... params) {
		Bitmap bm;

		if (file.equals("nobackdrop")) {
			Context c = (Context) params[4];
			if (MizLib.runsInPortraitMode(c))
				return BitmapFactory.decodeResource(c.getResources(), R.drawable.nobackdrop);
		}

		if (width > 0 && height > 0) {
			bm = MizLib.decodeSampledBitmapFromFile(file, width, height);
		} else {
			if (img.getWidth() > 0 && img.getHeight() > 0) {
				bm = MizLib.decodeSampledBitmapFromFile(file, img.getWidth(), img.getHeight());
			} else {
				bm = BitmapFactory.decodeFile(file);
			}
		}

		return bm;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result != null)
			img.setImageBitmap(result);
		else
			img.setImageResource(onFail);

		if (duration > 0) {
			img.setAlpha(startAlpha);
			ObjectAnimator.ofFloat(img, "alpha", startAlpha, endAlpha).setDuration(duration).start();
		}
	}
}