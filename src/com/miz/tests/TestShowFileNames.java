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

package com.miz.tests;

import junit.framework.Assert;

import android.test.AndroidTestCase;

import com.miz.functions.DecryptedShowEpisode;
import com.miz.functions.MizLib;

public class TestShowFileNames extends AndroidTestCase {

	private final String[] TEST = new String[]{
			"/HIMYM S01E01.mkv", // 0
			"/Arrow.S01E01.Pilot.1080p.WEB-DL.DD5.1.H.264-ECI.mkv", // 1
			"/An Idiot Abroad s02e02.thebox.hannibal-LF.mkv", // 2
			"/Borgen S01E01.mkv", // 3
			"/Californication s04e02.720p.hdtv.x264-immerse.mkv", // 4
			"/How_I_Met_Your_Mother_1x11_-_The_Limo.avi", // 5
			"/Lost.306.m720p-FReeLOVE.mkv", // 6
			"/The Big Bang Theory - 0x01 Unaired Pilot.avi", // 7
			"/The Fresh Prince of Bel-Air - 109 - Someday Your Prince Will Be in Effect (2).avi", // 8
			"/The.Inbetweeners.s03e00.Prequel.avi", // 9
			"/American Dad (2005) s01e01 - pilot.avi", // 10
			"/The_Office_S02E09_E-mail_Surveillance_720p_h264-CtrlHD.mkv", // 11
			"/How I Met Your Mother S01E01.mkv", // 12
			"/Lost/Lost S01E01.mp4", // 13
			"/Suits/S01/Suits S01E01.mkv", // 14
			"/How I Met Your Mother/S01/How I Met Your Mother S01E01.mkv", // 15
			"/2 Broke Girls/Season 1/2 Broke Girls - S01E01 - Reboot in Brooklyn.avi", // 16
			"/Breaking Bad/Season 1/Breaking.Bad.S01E01.Pilot.avi", // 17
			"/Californication/Season 1/Californication S01E01 Eine verhaengnisvolle Affaere.avi", // 18
			"/TV Shows/Game of Thrones (2011)/Season 1/S01E01 - Winter is Coming.mkv", // 19
			"/TV Shows/The IT Crowd (2006)/Season 3/S03E05 - Friendface.avi", // 20
			"/TV Shows/Battlestar Galactica (2003)/Season 0/S00E03 - The Story So Far.mkv", // 21
			"/Serien/Andromeda/Staffel 1/Andromeda - 1x08 - Reise in die Vergangenheit - 2000-11-20 - Sci-Fi.avi", // 22
			"/Serien/Falling Skies/Staffel 1/Falling Skies - 1x05 - Die Rettung - 2011-07-10 - Sci-Fi-Serie.avi", // 23
			"/Series/The.Big.Bang.Theory/Season 2/The Big Bang Theory - 201 - The Bad Fish Paradigm.avi", // 24
			"/House MD Season 1,2,3,4,5,6,7 + Extras (Deleted Scenes etc) DVDR/Season 1/House MD Season 1 Episode 01 - Pilot.avi", // 25
			"/Breaking Bad Season 1, 2, 3 & 4 + Extras DVDRip HDTV TSV/Season 1/Breaking Bad Season 1 Episode 01 - Pilot.avi", // 26
			"/Serien/Damages/Damages-s03e01.avi", // 27
			"/Mad Men/Season 6/Mad Men - 06x10 - A Tale Of Two Cities.mkv", // 28
			"/Curb Your Enthusiasm/Season 1/Curb Your Enthusiasm Season 1 Episode 04 - The Bracelet.mkv", // 29
			"/World War II in HD/Season 1/World War II in HD 01 Darkness Falls.mkv", // 30
			"/Welcome.To.India/Season 1/Welcome.To.India.S01E01.HDTV.XviD-AFG.mkv", // 31
			"/The.British.Empire.in.Colour/Season1 /The British Empire in Colour Part 1.mkv", // 32
			"/The.Bletchley.Circle/Season 1/the_bletchley_circle.S01e01.hdtv_x264-fov.mkv", // 33
			"/Louie/Season1/louie.s01e01.720p.hdtv.x264-ctu.mkv", // 34
			"/I.Claudius,Season 1/I.Claudius.E01.A.Touch.Of.Murder-ZOXX.mkv", // 35
			"/The.Inspector.Montalbano/Season 1/01_The Snack Thief.avi", // 36
			"/Hatfields.and.McCoys/Season 1/Hatfields_and_McCoys_Part_1_2012_720p.mkv", // 37
			"/Boss (2011)/Season 1/Boss.S01E01.720p.BluRay.X264-CLUE.mkv", // 38
			"/Appropriate.Adult/appropriate.adult.part01.720p.hdtv.x264-bia.mkv", // 39
			"/The.Corner/Season 1/The Corner - Episode 1 -Gary's Blues.avi", // 40
			"/Suits/Season 2/Suits - S02E01 - She Knows.mp4", // 41
			"/Series/Touch/Touch Season 01/Touch.S01E02.720p.HDTV.X264-DIMENSION.mkv", // 42
			"/Series/How I Met Your Mother/How I Met Your Mother Season 08/How.I.Met.Your.Mother.S08E01.720p.HDTV.x264-DIMENSION.mkv", // 43
			"/Doctor.Who.2005.S07.720p.BluRay.DTS.x264/trips-doctor.who.s07e06.mkv", // 44
			"/Doctor.Who.2005.S07.720p.BluRay.DTS.x264/doctor.who.2005.s07e01.720p.bluray.x264-bia.mkv", // 45
			"/Skins.S01.COMPLETE.ENGLISH.HDTVRip.720p.x264-TvR/Skins.S01E01.Tony.ENGLISH.HDTVRip.720p.x264-TvR/tvr-skins-s01e01-720p.mkv", // 46
			"/Talespin V1-2 DVDRip x264-panos/1/1e16-Her Chance to Dream.mkv", // 47
			"/Secret Files of the Spy dogs/Secret Files of the Spy Dogs - 1x06 - Small - Water (DVDRip) (s-mouche) [toonslive].mkv", // 48
			"/Serier/The Fresh Prince of Bel-Air/S01/The Fresh Prince of Bel-Air - 101 - The Fresh Prince Project.avi", // 49
			"/TV/It's Always Sunny in Philadelphia/Season 2/01 - Charlie Gets Crippled.mkv", // 50
			"/Serier/Doctor Who (2005)/Doctor Who (2005) - S01E02 - The End of the World.avi", // 51
			"/TV/Breaking Bad/Season 5/5x03 - Hazard Pay.mkv", // 52
			"/Serier/The Big Bang Theory/Season 3/Episode 5/TBBT S02E01.mkv", // 53
			"/Looney Tunes/Season 1940/Looney Tunes.s1940e07.m4v", // 54
			"/Looney Tunes/Looney Tunes Season 1960/Looney Tunes S1960E05 episodename.avi" // 55
	};
	
