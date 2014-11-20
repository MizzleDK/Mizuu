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

import com.miz.abstractclasses.AbstractFileSourceBrowser;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.support.model.container.Container;

import java.util.HashMap;

public class BrowserUpnp extends AbstractFileSourceBrowser<Device<?, ?, ?>> {

	private Service<?,?> mService;
	private Container mContainer;
	private HashMap<String, String> mParentIds = new HashMap<String, String>();
	private String mServer, mSerial;

	public BrowserUpnp(Device<?, ?, ?> folder, String server, String serial) {
		super(folder);
		
		mServer = server;
		mSerial = serial;
	}

	public String getServer() {
		return mServer;
	}
	
	public String getSerial() {
		return mSerial;
	}
	
	@Override
	public boolean browse(Device<?, ?, ?> folder) {
		return browseParent();
	}

	@Override
	public String getSubtitle() {
		return getServer();
	}

	@Override
	public boolean browseParent() {
		return true;
	}

	public void addParentId(String id, String parentId) {
		if (!containsId(id))
			mParentIds.put(id, parentId);
	}

	public boolean containsId(String id) {
		return mParentIds.containsKey(id);
	}

	public String getParentId(String id) {
		return mParentIds.get(id);
	}

	public void setContainer(Container container) {
		mContainer = container;
	}

	public Container getContainer() {
		return mContainer;
	}

	public void setService(Service<?,?> service) {
		mService = service;
	}

	public Service<?,?> getService() {
		return mService;
	}
}