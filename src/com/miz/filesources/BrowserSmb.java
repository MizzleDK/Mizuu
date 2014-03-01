package com.miz.filesources;

import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbFile;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.functions.BrowserFileObject;

public class BrowserSmb extends AbstractFileSourceBrowser<SmbFile> {

	public BrowserSmb(SmbFile folder) {
		super(folder);
	}

	@Override
	public List<BrowserFileObject> browse(int index) {
		return browse(getCurrentFiles()[index]);
	}

	@Override
	public List<BrowserFileObject> browse(SmbFile folder) {
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();

		try {
			SmbFile[] listFiles = folder.listFiles();
			if (listFiles == null)
				return null;

			setParentFolder(new SmbFile(folder.getParent()));
			setCurrentFolder(folder);
			setCurrentFiles(listFiles);

			for (SmbFile f : listFiles)
				list.add(new BrowserFileObject(f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length()));

			setBrowserFiles(list);
		} catch (Exception e) {}

		return list;
	}
}
