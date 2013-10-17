package com.miz.functions;

import android.graphics.drawable.Drawable;

public class MenuItem {
	
	private boolean isHeader, isThirdPartyApp;
	private int title, count, icon;
	private Class<?> className;
	private Drawable iconDrawable;
	private String stringTitle, packageName;
	
	public MenuItem(int title, int icon, int count, boolean isHeader, Class<?> className) {
		this.title = title;
		this.icon = icon;
		this.count = count;
		this.isHeader = isHeader;
		this.className = className;
	}
	
	public MenuItem(String title, Drawable icon, int count, boolean isHeader, String packageName) {
		this.stringTitle = title;
		this.iconDrawable = icon;
		this.count = count;
		this.isHeader = isHeader;
		this.packageName = packageName;
		isThirdPartyApp = true;
	}
	
	public int getTitle() {
		return title;
	}
	
	public String getStringTitle() {
		return stringTitle;
	}
	
	public int getIcon() {
		return icon;
	}
	
	public Drawable getDrawable() {
		return iconDrawable;
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