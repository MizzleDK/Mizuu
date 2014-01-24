package com.miz.mizuu.fragments;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miz.functions.Actor;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.squareup.picasso.Picasso;

public class ActorBrowserFragmentTv extends Fragment {

	private int mImageThumbSize, mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ArrayList<Actor> actors = new ArrayList<Actor>();
	private GridView mGridView = null;
	private String TVDB_ID;
	private ProgressBar pbar;
	private boolean setBackground;
	private Picasso mPicasso;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ActorBrowserFragmentTv() {}

	public static ActorBrowserFragmentTv newInstance(String tvdbId, boolean setBackground) {
		ActorBrowserFragmentTv pageFragment = new ActorBrowserFragmentTv();
		Bundle b = new Bundle();
		b.putString("tvdbId", tvdbId);
		b.putBoolean("setBackground", setBackground);
		pageFragment.setArguments(b);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		
		setBackground = getArguments().getBoolean("setBackground");

		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		TVDB_ID = getArguments().getString("tvdbId");
		
		mPicasso = MizuuApplication.getPicasso(getActivity());

		new GetCoverImages().execute(TVDB_ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		if (setBackground && !MizLib.runsInPortraitMode(getActivity()))
			v.findViewById(R.id.container).setBackgroundResource(R.drawable.bg);
		
		MizLib.addActionBarPadding(getActivity(), v.findViewById(R.id.container));

		pbar = (ProgressBar) v.findViewById(R.id.progress);
		if (actors.size() > 0) pbar.setVisibility(View.GONE); // Hack to remove the ProgressBar on orientation change

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
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse("http://www.imdb.com/find?s=nm&q=" + actors.get(arg2).getName().replace(" ", "+")));
				startActivity(i);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	private class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			super();
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mImageViewLayoutParams = new GridView.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		public int getCount() {
			return actors.size();
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
			CoverItem holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.grid_item_cover, container, false);
				holder = new CoverItem();
				holder.layout = (RelativeLayout) convertView.findViewById(R.id.cover_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.text.setVisibility(View.VISIBLE);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);
				holder.subtext.setVisibility(View.VISIBLE);
				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			holder.text.setText(actors.get(position).getName());
			holder.subtext.setVisibility(View.VISIBLE);
			holder.subtext.setText(actors.get(position).getCharacter());

			// Finally load the image asynchronously into the ImageView, this also takes care of
			// setting a placeholder image while the background thread runs
			mPicasso.load(actors.get(position).getUrl()).placeholder(R.drawable.gray).error(R.drawable.noactor).into(holder.cover);

			return convertView;
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
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, (int) (mItemHeight * 1.5));
			notifyDataSetChanged();
		}
	}

	protected class GetCoverImages extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse("http://thetvdb.com/api/" + MizLib.TVDBAPI + "/series/" + params[0] + "/actors.xml");
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("Actors");
				if (nodeList.getLength() > 0) {
					Node firstNode = nodeList.item(0);
					if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
						nodeList = doc.getElementsByTagName("Actor");
						for (int i = 0; i < nodeList.getLength(); i++) {
							String id = "", name = "", image = "", role = "";
							NodeList children = nodeList.item(i).getChildNodes();

							for (int j = 0; j < children.getLength(); j++) {
								if (children.item(j).getNodeName().equals("id"))
									id = children.item(j).getTextContent();
								if (children.item(j).getNodeName().equals("Image"))
									image = "http://thetvdb.com/banners/" + children.item(j).getTextContent();
								if (children.item(j).getNodeName().equals("Name"))
									name = children.item(j).getTextContent();
								if (children.item(j).getNodeName().equals("Role"))
									role = children.item(j).getTextContent();
							}

							actors.add(new Actor(name, role, id, image));
						}
					}
				}
			} catch (Exception e) {}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isAdded()) {
				pbar.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
}