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
	private View mContent, mProgress;
	private int mType;
	private BrowseFolder mBrowseFolder;
	private CurrentFolderAdapter mCurrentFolderAdapter;
	private boolean mIsLoading = false;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FileSourceBrowserFragment() {}

	public static FileSourceBrowserFragment newInstanceFile() {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.FILE);
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

		mContent = v.findViewById(R.id.content);
		mProgress = v.findViewById(R.id.progress);
		
		mParent = (ListView) v.findViewById(R.id.parentList);
		mCurrent = (ListView) v.findViewById(R.id.currentList);

		mCurrentFolderAdapter = new CurrentFolderAdapter();
		mCurrent.setAdapter(mCurrentFolderAdapter);
		mCurrent.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				browse(arg2);
			}

		});

		browse(INITIALIZE);
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

	private class BrowseFolder extends AsyncTask<Void, Void, Void> {

		private int mIndex;

		public BrowseFolder(int index) {
			mIndex = index;
		}

		@Override
		protected void onPreExecute() {
			setLoading(true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (mIndex == INITIALIZE) {
				try {
					loadFilesource();
				} catch (Exception e) {}
			} else if (mIndex == GO_BACK) {
				mBrowser.goUp();
			} else {
				mBrowser.browse(mIndex);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			setLoading(false);
			mCurrentFolderAdapter.notifyDataSetChanged();
		}
	}

	private void browse(int index) {
		if (mIsLoading)
			return;
		
		if (mBrowseFolder != null)
			mBrowseFolder.cancel(true);

		mBrowseFolder = new BrowseFolder(index);
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

	private void onBackPressed() {
		browse(GO_BACK);
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
			mContent.setVisibility(View.GONE);
		} else {
			mProgress.setVisibility(View.GONE);
			mContent.setVisibility(View.VISIBLE);
		}	
	}
}