	private DecryptedShowEpisode[] d;
	
	public TestShowFileNames() {
		d = new DecryptedShowEpisode[TEST.length];
		for (int i = 0; i < TEST.length; i++) {
			d[i] = MizLib.decryptEpisode(TEST[i], "");
		}
	}
	
	public void testDecryptedFilenameYears() {
		Assert.assertEquals("", d[0].getFileNameYear());
		Assert.assertEquals("", d[1].getFileNameYear());
		Assert.assertEquals("", d[2].getFileNameYear());
		Assert.assertEquals("", d[3].getFileNameYear());
		Assert.assertEquals("", d[4].getFileNameYear());
		Assert.assertEquals("", d[5].getFileNameYear());
		Assert.assertEquals("", d[6].getFileNameYear());
		Assert.assertEquals("", d[7].getFileNameYear());
		Assert.assertEquals("", d[8].getFileNameYear());
		Assert.assertEquals("", d[9].getFileNameYear());
		Assert.assertEquals("2005", d[10].getFileNameYear());
		Assert.assertEquals("", d[11].getFileNameYear());
		Assert.assertEquals("", d[12].getFileNameYear());
		Assert.assertEquals("", d[13].getFileNameYear());
		Assert.assertEquals("", d[14].getFileNameYear());
		Assert.assertEquals("", d[15].getFileNameYear());
		Assert.assertEquals("", d[16].getFileNameYear());
		Assert.assertEquals("", d[17].getFileNameYear());
		Assert.assertEquals("", d[18].getFileNameYear());
		Assert.assertEquals("", d[19].getFileNameYear());
		Assert.assertEquals("", d[20].getFileNameYear());
		Assert.assertEquals("", d[21].getFileNameYear());
		Assert.assertEquals("2000", d[22].getFileNameYear());
		Assert.assertEquals("2011", d[23].getFileNameYear());
		Assert.assertEquals("", d[24].getFileNameYear());
		Assert.assertEquals("", d[25].getFileNameYear());
		Assert.assertEquals("", d[26].getFileNameYear());
		Assert.assertEquals("", d[27].getFileNameYear());
		Assert.assertEquals("", d[28].getFileNameYear());
		Assert.assertEquals("", d[29].getFileNameYear());
		Assert.assertEquals("", d[30].getFileNameYear());
		Assert.assertEquals("", d[31].getFileNameYear());
		Assert.assertEquals("", d[32].getFileNameYear());
		Assert.assertEquals("", d[33].getFileNameYear());
		Assert.assertEquals("", d[34].getFileNameYear());
		Assert.assertEquals("", d[35].getFileNameYear());
		Assert.assertEquals("", d[36].getFileNameYear());
		Assert.assertEquals("2012", d[37].getFileNameYear());
		Assert.assertEquals("", d[38].getFileNameYear());
		Assert.assertEquals("", d[39].getFileNameYear());
		Assert.assertEquals("", d[40].getFileNameYear());
		Assert.assertEquals("", d[41].getFileNameYear());
		Assert.assertEquals("", d[42].getFileNameYear());
		Assert.assertEquals("", d[43].getFileNameYear());
		Assert.assertEquals("", d[44].getFileNameYear());
		Assert.assertEquals("2005", d[45].getFileNameYear());
		Assert.assertEquals("", d[46].getFileNameYear());
		Assert.assertEquals("", d[47].getFileNameYear());
		Assert.assertEquals("", d[48].getFileNameYear());
		Assert.assertEquals("", d[49].getFileNameYear());
		Assert.assertEquals("", d[50].getFileNameYear());
		Assert.assertEquals("2005", d[51].getFileNameYear());
		Assert.assertEquals("", d[52].getFileNameYear());
		//Assert.assertEquals("", d[53].getFileNameYear());
		Assert.assertEquals("1940", d[54].getFileNameYear());
		Assert.assertEquals("1960", d[55].getFileNameYear());
	}
	
