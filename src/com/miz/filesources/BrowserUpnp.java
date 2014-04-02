package com.miz.filesources;

import java.util.HashMap;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.support.model.container.Container;

import com.miz.abstractclasses.AbstractFileSourceBrowser;

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