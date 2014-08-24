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
}
