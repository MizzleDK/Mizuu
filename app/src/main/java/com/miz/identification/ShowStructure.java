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

package com.miz.identification;

import android.text.TextUtils;

import com.miz.functions.MizLib;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowStructure {

	private final String mFilepath;
	private String mFilename, mSeasonFolder = "", mShowFolder = "", mImdbId, mCustomTags = "";
	private int mSeasonFolderNumber;
	private ArrayList<Episode> mEpisodes = new ArrayList<Episode>();

	public ShowStructure(String filepath) {
		mFilepath = filepath;
		split();
		checkImdbId();
		mEpisodes = new ArrayList<Episode>(decryptEpisodes(getFilename()));
		seasonFolderOverride();
	}

	public void setCustomTags(String customTags) {
		mCustomTags = customTags;
	}

	public void split() {
		Pattern splitPattern = Pattern.compile("/"); // Pre-compiled pattern to speed things up
		String[] split = splitPattern.split(mFilepath.contains("<MiZ>") ? mFilepath.split("<MiZ>")[0] : mFilepath);
		if (split.length >= 3) {
			mFilename = split[split.length - 1];

			// Check if it's a valid season folder (returns -1 if it's not)
			int tempSeasonCheck = getSeasonFolderNumber(split[split.length - 2].trim());

			if (tempSeasonCheck >= 0) {
				// It's valid! Use it as the season folder
				// and its parent folder as the show folder
				mSeasonFolder = split[split.length - 2].trim();
				mSeasonFolderNumber = tempSeasonCheck;
				mShowFolder = split[split.length - 3].trim();
			} else {
				// It's not... Use it as the show folder
				mShowFolder = split[split.length - 2].trim();
			}
		} else {
			// We're dealing with two or less parts
			mFilename = split[split.length - 1].trim();
			if (split.length == 2) {
				mShowFolder = split[0].trim();
			}
		}
	}

	public void checkImdbId() {
		// Prioritize the show folder IMDB ID
		if (hasShowFolder()) {
			String temp = MizLib.decryptImdbId(getShowFolderName());
			if (null != temp) {
				mImdbId = temp;
				return;
			}
		}

		String temp = MizLib.decryptImdbId(getFilename());
		if (null != temp)
			mImdbId = temp;
	}

	public void seasonFolderOverride() {
		if (hasSeasonFolder()) {
			for (int i = 0; i < mEpisodes.size(); i++)
				mEpisodes.get(i).setSeason(mSeasonFolderNumber);
		}
	}

	public String getFilepath() {
		return mFilepath;
	}
	
	public String getFilename() {
		return mFilename;
	}

	public String getDecryptedFilename() {
		if (mEpisodes.size() > 0)
			return MizLib.decryptName(mEpisodes.get(0).getBefore(), mCustomTags);
		return MizLib.decryptName(getFilename(), mCustomTags);
	}

	public boolean hasSeasonFolder() {
		return !TextUtils.isEmpty(getSeasonFolderName());
	}

	public String getSeasonFolderName() {
		return mSeasonFolder;
	}

	public boolean hasShowFolder() {
		return !TextUtils.isEmpty(getShowFolderName());
	}

	public String getDecryptedShowFolderName() {
		return MizLib.decryptName(getShowFolderName(), mCustomTags);
	}

	public String getShowFolderName() {
		return mShowFolder;
	}

	public boolean hasImdbId() {
		return null != getImdbId();
	}

	public String getImdbId() {
		return mImdbId;
	}

	public int getSeasonFolderNumber() {
		return mSeasonFolderNumber;
	}

	public int getSeasonFolderNumber(String folderName) {
		folderName = folderName.trim();

		// Season ## or Season## [1-4] [has to begin with it]
		Pattern pattern = Pattern.compile("^(?:season|staffel|series)[-_ \\.]?(\\d{1,4}).*?$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(folderName);
		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

		// S## or S ## [1-4] [has to begin with it]
		pattern = Pattern.compile("^s[-_ \\.]?(\\d{1,4}).*?$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(folderName);
		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

		// ## [1-4] [has to contain just that]
		pattern = Pattern.compile("^(\\d{1,4})$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(folderName);
		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

        // ## season / staffel / series [1-4] [has to contain just that]
        pattern = Pattern.compile("^(\\d{1,4})[-_ \\.]?(?:season|staffel|series)$", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(folderName);
        if (matcher.find())
            return MizLib.getInteger(matcher.group(1));

		// special / specials / special episode / special episodes [has to contain just that]
		pattern = Pattern.compile("^(([s][p][e][c][i][a][l](?:([s]*)|([-_ \\.]?[e][p][i][s][o][d][e][s]*)))|([e][x][t][r][a][s]*))$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(folderName);
		if (matcher.find())
			return 0; // Specials use 0 as the season number

		return -1;
	}

	public ArrayList<Episode> getEpisodes() {
		return mEpisodes;
	}

	/**
	 * Get release year from show folder name (priority) or file name.
	 * @return Release year if found, -1 otherwise.
	 */
	public int getReleaseYear() {
		if (mEpisodes.size() == 0)
			return -1;

		Pattern pattern = Pattern.compile("^.*?((?:18|19|20)[0-9][0-9]).*?$");

		// Attempt to match it against the show folder name first
		Matcher matcher = pattern.matcher(getShowFolderName());

		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

		// Check if there's a release year in the "before" part
		// of a filename, i.e. 2008 for "anything (2008) S01E01.mkv"
		matcher = pattern.matcher(mEpisodes.get(0).getBefore()); // Safe to use the 0-th element for the "before" part regardless of mEpisodes.size()
		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

		// Check if there's a release year in the "after" part
		// of a filename, i.e. 2008 for "anything S01E01 (2008).mkv"
		matcher = pattern.matcher(mEpisodes.get(mEpisodes.size() - 1).getAfter()); // Use the last element to check the "after" part
		if (matcher.find())
			return MizLib.getInteger(matcher.group(1));

		return -1;
	}

	public boolean hasReleaseYear() {
		return getReleaseYear() >= 0;
	}

	public ArrayList<Episode> decryptEpisodes(String filename) {
		// Remove known tags that can mess up the decryption stuff
		filename = filename.replaceAll("(?i)(?:(m?[-]?\\d{3,4}[ip])|[hx]264|\\d{3,4}mb)", ""); // i.e. m480p, 720p, 1080i, h264, x264, 700mb

		ArrayList<Episode> episodes = new ArrayList<Episode>();

		// S##E##
		Pattern pattern = Pattern.compile("(.*?)[s](\\d{1,4})[ ._-]*[e](\\d{1,3})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(filename);

		if (matcher.find()) {			
			int count = 1; // We already have one match
			while (matcher.find())
				count++; // Count any additional matches

			if (count > 1) {
				// We're dealing with the S##E## S##E## format

				// Reset the Matcher so we can find all the matches again
				matcher.reset();

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					int season = MizLib.getInteger(matcher.group(2));
					int episode = MizLib.getInteger(matcher.group(3));
					String after = filename.substring(matcher.end(), filename.length());
					episodes.add(new Episode(season, episode, before, after));
				}
			} else {
				// We're dealing with a single instance of S##E## or a multi-episode format (i.e. S##E##E##E##)

				pattern = Pattern.compile("(.*?)[s](\\d{1,4})[ ._-]*[e](\\d{1,3}(?:[-_ex]\\d{1,3})*)(.*)", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(filename);

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					String after = matcher.group(4);
					int season = MizLib.getInteger(matcher.group(2));

					// Pre-compiled Pattern to speed up to the splitting process
					Pattern splitPattern = Pattern.compile("[-_ex]", Pattern.CASE_INSENSITIVE);

					for (String episode : splitPattern.split(matcher.group(3)))
						episodes.add(new Episode(season, MizLib.getInteger(episode), before, after));				
				}
			}

		}

		if (episodes.size() > 0)
			return episodes;

		// ##E## (has to begin with it)
		pattern = Pattern.compile("^(\\d{1,4})[ ._-]*[e](\\d{1,3})", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);

		if (matcher.find()) {
			int season = MizLib.getInteger(matcher.group(1));
			int episode = MizLib.getInteger(matcher.group(2));
			String after = filename.substring(matcher.end(), filename.length());
			episodes.add(new Episode(season, episode, "", after));
		}

		if (episodes.size() > 0)
			return episodes;

		// season ## episode ##
		pattern = Pattern.compile("(.*?)(?:season|staffel|series)[ ._-]*(\\d{1,4})[ ._-]*episode[ ._-]*(\\d{1,3})(.*)", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);

		if (matcher.find()) {
			String before = matcher.group(1);
			int season = MizLib.getInteger(matcher.group(2));
			int episode = MizLib.getInteger(matcher.group(3));
			String after = filename.substring(matcher.end(), filename.length());
			episodes.add(new Episode(season, episode, before, after));
		}

		if (episodes.size() > 0)
			return episodes;

		// ep##, episode##
		pattern = Pattern.compile("(.*?)[e][p](?:[i][s][o][d][e])?[ ._-]*(\\d{1,3})", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);

		if (matcher.find()) {
			int count = 1; // We already have one match
			while (matcher.find())
				count++; // Count any additional matches

			if (count > 1) {
				// We're dealing with the ep## ep## format

				// Reset the Matcher so we can find all matches again
				matcher.reset();

				int season = 1; // Assumed since there's no season information with this naming convention

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					int episode = MizLib.getInteger(matcher.group(2));
					String after = filename.substring(matcher.end(), filename.length());
					episodes.add(new Episode(season, episode, before, after));
				}
			} else {
				// We're dealing with a single instance of ep## or a multi-episode format (i.e. ep##x##e##)

				pattern = Pattern.compile("(.*?)[e][p](?:[i][s][o][d][e])?[ ._-]*(\\d{1,3}(?:[-_ex]\\d{1,3})*)(.*)", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(filename);

				int season = 1; // Assumed since there's no season information with this naming convention

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					String after = matcher.group(3);

					// Pre-compiled Pattern to speed up to the splitting process
					Pattern splitPattern = Pattern.compile("[-_ex]", Pattern.CASE_INSENSITIVE);

					for (String episode : splitPattern.split(matcher.group(2)))
						episodes.add(new Episode(season, MizLib.getInteger(episode), before, after));				
				}
			}

		}

		if (episodes.size() > 0)
			return episodes;

		// part##, pt##
		pattern = Pattern.compile("(.*?)[p](?:[a][r])?[t][ ._-]*(\\d{1,3})", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);

		if (matcher.find()) {
			int count = 1; // We already have one match
			while (matcher.find())
				count++; // Count any additional matches

			if (count > 1) {
				// We're dealing with the pt## part## format

				// Reset the Matcher so we can find all matches again
				matcher.reset();

				int season = 1; // Assumed since there's no season information with this naming convention

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					int episode = MizLib.getInteger(matcher.group(2));
					String after = filename.substring(matcher.end(), filename.length());
					episodes.add(new Episode(season, episode, before, after));
				}
			} else {
				// We're dealing with a single instance of part## or a multi-episode format (i.e. pt##x##e##)

				pattern = Pattern.compile("(.*?)[p](?:[a][r])?[t][ ._-]*(\\d{1,3}(?:[-_ex]\\d{1,3})*)(.*)", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(filename);

				int season = 1; // Assumed since there's no season information with this naming convention

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					String after = matcher.group(3);

					// Pre-compiled Pattern to speed up to the splitting process
					Pattern splitPattern = Pattern.compile("[-_ex]", Pattern.CASE_INSENSITIVE);

					for (String episode : splitPattern.split(matcher.group(2)))
						episodes.add(new Episode(season, MizLib.getInteger(episode), before, after));				
				}
			}

		}

		if (episodes.size() > 0)
			return episodes;

		// ##x##
		pattern = Pattern.compile("(.*?)(\\d{1,4})[ ._-]*[x](\\d{1,3})", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);

		if (matcher.find()) {
			int count = 1; // We already have one match
			while (matcher.find())
				count++; // Count any additional matches

			if (count > 1) {
				// We're dealing with the ##x## ##x## format

				// Reset the Matcher so we can find all matches again
				matcher.reset();

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					int season = MizLib.getInteger(matcher.group(2));
					int episode = MizLib.getInteger(matcher.group(3));
					String after = filename.substring(matcher.end(), filename.length());
					episodes.add(new Episode(season, episode, before, after));
				}
			} else {
				// We're dealing with a single instance of ##x## or a multi-episode format (i.e. ##x##x##e##)

				pattern = Pattern.compile("(.*?)(\\d{1,4})[ ._-]*[x](\\d{1,3}(?:[-_ex]\\d{1,3})*)(.*)", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(filename);

				// Go through all matches and add each one
				while (matcher.find()) {
					String before = matcher.group(1);
					String after = matcher.group(4);
					int season = MizLib.getInteger(matcher.group(2));

					// Pre-compiled Pattern to speed up to the splitting process
					Pattern splitPattern = Pattern.compile("[-_ex]", Pattern.CASE_INSENSITIVE);

					for (String episode : splitPattern.split(matcher.group(3)))
						episodes.add(new Episode(season, MizLib.getInteger(episode), before, after));				
				}
			}

		}

		if (episodes.size() > 0)
			return episodes;

		// ### [3-7]
		pattern = Pattern.compile("(^.*?)((\\d){3,7})(.*?)$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		if (matcher.find()) { // We only want a single set of results here	
			int season = 0, episode = 0;
			String before = matcher.group(1);
			String group = matcher.group(2);
			String after = matcher.group(3);

			switch (group.length()) {
			case 3: // ### (season [1], episode [2])
				season = MizLib.getInteger(group.substring(0, 1));
				episode = MizLib.getInteger(group.substring(1, 3));
				break;
			case 4: // #### (season [2], episode [2])
				season = MizLib.getInteger(group.substring(0, 2));
				episode = MizLib.getInteger(group.substring(2, 4));
				break;
			case 5: // ##### (season [2], episode [3])
				season = MizLib.getInteger(group.substring(0, 2));
				episode = MizLib.getInteger(group.substring(2, 5));
				break;
			case 6: // ###### (season [3], episode [3])
				season = MizLib.getInteger(group.substring(0, 3));
				episode = MizLib.getInteger(group.substring(3, 6));
				break;
			case 7: // ####### (season [4], episode [3])
				season = MizLib.getInteger(group.substring(0, 4));
				episode = MizLib.getInteger(group.substring(4, 7));
				break;
			}

			episodes.add(new Episode(season, episode, before, after));
		}

		if (episodes.size() > 0)
			return episodes;

		// ## [1-3] episode information only (has to start with this)
		pattern = Pattern.compile("^(\\d{1,3})(.*?)$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		if (matcher.find()) { // We only want a single set of results here	
			int season = 1; // Assumed since there's no season information
			int episode = MizLib.getInteger(matcher.group(1));
			String after = matcher.group(2);

			episodes.add(new Episode(season, episode, "", after)); // No "before" string
		}

		if (episodes.size() > 0)
			return episodes;

		// ## [1-2] episode information only (anywhere in the string)
		pattern = Pattern.compile("(.*?)(\\d{1,2})(.*?)$", Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		if (matcher.find()) { // We only want a single set of results here
			String before = matcher.group(1);
			int season = 1; // Assumed since there's no season information
			int episode = MizLib.getInteger(matcher.group(2));
			String after = matcher.group(3);

			episodes.add(new Episode(season, episode, before, after));
		}

		if (episodes.size() > 0)
			return episodes;

		return episodes;
	}
}