	public void testDecryptedFilenameEpisodes() {
		Assert.assertEquals(1, d[0].getEpisode());
		Assert.assertEquals(1, d[1].getEpisode());
		Assert.assertEquals(2, d[2].getEpisode());
		Assert.assertEquals(1, d[3].getEpisode());
		Assert.assertEquals(2, d[4].getEpisode());
		Assert.assertEquals(11, d[5].getEpisode());
		Assert.assertEquals(6, d[6].getEpisode());
		Assert.assertEquals(1, d[7].getEpisode());
		Assert.assertEquals(9, d[8].getEpisode());
		Assert.assertEquals(0, d[9].getEpisode());
		Assert.assertEquals(1, d[10].getEpisode());
		Assert.assertEquals(9, d[11].getEpisode());
		Assert.assertEquals(1, d[12].getEpisode());
		Assert.assertEquals(1, d[13].getEpisode());
		Assert.assertEquals(1, d[14].getEpisode());
		Assert.assertEquals(1, d[15].getEpisode());
		Assert.assertEquals(1, d[16].getEpisode());
		Assert.assertEquals(1, d[17].getEpisode());
		Assert.assertEquals(1, d[18].getEpisode());
		Assert.assertEquals(1, d[19].getEpisode());
		Assert.assertEquals(5, d[20].getEpisode());
		Assert.assertEquals(3, d[21].getEpisode());
		Assert.assertEquals(8, d[22].getEpisode());
		Assert.assertEquals(5, d[23].getEpisode());
		Assert.assertEquals(1, d[24].getEpisode());
		Assert.assertEquals(1, d[25].getEpisode());
		Assert.assertEquals(1, d[26].getEpisode());
		Assert.assertEquals(1, d[27].getEpisode());
		Assert.assertEquals(10, d[28].getEpisode());
		Assert.assertEquals(4, d[29].getEpisode());
		Assert.assertEquals(1, d[30].getEpisode());
		Assert.assertEquals(1, d[31].getEpisode());
		Assert.assertEquals(1, d[32].getEpisode());
		Assert.assertEquals(1, d[33].getEpisode());
		Assert.assertEquals(1, d[34].getEpisode());
		Assert.assertEquals(1, d[35].getEpisode());
		Assert.assertEquals(1, d[36].getEpisode());
		Assert.assertEquals(1, d[37].getEpisode());
		Assert.assertEquals(1, d[38].getEpisode());
		Assert.assertEquals(1, d[39].getEpisode());
		Assert.assertEquals(1, d[40].getEpisode());
		Assert.assertEquals(1, d[41].getEpisode());
		Assert.assertEquals(2, d[42].getEpisode());
		Assert.assertEquals(1, d[43].getEpisode());
		Assert.assertEquals(6, d[44].getEpisode());
		Assert.assertEquals(1, d[45].getEpisode());
		Assert.assertEquals(1, d[46].getEpisode());
		Assert.assertEquals(16, d[47].getEpisode());
		Assert.assertEquals(6, d[48].getEpisode());
		Assert.assertEquals(1, d[49].getEpisode());
		Assert.assertEquals(1, d[50].getEpisode());
		Assert.assertEquals(2, d[51].getEpisode());
		Assert.assertEquals(3, d[52].getEpisode());
		//Assert.assertEquals(5, d[53].getEpisode());
		Assert.assertEquals(7, d[54].getEpisode());
		Assert.assertEquals(5, d[55].getEpisode());
	}
	
