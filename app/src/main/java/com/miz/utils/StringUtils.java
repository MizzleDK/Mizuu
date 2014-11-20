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

package com.miz.utils;

import java.io.File;

public class StringUtils {

	// TODO Move all related methods from MizLib into this class

	private StringUtils() {} // No instantiation

	public static String replacePipesWithCommas(String input) {
		// Remove pipe in beginning of input
		if (input.startsWith("|"))
			input = input.substring(1, input.length());

		// Remove pipe at end of input
		if (input.endsWith("|"))
			input = input.substring(0, input.length() - 1);

		// Replace all other pipes and return the output
		return input.replace("|", ", ");
	}

	public static String getFilenameWithoutExtension(String input) {
		File fileName = new File(input);
		int pointPosition = fileName.getName().lastIndexOf(".");
		return (pointPosition == -1) ? fileName.getName() : fileName.getName().substring(0, pointPosition);
	}

	public static String getMovieIdFromImagePath(String path) {
		try {
			return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf(".jpg")).replace("_bg", "");
		} catch (Exception e) { // NullPointerException and OutOutBoundsException
			return "";
		}
	}
	
	/*
	 * START OF APACHE COMMONS CODE
	 * from http://svn.apache.org/repos/asf/commons/proper/io/trunk/src/main/java/org/apache/commons/io/FilenameUtils.java
	 */

	/*
	 * Licensed to the Apache Software Foundation (ASF) under one or more
	 * contributor license agreements.  See the NOTICE file distributed with
	 * this work for additional information regarding copyright ownership.
	 * The ASF licenses this file to You under the Apache License, Version 2.0
	 * (the "License"); you may not use this file except in compliance with
	 * the License.  You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */

	private static final int NOT_FOUND = -1;
	public static final char EXTENSION_SEPARATOR = '.';
	private static final char UNIX_SEPARATOR = '/';
	private static final char WINDOWS_SEPARATOR = '\\';

	public static String getExtension(final String filename) {
		if (filename == null) {
			return null;
		}
		final int index = indexOfExtension(filename);
		if (index == NOT_FOUND) {
			return "";
		} else {
			return filename.substring(index + 1);
		}
	}

	public static int indexOfExtension(final String filename) {
		if (filename == null) {
			return NOT_FOUND;
		}
		final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
		final int lastSeparator = indexOfLastSeparator(filename);
		return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
	}

	public static int indexOfLastSeparator(final String filename) {
		if (filename == null) {
			return NOT_FOUND;
		}
		final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
		final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
		return Math.max(lastUnixPos, lastWindowsPos);
	}

	/*
	 * END OF APACHE COMMONS CODE
	 */
}
