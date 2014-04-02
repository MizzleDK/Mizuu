package com.miz.mizuu.fragments;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import jcifs.smb.SmbFile;

import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.TYPE;
import static com.miz.functions.MizLib.DOMAIN;
import static com.miz.functions.MizLib.USER;
import static com.miz.functions.MizLib.PASSWORD;
import static com.miz.functions.MizLib.SERVER;
import static com.miz.functions.MizLib.SERIAL_NUMBER;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.support.contentdirectory.callback.Browse;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.db.DbAdapterSources;
import com.miz.filesources.BrowserFile;
import com.miz.filesources.BrowserSmb;
import com.miz.filesources.BrowserUpnp;
import com.miz.functions.BrowserFileObject;
import com.miz.functions.ContentItem;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.service.WireUpnpService;

public class FileSourceBrowserFragment extends Fragment {

	public static final int INITIALIZE = -2;
	public static final int GO_BACK = -1;

	private AbstractFileSourceBrowser<?> mBrowser;
	private ListView mParent, mCurrent;
	private View mProgress, mEmpty, mEmptyParent;
	private int mType;
	private BrowseFolder mBrowseFolder;
	private ParentFolderAdapter mParentFolderAdapter;
	private CurrentFolderAdapter mCurrentFolderAdapter;
	private boolean mIsLoading = false, mIsMovie = false;

	private AndroidUpnpService upnpService;

	private ArrayAdapter<ContentItem> contentListAdapter;

	private CountDownLatch latch = new CountDownLatch(1);

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FileSourceBrowserFragment() {}

