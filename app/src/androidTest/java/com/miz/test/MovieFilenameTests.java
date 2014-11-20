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

import com.miz.identification.MovieStructure;

public class MovieFilenameTests extends AndroidTestCase {

	public void testCase1() {
		MovieStructure ms = new MovieStructure("/Avatar (2009).mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Avatar (2009).mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Avatar", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2009, ms.getReleaseYear());
	}
	
	public void testCase2() {
		MovieStructure ms = new MovieStructure("/Avatar (2009) (tt0499549).mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Avatar (2009) (tt0499549).mkv", ms.getFilename());
		assertEquals(true, ms.hasImdbId());
		assertEquals("tt0499549", ms.getImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Avatar", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2009, ms.getReleaseYear());
	}
	
	public void testCase3() {
		MovieStructure ms = new MovieStructure("/The Thing 2011.mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("The Thing 2011.mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("The Thing", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2011, ms.getReleaseYear());
	}
	
	public void testCase4() {
		MovieStructure ms = new MovieStructure("/Green Lantern.mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Green Lantern.mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Green Lantern", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}
	
	public void testCase5() {
		MovieStructure ms = new MovieStructure("/Shaun of the Dead/Shaun of the Dead.mp4");
		assertEquals("Shaun of the Dead", ms.getParentFolderName());
		assertEquals("Shaun of the Dead.mp4", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("Shaun of the Dead", ms.getDecryptedParentFolderName());
		assertEquals("Shaun of the Dead", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}
	
	public void testCase6() {
		MovieStructure ms = new MovieStructure("/Movies/Fight Club (1999)/Fight Club (1999).mkv");
		assertEquals("Fight Club (1999)", ms.getParentFolderName());
		assertEquals("Fight Club (1999).mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("Fight Club", ms.getDecryptedParentFolderName());
		assertEquals("Fight Club", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(1999, ms.getReleaseYear());
	}
	
	public void testCase7() {
		MovieStructure ms = new MovieStructure("/Movies/Fight Club (1995)/Fight Club (1999).mkv");
		assertEquals("Fight Club (1995)", ms.getParentFolderName());
		assertEquals("Fight Club (1999).mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("Fight Club", ms.getDecryptedParentFolderName());
		assertEquals("Fight Club", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(1999, ms.getReleaseYear()); // Prioritize filename year
	}
	
	public void testCase8() {
		MovieStructure ms = new MovieStructure("/Movies/Fight Club (1999)/Fight Club.mkv");
		assertEquals("Fight Club (1999)", ms.getParentFolderName());
		assertEquals("Fight Club.mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("Fight Club", ms.getDecryptedParentFolderName());
		assertEquals("Fight Club", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(1999, ms.getReleaseYear()); // Get year from parent folder name
	}
	
	public void testCase9() {
		MovieStructure ms = new MovieStructure("/movies/Jack.the.Giant.Slayer.2013.1080p.BluRay.x264-SPARKS [PublicHD].mkv");
		assertEquals("movies", ms.getParentFolderName());
		assertEquals("Jack.the.Giant.Slayer.2013.1080p.BluRay.x264-SPARKS [PublicHD].mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("movies", ms.getDecryptedParentFolderName());
		assertEquals("Jack the Giant Slayer", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2013, ms.getReleaseYear());
	}
	
	public void testCase10() {
		MovieStructure ms = new MovieStructure("/AE, Apocalypse Earth.avi");
		assertEquals("", ms.getParentFolderName());
		assertEquals("AE, Apocalypse Earth.avi", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("AE Apocalypse Earth", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}
	
	public void testCase11() {
		MovieStructure ms = new MovieStructure("/Matando Cabos - DVDRIP - CD1.avi");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Matando Cabos - DVDRIP - CD1.avi", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Matando Cabos", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}

	public void testCase12() {
		MovieStructure ms = new MovieStructure("/Matando Cabos - DVDRIP - CD2.avi");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Matando Cabos - DVDRIP - CD2.avi", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Matando Cabos", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}
	
	public void testCase13() {
		MovieStructure ms = new MovieStructure("/Cenizas del  cielo (DVDRip) (EliteTorrent.net).avi");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Cenizas del  cielo (DVDRip) (EliteTorrent.net).avi", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Cenizas del cielo", ms.getDecryptedFilename());
		assertEquals(false, ms.hasReleaseYear());
	}
	
	public void testCase14() {
		MovieStructure ms = new MovieStructure("/Prometheus [BRrip][AC3 5.1 Espanol Castellano][2012][www.newpct.com].avi");
		assertEquals("", ms.getParentFolderName());
		assertEquals("Prometheus [BRrip][AC3 5.1 Espanol Castellano][2012][www.newpct.com].avi", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("Prometheus", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2012, ms.getReleaseYear());
	}
	
	public void testCase15() {
		MovieStructure ms = new MovieStructure("/A.Day.at.the.Races.1937/A.Day.at.the.Races.1937.720p.WEB-DL.AAC2.0.H.264-HDStar.mkv");
		assertEquals("A.Day.at.the.Races.1937", ms.getParentFolderName());
		assertEquals("A.Day.at.the.Races.1937.720p.WEB-DL.AAC2.0.H.264-HDStar.mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("A Day at the Races", ms.getDecryptedParentFolderName());
		assertEquals("A Day at the Races", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(1937, ms.getReleaseYear());
	}
	
	public void testCase16() {
		MovieStructure ms = new MovieStructure("/2012 (2009).mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("2012 (2009).mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("2012", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2009, ms.getReleaseYear());
	}
	
	public void testCase17() {
		MovieStructure ms = new MovieStructure("/G.I. Joe Retaliation (2013).mkv");
		assertEquals("", ms.getParentFolderName());
		assertEquals("G.I. Joe Retaliation (2013).mkv", ms.getFilename());
		assertEquals(false, ms.hasImdbId());
		assertEquals("", ms.getDecryptedParentFolderName());
		assertEquals("GI Joe Retaliation", ms.getDecryptedFilename());
		assertEquals(true, ms.hasReleaseYear());
		assertEquals(2013, ms.getReleaseYear());
	}
}