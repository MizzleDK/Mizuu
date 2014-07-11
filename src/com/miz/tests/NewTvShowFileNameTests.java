package com.miz.tests;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import android.test.AndroidTestCase;

public class NewTvShowFileNameTests extends AndroidTestCase {

	private String mSeparators = "[\\.\\-_\\s]?";

	private String mMultiPart = "[-_EeXx]+([0-9]+)";

	// (S)[1-4]E[1-3]
	private String mNamingConvention1 = "(^.*?)[S](\\d{1,4})[ ._-]*[E](\\d{1,3})(.*?)$";
	private String mNamingConvention1Multi = "[S](\\d{1,4})[ ._-]*[E](\\d{1,3})";

	// ep[1-3]
	private String mNamingConvention2 = "(^.*?)[e][p]" + mSeparators + "(\\d{1,3})(.*?)$";
	private String mNamingConvention2Multi = "[e][p]" + mSeparators + "(\\d{1,3})";

	// [1-4]x[1-3]
	private String mNamingConvention3 = "(^.*?)(\\d{1,4})x(\\d{1,3})(.*?)$";
	private String mNamingConvention3Multi = "(\\d{1,4})x(\\d{1,3})";

	// [3-7]
	private String mNamingConvention4 = "(^.*?)((\\d){3,7})(.*?)$";