	public static FileSourceBrowserFragment newInstanceFile(boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.FILE);
		args.putBoolean(MOVIE, isMovie);	
		frag.setArguments(args);
		return frag;
	}

	public static FileSourceBrowserFragment newInstanceSmbFile(String server, String user, String pass, String domain, boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.SMB);
		args.putString(SERVER, server.replace("smb://", ""));
		args.putString(USER, user);
		args.putString(PASSWORD, pass);
		args.putString(DOMAIN, domain);
		args.putBoolean(MOVIE, isMovie);		
		frag.setArguments(args);
		return frag;
	}

	public static FileSourceBrowserFragment newInstanceUpnp(String server, String serialNumber, boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.UPNP);
		args.putString(SERIAL_NUMBER, serialNumber);
		args.putString(SERVER, server);
		args.putBoolean(MOVIE, isMovie);	
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		mIsMovie = getArguments().getBoolean(MOVIE, false);
		mType = getArguments().getInt(TYPE, FileSource.FILE);

		if (mType == FileSource.UPNP) {
			contentListAdapter = new ArrayAdapter<ContentItem>(getActivity(),
					android.R.layout.simple_list_item_1);
		}

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("onBackPressed"));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);	
		inflater.inflate(R.menu.add_file_source, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.add_source:
			addFileSource();
			break;
		}

		return true;
	}

	private void addFileSource() {

		String contentType = mIsMovie ? DbAdapterSources.KEY_TYPE_MOVIE : DbAdapterSources.KEY_TYPE_SHOW;

		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
		switch (mType) {
		case FileSource.FILE:
			dbHelper.createSource(mBrowser.getSubtitle(), contentType, FileSource.FILE, "", "", "");
			break;
		case FileSource.SMB:
			dbHelper.createSource("smb://" + mBrowser.getSubtitle().replace("smb://", ""), contentType, FileSource.SMB, getArguments().getString(USER), getArguments().getString(PASSWORD), getArguments().getString(DOMAIN));
			break;
		case FileSource.UPNP:

			break;
		}

		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-filesource-change"));
		getActivity().finish();
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
				if (mType != FileSource.UPNP) {
					if (mBrowser.getBrowserFiles().get(arg2).isDirectory())
						browse(arg2, false);
				} else {
					if (!mIsLoading) {
						mIsLoading = true;
						ContentItem content = contentListAdapter.getItem(arg2);
						if (content.isContainer()) {
							upnpService.getControlPoint().execute(new ContentBrowseCallback(getActivity(), content.getService(), content.getContainer(), contentListAdapter, true));
						}
					}
				}
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
					URLEncoder.encode(getArguments().getString(PASSWORD), "utf-8"),
					getArguments().getString(SERVER),
					true)));
			break;
		case FileSource.UPNP:
			if (mBrowser == null)
				mBrowser = new BrowserUpnp(null, getArguments().getString(SERVER), getArguments().getString(SERIAL_NUMBER));
			
			updateSubtitle();
			
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

			if (mType == FileSource.UPNP) {
				getActivity().getApplicationContext().bindService(
						new Intent(getActivity(), WireUpnpService.class), serviceConnection,
						Context.BIND_AUTO_CREATE);
			}
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
				if (mType != FileSource.UPNP)
					mCurrent.setAdapter(mCurrentFolderAdapter);
				else
					mCurrent.setAdapter(contentListAdapter);
				mCurrent.setEmptyView(mEmpty);
			}

			if (hasParentView())
				if (mParent.getAdapter() == null) {
					if (mType != FileSource.UPNP)
						mParent.setAdapter(mParentFolderAdapter);
					mParent.setEmptyView(mEmptyParent);
				}

			notifyDataSetChanged();

			updateSubtitle();

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
		if (mType != FileSource.UPNP)
			browse(GO_BACK, false);
		else {
			Container parentContainer = new Container();

			String id = "0";
			if (((BrowserUpnp) mBrowser).getContainer() != null) {
				id = ((BrowserUpnp) mBrowser).getParentId(((BrowserUpnp) mBrowser).getContainer().getId());
			}

			parentContainer.setId(id);

			System.out.println("SERVICE IS NULL: " + (((BrowserUpnp) mBrowser).getService() == null));

			upnpService.getControlPoint().execute(new ContentBrowseCallback(getActivity(), ((BrowserUpnp) mBrowser).getService(), parentContainer, contentListAdapter, false));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);

		if (mType == FileSource.UPNP)
			getActivity().getApplicationContext().unbindService(serviceConnection);
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
		if (mType != FileSource.UPNP) {
			mCurrentFolderAdapter.notifyDataSetChanged();
			if (hasParentView())
				mParentFolderAdapter.notifyDataSetChanged();
		} else {
			contentListAdapter.notifyDataSetChanged();
		}
	}

	private void updateSubtitle() {
		getActivity().getActionBar().setSubtitle(mBrowser.getSubtitle());
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;

			boolean found = false;

			for (Device<?, ?, ?> device : upnpService.getRegistry().getDevices()) {
				try {
					if (device.getDetails().getSerialNumber().equals(((BrowserUpnp) mBrowser).getSerial())) {
						startBrowse(device);
						found = true;
					}
				} catch (Exception e) {}
			}

			if (!found)
				Toast.makeText(getActivity(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}

		private void startBrowse(Device<?, ?, ?> device) {
			Service<?, ?> service = device.findService(new UDAServiceType("ContentDirectory"));
			((BrowserUpnp) mBrowser).setService(service);

			Container rootContainer = new Container();
			rootContainer.setId("0");
			rootContainer.setTitle(device.getDetails().getFriendlyName());

			upnpService.getControlPoint().execute(new ContentBrowseCallback(getActivity(), service, rootContainer, contentListAdapter, true));
		}
	};

	private int mFolderCount = 0, mScannedCount = 0;

	private class TestCallback extends Browse {

		private Service<?, ?> mService;

		public TestCallback(Service<?, ?> service, String containerId, BrowseFlag flag) {
			super(service, containerId, BrowseFlag.DIRECT_CHILDREN, "*", 0, null, new SortCriterion(true, "dc:title"));
			mService = service;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void received(ActionInvocation arg0, DIDLContent didl) {
			try {
				for (Container childContainer : didl.getContainers()) {
					mFolderCount++;
					upnpService.getControlPoint().execute(new TestCallback(mService, childContainer.getId(), null));
					//System.out.println(childContainer.getTitle() + " " + childContainer.getId() + " " + childContainer.getParentID());
				}
				// Now items
				for (Item childItem : didl.getItems()) {
					//System.out.println(childItem.getTitle() + " " + childItem.getFirstResource().getValue() + " " + childItem.getId() + " " + childItem.getParentID());
				}

				mScannedCount++;

				System.out.println("mFolder: " + mFolderCount + ". " + mScannedCount);

				if (mFolderCount == mScannedCount)
					latch.countDown();

			} catch (Exception e) {}
		}

		@Override
		public void updateStatus(Status arg0) {}

		@SuppressWarnings("rawtypes")
		@Override
		public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {}

	}

	private class ContentBrowseCallback extends Browse {

		private Service<?, ?> service;
		private ArrayAdapter<ContentItem> listAdapter;
		private Activity activity;
		private Container mContainer;

		@SuppressWarnings("rawtypes")
		public ContentBrowseCallback(Activity activity, Service service, Container container, ArrayAdapter<ContentItem> listadapter, boolean addToStack) {
			super(service, container.getId(), BrowseFlag.DIRECT_CHILDREN, "*", 0, null, new SortCriterion(true, "dc:title"));
			this.activity = activity;
			this.service = service;
			this.listAdapter = listadapter;

			mContainer = container;
		}

		@SuppressWarnings("rawtypes")
		public void received(final ActionInvocation actionInvocation, final DIDLContent didl) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					try {
						listAdapter.clear();
						// Containers first
						for (Container childContainer : didl.getContainers()) {
							listAdapter.add(new ContentItem(childContainer, service));
						}
						// Now items
						for (Item childItem : didl.getItems()) {
							listAdapter.add(new ContentItem(childItem, service));
						}

						((BrowserUpnp) mBrowser).setContainer(mContainer);

						((BrowserUpnp) mBrowser).addParentId(mContainer.getId(), mContainer.getParentID());

						mIsLoading = false;
					} catch (Exception ex) {
						mIsLoading = false;
						System.out.println("Creating DIDL tree nodes failed: " + ex);
					}
				}
			});
		}

		public void updateStatus(final Status status) {}

		@SuppressWarnings("rawtypes")
		@Override
		public void failure(ActionInvocation invocation, UpnpResponse operation, final String defaultMsg) {}
	}
}