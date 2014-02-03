package com.miz.functions;

public class MenuItem {
	
	private boolean isHeader, isThirdPartyApp;
	private int count;
	private String title, packageName;
	
	public MenuItem(String title, int count, boolean isHeader) {
		this.title = title;
		this.count = count;
		this.isHeader = isHeader;
	}
	
	public MenuItem(String title, int count, boolean isHeader, String packageName) {
		this.title = title;
		this.count = count;
		this.isHeader = isHeader;
		this.packageName = packageName;
		isThirdPartyApp = true;
	}
	
	public String getTitle() {
		return title;
	}
	
	public int getCount() {
		return count;
	}
	
	public boolean isHeader() {
		return isHeader;
	}
	
	public boolean isThirdPartyApp() {
		return isThirdPartyApp;
	}
	
	public String getPackageName() {
		return packageName;
	}
}