	public void testDecryptedFilenameSeasons() {
		Assert.assertEquals(1, d[0].getSeason());
		Assert.assertEquals(1, d[1].getSeason());
		Assert.assertEquals(2, d[2].getSeason());
		Assert.assertEquals(1, d[3].getSeason());
		Assert.assertEquals(4, d[4].getSeason());
		Assert.assertEquals(1, d[5].getSeason());
		Assert.assertEquals(3, d[6].getSeason());
		Assert.assertEquals(0, d[7].getSeason());
		Assert.assertEquals(1, d[8].getSeason());
		Assert.assertEquals(3, d[9].getSeason());
		Assert.assertEquals(1, d[10].getSeason());
		Assert.assertEquals(2, d[11].getSeason());
		Assert.assertEquals(1, d[12].getSeason());
		Assert.assertEquals(1, d[13].getSeason());
		Assert.assertEquals(1, d[14].getSeason());
		Assert.assertEquals(1, d[15].getSeason());
		Assert.assertEquals(1, d[16].getSeason());
		Assert.assertEquals(1, d[17].getSeason());
		Assert.assertEquals(1, d[18].getSeason());
		Assert.assertEquals(1, d[19].getSeason());
		Assert.assertEquals(3, d[20].getSeason());
		Assert.assertEquals(0, d[21].getSeason());
		Assert.assertEquals(1, d[22].getSeason());
		Assert.assertEquals(1, d[23].getSeason());
		Assert.assertEquals(2, d[24].getSeason());
		Assert.assertEquals(1, d[25].getSeason());
		Assert.assertEquals(1, d[26].getSeason());
		Assert.assertEquals(3, d[27].getSeason());
		Assert.assertEquals(6, d[28].getSeason());
		Assert.assertEquals(1, d[29].getSeason());
		Assert.assertEquals(1, d[30].getSeason());
		Assert.assertEquals(1, d[31].getSeason());
		Assert.assertEquals(1, d[32].getSeason());
		Assert.assertEquals(1, d[33].getSeason());
		Assert.assertEquals(1, d[34].getSeason());
		Assert.assertEquals(1, d[35].getSeason());
		Assert.assertEquals(1, d[36].getSeason());
		Assert.assertEquals(1, d[37].getSeason());
		Assert.assertEquals(1, d[38].getSeason());
		Assert.assertEquals(0, d[39].getSeason());
		Assert.assertEquals(1, d[40].getSeason());
		Assert.assertEquals(2, d[41].getSeason());
		Assert.assertEquals(1, d[42].getSeason());
		Assert.assertEquals(8, d[43].getSeason());
		Assert.assertEquals(7, d[44].getSeason());
		Assert.assertEquals(7, d[45].getSeason());
		Assert.assertEquals(1, d[46].getSeason());
		Assert.assertEquals(1, d[47].getSeason());
		Assert.assertEquals(1, d[48].getSeason());
		Assert.assertEquals(1, d[49].getSeason());
		Assert.assertEquals(2, d[50].getSeason());
		Assert.assertEquals(1, d[51].getSeason());
		Assert.assertEquals(5, d[52].getSeason());
		//Assert.assertEquals(3, d[53].getSeason());
		Assert.assertEquals(1940, d[54].getSeason());
		Assert.assertEquals(1960, d[55].getSeason());
	}
	
