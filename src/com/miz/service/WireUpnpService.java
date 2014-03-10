package com.miz.service;

import android.net.wifi.WifiManager;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidUpnpServiceImpl;

public class WireUpnpService extends AndroidUpnpServiceImpl {
	@Override
	protected AndroidUpnpServiceConfiguration createConfiguration(WifiManager wifiManager) {
		return new AndroidUpnpServiceConfiguration(wifiManager);
	}
}