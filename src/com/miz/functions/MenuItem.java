package com.miz.functions;

public class MenuItem {
	
	private boolean isHeader, isThirdPartyApp;
	private int count;
	private Class<?> className;
	private String title, packageName;
	
	public MenuItem(String title, int count, boolean isHeader, Class<?> className) {
		this.title = title;
		this.count = count;
		this.isHeader = isHeader;
		this.className = className;
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
	
	public Class<?> getClassName() {
		return className;
	}
	
	public String getPackageName() {
		return packageName;
	}
}