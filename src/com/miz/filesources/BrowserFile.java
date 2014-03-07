package com.miz.filesources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.functions.BrowserFileObject;

public class BrowserFile extends AbstractFileSourceBrowser<File> {

	public BrowserFile(File folder) {
		super(folder);
	}

	@Override
	public boolean browse(int index) {
		return browse(getCurrentFiles()[index]);
	}
	
	@Override
	public boolean browse(File folder) {
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		
		File[] listFiles = folder.listFiles();
		if (listFiles == null)
			return false;
		
		setParentFolder(folder.getParentFile());
		setCurrentFolder(folder);
		setCurrentFiles(listFiles);
		
		for (File f : listFiles)
			list.add(new BrowserFileObject(f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length()));
		
		setBrowserFiles(list);
		
		return true;
	}
}
