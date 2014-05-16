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