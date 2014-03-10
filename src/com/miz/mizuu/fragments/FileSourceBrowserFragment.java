package com.miz.mizuu.fragments;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbFile;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
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
import com.miz.filesources.BrowserFile;
import com.miz.filesources.BrowserSmb;
import com.miz.filesources.BrowserUpnp;
import com.miz.functions.BrowserFileObject;
import com.miz.functions.ContentItem;
import com.miz.functions.DeviceItem;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.R;
import com.miz.service.WireUpnpService;

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

	private AndroidUpnpService upnpService;
	private DeviceListRegistryListener deviceListRegistryListener;

	private ArrayAdapter<DeviceItem> deviceListAdapter;
	private ArrayAdapter<ContentItem> contentListAdapter;

	private String mUpnpId;
	private Device mSelectedDevice;
	private ContentItem mContentItem;
	private Container mContainer;
	private Service<?,?> mService;

	private HashMap<String, String> mParentIds = new HashMap<String, String>();

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

	public static FileSourceBrowserFragment newInstanceUpnp(boolean isMovie) {
		FileSourceBrowserFragment frag = new FileSourceBrowserFragment();
		Bundle args = new Bundle();
		args.putInt(TYPE, FileSource.UPNP);
		args.putBoolean(IS_MOVIE, isMovie);	
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		deviceListRegistryListener = new DeviceListRegistryListener();

		mType = getArguments().getInt(TYPE, FileSource.FILE);

		if (mType == FileSource.UPNP) {
			deviceListAdapter = new ArrayAdapter<DeviceItem>(getActivity(),
					android.R.layout.simple_list_item_1);
			contentListAdapter = new ArrayAdapter<ContentItem>(getActivity(),
					android.R.layout.simple_list_item_1);
		}

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
				if (mType != FileSource.UPNP) {
					if (mBrowser.getBrowserFiles().get(arg2).isDirectory())
						browse(arg2, false);
				} else {
					ContentItem content = contentListAdapter.getItem(arg2);
					if (content.isContainer()) {
						mUpnpId = content.getContainer().getParentID();
						mContentItem = content;
						upnpService.getControlPoint().execute(
								new ContentBrowseCallback(getActivity(),
										content.getService(), content.getContainer(),
										contentListAdapter));
					}
				}
			}
		});

		if (hasParentView()) {
			mParentFolderAdapter = new ParentFolderAdapter();
			mParent.setOnItemClickListener(new OnItemClickListener() {
				protected Container createRootContainer(Service<?, ?> service) {
					Container rootContainer = new Container();
					rootContainer.setId("0");
					rootContainer.setTitle("Content Directory on "
							+ service.getDevice().getDisplayString());
					return rootContainer;
				}

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if (mType != FileSource.UPNP)
						browse(arg2, true);
					else {
						Device<?, ?, ?> device = deviceListAdapter.getItem(arg2).getDevice();
						mSelectedDevice = device;
						Service<?, ?> service = device.findService(new UDAServiceType(
								"ContentDirectory"));
						upnpService.getControlPoint().execute(
								new ContentBrowseCallback(getActivity(), service,
										createRootContainer(service), contentListAdapter));
					}
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
		case FileSource.UPNP:
			mBrowser = new BrowserUpnp(null);
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
					else
						mParent.setAdapter(deviceListAdapter);
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
		if (mType != FileSource.UPNP)
			browse(GO_BACK, false);
		else {
			Container parentContainer = new Container();
			parentContainer.setId(mParentIds.get(mContainer.getId()));

			upnpService.getControlPoint().execute(
					new ContentBrowseCallback(getActivity(),
							mService, parentContainer,
							contentListAdapter));
			//}

			/*Device<?, ?, ?> device = deviceListAdapter.getItem(0).getDevice();
			Service<?, ?> service = device.findService(new UDAServiceType("ContentDirectory"));

			Container parentContainer = new Container();
			parentContainer.setId(mUpnpId);
			parentContainer.setTitle("Content Directory on " + service.getDevice().getDisplayString());

			device.getParentDevice().

			upnpService.getControlPoint().execute(
					new ContentBrowseCallback(getActivity(),
							service, parentContainer,
							contentListAdapter));*/
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);

		if (mType == FileSource.UPNP) {
			if (upnpService != null)
				upnpService.getRegistry().removeListener(deviceListRegistryListener);
			getActivity().getApplicationContext().unbindService(serviceConnection);
		}
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
			if (hasParentView())
				deviceListAdapter.notifyDataSetChanged();
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;

			//deviceListAdapter.clear();
			for (Device<?, ?, ?> device : upnpService.getRegistry().getDevices()) {
				deviceListRegistryListener.deviceAdded(new DeviceItem(device));
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(deviceListRegistryListener);
			// Refresh device list
			upnpService.getControlPoint().search();
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	public class DeviceListRegistryListener extends DefaultRegistryListener {
		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaServer")) {
				final DeviceItem display = new DeviceItem(device, device
						.getDetails().getFriendlyName(),
						device.getDisplayString(), "(REMOTE) "
								+ device.getType().getDisplayString());
				deviceAdded(display);
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			final DeviceItem display = new DeviceItem(device, device
					.getDetails().getFriendlyName(), device.getDisplayString(),
					"(REMOTE) " + device.getType().getDisplayString());
			deviceAdded(display);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);
		}

		public void deviceAdded(final DeviceItem di) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					int position = deviceListAdapter.getPosition(di);
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						deviceListAdapter.remove(di);
						deviceListAdapter.insert(di, position);
					} else {
						deviceListAdapter.add(di);
					}
				}
			});
		}

		public void deviceRemoved(final DeviceItem di) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					deviceListAdapter.remove(di);
				}
			});
		}
	}

	private class ContentBrowseCallback extends Browse {

		private Service<?, ?> service;
		private ArrayAdapter<ContentItem> listAdapter;
		private Activity activity;

		@SuppressWarnings("rawtypes")
		public ContentBrowseCallback(Activity activity, Service service, Container container, ArrayAdapter<ContentItem> listadapter) {
			super(service, container.getId(), BrowseFlag.DIRECT_CHILDREN, "*", 0, null, new SortCriterion(true, "dc:title"));
			this.activity = activity;
			this.service = service;
			mService = service;
			this.listAdapter = listadapter;
			mContainer = container;

			if (!mParentIds.containsKey(container.getId()))
				mParentIds.put(container.getId(), container.getParentID());
		}

		@SuppressWarnings("rawtypes")
		public void received(final ActionInvocation actionInvocation, final DIDLContent didl) {
			System.out.println("Received browse action DIDL descriptor, creating tree nodes");
			activity.runOnUiThread(new Runnable() {
				public void run() {
					try {
						listAdapter.clear();
						// Containers first
						for (Container childContainer : didl.getContainers()) {
							System.out.println("add child container " + childContainer.getTitle());
							listAdapter.add(new ContentItem(childContainer, service));
						}
						// Now items
						for (Item childItem : didl.getItems()) {
							System.out.println("add child item" + childItem.getTitle());
							listAdapter.add(new ContentItem(childItem, service));
						}
					} catch (Exception ex) {
						System.out.println("Creating DIDL tree nodes failed: " + ex);
						actionInvocation.setFailure(new ActionException(
								ErrorCode.ACTION_FAILED,
								"Can't create list childs: " + ex, ex));
						failure(actionInvocation, null);
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