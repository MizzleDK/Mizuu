package com.miz.mizuu.fragments;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.widgets.MovieBackdropWidgetProvider;
import com.squareup.picasso.Picasso;

public class FanartSearchFragmentTv extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<String> pics_sources = new ArrayList<String>();
	private GridView mGridView = null;
	private ProgressBar pbar;
	private String TVDB_ID;
	private Picasso mPicasso;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FanartSearchFragmentTv() {}

	public static FanartSearchFragmentTv newInstance(String tvdbId) {
		FanartSearchFragmentTv pageFragment = new FanartSearchFragmentTv();
		Bundle b = new Bundle();
		b.putString("tvdbId", tvdbId);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);   

		setHasOptionsMenu(true);
		setRetainInstance(true);

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.backdrop_thumbnail_width);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		TVDB_ID = getArguments().getString("tvdbId");
		
		mPicasso = MizuuApplication.getPicasso(getActivity());

		new GetCoverImages().execute(TVDB_ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.backdrop_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (pics_sources.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mAdapter = new ImageAdapter(getActivity());
		mGridView.setAdapter(mAdapter);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(
								mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
						if (numColumns > 0) {
							final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
							mAdapter.setItemHeight(columnWidth);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				new DownloadThread(pics_sources.get(arg2).replace("/_cache/", "/")).start();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			super();
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		public int getCount() {
			return pics_sources.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			// Now handle the main ImageView thumbnails
			ImageView imageView;
			if (convertView == null) { // if it's not recycled, instantiate and initialize
				imageView = new ImageView(mContext);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(mImageViewLayoutParams);
			} else { // Otherwise re-use the converted view
				imageView = (ImageView) convertView;
			}

			// Check the height matches our calculated column width
			if (imageView.getLayoutParams().height != mItemHeight) {
				imageView.setLayoutParams(mImageViewLayoutParams);
			}

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(pics_sources.get(position)).placeholder(R.drawable.gray).error(R.drawable.nobackdrop).config(MizuuApplication.getBitmapConfig()).into(imageView);

			return imageView;
		}

		/**
		 * Sets the item height. Useful for when we know the column width so the height can be set
		 * to match.
		 *
		 * @param height
		 */
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 0.58));
			notifyDataSetChanged();
		}
	}

	protected class GetCoverImages extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse("http://thetvdb.com/api/" + MizLib.TVDBAPI + "/series/" + params[0] + "/banners.xml");
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("Banners");
				if (nodeList.getLength() > 0) {

					Node firstNode = nodeList.item(0);

					if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
						Element firstElement = (Element) firstNode;
						NodeList list, list2;
						Element element;

						list = firstElement.getChildNodes();
						list2 = firstElement.getChildNodes();
						nodeList = doc.getElementsByTagName("Banner");
						if (nodeList.getLength() > 0) {
							try {
								list = firstElement.getElementsByTagName("BannerType");
								list2 = firstElement.getElementsByTagName("BannerPath");
								for (int i = 0; i < list.getLength(); i++) {
									element = (Element) list.item(i);
									if (element.getTextContent().equals("fanart"))
										pics_sources.add("http://thetvdb.com/banners/_cache/" + ((Element) list2.item(i)).getTextContent().toString());
								}
							} catch(Exception e) {} // No such tag
						}
					}
				} else Log.v("NODE", "Error");
			} catch (Exception e) {	e.printStackTrace(); }
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();

				Intent intent = new Intent("mizuu-cover-search-fragment-tv");
				intent.putExtra("backdropCount", pics_sources.size());
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
			}
		}
	}

	public class DownloadThread extends Thread {

		private String url;

		public DownloadThread(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			if (isAdded()) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity(), getString(R.string.addingCover), Toast.LENGTH_SHORT).show();
					}}
						);

				MizLib.downloadFile(url, new File(MizLib.getTvShowBackdropFolder(getActivity()), TVDB_ID + "_tvbg.jpg").getAbsolutePath());

				updateWidgets();

				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-show-backdrop-change"));
			}

			if (isAdded()) {
				getActivity().finish();
				return;
			}
		}
	}

	private void updateWidgets() {
		if (isAdded()) {
			AppWidgetManager awm = AppWidgetManager.getInstance(getActivity());
			awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(getActivity(), MovieBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			getActivity().onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}