	public void testDecryptedFilenames() {
		Assert.assertEquals("HIMYM", d[0].getDecryptedFileName());
		Assert.assertEquals("Arrow", d[1].getDecryptedFileName());
		Assert.assertEquals("An Idiot Abroad", d[2].getDecryptedFileName());
		Assert.assertEquals("Borgen", d[3].getDecryptedFileName());
		Assert.assertEquals("Californication", d[4].getDecryptedFileName());
		Assert.assertEquals("How I Met Your Mother", d[5].getDecryptedFileName());
		Assert.assertEquals("Lost", d[6].getDecryptedFileName());
		Assert.assertEquals("The Big Bang Theory", d[7].getDecryptedFileName());
		Assert.assertEquals("The Fresh Prince Of BelAir", d[8].getDecryptedFileName());
		Assert.assertEquals("The Inbetweeners", d[9].getDecryptedFileName());
		Assert.assertEquals("American Dad", d[10].getDecryptedFileName());
		Assert.assertEquals("The Office", d[11].getDecryptedFileName());
		Assert.assertEquals("How I Met Your Mother", d[12].getDecryptedFileName());
		Assert.assertEquals("Lost", d[13].getDecryptedFileName());
		Assert.assertEquals("Suits", d[14].getDecryptedFileName());
		Assert.assertEquals("How I Met Your Mother", d[15].getDecryptedFileName());
		Assert.assertEquals("2 Broke Girls", d[16].getDecryptedFileName());
		Assert.assertEquals("Breaking Bad", d[17].getDecryptedFileName());
		Assert.assertEquals("Californication", d[18].getDecryptedFileName());
		Assert.assertEquals("Game Of Thrones", d[19].getDecryptedFileName());
		Assert.assertEquals("The IT Crowd", d[20].getDecryptedFileName());
		Assert.assertEquals("Battlestar Galactica", d[21].getDecryptedFileName());
		Assert.assertEquals("Andromeda", d[22].getDecryptedFileName());
		Assert.assertEquals("Falling Skies", d[23].getDecryptedFileName());
		Assert.assertEquals("The Big Bang Theory", d[24].getDecryptedFileName());
		Assert.assertEquals("House MD", d[25].getDecryptedFileName());
		Assert.assertEquals("Breaking Bad", d[26].getDecryptedFileName());
		Assert.assertEquals("Damages", d[27].getDecryptedFileName());
		Assert.assertEquals("Mad Men", d[28].getDecryptedFileName());
		Assert.assertEquals("Curb Your Enthusiasm", d[29].getDecryptedFileName());
		Assert.assertEquals("World War II In HD", d[30].getDecryptedFileName());
		Assert.assertEquals("Welcome To India", d[31].getDecryptedFileName());
		Assert.assertEquals("The British Empire In Colour", d[32].getDecryptedFileName());
		Assert.assertEquals("The Bletchley Circle", d[33].getDecryptedFileName());
		Assert.assertEquals("Louie", d[34].getDecryptedFileName());
		Assert.assertEquals("I Claudius", d[35].getDecryptedFileName());
		Assert.assertEquals("The Inspector Montalbano", d[36].getDecryptedFileName());
		Assert.assertEquals("Hatfields And McCoys", d[37].getDecryptedFileName());
		Assert.assertEquals("Boss", d[38].getDecryptedFileName());
		Assert.assertEquals("Appropriate Adult", d[39].getDecryptedFileName());
		Assert.assertEquals("The Corner", d[40].getDecryptedFileName());
		Assert.assertEquals("Suits", d[41].getDecryptedFileName());
		Assert.assertEquals("Touch", d[42].getDecryptedFileName());
		Assert.assertEquals("How I Met Your Mother", d[43].getDecryptedFileName());
		Assert.assertEquals("Tripsdoctor Who", d[44].getDecryptedFileName());
		Assert.assertEquals("Doctor Who", d[45].getDecryptedFileName());
		Assert.assertEquals("Skins", d[46].getDecryptedFileName());
		Assert.assertEquals("Talespin V12 Panos", d[47].getDecryptedFileName());
		Assert.assertEquals("Secret Files Of The Spy Dogs", d[48].getDecryptedFileName());
		Assert.assertEquals("The Fresh Prince Of BelAir", d[49].getDecryptedFileName());
		Assert.assertEquals("Doctor Who", d[51].getDecryptedFileName());
		Assert.assertEquals("Breaking Bad", d[52].getDecryptedFileName());
		//Assert.assertEquals("TBBT", d[53].getDecryptedFileName());
		Assert.assertEquals("Looney Tunes", d[54].getDecryptedFileName());
		Assert.assertEquals("Looney Tunes", d[55].getDecryptedFileName());
	}
	
