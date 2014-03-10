package com.miz.filesources;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.functions.DeviceItem;

public class BrowserUpnp extends AbstractFileSourceBrowser<DeviceItem> {

	public BrowserUpnp(DeviceItem folder) {
		super(folder);
	}

	@Override
	public boolean browse(DeviceItem folder) {


		return browseParent();
	}

	@Override
	public String getSubtitle() {
		if (getCurrentFolder() != null)
			return getCurrentFolder().toString();
		return "";
	}

	@Override
	public boolean browseParent() {


		return true;
	}
	
	public void addDevice(DeviceItem di) {
		
	}
}
