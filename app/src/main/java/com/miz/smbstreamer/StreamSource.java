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

package com.miz.smbstreamer;

import com.miz.utils.VideoUtils;

import java.io.IOException;
import java.io.InputStream;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStreamOld;

public class StreamSource {

	protected String mime, name;
	protected long fp, len;
	protected int bufferSize;
	protected SmbFile file;
	protected InputStream input;

	public StreamSource(SmbFile file) throws SmbException{
		fp = 0;
		len = file.length();
		mime = VideoUtils.getMimeType(file.getName(), false);
		name = file.getName();
		this.file = file;
		bufferSize = 16 * 1024;
	}

	public void open() throws IOException {
		try {
			input = new SmbFileInputStreamOld(file, bufferSize, 1);
			if (fp > 0)
				input.skip(fp);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public int read(byte[] buff) throws IOException{
		return read(buff, 0, buff.length);
	}

	public int read(byte[] bytes, int start, int offs) throws IOException {
		int read =  input.read(bytes, start, offs);
		fp += read;
		return read;
	}

	public long moveTo(long position) throws IOException {
		fp = position;
		return fp;
	}

	public void close() {
		try {
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getMimeType(){
		return mime;
	}

	public long length(){
		return len;
	}

	public String getName(){
		return name;
	}

	public long available(){
		return len - fp;
	}

	public void reset(){
		fp = 0;
	}

	public SmbFile getFile(){
		return file;
	}

	public int getBufferSize(){
		return bufferSize;
	}  
}