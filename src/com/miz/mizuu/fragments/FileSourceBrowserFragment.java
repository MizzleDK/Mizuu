package com.miz.mizuu.fragments;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbFile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.filesources.BrowserFile;
import com.miz.filesources.BrowserSmb;
import com.miz.functions.BrowserFileObject;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;

public class FileSourceBrowserFragment extends Fragment {

	public static final int INITIALIZE = -2;
	public static final int GO_BACK = -1;

	private final static String TYPE = "type";
	private final static String SERVER = "server";
	private final static String USER = "user";
	private final static String PASS = "pass";
	private final static String DOMAIN = "domain";
	private final static String IS_MOVIE = "isMovie";

	private AbstractFileSourceBrowser<?> mBrowser;
	private ListView mParent, mCurrent;
	private View mProgress, mEmpty, mEmptyParent;
	private int mType;
	private BrowseFolder mBrowseFolder;
	private ParentFolderAdapter mParentFolderAdapter;
	private CurrentFolderAdapter mCurrentFolderAdapter;
	private boolean mIsLoading = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FileSourceBrowserFragment() {}

	public static FileSourceBrowserFragment newInstanceFile(boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.FILE);
		args.putBoolean(IS_MOVIE, isMovie);	
		frag.setArguments(args);
		return frag;
	}

	public static FileSourceBrowserFragment newInstanceSmbFile(String server, String user, String pass, String domain, boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.SMB);
		args.putString(SERVER, server.replace("smb://", ""));
		args.putString(USER, user);
		args.putString(PASS, pass);
		args.putString(DOMAIN, domain);
		args.putBoolean(IS_MOVIE, isMovie);		
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		mType = getArguments().getInt(TYPE, FileSource.FILE);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("onBackPressed"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			onBackPressed();
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.filesource_browser, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgress = v.findViewById(R.id.progress);
		mEmpty = v.findViewById(R.id.no_content);
		mEmptyParent = v.findViewById(R.id.no_content_parent);

		mParent = (ListView) v.findViewById(R.id.parentList);
		mCurrent = (ListView) v.findViewById(R.id.currentList);

		mCurrentFolderAdapter = new CurrentFolderAdapter();
		mCurrent.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (mBrowser.getBrowserFiles().get(arg2).isDirectory())
					browse(arg2, false);
			}
		});

		if (hasParentView()) {
			mParentFolderAdapter = new ParentFolderAdapter();
			mParent.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					browse(arg2, true);
				}
			});
		}

		browse(INITIALIZE, false);
	}

	private void loadFilesource() throws MalformedURLException, UnsupportedEncodingException {
		switch (mType) {
		case FileSource.FILE:
			mBrowser = new BrowserFile(Environment.getExternalStorageDirectory());
			break;
		case FileSource.SMB:
			mBrowser = new BrowserSmb(new SmbFile(MizLib.createSmbLoginString(URLEncoder.encode(getArguments().getString(DOMAIN), "utf-8"),
					URLEncoder.encode(getArguments().getString(USER), "utf-8"),
					URLEncoder.encode(getArguments().getString(PASS), "utf-8"),
					getArguments().getString(SERVER),
					true)));
			break;
		}
	}

	private class BrowseFolder extends AsyncTask<Void, Void, Boolean> {

		private int mIndex;
		private boolean mFromParent;

		public BrowseFolder(int index, boolean fromParent) {
			mIndex = index;
			mFromParent = fromParent;
		}

		@Override
		protected void onPreExecute() {
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result = false;

			if (mIndex == INITIALIZE) {
				try {
					loadFilesource();
				} catch (Exception e) {}
			} else if (mIndex == GO_BACK) {
				result = mBrowser.goUp();
			} else {
				result = mBrowser.browse(mIndex, mFromParent);
			}

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!isAdded())
				return;

			if (mIndex >= GO_BACK && !result)
				Toast.makeText(getActivity(), R.string.couldnt_load_folder, Toast.LENGTH_SHORT).show();

			setLoading(false);

			if (mCurrent.getAdapter() == null) {
				mCurrent.setAdapter(mCurrentFolderAdapter);
				mCurrent.setEmptyView(mEmpty);
			}

			if (hasParentView())
				if (mParent.getAdapter() == null) {
					mParent.setAdapter(mParentFolderAdapter);
					mParent.setEmptyView(mEmptyParent);
				}

			notifyDataSetChanged();

			getActivity().getActionBar().setSubtitle(mBrowser.getSubtitle());

			mCurrent.setSelectionAfterHeaderView();
		}
	}

	private void browse(int index, boolean fromParent) {
		if (mIsLoading)
			return;

		if (mBrowseFolder != null)
			mBrowseFolder.cancel(true);

		mBrowseFolder = new BrowseFolder(index, fromParent);
		mBrowseFolder.execute();
	}

	static class ViewHolder {
		TextView name, size;
		ImageView icon;
	}

	private class CurrentFolderAdapter extends BaseAdapter {

		private List<BrowserFileObject> mItems = new ArrayList<BrowserFileObject>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		@Override
		public int getCount() {
			return mItems.size();
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
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.file_list_item, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.name.setText(mItems.get(position).getName());

			if (mItems.get(position).isDirectory()) {
				holder.icon.setImageResource(R.drawable.folder);
				holder.size.setText("");
				holder.size.setVisibility(View.GONE);
			} else {
				if (MizLib.isVideoFile(mItems.get(position).getName()))
					holder.icon.setImageResource(R.drawable.ic_action_movie);
				else if (MizLib.isSubtitleFile(mItems.get(position).getName()))
					holder.icon.setImageResource(R.drawable.ic_action_font_smaller);
				else
					holder.icon.setImageResource(R.drawable.file);
				holder.size.setText(MizLib.filesizeToString(mItems.get(position).getSize()));
				holder.size.setVisibility(View.VISIBLE);
			}

			return convertView;
		}

		@Override
		public void notifyDataSetChanged() {
			mItems = mBrowser.getBrowserFiles();

			super.notifyDataSetChanged();
		}
	}

	private class ParentFolderAdapter extends BaseAdapter {

		private List<BrowserFileObject> mItems = new ArrayList<BrowserFileObject>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		@Override
		public int getCount() {
			return mItems.size();
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
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.file_list_item, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.size.setVisibility(View.GONE);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.icon.setImageResource(R.drawable.folder);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.name.setText(mItems.get(position).getName());

			return convertView;
		}

		@Override
		public void notifyDataSetChanged() {
			mItems = mBrowser.getBrowserParentFiles();

			super.notifyDataSetChanged();
		}
	}

	private void onBackPressed() {
		browse(GO_BACK, false);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	private void setLoading(boolean loading) {
		mIsLoading = loading;
		if (loading) {
			mProgress.setVisibility(View.VISIBLE);
			mCurrent.setVisibility(View.GONE);
		} else {
			mProgress.setVisibility(View.GONE);
			mCurrent.setVisibility(View.VISIBLE);
		}	
	}

	private boolean hasParentView() {
		return mParent != null;
	}

	private void notifyDataSetChanged() {
		mCurrentFolderAdapter.notifyDataSetChanged();
		if (hasParentView())
			mParentFolderAdapter.notifyDataSetChanged();
	}
}