	/**
	 * Names like S##E## with information about a single episode
	 */
	public void testNamingConvention1Single() {
		String testFilename = "foo.s01.e02";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "foo.s01_e02";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "S01E02 foo";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "S01 - E02";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_s01e02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_s1e2.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_s01.e02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_s01_e02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());
	}

	/**
	 * Names like S##E##E## or S##E##_S##E## with information about multiple episodes
	 */
	public void testNamingConvention1Multi() {
		String testFilename = "anything_s01e01_s01e02.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.s01e01.episode1.title.s01e02.episode2.title.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.s01e01.s01e02.s01e03.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(3, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());
		Assert.assertEquals(1, testArray.get(2).getSeason());
		Assert.assertEquals(3, testArray.get(2).getEpisode());

		testFilename = "anything.s01e01e02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.s01e01-02-03.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(3, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());
		Assert.assertEquals(1, testArray.get(2).getSeason());
		Assert.assertEquals(3, testArray.get(2).getEpisode());
	}

	/**
	 * Names like ##x## with information about a single episode
	 */
	public void testNamingConvention2Single() {
		String testFilename = "anything_1x02.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "foo.1x09";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(9, testArray.get(0).getEpisode());

		testFilename = "1x09";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(9, testArray.get(0).getEpisode());
	}

	/**
	 * Names like #x##x## or ##x##_##x## with information about multiple episodes
	 */
	public void testNamingConvention2Multi() {
		String testFilename = "anything.1x01_1x02.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.1x01x02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.1x01_1x02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "name.1x01e02_03-x-04.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(4, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());
		Assert.assertEquals(1, testArray.get(2).getSeason());
		Assert.assertEquals(3, testArray.get(2).getEpisode());
		Assert.assertEquals(1, testArray.get(3).getSeason());
		Assert.assertEquals(4, testArray.get(3).getEpisode());
	}

	/**
	 * Names like ep## with information about a single episode
	 */
	public void testNamingConvention3Single() {
		String testFilename = "anything_ep02.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_ep_02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());
	}

	/**
	 * Names like ep##.ep## or ep##_## with information about multiple episodes
	 */
	public void testNamingConvention3Multi() {
		String testFilename = "anything.ep01.ep02.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.ep01_02.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(2, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());

		testFilename = "anything.ep01_02_03.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(3, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());
		Assert.assertEquals(1, testArray.get(2).getSeason());
		Assert.assertEquals(3, testArray.get(2).getEpisode());

		testFilename = "anything.ep01_02x03e04-5.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(5, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(1, testArray.get(0).getEpisode());
		Assert.assertEquals(1, testArray.get(1).getSeason());
		Assert.assertEquals(2, testArray.get(1).getEpisode());
		Assert.assertEquals(1, testArray.get(2).getSeason());
		Assert.assertEquals(3, testArray.get(2).getEpisode());
		Assert.assertEquals(1, testArray.get(3).getSeason());
		Assert.assertEquals(4, testArray.get(3).getEpisode());
		Assert.assertEquals(1, testArray.get(4).getSeason());
		Assert.assertEquals(5, testArray.get(4).getEpisode());
	}
	
	/**
	 * Names like ### with information about a single episode
	 */
	public void testNamingConvention4Single() {
		String testFilename = "anything_102.ext";
		ArrayList<Episode> testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(1, testArray.get(0).getSeason());
		Assert.assertEquals(2, testArray.get(0).getEpisode());

		testFilename = "anything_1021.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(10, testArray.get(0).getSeason());
		Assert.assertEquals(21, testArray.get(0).getEpisode());
		
		testFilename = "anything_25123.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(25, testArray.get(0).getSeason());
		Assert.assertEquals(123, testArray.get(0).getEpisode());
		
		testFilename = "anything_025123.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(25, testArray.get(0).getSeason());
		Assert.assertEquals(123, testArray.get(0).getEpisode());
		
		testFilename = "anything_2014023.ext";
		testArray = getEpisodes(testFilename);
		Assert.assertEquals(1, testArray.size());
		Assert.assertEquals(2014, testArray.get(0).getSeason());
		Assert.assertEquals(23, testArray.get(0).getEpisode());
	}

	private ArrayList<Episode> getEpisodes(String filename) {
		ArrayList<Episode> episodes = new ArrayList<Episode>();

		// S##E##
		Pattern pattern = Pattern.compile(mNamingConvention1, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(filename);
		while (matcher.find()) {

			String before = matcher.group(1);
			int season = getInteger(matcher.group(2));
			int episode = getInteger(matcher.group(3));
			String after = matcher.group(4);

			episodes.add(new Episode(season, episode, before, after));

			// Check if this is a multi-part file
			if (!after.isEmpty()) {				
				Pattern p = Pattern.compile(mMultiPart, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(after);

				// S##E##E##E##
				if (after.matches("^[-_EeXx]+[^sS].*")) {
					while (m.find()) {
						episode = getInteger(m.group(1));
						episodes.add(new Episode(season, episode, before, after));
					}
				} else { // S##EE S##E##
					p = Pattern.compile(mNamingConvention1Multi, Pattern.CASE_INSENSITIVE);
					m = p.matcher(after);
					while (m.find()) {
						season = getInteger(m.group(1));
						episode = getInteger(m.group(2));
						episodes.add(new Episode(season, episode, before, after));
					}
				}
			}
		}
		if (episodes.size() > 0)
			return episodes;

		// ep##
		pattern = Pattern.compile(mNamingConvention2, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		while (matcher.find()) {
			String before = matcher.group(1);
			int season = 1; // Assumed in this naming convention as there's no season information
			int episode = getInteger(matcher.group(2));
			String after = matcher.group(3);

			episodes.add(new Episode(season, episode, before, after));

			// Check if this is a multi-part file
			if (!after.isEmpty()) {				
				Pattern p = Pattern.compile(mNamingConvention2Multi, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(after);

				// ep## ep##
				if (after.matches(".*" + mNamingConvention2Multi + ".*")) {					
					m = p.matcher(after);
					while (m.find()) {						
						episode = getInteger(m.group(1));
						episodes.add(new Episode(season, episode, before, after));
					}
				} else { // ep##_##-##x##e##
					p = Pattern.compile(mMultiPart, Pattern.CASE_INSENSITIVE);
					m = p.matcher(after);
					while (m.find()) {
						episode = getInteger(m.group(1));
						episodes.add(new Episode(season, episode, before, after));
					}
				}
			}
		}
		if (episodes.size() > 0)
			return episodes;

		// ##x##
		pattern = Pattern.compile(mNamingConvention3, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		while (matcher.find()) {

			String before = matcher.group(1);
			int season = getInteger(matcher.group(2));
			int episode = getInteger(matcher.group(3));
			String after = matcher.group(4);

			episodes.add(new Episode(season, episode, before, after));

			// Check if this is a multi-part file
			if (!after.isEmpty()) {
				Pattern p = Pattern.compile(mNamingConvention3Multi, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(after);

				// ##x## ##x##
				if (after.matches(".*" + mNamingConvention3Multi + ".*")) {
					while (m.find()) {
						season = getInteger(m.group(1));
						episode = getInteger(m.group(2));
						episodes.add(new Episode(season, episode, before, after));
					}
				} else { // ##x##_##e##x##-##
					p = Pattern.compile(mMultiPart, Pattern.CASE_INSENSITIVE);
					m = p.matcher(after);
					while (m.find()) {
						episode = getInteger(m.group(1));
						episodes.add(new Episode(season, episode, before, after));
					}
				}
			}
		}
		if (episodes.size() > 0)
			return episodes;

		// ### [3-7]
		pattern = Pattern.compile(mNamingConvention4, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(filename);
		if (matcher.find()) { // We only want a single set of results here	
			int season = 0, episode = 0;
			String before = matcher.group(1);
			String group = matcher.group(2);
			String after = matcher.group(3);

			switch (group.length()) {
			case 3: // ### (season [1], episode [2])
				season = getInteger(group.substring(0, 1));
				episode = getInteger(group.substring(1, 3));
				break;
			case 4: // #### (season [2], episode [2])
				season = getInteger(group.substring(0, 2));
				episode = getInteger(group.substring(2, 4));
				break;
			case 5: // ##### (season [2], episode [3])
				season = getInteger(group.substring(0, 2));
				episode = getInteger(group.substring(2, 5));
				break;
			case 6: // ###### (season [3], episode [3])
				season = getInteger(group.substring(0, 3));
				episode = getInteger(group.substring(3, 6));
				break;
			case 7: // ####### (season [4], episode [3])
				season = getInteger(group.substring(0, 4));
				episode = getInteger(group.substring(4, 7));
				break;
			}

			episodes.add(new Episode(season, episode, before, after));
		}

		if (episodes.size() > 0)
			return episodes;

		return episodes;
	}

	private class Episode {

		private String mBefore, mAfter;
		private int mSeason, mEpisode;

		public Episode(int season, int episode, String before, String after) {
			mSeason = season;
			mEpisode = episode;
			mBefore = before;
			mAfter = after;
		}

		public int getSeason() {
			return mSeason;
		}

		public int getEpisode() {
			return mEpisode;
		}

		public String getBefore() {
			return mBefore;
		}

		public String getAfter() {
			return mAfter;
		}
	}

	private int getInteger(String number) {
		try {
			return Integer.valueOf(number);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
}