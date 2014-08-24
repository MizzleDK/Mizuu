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

import java.util.Properties;

/**
 * HTTP response class
 * @author Michell
 *
 */
public class Response {

	//Some HTTP response status codes
	public static final String
	HTTP_OK = "200 OK",
	HTTP_PARTIALCONTENT = "206 Partial Content",
	HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable",
	HTTP_FORBIDDEN = "403 Forbidden",
	HTTP_NOTFOUND = "404 Not Found",
	HTTP_BADREQUEST = "400 Bad Request",
	HTTP_INTERNALERROR = "500 Internal Server Error";

	public String status, mimeType;
	public StreamSource data;
	public Properties header = new Properties();

	/**
	 * Default constructor: response = HTTP_OK, data = mime = 'null'
	 */
	public Response() {
		status = HTTP_OK;
	}

	/**
	 * Basic constructor.
	 */
	public Response(String status, String mimeType, StreamSource data) {
		this.status = status;
		this.mimeType = mimeType;
		this.data = data;
	}

	/**
	 * Adds given line to the header.
	 */
	public void addHeader(String name, String value) {
		header.put(name, value);
	}
}
