package com.miz.functions;

public class MovieVersion {
	
	private int rowId;
	private String filepath;

	public MovieVersion(int rowId, String filepath) {
		this.rowId = rowId;
		this.filepath = filepath;
	}

	public int getRowId() {
		return rowId;
	}

	public String getFilepath() {
		return filepath;
	}
}