	public void testDecryptedParentNames() {
		Assert.assertEquals("", d[0].getDecryptedParentName());
		Assert.assertEquals("", d[1].getDecryptedParentName());
		Assert.assertEquals("", d[2].getDecryptedParentName());
		Assert.assertEquals("", d[3].getDecryptedParentName());
		Assert.assertEquals("", d[4].getDecryptedParentName());
		Assert.assertEquals("", d[5].getDecryptedParentName());
		Assert.assertEquals("", d[6].getDecryptedParentName());
		Assert.assertEquals("", d[7].getDecryptedParentName());
		Assert.assertEquals("", d[8].getDecryptedParentName());
		Assert.assertEquals("", d[9].getDecryptedParentName());
		Assert.assertEquals("", d[10].getDecryptedParentName());
		Assert.assertEquals("", d[11].getDecryptedParentName());
		Assert.assertEquals("", d[12].getDecryptedParentName());
		Assert.assertEquals("Lost", d[13].getDecryptedParentName());
		Assert.assertEquals("Suits", d[14].getDecryptedParentName());
		Assert.assertEquals("How I Met Your Mother", d[15].getDecryptedParentName());
		Assert.assertEquals("2 Broke Girls", d[16].getDecryptedParentName());
		Assert.assertEquals("Breaking Bad", d[17].getDecryptedParentName());
		Assert.assertEquals("Californication", d[18].getDecryptedParentName());
		Assert.assertEquals("Game Of Thrones", d[19].getDecryptedParentName());
		Assert.assertEquals("The IT Crowd", d[20].getDecryptedParentName());
		Assert.assertEquals("Battlestar Galactica", d[21].getDecryptedParentName());
		Assert.assertEquals("Andromeda", d[22].getDecryptedParentName());
		Assert.assertEquals("Falling Skies", d[23].getDecryptedParentName());
		Assert.assertEquals("The Big Bang Theory", d[24].getDecryptedParentName());
		Assert.assertEquals("House MD", d[25].getDecryptedParentName());
		Assert.assertEquals("Breaking Bad", d[26].getDecryptedParentName());
		Assert.assertEquals("Damages", d[27].getDecryptedParentName());
		Assert.assertEquals("Mad Men", d[28].getDecryptedParentName());
		Assert.assertEquals("Curb Your Enthusiasm", d[29].getDecryptedParentName());
		Assert.assertEquals("World War II In HD", d[30].getDecryptedParentName());
		Assert.assertEquals("Welcome To India", d[31].getDecryptedParentName());
		Assert.assertEquals("The British Empire In Colour", d[32].getDecryptedParentName());
		Assert.assertEquals("The Bletchley Circle", d[33].getDecryptedParentName());
		Assert.assertEquals("Louie", d[34].getDecryptedParentName());
		Assert.assertEquals("I Claudius", d[35].getDecryptedParentName());
		Assert.assertEquals("The Inspector Montalbano", d[36].getDecryptedParentName());
		Assert.assertEquals("Hatfields And McCoys", d[37].getDecryptedParentName());
		Assert.assertEquals("Boss", d[38].getDecryptedParentName());
		Assert.assertEquals("Appropriate Adult", d[39].getDecryptedParentName());
		Assert.assertEquals("The Corner", d[40].getDecryptedParentName());
		Assert.assertEquals("Suits", d[41].getDecryptedParentName());
		Assert.assertEquals("Touch", d[42].getDecryptedParentName());
		Assert.assertEquals("How I Met Your Mother", d[43].getDecryptedParentName());
		Assert.assertEquals("Doctor Who", d[44].getDecryptedParentName());
		Assert.assertEquals("Doctor Who", d[45].getDecryptedParentName());
		Assert.assertEquals("Skins", d[46].getDecryptedParentName());
		Assert.assertEquals("Talespin V12 Panos", d[47].getDecryptedParentName());
		Assert.assertEquals("Secret Files Of The Spy Dogs", d[48].getDecryptedParentName());
		Assert.assertEquals("The Fresh Prince Of BelAir", d[49].getDecryptedParentName());
		Assert.assertEquals("Its Always Sunny In Philadelphia", d[50].getDecryptedParentName());
		Assert.assertEquals("Doctor Who", d[51].getDecryptedParentName());
		Assert.assertEquals("Breaking Bad", d[52].getDecryptedParentName());
		//Assert.assertEquals("The Big Bang Theory", d[53].getDecryptedParentName());
		Assert.assertEquals("Looney Tunes", d[54].getDecryptedParentName());
		Assert.assertEquals("Looney Tunes", d[55].getDecryptedParentName());
	}
	
