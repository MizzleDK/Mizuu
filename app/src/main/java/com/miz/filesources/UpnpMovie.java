/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.filesources;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.text.TextUtils;

import com.miz.abstractclasses.MovieFileSource;
import com.miz.db.DbAdapterMovieMappings;
import com.miz.functions.ColumnIndexCache;
import com.miz.functions.DbMovie;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.service.WireUpnpService;
import com.miz.utils.MovieDatabaseUtils;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.support.contentdirectory.callback.Browse;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UpnpMovie extends MovieFileSource<String> {

	private TreeSet<String> results = new TreeSet<String>();
	private HashMap<String, String> existingMovies = new HashMap<String, String>();
	private CountDownLatch mLatch = new CountDownLatch(1);
	private AndroidUpnpService mUpnpService;

	public UpnpMovie(Context context, FileSource fileSource, boolean clearLibrary) {
		super(context, fileSource, clearLibrary);
	}

	@Override
	public void removeUnidentifiedFiles() {
		List<DbMovie> dbMovies = getDbMovies();

		int count = dbMovies.size();
		for (int i = 0; i < count; i++) {
			if (dbMovies.get(i).isUpnpFile() && dbMovies.get(i).isUnidentified() && MizLib.exists(dbMovies.get(i).getFilepath()))
                MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
		}
	}

	@Override
	public void removeUnavailableFiles() {
		List<DbMovie> dbMovies = getDbMovies();

		int count = dbMovies.size();
		for (int i = 0; i < count; i++) {
			if (dbMovies.get(i).isUpnpFile() && !MizLib.exists(dbMovies.get(i).getFilepath())) {
                MovieDatabaseUtils.deleteMovie(mContext, dbMovies.get(i).getTmdbId());
			}
		}
	}

	@Override
	public List<String> searchFolder() {
		DbAdapterMovieMappings dbHelper = MizuuApplication.getMovieMappingAdapter();
		Cursor cursor = dbHelper.getAllFilepaths(false); // Query database to return all filepaths in a cursor
		ColumnIndexCache cache = new ColumnIndexCache();
		
		try {
			while (cursor.moveToNext()) {// Add all movies in cursor to ArrayList of all existing movies
				existingMovies.put(cursor.getString(cache.getColumnIndex(cursor, DbAdapterMovieMappings.KEY_FILEPATH)), "");
			}
		} catch (Exception e) {
		} finally {
			cursor.close(); // Close cursor
			cache.clear();
		}

		// Do a recursive search in the file source folder
		recursiveSearch(getFolder(), results);

		List<String> list = new ArrayList<String>();

		Iterator<String> it = results.iterator();
		while (it.hasNext())
			list.add(it.next());

		return list;
	}

	@Override
	public void recursiveSearch(String folder, TreeSet<String> results) {
		mContext.getApplicationContext().bindService(new Intent(mContext, WireUpnpService.class), serviceConnection, Context.BIND_AUTO_CREATE);

		try {
			mLatch.await(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void addToResults(String file, long size, TreeSet<String> results) {
		if (MizLib.checkFileTypes(file)) {
			if (size < getFileSizeLimit())
				return;
			
			if (!clearLibrary())
				if (existingMovies.get(file.split("<MiZ>")[1]) != null || existingMovies.get(file) != null) return;

			String tempFileName = file.substring(0, file.lastIndexOf("."));
			if (tempFileName.toLowerCase(Locale.ENGLISH).matches(".*part[2-9]|cd[2-9]")) return;

			//Add the file if it reaches this point
			results.add(file);
		}
	}

	@Override
	public String getRootFolder() {
		return mFileSource.getUpnpFolderId();
	}

	@Override
	public String toString() {
		return mFileSource.getFilepath();
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mUpnpService = (AndroidUpnpService) service;

			boolean found = false;

			for (Device<?, ?, ?> device : mUpnpService.getRegistry().getDevices()) {
				try {
					if (!TextUtils.isEmpty(device.getDetails().getSerialNumber())) {
						if (device.getDetails().getSerialNumber().equals(mFileSource.getUpnpSerialNumber())) {
							startBrowse(device);
							found = true;
						}
					} else {
						if (device.getIdentity().getUdn().toString().equals(mFileSource.getUpnpSerialNumber())) {
							startBrowse(device);
							found = true;
						}
					}
				} catch (Exception e) {}
			}

			if (!found) {
				mUpnpService.getRegistry().addListener(new DeviceListRegistryListener());
				mUpnpService.getControlPoint().search();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mUpnpService = null;
		}

		private void startBrowse(Device<?, ?, ?> device) {
			Service<?, ?> service = device.findService(new UDAServiceType("ContentDirectory"));
			browse(service, getRootFolder(), mFileSource.getUpnpName());
		}
	};

	private int mFolderCount = 0, mScannedCount = 0;

	private class BrowseCallback extends Browse {

		private Service<?, ?> mService;
		private String mPrefix;

		public BrowseCallback(String prefix, Service<?, ?> service, String containerId) {  
			super(service, containerId, BrowseFlag.DIRECT_CHILDREN, "*", 0, null, new SortCriterion(true, "dc:title"));
			mService = service;
			mPrefix = prefix;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void received(ActionInvocation arg0, DIDLContent didl) {
			try {
				for (Container childContainer : didl.getContainers()) {
					mFolderCount++;
					mUpnpService.getControlPoint().execute(new BrowseCallback(mPrefix + "/" + childContainer.getTitle(), mService, childContainer.getId()));
				}

				for (Item childItem : didl.getItems()) {
					addToResults(mPrefix + "/" + childItem.getTitle() + "<MiZ>" + childItem.getFirstResource().getValue(), childItem.getFirstResource().getSize(), results);
				}

				mScannedCount++;

				if (mFolderCount == mScannedCount) {
					mLatch.countDown();
					mContext.unbindService(serviceConnection);
				}

			} catch (Exception e) {}
		}

		@Override
		public void updateStatus(Status arg0) {}

		@SuppressWarnings("rawtypes")
		@Override
		public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {}
	}

	public class DeviceListRegistryListener extends DefaultRegistryListener {

		private boolean found = false;

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaServer")) {
				if (!found) {
					try {
						if (!TextUtils.isEmpty(device.getDetails().getSerialNumber())) {
							if (device.getDetails().getSerialNumber().equals(mFileSource.getUpnpSerialNumber())) {
								startBrowse(device);
								found = true;
							}
						} else {
							if (device.getIdentity().getUdn().toString().equals(mFileSource.getUpnpSerialNumber())) {
								startBrowse(device);
								found = true;
							}
						}
					} catch (Exception e) {}
				}
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			if (!found) {
				try {
					if (!TextUtils.isEmpty(device.getDetails().getSerialNumber())) {
						if (device.getDetails().getSerialNumber().equals(mFileSource.getUpnpSerialNumber())) {
							startBrowse(device);
							found = true;
						}
					} else {
						if (device.getIdentity().getUdn().toString().equals(mFileSource.getUpnpSerialNumber())) {
							startBrowse(device);
							found = true;
						}
					}
				} catch (Exception e) {}
			}
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {}

		private void startBrowse(Device<?, ?, ?> device) {
			mFolderCount++;

			Service<?, ?> service = device.findService(new UDAServiceType("ContentDirectory"));
			browse(service, getRootFolder(), mFileSource.getFilepath().substring(mFileSource.getFilepath().lastIndexOf("/") + 1, mFileSource.getFilepath().length()));
		}
	}

	private void browse(Service<?, ?> service, String containerId, String title) {		
		mUpnpService.getControlPoint().execute(new BrowseCallback(title, service, containerId));
	}

	@Override
	public void addToResults(String folder, TreeSet<String> results) {}
}