package com.miz.test;/*
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

import android.test.AndroidTestCase;

import com.miz.identification.ShowStructure;

public class TvShowFilenameTests extends AndroidTestCase {

	public void testCase1() {
		ShowStructure ss = new ShowStructure("/2 Broke Girls/Season 1/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals("2 Broke Girls", ss.getShowFolderName());
		assertEquals("2 Broke Girls - S01E01 - Reboot in Brooklyn.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("2 Broke Girls", ss.getDecryptedShowFolderName());
		assertEquals("2 Broke Girls", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase2() {
		ShowStructure ss = new ShowStructure("/Lost/Lost S01E01.mp4");
		assertEquals("Lost", ss.getShowFolderName());
		assertEquals("Lost S01E01.mp4", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Lost", ss.getDecryptedShowFolderName());
		assertEquals("Lost", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase3() {
		ShowStructure ss = new ShowStructure("/The.British.Empire.in.Colour/Season1 /The British Empire in Colour Part 1.mkv");
		assertEquals("The.British.Empire.in.Colour", ss.getShowFolderName());
		assertEquals("The British Empire in Colour Part 1.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("The British Empire in Colour", ss.getDecryptedShowFolderName());
		assertEquals("The British Empire in Colour", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase4() {
		ShowStructure ss = new ShowStructure("/An Idiot Abroad s02e02.thebox.hannibal-LF.mkv");
		assertEquals(false, ss.hasShowFolder());
		assertEquals("An Idiot Abroad s02e02.thebox.hannibal-LF.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(2, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());
		assertEquals("An Idiot Abroad", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase5() {
		ShowStructure ss = new ShowStructure("/Lost.306.m720p-FReeLOVE.mkv");
		assertEquals(false, ss.hasShowFolder());
		assertEquals("Lost.306.m720p-FReeLOVE.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(3, ss.getEpisodes().get(0).getSeason());
		assertEquals(6, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Lost", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase6() {
		ShowStructure ss = new ShowStructure("/The_Office_S02E09_720p_h264-CtrlHD.mkv");
		assertEquals(false, ss.hasShowFolder());
		assertEquals("The_Office_S02E09_720p_h264-CtrlHD.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());		
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(2, ss.getEpisodes().get(0).getSeason());
		assertEquals(9, ss.getEpisodes().get(0).getEpisode());
		assertEquals("The Office", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}

	public void testCase7() {
		ShowStructure ss = new ShowStructure("/Serien/Falling Skies/Staffel 1/Falling Skies - 1x05 - Die Rettung - 2011-07-10 - Sci-Fi-Serie.avi");
		assertEquals("Falling Skies", ss.getShowFolderName());
		assertEquals("Falling Skies - 1x05 - Die Rettung - 2011-07-10 - Sci-Fi-Serie.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Staffel 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(5, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Falling Skies", ss.getDecryptedShowFolderName());
		assertEquals("Falling Skies", ss.getDecryptedFilename());
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2011, ss.getReleaseYear());
	}
	
	public void testCase8() {
		ShowStructure ss = new ShowStructure("/Looney Tunes/Season 1960/Looney Tunes S1960E05 episodename.avi");
		assertEquals("Looney Tunes", ss.getShowFolderName());
		assertEquals("Looney Tunes S1960E05 episodename.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1960", ss.getSeasonFolderName());
		assertEquals(1960, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1960, ss.getEpisodes().get(0).getSeason());
		assertEquals(5, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Looney Tunes", ss.getDecryptedShowFolderName());
		assertEquals("Looney Tunes", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase9() {
		ShowStructure ss = new ShowStructure("/Talespin V1-2 DVDRip x264-panos/1/1e16-Her Chance to Dream.mkv");
		assertEquals("Talespin V1-2 DVDRip x264-panos", ss.getShowFolderName());
		assertEquals("1e16-Her Chance to Dream.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(16, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Talespin V12 panos", ss.getDecryptedShowFolderName());
		assertEquals("", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase10() {
		ShowStructure ss = new ShowStructure("/The.Corner/Season 1/The Corner - Episode 1 -Gary's Blues.avi");
		assertEquals("The.Corner", ss.getShowFolderName());
		assertEquals("The Corner - Episode 1 -Gary's Blues.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("The Corner", ss.getDecryptedShowFolderName());
		assertEquals("The Corner", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase11() {
		ShowStructure ss = new ShowStructure("/Doctor.Who.2005.S07.720p.BluRay.DTS.x264/trips-doctor.who.s07e06.mkv");
		assertEquals("Doctor.Who.2005.S07.720p.BluRay.DTS.x264", ss.getShowFolderName());
		assertEquals("trips-doctor.who.s07e06.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(7, ss.getEpisodes().get(0).getSeason());
		assertEquals(6, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Doctor Who", ss.getDecryptedShowFolderName());
		assertEquals("tripsdoctor who", ss.getDecryptedFilename());
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());
	}
	
	public void testCase12() {
		ShowStructure ss = new ShowStructure("/Doctor.Who.2005.S07.720p.BluRay.DTS.x264/doctor.who.2005.s07e01.720p.bluray.x264-bia.mkv");
		assertEquals("Doctor.Who.2005.S07.720p.BluRay.DTS.x264", ss.getShowFolderName());
		assertEquals("doctor.who.2005.s07e01.720p.bluray.x264-bia.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(7, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Doctor Who", ss.getDecryptedShowFolderName());
		assertEquals("doctor who", ss.getDecryptedFilename());
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());
	}
	
	public void testCase13() {
		ShowStructure ss = new ShowStructure("/Serier/The Fresh Prince of Bel-Air/S01/The Fresh Prince of Bel-Air - 101 - The Fresh Prince Project.avi");
		assertEquals("The Fresh Prince of Bel-Air", ss.getShowFolderName());
		assertEquals("The Fresh Prince of Bel-Air - 101 - The Fresh Prince Project.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("S01", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("The Fresh Prince of BelAir", ss.getDecryptedShowFolderName());
		assertEquals("The Fresh Prince of BelAir", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase14() {
		ShowStructure ss = new ShowStructure("/TV/It's Always Sunny in Philadelphia/Season 2/01 - Charlie Gets Crippled.mkv");
		assertEquals("It's Always Sunny in Philadelphia", ss.getShowFolderName());
		assertEquals("01 - Charlie Gets Crippled.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 2", ss.getSeasonFolderName());
		assertEquals(2, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(2, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Its Always Sunny in Philadelphia", ss.getDecryptedShowFolderName());
		assertEquals("", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase15() {
		ShowStructure ss = new ShowStructure("/TV Shows/Battlestar Galactica (2003)/Season 0/S00E03 - The Story So Far.mkv");
		assertEquals("Battlestar Galactica (2003)", ss.getShowFolderName());
		assertEquals("S00E03 - The Story So Far.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 0", ss.getSeasonFolderName());
		assertEquals(0, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(0, ss.getEpisodes().get(0).getSeason());
		assertEquals(3, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Battlestar Galactica", ss.getDecryptedShowFolderName());
		assertEquals("", ss.getDecryptedFilename());
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2003, ss.getReleaseYear());
	}

	public void testCase16() {
		ShowStructure ss = new ShowStructure("/Curb Your Enthusiasm/Season 1/Curb Your Enthusiasm Season 1 Episode 04 - The Bracelet.mkv");
		assertEquals("Curb Your Enthusiasm", ss.getShowFolderName());
		assertEquals("Curb Your Enthusiasm Season 1 Episode 04 - The Bracelet.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(4, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Curb Your Enthusiasm", ss.getDecryptedShowFolderName());
		assertEquals("Curb Your Enthusiasm", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase17() {
		ShowStructure ss = new ShowStructure("/World War II in HD/Season 1/World War II in HD 01 Darkness Falls.mkv");
		assertEquals("World War II in HD", ss.getShowFolderName());
		assertEquals("World War II in HD 01 Darkness Falls.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("World War II in HD", ss.getDecryptedShowFolderName());
		assertEquals("World War II in HD", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase18() {
		ShowStructure ss = new ShowStructure("/I.Claudius,Season 1/I.Claudius.E01.A.Touch.Of.Murder-ZOXX.mkv");
		assertEquals("I.Claudius,Season 1", ss.getShowFolderName());
		assertEquals("I.Claudius.E01.A.Touch.Of.Murder-ZOXX.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("I Claudius", ss.getDecryptedShowFolderName());
		assertEquals("I Claudius E", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase19() {
		ShowStructure ss = new ShowStructure("/House MD Season 1,2,3,4,5,6,7 + Extras (Deleted Scenes etc) DVDR/Season 1/House MD Season 1 Episode 01 - Pilot.avi");
		assertEquals("House MD Season 1,2,3,4,5,6,7 + Extras (Deleted Scenes etc) DVDR", ss.getShowFolderName());
		assertEquals("House MD Season 1 Episode 01 - Pilot.avi", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals(1, ss.getSeasonFolderNumber());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("House MD", ss.getDecryptedShowFolderName());
		assertEquals("House MD", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase20() {
		ShowStructure ss = new ShowStructure("/Skins.S01.COMPLETE.ENGLISH.HDTVRip.720p.x264-TvR/Skins.S01E01.Tony.ENGLISH.HDTVRip.720p.x264-TvR/tvr-skins-s01e01-720p.mkv");
		assertEquals("Skins.S01E01.Tony.ENGLISH.HDTVRip.720p.x264-TvR", ss.getShowFolderName());
		assertEquals("tvr-skins-s01e01-720p.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(false, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals("Skins", ss.getDecryptedShowFolderName());
		assertEquals("skins", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testCase21() {
		ShowStructure ss = new ShowStructure("Music/iTunes/iTunes Media/TV Shows/One Piece/Season 1/11 Expose the Plot! Pirate Butler, C.mkv");
		assertEquals("One Piece", ss.getShowFolderName());
		assertEquals("Season 1", ss.getSeasonFolderName());
		assertEquals("11 Expose the Plot! Pirate Butler, C.mkv", ss.getFilename());
		assertEquals(false, ss.hasImdbId());
		assertEquals(true, ss.hasSeasonFolder());
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(11, ss.getEpisodes().get(0).getEpisode());
		assertEquals("One Piece", ss.getDecryptedShowFolderName());
		assertEquals("", ss.getDecryptedFilename());
		assertEquals(false, ss.hasReleaseYear());
	}
	
	public void testSpecialsSeasonFolder() {
		ShowStructure ss = new ShowStructure("/2 Broke Girls/Special/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());

		ss = new ShowStructure("/2 Broke Girls/Specials/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());

		ss = new ShowStructure("/2 Broke Girls/Special episode/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());

		ss = new ShowStructure("/2 Broke Girls/Special episodes/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());

		ss = new ShowStructure("/2 Broke Girls/extra/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());

		ss = new ShowStructure("/2 Broke Girls/extras/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");
		assertEquals(0, ss.getSeasonFolderNumber());
	}

	public void testCustomTags() {
		ShowStructure ss = new ShowStructure("/2 Broke Girls/Season 1/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi");

		// Decrypted without custom tags
		assertEquals("2 Broke Girls", ss.getDecryptedShowFolderName());
		assertEquals("2 Broke Girls", ss.getDecryptedFilename());

		// Decrypted with custom tags
		ss.setCustomTags("Girls");
		assertEquals("2 Broke", ss.getDecryptedShowFolderName());
		assertEquals("2 Broke", ss.getDecryptedFilename());
	}

	/**
	 * Names like S##E## with information about a single episode
	 */
	public void testNamingConvention1Single() {
		String testFilename = "foo.s01.e02.mkv";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "foo.s01_e02.mkv";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "S01E02 foo.mkv";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "S01 - E02.mkv";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_s01e02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_s1e2.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_s01.e02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_s01_e02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());
	}

	/**
	 * Names like S##E##E## or S##E##_S##E## with information about multiple episodes
	 */
	public void testNamingConvention1Multi() {
		String testFilename = "anything_s01e01_s01e02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.s01e01.episode1.title.s01e02.episode2.title.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.s01e01.s01e02.s01e03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());

		testFilename = "anything.s01e01e02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.s01e01-02-03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
	}

	/**
	 * Names like ##x## with information about a single episode
	 */
	public void testNamingConvention2Single() {
		String testFilename = "anything_1x02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "foo.1x09";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(9, ss.getEpisodes().get(0).getEpisode());

		testFilename = "1x09";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(9, ss.getEpisodes().get(0).getEpisode());
	}

	/**
	 * Names like #x##x## or ##x##_##x## with information about multiple episodes
	 */
	public void testNamingConvention2Multi() {
		String testFilename = "anything.1x01_1x02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.1x01x02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.1x01_1x02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "name.1x01e02_03-04.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(4, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
		assertEquals(1, ss.getEpisodes().get(3).getSeason());
		assertEquals(4, ss.getEpisodes().get(3).getEpisode());
	}

	/**
	 * Names like ep## or episode## with information about a single episode
	 */
	public void testNamingConvention3Single() {
		String testFilename = "anything_ep02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_episode02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_ep_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_episode_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());
	}

	/**
	 * Names like ep##.ep## or episode##_## with information about multiple episodes
	 */
	public void testNamingConvention3Multi() {
		String testFilename = "anything.ep01.ep02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.episode01.episode02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.ep01_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.episode01_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.ep01_02_03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());

		testFilename = "anything.episode01_02_03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());

		testFilename = "anything.ep01_02x03e04-5.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(5, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
		assertEquals(1, ss.getEpisodes().get(3).getSeason());
		assertEquals(4, ss.getEpisodes().get(3).getEpisode());
		assertEquals(1, ss.getEpisodes().get(4).getSeason());
		assertEquals(5, ss.getEpisodes().get(4).getEpisode());

		testFilename = "anything.episode01_02x03e04-5.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(5, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
		assertEquals(1, ss.getEpisodes().get(3).getSeason());
		assertEquals(4, ss.getEpisodes().get(3).getEpisode());
		assertEquals(1, ss.getEpisodes().get(4).getSeason());
		assertEquals(5, ss.getEpisodes().get(4).getEpisode());
		
		// Let's try to fool it!
		testFilename = "anything.ep01_02 - 2011-07-10.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size()); // Hah, we failed!
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
	}

	/**
	 * Names like ### with information about a single episode
	 */
	public void testNamingConvention4Single() {
		String testFilename = "anything_102.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_1021.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(10, ss.getEpisodes().get(0).getSeason());
		assertEquals(21, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_25123.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(25, ss.getEpisodes().get(0).getSeason());
		assertEquals(123, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_025123.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(25, ss.getEpisodes().get(0).getSeason());
		assertEquals(123, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_2014023.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(2014, ss.getEpisodes().get(0).getSeason());
		assertEquals(23, ss.getEpisodes().get(0).getEpisode());
	}

	/**
	 * Names like pt## / part## with information about a single episode
	 */
	public void testNamingConvention5Single() {
		String testFilename = "anything_part02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_pt02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_part_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());

		testFilename = "anything_pt_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(1, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(2, ss.getEpisodes().get(0).getEpisode());
	}

	/**
	 * Names like part##.part## or pt##_## with information about multiple episodes
	 */
	public void testNamingConvention5Multi() {
		String testFilename = "anything.pt01.pt02.ext";
		ShowStructure ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.part01.pt02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.pt01_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.part01_02.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(2, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());

		testFilename = "anything.pt01_02_03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());

		testFilename = "anything.part01_02_03.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(3, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());

		testFilename = "anything.pt01_02x03e04-5.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(5, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
		assertEquals(1, ss.getEpisodes().get(3).getSeason());
		assertEquals(4, ss.getEpisodes().get(3).getEpisode());
		assertEquals(1, ss.getEpisodes().get(4).getSeason());
		assertEquals(5, ss.getEpisodes().get(4).getEpisode());

		testFilename = "anything.part01_02x03e04-5.ext";
		ss = new ShowStructure(testFilename);
		assertEquals(5, ss.getEpisodes().size());
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
		assertEquals(1, ss.getEpisodes().get(1).getSeason());
		assertEquals(2, ss.getEpisodes().get(1).getEpisode());
		assertEquals(1, ss.getEpisodes().get(2).getSeason());
		assertEquals(3, ss.getEpisodes().get(2).getEpisode());
		assertEquals(1, ss.getEpisodes().get(3).getSeason());
		assertEquals(4, ss.getEpisodes().get(3).getEpisode());
		assertEquals(1, ss.getEpisodes().get(4).getSeason());
		assertEquals(5, ss.getEpisodes().get(4).getEpisode());
	}

	/**
	 * Mizuu should prioritize certain naming conventions over others in order
	 * to avoid using any release years as a naming convention.
	 */
	public void testPrioritizing() {

		ShowStructure ss = new ShowStructure("Doctor Who (2005) S01E01.mkv");
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());

		ss = new ShowStructure("Doctor Who (2005) 01x01.mkv");
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());

		ss = new ShowStructure("Doctor Who (2005) ep01.mkv");
		assertEquals(1, ss.getEpisodes().get(0).getSeason());
		assertEquals(1, ss.getEpisodes().get(0).getEpisode());
	}

	public void testSlashSplitting() {
		ShowStructure ss = new ShowStructure("/TV Shows/Chuck/Season 5/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("Season 5", ss.getSeasonFolderName());
		assertEquals("Chuck", ss.getShowFolderName());

		ss = new ShowStructure("/Chuck/Season05/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("Season05", ss.getSeasonFolderName());
		assertEquals("Chuck", ss.getShowFolderName());

		ss = new ShowStructure("/Chuck/S05/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("S05", ss.getSeasonFolderName());
		assertEquals("Chuck", ss.getShowFolderName());

		ss = new ShowStructure("Chuck/05/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("05", ss.getSeasonFolderName());
		assertEquals("Chuck", ss.getShowFolderName());

		ss = new ShowStructure("Chuck/05 lulz/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("", ss.getSeasonFolderName());
		assertEquals("05 lulz", ss.getShowFolderName());

		ss = new ShowStructure("Chuck/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("", ss.getSeasonFolderName());
		assertEquals("Chuck", ss.getShowFolderName());

		ss = new ShowStructure("/S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("", ss.getSeasonFolderName());
		assertEquals("", ss.getShowFolderName());

		ss = new ShowStructure("S05E01.mkv");
		assertEquals("S05E01.mkv", ss.getFilename());
		assertEquals("", ss.getSeasonFolderName());
		assertEquals("", ss.getShowFolderName());
	}

	public void testReleaseYear() {
		ShowStructure ss = new ShowStructure("/TBBT/S01E05.mkv");
		assertEquals(false, ss.hasReleaseYear());

		ss = new ShowStructure("/TBBT (2005)/S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		ss = new ShowStructure("/TBBT (2005)/S01/S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// 2012 is a valid season number (for some daily TV shows, etc.)
		// so even if there is a 2012 season, it should use the show name release year (2005)
		ss = new ShowStructure("/TBBT (2005)/2012/S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Disregard filename year if show folder contains it
		ss = new ShowStructure("/TBBT (2005)/2004 S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Disregard filename year if show folder contains it
		ss = new ShowStructure("/TBBT (2005)/S01E05 2004.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Use filename year if show folder doesn't contains it
		ss = new ShowStructure("/TBBT/2004 S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2004, ss.getReleaseYear());

		// Use filename year if show folder doesn't contains it
		ss = new ShowStructure("/TBBT/S01E05 2004.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2004, ss.getReleaseYear());

		// Use filename year if show folder doesn't contains it
		ss = new ShowStructure("/TBBT/TBBT (2004) S01E05.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2004, ss.getReleaseYear());

		// Use filename year if show folder doesn't contains it
		ss = new ShowStructure("/TBBT/TBBT S01E05 (2004).mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2004, ss.getReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.1x01_1x02.mkv");
		assertEquals(false, ss.hasReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.2005.1x01_1x02.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.s05e01_s05e02 (2005).mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.1x01_1x02 (2005).mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.1x01_1x02.2005.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());

		// Handle multi-episode files as well
		ss = new ShowStructure("/TBBT/TBBT.1x01x02.2005.mkv");
		assertEquals(true, ss.hasReleaseYear());
		assertEquals(2005, ss.getReleaseYear());
	}

	public void testImdb() {
		ShowStructure ss = new ShowStructure("/The Big Bang Theory tt0898266/S05E01.mkv");
		assertEquals("tt0898266", ss.getImdbId());

		ss = new ShowStructure("/The Big Bang Theory/S05E01 tt0898266.mkv");
		assertEquals("tt0898266", ss.getImdbId());

		ss = new ShowStructure("/The Big Bang Theory tt0898266/S05E01 tt0898265.mkv"); // prioritize the show folder one
		assertEquals("tt0898266", ss.getImdbId());
	}
}