	public void testDecryptedParentNameYears() {
		Assert.assertEquals("", d[0].getParentNameYear());
		Assert.assertEquals("", d[1].getParentNameYear());
		Assert.assertEquals("", d[2].getParentNameYear());
		Assert.assertEquals("", d[3].getParentNameYear());
		Assert.assertEquals("", d[4].getParentNameYear());
		Assert.assertEquals("", d[5].getParentNameYear());
		Assert.assertEquals("", d[6].getParentNameYear());
		Assert.assertEquals("", d[7].getParentNameYear());
		Assert.assertEquals("", d[8].getParentNameYear());
		Assert.assertEquals("", d[9].getParentNameYear());
		Assert.assertEquals("", d[10].getParentNameYear());
		Assert.assertEquals("", d[11].getParentNameYear());
		Assert.assertEquals("", d[12].getParentNameYear());
		Assert.assertEquals("", d[13].getParentNameYear());
		Assert.assertEquals("", d[14].getParentNameYear());
		Assert.assertEquals("", d[15].getParentNameYear());
		Assert.assertEquals("", d[16].getParentNameYear());
		Assert.assertEquals("", d[17].getParentNameYear());
		Assert.assertEquals("", d[18].getParentNameYear());
		Assert.assertEquals("2011", d[19].getParentNameYear());
		Assert.assertEquals("2006", d[20].getParentNameYear());
		Assert.assertEquals("2003", d[21].getParentNameYear());
		Assert.assertEquals("", d[22].getParentNameYear());
		Assert.assertEquals("", d[23].getParentNameYear());
		Assert.assertEquals("", d[24].getParentNameYear());
		Assert.assertEquals("", d[25].getParentNameYear());
		Assert.assertEquals("", d[26].getParentNameYear());
		Assert.assertEquals("", d[27].getParentNameYear());
		Assert.assertEquals("", d[28].getParentNameYear());
		Assert.assertEquals("", d[29].getParentNameYear());
		Assert.assertEquals("", d[30].getParentNameYear());
		Assert.assertEquals("", d[31].getParentNameYear());
		Assert.assertEquals("", d[32].getParentNameYear());
		Assert.assertEquals("", d[33].getParentNameYear());
		Assert.assertEquals("", d[34].getParentNameYear());
		Assert.assertEquals("", d[35].getParentNameYear());
		Assert.assertEquals("", d[36].getParentNameYear());
		Assert.assertEquals("", d[37].getParentNameYear());
		Assert.assertEquals("2011", d[38].getParentNameYear());
		Assert.assertEquals("", d[39].getParentNameYear());
		Assert.assertEquals("", d[40].getParentNameYear());
		Assert.assertEquals("", d[41].getParentNameYear());
		Assert.assertEquals("", d[42].getParentNameYear());
		Assert.assertEquals("", d[43].getParentNameYear());
		Assert.assertEquals("2005", d[44].getParentNameYear());
		Assert.assertEquals("2005", d[45].getParentNameYear());
		Assert.assertEquals("", d[46].getParentNameYear());
		Assert.assertEquals("", d[47].getParentNameYear());
		Assert.assertEquals("", d[48].getParentNameYear());
		Assert.assertEquals("", d[49].getParentNameYear());
		Assert.assertEquals("", d[50].getParentNameYear());
		Assert.assertEquals("2005", d[51].getParentNameYear());
		Assert.assertEquals("", d[52].getParentNameYear());
		//Assert.assertEquals("", d[53].getParentNameYear());
		Assert.assertEquals("", d[54].getParentNameYear());
		Assert.assertEquals("", d[55].getParentNameYear());
	}
}