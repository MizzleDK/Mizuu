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

import com.miz.functions.DecryptedMovie;
import com.miz.functions.MizLib;

import android.test.AndroidTestCase;

public class TestMovieFileNames extends AndroidTestCase {
	
	private final String[] TEST_FILENAMES = new String[]{
			"/Avatar (2009).mkv", // 0
			"/Star Trek (2009).mp4", // 1
			"/The Thing 2011.mkv", // 2
			"/Green Lantern.mkv", // 3
			"/Shaun of the Dead/Shaun of the Dead.mp4", // 4
			"/Frontiers/Frontiers.mkv", // 5
			"/Movies/Wargames (1983)/Wargames (1983).mkv", // 6
			"/Movies/Fight Club (1999)/Fight Club (1999).mkv", // 7
			"/Movies/Hackers (1995)/Hackers (1995).mkv", // 8
			"/Abenteuer/Asterix und Obelix - Mission Kleopatra - 2002 - Abenteuer.avi", // 9
			"/Filme/Animiert/Drachenz�hmen leicht gemacht - 2010 - Animation.avi", // 10
			"/movies/Jack.the.Giant.Slayer.2013.1080p.BluRay.x264-SPARKS [PublicHD].mkv", // 11
			"/AE, Apocalypse Earth.avi", // 12
			"/Amerrika DVDSCR.avi", // 13
			"/Atrapa.El.Fuego.DVDRIP.avi", // 14
			"/Blade Runner The Final Cut DVDRIP.avi", // 15
			"/En Un Mundo Mejor BRSCR - 2011.avi", // 16
			"/Matando Cabos - DVDRIP - CD1.avi", // 17
			"/Matando Cabos - DVDRIP - CD2.avi", // 18
			"/[SET Star Wars] Star Wars I - La Amenaza Fantasma DVDRIP.avi", // 19
			"/[SET Star Wars] Star Wars II - El Ataque De Los Clones DVDRIP.avi", // 20
			"/[SET Star Wars] Star Wars III - La Venganza De Los Shit DVDRiP.avi", // 21
			"/360 Juego de destinos - BRScreener.avi", // 22
			"/Cumbres borrascosas (HDRip) (EliteTorrent.net).avi", // 23
			"/Cenizas del  cielo (DVDRip) (EliteTorrent.net).avi", // 24
			"/Hansel y Gretel (V.Ext) (BR-Screener) (Elitetorrent.net).avi", // 25
			"/El peso del agua (DVDRip) (EliteTorrent.net).avi", // 26
			"/Iron Man 3 (BR-Screener Line) (EliteTorrent.net).avi", // 27
			"/Albert Nobbs [DVD Screener][Spanish][2012].avi", // 28
			"/Algo Prestado [DVDScreener][Spanish][2011].avi", // 29
			"/Amenazados [DVDRIP][Spanish AC3 5.1][2011][www.pctestrenos.com].avi", // 30
			"/Babycall [BluRay Screener][Espa�olCastellano][2012][www.newpct.com].avi", // 31
			"/Batman Begins [DVDrip][AC3.5.1 Spanish][www.newpct.com].avi", // 32
			"/Berserk La Edad de Oro I El huevo del rey conquistador [BRrip][AC3 5.1 Espa�ol Castellano][2012][www.newpct.com].avi", // 33
			"/BlackThorn (Sin Destino) [DVDRIP][Spanish AC3 5.1][2011][www.newpct.com].avi", // 34
			"/Bunraku [BluRay Screener][Spanish HQ][2012].avi", // 35
			"/Chocolate [DVDrip][Espa�ol Castellano][2012][www.newpct.com].avi", // 36
			"/El Alucinante Mundo de Norman [BluRay Screener][Espa�ol Castellano SINCRONIZADO][2012][www.newpct.com].avi", // 37
			"/El Caballero Oscuro La Leyenda Renace [DVDRip][AC3 5.1 Espa�ol Castellano][2012][www.newpct.com].avi", // 38
			"/El Ultimo Gran Dia [BluRay RIP][Spanish_English AC3 5.1][www.newpct.com].avi", // 39
			"/Encontraras Dragones [DVDRIP][Spanish AC3 5.1][2011][www.newpct.com].avi", // 40
			"/Gantz 2 [DVDRIP][Spanish AC3 5.1][2011][www.descargaya.es].avi", // 41
			"/Infiltrado [DVDrip][Spanish][newpct.com].avi", // 42
			"/Intruders [DVD Screener][Spanish][2011].avi", // 43
			"/Largo Winch 2 [DVDRIP][Spanish AC3 5.1][2012].avi", // 44
			"/Los Idus De Marzo [ALTA DEFINICION 720p].avi", // 45
			"/Otra Tierra [DVDRIP][Spanish AC3 5.1][2012][www.pctestrenos.com].avi", // 46
			"/Prometheus [BRrip][AC3 5.1 Espa�ol Castellano][2012][www.newpct.com].avi", // 47
			"/Redenci�n (Tyrannosaur) [BluRay Screener][Spaniish][2012].avi", // 48
			"/Somos Marshall [DVDrip][AC3.Spanish][www.newpct.com].avi", // 49
			"/The Amazing Spider-Man (Proper)[BluRayRIP][AC3 5.1 Espa�ol Castellano][2012][www.newpct.com].avi", // 50
			"/Todos los D�as de mi Vida [DVD Screener][Spanish][2012].avi", // 51
			"/Verbo [BluRayRIP][Spanish AC3 5.1][2012][www.newpct.com].avi", // 52
			"/Valor De Ley [BluRay Screener][Spanish][2011].avi", // 53
			"/Avatar DVDRIP.avi", // 54
			"/El Deminio Bajo La Piel BRSCR.avi", // 55
			"/Futurama - El Gran Golpe De Bender - DVDRIP.avi", // 56
			"/la familia savages cd1.avi", // 57
			"/la familia savages cd2.avi", // 58
			"/Los Simpsons La Pelicula - BLURAY.mkv", // 59
			"/Mas Extra�o Que La Ficci�n - BLURAY.mkv", // 60
			"/Mi Ex Mi Novia y Yo - 2011 - DVDRIP.avi", // 61
			"/L_HOMME_QUI_RIT/L_HOMME_QUI_RIT.mp4", // 62
			"/Dr. No.(1962).avi", // 63
			"/Never.Say.Never.Again.(1983).mp4", // 64
			"/Quantum.Of.Solace.(2008).mp4", // 65
			"/The.Assassin.Next.Door.(2009).[Kirot].[ENGSUB].avi", // 66
			"/Battle.Royale.(2000).[Batoru.rowaiaru].[DC].mp4", // 67
			"/C.etait.Un.Rendez.Vous.(1976).avi", // 68
			"/Death.Race.2.(2010).avi", // 69
			"/Die.Hard (1988).mp4", // 70
			"/Die.Hard.3.(1995).mp4", // 71
			"/Kill.Bill.Vol.1.(2003).mp4", // 72
			"/Mission.Impossible.Ghost.Protocol.(2011).mkv", // 73
			"/Kill.Bill.Vol.1.(2003).mp4", // 74
			"/La.Proie.(2011).avi", // 75
			"/S.W.A.T.(2003).avi", // 76
			"/xXx (2002).avi", // 77
			"/eXistenz.(1999).avi", // 78
			"/2001.A.Space.Odyssey.(1968).mp4", // 79
			"/The.Fellowship.of.the.Ring.(2001).[Ext].avi", // 80
			"/The.Sorcerer's.Apprentice.(2010).avi", // 81
			"/Le.Huiti�me.Jour.(1996).avi", // 82
			"/Hysteria...The.Def.Leppard.Story.(2001).[ENGSUB].avi", // 83
			"/Donnie.Darko.(2001).[DC].mp4", // 84
			"/Ne.Le.Dis.A.Personne.(2006).[Tel.No.One].avi", // 85
			"/10.years.2011/10.years.2011.limited.720p.bluray.x264-psychd.mkv", // 86
			"/A.Day.at.the.Races.1937/A.Day.at.the.Races.1937.720p.WEB-DL.AAC2.0.H.264-HDStar.mkv", // 87
			"/a.late.quartet.2012/a.late.quartet.2012.limited.720p.bluray.x264-geckos.mkv", // 88
			"/A.Night.at.the.Opera.1935/A.Night.at.the.Opera.1935.720p.WEB-DL.AAC2.0.H.264-HDStar.mkv", // 89
			"/close.encounters.of.the.third.kind.1977/close.encounters.of.the.third.kind.1977.special.edition.720p.bluray.x264-hdnordic.mkv", // 90
			"/Kapringen.aka.A.Hijacking.2012/a.hijacking.2012.720p.bluray.dts.x264-publichd.mkv", // 91
			"/2012 (2009).mkv", // 92
			"/Home On The Range 2004 1080p DTS/Home.On.The.Range.2004.Blu-ray.1080p.x264.DTS.HD.mkv", // 93
			"/Heat.1995.720p.BluRay.x264-HD/Heat.1995.720p.BluRay.x264-HD.mkv", // 94
			"/Gnomeo.and.Juliet/Gnomeo.and.Juliet.1080p.BluRay.x264-HD.mkv", // 95
			"/Crazy.Stupid.Love.2011.BluRay.720p.DTS.x264-HD/Crazy.Stupid.Love.2011.BluRay.720p.DTS.x264-HD.mkv", // 96
			"/Dead Snow/Dead.Snow.(2009).Blu-Ray.720p.x264.HD.mkv", // 97
			"/And Soon the Darkness 2010 720p Blu-Ray DD51 x264-HD/And Soon the Darkness 2010 720p Blu-Ray DD51 x264-HD.mkv", // 98
			"/Aftershock.2010.BluRay.720P.DTS.2Audio.x264-HD/Aftershock.2010.BluRay.720P.DTS.2Audio.x264-HD.mkv", // 99
			"/Alpha and Omega 2010 720p BluRay x264-HD/Alpha and Omega 2010 720p BluRay x264-HD.mkv", // 100
			"/Battleground 2012 1080p DTS/Battleground.2012.BluRay.1080p.DTS.x264-HD.mkv", // 101
			"/Battle Force 2012 720p/Battle.Force.2012.720p.BluRay.x264.AC3-HD.mkv", // 102
			"/Cloudy with a chance of meatballs.2009.720/Cloudy.With.A.Chance.Of.Meatballs.2009.720p.BluRay.x264.DTS-HD.mkv", // 103
			"/Game.of.Death.2010.Bluray.720p.DTS.x264-HD/Game.of.Death.2010.Bluray.720p.DTS.x264-HD.mkv", // 104
			"/Igor 2008 720p BluRay DTS x264-HD/Igor 2008 720p BluRay DTS x264-HD.mkv", // 105
			"/Room.In.Rome.2010.720p.BluRay.x264.DTS-HD/Room.In.Rome.2010.720p.BluRay.x264.DTS-HD.mkv", // 106
			"/Look.DVDRip.XviD-TFE.avi", // 107
			"/Funny.People.Unrated.2009.DVDRip.x264-DiRTY.mkv", // 108
			"/Frequently Asked Questions About Time Travel 2009 720p HDTV DD5.1 x264-Cache.mkv", // 109
			"/Enter.The.Void.720p.BluRay.x264.mkv", // 110
			"/Test.This.Shit.mkv", // 111
			"./Test", // 112
			"/Hello.Friend/Hello", // 113
			"/Hello.Friend/Hello.Friend", // 114
			"/Hello.Friend/Hello.Friend.mkv", // 115
			"/Movies/Avatar (2009) (tt0499549).mkv", // 116
			"/Test tt0499549.mkv" // 117
	};
	private DecryptedMovie[] d;
	
	public TestMovieFileNames() {
		d = new DecryptedMovie[TEST_FILENAMES.length];
		for (int i = 0; i < TEST_FILENAMES.length; i++)
			d[i] = MizLib.decryptMovie(TEST_FILENAMES[i], "");
	}

	public void testDecryptedFilenames() {
		assertEquals("Avatar", d[0].getDecryptedFileName());
		assertEquals("Star Trek", d[1].getDecryptedFileName());
		assertEquals("The Thing", d[2].getDecryptedFileName());
		assertEquals("Green Lantern", d[3].getDecryptedFileName());
		assertEquals("Shaun of the Dead", d[4].getDecryptedFileName());
		assertEquals("Frontiers", d[5].getDecryptedFileName());
		assertEquals("Wargames", d[6].getDecryptedFileName());
		assertEquals("Fight Club", d[7].getDecryptedFileName());
		assertEquals("Hackers", d[8].getDecryptedFileName());
		assertEquals("Asterix und Obelix Mission Kleopatra", d[9].getDecryptedFileName());
		assertEquals("Drachenz�hmen leicht gemacht", d[10].getDecryptedFileName());
		assertEquals("Jack the Giant Slayer", d[11].getDecryptedFileName());
		assertEquals("AE Apocalypse Earth", d[12].getDecryptedFileName());
		assertEquals("Amerrika", d[13].getDecryptedFileName());
		assertEquals("Atrapa El Fuego", d[14].getDecryptedFileName());
		assertEquals("Blade Runner The Final Cut", d[15].getDecryptedFileName());
		assertEquals("En Un Mundo Mejor", d[16].getDecryptedFileName());
		assertEquals("Matando Cabos", d[17].getDecryptedFileName());
		assertEquals("Matando Cabos", d[18].getDecryptedFileName());
		assertEquals("Star Wars I La Amenaza Fantasma", d[19].getDecryptedFileName());
		assertEquals("Star Wars II El Ataque De Los Clones", d[20].getDecryptedFileName());
		assertEquals("Star Wars III La Venganza De Los Shit", d[21].getDecryptedFileName());
		assertEquals("360 Juego de destinos", d[22].getDecryptedFileName());
		assertEquals("Cumbres borrascosas", d[23].getDecryptedFileName());
		assertEquals("Cenizas del cielo", d[24].getDecryptedFileName());
		assertEquals("Hansel y Gretel", d[25].getDecryptedFileName());
		assertEquals("El peso del agua", d[26].getDecryptedFileName());
		assertEquals("Iron Man 3", d[27].getDecryptedFileName());
		assertEquals("Albert Nobbs", d[28].getDecryptedFileName());
		assertEquals("Algo Prestado", d[29].getDecryptedFileName());
		assertEquals("Amenazados", d[30].getDecryptedFileName());
		assertEquals("Babycall", d[31].getDecryptedFileName());
		assertEquals("Batman Begins", d[32].getDecryptedFileName());
		assertEquals("Berserk La Edad de Oro I El huevo del rey conquistador", d[33].getDecryptedFileName());
		assertEquals("BlackThorn", d[34].getDecryptedFileName());
		assertEquals("Bunraku", d[35].getDecryptedFileName());
		assertEquals("Chocolate", d[36].getDecryptedFileName());
		assertEquals("El Alucinante Mundo de Norman", d[37].getDecryptedFileName());
		assertEquals("El Caballero Oscuro La Leyenda Renace", d[38].getDecryptedFileName());
		assertEquals("El Ultimo Gran Dia", d[39].getDecryptedFileName());
		assertEquals("Encontraras Dragones", d[40].getDecryptedFileName());
		assertEquals("Gantz 2", d[41].getDecryptedFileName());
		assertEquals("Infiltrado", d[42].getDecryptedFileName());
		assertEquals("Intruders", d[43].getDecryptedFileName());
		assertEquals("Largo Winch 2", d[44].getDecryptedFileName());
		assertEquals("Los Idus De Marzo", d[45].getDecryptedFileName());
		assertEquals("Otra Tierra", d[46].getDecryptedFileName());
		assertEquals("Prometheus", d[47].getDecryptedFileName());
		assertEquals("Redenci�n", d[48].getDecryptedFileName());
		assertEquals("Somos Marshall", d[49].getDecryptedFileName());
		assertEquals("The Amazing SpiderMan", d[50].getDecryptedFileName());
		assertEquals("Todos los D�as de mi Vida", d[51].getDecryptedFileName());
		assertEquals("Verbo", d[52].getDecryptedFileName());
		assertEquals("Valor De Ley", d[53].getDecryptedFileName());
		assertEquals("Avatar", d[54].getDecryptedFileName());
		assertEquals("El Deminio Bajo La Piel", d[55].getDecryptedFileName());
		assertEquals("Futurama El Gran Golpe De Bender", d[56].getDecryptedFileName());
		assertEquals("la familia savages", d[57].getDecryptedFileName());
		assertEquals("la familia savages", d[58].getDecryptedFileName());
		assertEquals("Los Simpsons La Pelicula", d[59].getDecryptedFileName());
		assertEquals("Mas Extra�o Que La Ficci�n", d[60].getDecryptedFileName());
		assertEquals("Mi Ex Mi Novia y Yo", d[61].getDecryptedFileName());
		assertEquals("L'HOMME QUI RIT", d[62].getDecryptedFileName());
		assertEquals("Dr No", d[63].getDecryptedFileName());
		assertEquals("Never Say Never Again", d[64].getDecryptedFileName());
		assertEquals("Quantum Of Solace", d[65].getDecryptedFileName());
		assertEquals("The Assassin Next Door", d[66].getDecryptedFileName());
		assertEquals("Battle Royale", d[67].getDecryptedFileName());
		assertEquals("C'etait Un Rendez Vous", d[68].getDecryptedFileName());
		assertEquals("Death Race 2", d[69].getDecryptedFileName());
		assertEquals("Die Hard", d[70].getDecryptedFileName());
		assertEquals("Die Hard 3", d[71].getDecryptedFileName());
		assertEquals("Kill Bill Vol 1", d[72].getDecryptedFileName());
		assertEquals("Mission Impossible Ghost Protocol", d[73].getDecryptedFileName());
		assertEquals("Kill Bill Vol 1", d[74].getDecryptedFileName());
		assertEquals("La Proie", d[75].getDecryptedFileName());
		assertEquals("SWAT", d[76].getDecryptedFileName());
		assertEquals("xXx", d[77].getDecryptedFileName());
		assertEquals("eXistenz", d[78].getDecryptedFileName());
		assertEquals("2001 A Space Odyssey", d[79].getDecryptedFileName());
		assertEquals("The Fellowship of the Ring", d[80].getDecryptedFileName());
		assertEquals("The Sorcerers Apprentice", d[81].getDecryptedFileName());
		assertEquals("Le Huiti�me Jour", d[82].getDecryptedFileName());
		assertEquals("Hysteria The Def Leppard Story", d[83].getDecryptedFileName());
		assertEquals("Donnie Darko", d[84].getDecryptedFileName());
		assertEquals("Ne Le Dis A Personne", d[85].getDecryptedFileName());
		assertEquals("10 years", d[86].getDecryptedFileName());
		assertEquals("A Day at the Races", d[87].getDecryptedFileName());
		assertEquals("a late quartet", d[88].getDecryptedFileName());
		assertEquals("A Night at the Opera", d[89].getDecryptedFileName());
		assertEquals("close encounters of the third kind", d[90].getDecryptedFileName());
		assertEquals("a hijacking", d[91].getDecryptedFileName());
		assertEquals("2012", d[92].getDecryptedFileName());
		assertEquals("Home On The Range", d[93].getDecryptedFileName());
		assertEquals("Heat", d[94].getDecryptedFileName());
		assertEquals("Gnomeo and Juliet HD", d[95].getDecryptedFileName());
		assertEquals("Crazy Stupid Love", d[96].getDecryptedFileName());
		assertEquals("Dead Snow", d[97].getDecryptedFileName());
		assertEquals("And Soon the Darkness", d[98].getDecryptedFileName());
		assertEquals("Aftershock", d[99].getDecryptedFileName());
		assertEquals("Alpha and Omega", d[100].getDecryptedFileName());
		assertEquals("Battleground", d[101].getDecryptedFileName());
		assertEquals("Battle Force", d[102].getDecryptedFileName());
		assertEquals("Cloudy With A Chance Of Meatballs", d[103].getDecryptedFileName());
		assertEquals("Game of Death", d[104].getDecryptedFileName());
		assertEquals("Igor", d[105].getDecryptedFileName());
		assertEquals("Room In Rome", d[106].getDecryptedFileName());
		assertEquals("Look", d[107].getDecryptedFileName());
		assertEquals("Funny People", d[108].getDecryptedFileName());
		assertEquals("Frequently Asked Questions About Time Travel", d[109].getDecryptedFileName());
		assertEquals("Enter The Void", d[110].getDecryptedFileName());
		assertEquals("Test This Shit", d[111].getDecryptedFileName());
		assertEquals("Test", d[112].getDecryptedFileName());
		assertEquals("Hello", d[113].getDecryptedFileName());
		assertEquals("Hello Friend", d[114].getDecryptedFileName());
		assertEquals("Hello Friend", d[115].getDecryptedFileName());
		assertEquals("Avatar", d[116].getDecryptedFileName());
		assertEquals("Test", d[117].getDecryptedFileName());
	}
	
	public void testDecryptedFilenameYears() {
		assertEquals("2009", d[0].getFileNameYear());
		assertEquals("2009", d[1].getFileNameYear());
		assertEquals("2011", d[2].getFileNameYear());
		assertEquals("", d[3].getFileNameYear());
		assertEquals("", d[4].getFileNameYear());
		assertEquals("", d[5].getFileNameYear());
		assertEquals("1983", d[6].getFileNameYear());
		assertEquals("1999", d[7].getFileNameYear());
		assertEquals("1995", d[8].getFileNameYear());
		assertEquals("2002", d[9].getFileNameYear());
		assertEquals("2010", d[10].getFileNameYear());
		assertEquals("2013", d[11].getFileNameYear());
		assertEquals("", d[12].getFileNameYear());
		assertEquals("", d[13].getFileNameYear());
		assertEquals("", d[14].getFileNameYear());
		assertEquals("", d[15].getFileNameYear());
		assertEquals("2011", d[16].getFileNameYear());
		assertEquals("", d[17].getFileNameYear());
		assertEquals("", d[18].getFileNameYear());
		assertEquals("", d[19].getFileNameYear());
		assertEquals("", d[20].getFileNameYear());
		assertEquals("", d[21].getFileNameYear());
		assertEquals("", d[22].getFileNameYear());
		assertEquals("", d[23].getFileNameYear());
		assertEquals("", d[24].getFileNameYear());
		assertEquals("", d[25].getFileNameYear());
		assertEquals("", d[26].getFileNameYear());
		assertEquals("", d[27].getFileNameYear());
		assertEquals("2012", d[28].getFileNameYear());
		assertEquals("2011", d[29].getFileNameYear());
		assertEquals("2011", d[30].getFileNameYear());
		assertEquals("2012", d[31].getFileNameYear());
		assertEquals("", d[32].getFileNameYear());
		assertEquals("2012", d[33].getFileNameYear());
		assertEquals("2011", d[34].getFileNameYear());
		assertEquals("2012", d[35].getFileNameYear());
		assertEquals("2012", d[36].getFileNameYear());
		assertEquals("2012", d[37].getFileNameYear());
		assertEquals("2012", d[38].getFileNameYear());
		assertEquals("", d[39].getFileNameYear());
		assertEquals("2011", d[40].getFileNameYear());
		assertEquals("2011", d[41].getFileNameYear());
		assertEquals("", d[42].getFileNameYear());
		assertEquals("2011", d[43].getFileNameYear());
		assertEquals("2012", d[44].getFileNameYear());
		assertEquals("", d[45].getFileNameYear());
		assertEquals("2012", d[46].getFileNameYear());
		assertEquals("2012", d[47].getFileNameYear());
		assertEquals("2012", d[48].getFileNameYear());
		assertEquals("", d[49].getFileNameYear());
		assertEquals("2012", d[50].getFileNameYear());
		assertEquals("2012", d[51].getFileNameYear());
		assertEquals("2012", d[52].getFileNameYear());
		assertEquals("2011", d[53].getFileNameYear());
		assertEquals("", d[54].getFileNameYear());
		assertEquals("", d[55].getFileNameYear());
		assertEquals("", d[56].getFileNameYear());
		assertEquals("", d[57].getFileNameYear());
		assertEquals("", d[58].getFileNameYear());
		assertEquals("", d[59].getFileNameYear());
		assertEquals("", d[60].getFileNameYear());
		assertEquals("2011", d[61].getFileNameYear());
		assertEquals("", d[62].getFileNameYear());
		assertEquals("1962", d[63].getFileNameYear());
		assertEquals("1983", d[64].getFileNameYear());
		assertEquals("2008", d[65].getFileNameYear());
		assertEquals("2009", d[66].getFileNameYear());
		assertEquals("2000", d[67].getFileNameYear());
		assertEquals("1976", d[68].getFileNameYear());
		assertEquals("2010", d[69].getFileNameYear());
		assertEquals("1988", d[70].getFileNameYear());
		assertEquals("1995", d[71].getFileNameYear());
		assertEquals("2003", d[72].getFileNameYear());
		assertEquals("2011", d[73].getFileNameYear());
		assertEquals("2003", d[74].getFileNameYear());
		assertEquals("2011", d[75].getFileNameYear());
		assertEquals("2003", d[76].getFileNameYear());
		assertEquals("2002", d[77].getFileNameYear());
		assertEquals("1999", d[78].getFileNameYear());
		assertEquals("1968", d[79].getFileNameYear());
		assertEquals("2001", d[80].getFileNameYear());
		assertEquals("2010", d[81].getFileNameYear());
		assertEquals("1996", d[82].getFileNameYear());
		assertEquals("2001", d[83].getFileNameYear());
		assertEquals("2001", d[84].getFileNameYear());
		assertEquals("2006", d[85].getFileNameYear());
		assertEquals("2011", d[86].getFileNameYear());
		assertEquals("1937", d[87].getFileNameYear());
		assertEquals("2012", d[88].getFileNameYear());
		assertEquals("1935", d[89].getFileNameYear());
		assertEquals("1977", d[90].getFileNameYear());
		assertEquals("2012", d[91].getFileNameYear());
		assertEquals("2009", d[92].getFileNameYear());
		assertEquals("2004", d[93].getFileNameYear());
		assertEquals("1995", d[94].getFileNameYear());
		assertEquals("", d[95].getFileNameYear());
		assertEquals("2011", d[96].getFileNameYear());
		assertEquals("2009", d[97].getFileNameYear());
		assertEquals("2010", d[98].getFileNameYear());
		assertEquals("2010", d[99].getFileNameYear());
		assertEquals("2010", d[100].getFileNameYear());
		assertEquals("2012", d[101].getFileNameYear());
		assertEquals("2012", d[102].getFileNameYear());
		assertEquals("2009", d[103].getFileNameYear());
		assertEquals("2010", d[104].getFileNameYear());
		assertEquals("2008", d[105].getFileNameYear());
		assertEquals("2010", d[106].getFileNameYear());
		assertEquals("", d[107].getFileNameYear());
		assertEquals("2009", d[108].getFileNameYear());
		assertEquals("2009", d[109].getFileNameYear());
		assertEquals("", d[110].getFileNameYear());
		assertEquals("", d[111].getFileNameYear());
		assertEquals("", d[112].getFileNameYear());
		assertEquals("", d[113].getFileNameYear());
		assertEquals("", d[114].getFileNameYear());
		assertEquals("", d[115].getFileNameYear());
		assertEquals("2009", d[116].getFileNameYear());
		assertEquals("", d[117].getFileNameYear());
	}
	
	public void testDecryptedParentNames() {
		assertEquals("", d[0].getDecryptedParentName());
		assertEquals("", d[1].getDecryptedParentName());
		assertEquals("", d[2].getDecryptedParentName());
		assertEquals("", d[3].getDecryptedParentName());
		assertEquals("Shaun of the Dead", d[4].getDecryptedParentName());
		assertEquals("Frontiers", d[5].getDecryptedParentName());
		assertEquals("Wargames", d[6].getDecryptedParentName());
		assertEquals("Fight Club", d[7].getDecryptedParentName());
		assertEquals("Hackers", d[8].getDecryptedParentName());
		assertEquals("Abenteuer", d[9].getDecryptedParentName());
		assertEquals("Animiert", d[10].getDecryptedParentName());
		assertEquals("movies", d[11].getDecryptedParentName());
		assertEquals("", d[12].getDecryptedParentName());
		assertEquals("", d[13].getDecryptedParentName());
		assertEquals("", d[14].getDecryptedParentName());
		assertEquals("", d[15].getDecryptedParentName());
		assertEquals("", d[16].getDecryptedParentName());
		assertEquals("", d[17].getDecryptedParentName());
		assertEquals("", d[18].getDecryptedParentName());
		assertEquals("", d[19].getDecryptedParentName());
		assertEquals("", d[20].getDecryptedParentName());
		assertEquals("", d[21].getDecryptedParentName());
		assertEquals("", d[22].getDecryptedParentName());
		assertEquals("", d[23].getDecryptedParentName());
		assertEquals("", d[24].getDecryptedParentName());
		assertEquals("", d[25].getDecryptedParentName());
		assertEquals("", d[26].getDecryptedParentName());
		assertEquals("", d[27].getDecryptedParentName());
		assertEquals("", d[28].getDecryptedParentName());
		assertEquals("", d[29].getDecryptedParentName());		
		assertEquals("", d[30].getDecryptedParentName());
		assertEquals("", d[31].getDecryptedParentName());
		assertEquals("", d[32].getDecryptedParentName());
		assertEquals("", d[33].getDecryptedParentName());
		assertEquals("", d[34].getDecryptedParentName());
		assertEquals("", d[35].getDecryptedParentName());
		assertEquals("", d[36].getDecryptedParentName());
		assertEquals("", d[37].getDecryptedParentName());
		assertEquals("", d[38].getDecryptedParentName());
		assertEquals("", d[39].getDecryptedParentName());
		assertEquals("", d[40].getDecryptedParentName());
		assertEquals("", d[41].getDecryptedParentName());
		assertEquals("", d[42].getDecryptedParentName());
		assertEquals("", d[43].getDecryptedParentName());
		assertEquals("", d[44].getDecryptedParentName());
		assertEquals("", d[45].getDecryptedParentName());
		assertEquals("", d[46].getDecryptedParentName());
		assertEquals("", d[47].getDecryptedParentName());
		assertEquals("", d[48].getDecryptedParentName());
		assertEquals("", d[49].getDecryptedParentName());
		assertEquals("", d[50].getDecryptedParentName());
		assertEquals("", d[51].getDecryptedParentName());
		assertEquals("", d[52].getDecryptedParentName());
		assertEquals("", d[53].getDecryptedParentName());
		assertEquals("", d[54].getDecryptedParentName());
		assertEquals("", d[55].getDecryptedParentName());		
		assertEquals("", d[56].getDecryptedParentName());
		assertEquals("", d[57].getDecryptedParentName());
		assertEquals("", d[58].getDecryptedParentName());
		assertEquals("", d[59].getDecryptedParentName());
		assertEquals("", d[60].getDecryptedParentName());
		assertEquals("", d[61].getDecryptedParentName());
		assertEquals("L'HOMME QUI RIT", d[62].getDecryptedParentName());
		assertEquals("", d[63].getDecryptedParentName());
		assertEquals("", d[64].getDecryptedParentName());
		assertEquals("", d[65].getDecryptedParentName());
		assertEquals("", d[66].getDecryptedParentName());
		assertEquals("", d[67].getDecryptedParentName());
		assertEquals("", d[68].getDecryptedParentName());
		assertEquals("", d[69].getDecryptedParentName());
		assertEquals("", d[70].getDecryptedParentName());
		assertEquals("", d[71].getDecryptedParentName());
		assertEquals("", d[72].getDecryptedParentName());
		assertEquals("", d[73].getDecryptedParentName());
		assertEquals("", d[74].getDecryptedParentName());
		assertEquals("", d[75].getDecryptedParentName());
		assertEquals("", d[76].getDecryptedParentName());
		assertEquals("", d[77].getDecryptedParentName());
		assertEquals("", d[78].getDecryptedParentName());
		assertEquals("", d[79].getDecryptedParentName());
		assertEquals("", d[80].getDecryptedParentName());
		assertEquals("", d[81].getDecryptedParentName());
		assertEquals("", d[82].getDecryptedParentName());
		assertEquals("", d[83].getDecryptedParentName());
		assertEquals("", d[84].getDecryptedParentName());
		assertEquals("", d[85].getDecryptedParentName());
		assertEquals("10 years", d[86].getDecryptedParentName());
		assertEquals("A Day at the Races", d[87].getDecryptedParentName());
		assertEquals("a late quartet", d[88].getDecryptedParentName());
		assertEquals("A Night at the Opera", d[89].getDecryptedParentName());
		assertEquals("close encounters of the third kind", d[90].getDecryptedParentName());
		assertEquals("Kapringen aka A Hijacking", d[91].getDecryptedParentName());
		assertEquals("", d[92].getDecryptedParentName());
		assertEquals("Home On The Range", d[93].getDecryptedParentName());
		assertEquals("Heat", d[94].getDecryptedParentName());
		assertEquals("Gnomeo and Juliet", d[95].getDecryptedParentName());
		assertEquals("Crazy Stupid Love", d[96].getDecryptedParentName());
		assertEquals("Dead Snow", d[97].getDecryptedParentName());
		assertEquals("And Soon the Darkness", d[98].getDecryptedParentName());
		assertEquals("Aftershock", d[99].getDecryptedParentName());
		assertEquals("Alpha and Omega", d[100].getDecryptedParentName());
		assertEquals("Battleground", d[101].getDecryptedParentName());
		assertEquals("Battle Force", d[102].getDecryptedParentName());
		assertEquals("Cloudy with a chance of meatballs", d[103].getDecryptedParentName());
		assertEquals("Game of Death", d[104].getDecryptedParentName());
		assertEquals("Igor", d[105].getDecryptedParentName());
		assertEquals("Room In Rome", d[106].getDecryptedParentName());
		assertEquals("", d[107].getDecryptedParentName());
		assertEquals("", d[108].getDecryptedParentName());
		assertEquals("", d[109].getDecryptedParentName());
		assertEquals("", d[110].getDecryptedParentName());
		assertEquals("", d[111].getDecryptedParentName());
		assertEquals("", d[112].getDecryptedParentName());
		assertEquals("Hello Friend", d[113].getDecryptedParentName());
		assertEquals("Hello Friend", d[114].getDecryptedParentName());
		assertEquals("Hello Friend", d[115].getDecryptedParentName());
		assertEquals("Movies", d[116].getDecryptedParentName());
		assertEquals("", d[117].getDecryptedParentName());
	}
	
	public void testDecryptedParentNameYears() {
		assertEquals("", d[0].getParentNameYear());
		assertEquals("", d[1].getParentNameYear());
		assertEquals("", d[2].getParentNameYear());
		assertEquals("", d[3].getParentNameYear());
		assertEquals("", d[4].getParentNameYear());
		assertEquals("", d[5].getParentNameYear());
		assertEquals("1983", d[6].getParentNameYear());
		assertEquals("1999", d[7].getParentNameYear());
		assertEquals("1995", d[8].getParentNameYear());
		assertEquals("", d[9].getParentNameYear());
		assertEquals("", d[10].getParentNameYear());
		assertEquals("", d[11].getParentNameYear());
		assertEquals("", d[12].getParentNameYear());
		assertEquals("", d[13].getParentNameYear());
		assertEquals("", d[14].getParentNameYear());
		assertEquals("", d[15].getParentNameYear());
		assertEquals("", d[16].getParentNameYear());
		assertEquals("", d[17].getParentNameYear());
		assertEquals("", d[18].getParentNameYear());
		assertEquals("", d[19].getParentNameYear());
		assertEquals("", d[20].getParentNameYear());
		assertEquals("", d[21].getParentNameYear());
		assertEquals("", d[22].getParentNameYear());
		assertEquals("", d[23].getParentNameYear());
		assertEquals("", d[24].getParentNameYear());
		assertEquals("", d[25].getParentNameYear());
		assertEquals("", d[26].getParentNameYear());
		assertEquals("", d[27].getParentNameYear());
		assertEquals("", d[28].getParentNameYear());
		assertEquals("", d[29].getParentNameYear());		
		assertEquals("", d[30].getParentNameYear());
		assertEquals("", d[31].getParentNameYear());
		assertEquals("", d[32].getParentNameYear());
		assertEquals("", d[33].getParentNameYear());
		assertEquals("", d[34].getParentNameYear());
		assertEquals("", d[35].getParentNameYear());
		assertEquals("", d[36].getParentNameYear());
		assertEquals("", d[37].getParentNameYear());
		assertEquals("", d[38].getParentNameYear());
		assertEquals("", d[39].getParentNameYear());
		assertEquals("", d[40].getParentNameYear());
		assertEquals("", d[41].getParentNameYear());
		assertEquals("", d[42].getParentNameYear());
		assertEquals("", d[43].getParentNameYear());
		assertEquals("", d[44].getParentNameYear());
		assertEquals("", d[45].getParentNameYear());
		assertEquals("", d[46].getParentNameYear());
		assertEquals("", d[47].getParentNameYear());
		assertEquals("", d[48].getParentNameYear());
		assertEquals("", d[49].getParentNameYear());
		assertEquals("", d[50].getParentNameYear());
		assertEquals("", d[51].getParentNameYear());
		assertEquals("", d[52].getParentNameYear());
		assertEquals("", d[53].getParentNameYear());
		assertEquals("", d[54].getParentNameYear());
		assertEquals("", d[55].getParentNameYear());		
		assertEquals("", d[56].getParentNameYear());
		assertEquals("", d[57].getParentNameYear());
		assertEquals("", d[58].getParentNameYear());
		assertEquals("", d[59].getParentNameYear());
		assertEquals("", d[60].getParentNameYear());
		assertEquals("", d[61].getParentNameYear());
		assertEquals("", d[62].getParentNameYear());
		assertEquals("", d[63].getParentNameYear());
		assertEquals("", d[64].getParentNameYear());
		assertEquals("", d[65].getParentNameYear());
		assertEquals("", d[66].getParentNameYear());
		assertEquals("", d[67].getParentNameYear());
		assertEquals("", d[68].getParentNameYear());
		assertEquals("", d[69].getParentNameYear());
		assertEquals("", d[70].getParentNameYear());
		assertEquals("", d[71].getParentNameYear());
		assertEquals("", d[72].getParentNameYear());
		assertEquals("", d[73].getParentNameYear());
		assertEquals("", d[74].getParentNameYear());
		assertEquals("", d[75].getParentNameYear());
		assertEquals("", d[76].getParentNameYear());
		assertEquals("", d[77].getParentNameYear());
		assertEquals("", d[78].getParentNameYear());
		assertEquals("", d[79].getParentNameYear());
		assertEquals("", d[80].getParentNameYear());
		assertEquals("", d[81].getParentNameYear());
		assertEquals("", d[82].getParentNameYear());
		assertEquals("", d[83].getParentNameYear());
		assertEquals("", d[84].getParentNameYear());
		assertEquals("", d[85].getParentNameYear());
		assertEquals("2011", d[86].getParentNameYear());
		assertEquals("1937", d[87].getParentNameYear());
		assertEquals("2012", d[88].getParentNameYear());
		assertEquals("1935", d[89].getParentNameYear());
		assertEquals("1977", d[90].getParentNameYear());
		assertEquals("2012", d[91].getParentNameYear());
		assertEquals("", d[92].getParentNameYear());
		assertEquals("2004", d[93].getParentNameYear());
		assertEquals("1995", d[94].getParentNameYear());
		assertEquals("", d[95].getParentNameYear());
		assertEquals("2011", d[96].getParentNameYear());
		assertEquals("", d[97].getParentNameYear());
		assertEquals("2010", d[98].getParentNameYear());
		assertEquals("2010", d[99].getParentNameYear());
		assertEquals("2010", d[100].getParentNameYear());
		assertEquals("2012", d[101].getParentNameYear());
		assertEquals("2012", d[102].getParentNameYear());
		assertEquals("2009", d[103].getParentNameYear());
		assertEquals("2010", d[104].getParentNameYear());
		assertEquals("2008", d[105].getParentNameYear());
		assertEquals("2010", d[106].getParentNameYear());
		assertEquals("", d[107].getParentNameYear());
		assertEquals("", d[108].getParentNameYear());
		assertEquals("", d[109].getParentNameYear());
		assertEquals("", d[110].getParentNameYear());
		assertEquals("", d[111].getParentNameYear());
		assertEquals("", d[112].getParentNameYear());
		assertEquals("", d[113].getParentNameYear());
		assertEquals("", d[114].getParentNameYear());
		assertEquals("", d[115].getParentNameYear());
		assertEquals("", d[116].getParentNameYear());
		assertEquals("", d[117].getParentNameYear());
	}
	
	public void testImdbIds() {
		// 0-115 contain no IMDb ID's
		for (int i = 0; i < 116; i++)
			assertEquals(null, d[i].getImdbId());
		
		assertEquals("tt0499549", d[116].getImdbId());
		assertEquals("tt0499549", d[117].getImdbId());
	}
	
	public void testCustomTags() {
		DecryptedMovie mMovie = MizLib.decryptMovie(TEST_FILENAMES[0], "avatar");
		assertEquals("", mMovie.getDecryptedFileName());
		
		mMovie = MizLib.decryptMovie(TEST_FILENAMES[0], "tar");
		assertEquals("Ava", mMovie.getDecryptedFileName());
	}
	
	public void testPartNumbers() {
		assertEquals(1, MizLib.getPartNumberFromFilepath("Avatar part1.mkv"));
		assertEquals(2, MizLib.getPartNumberFromFilepath("Avatar part2.mkv"));
		assertEquals(1, MizLib.getPartNumberFromFilepath("Avatar cd1.mkv"));
		assertEquals(2, MizLib.getPartNumberFromFilepath("Avatar cd2.mkv"));
		assertEquals(5, MizLib.getPartNumberFromFilepath("Avatar.cd5.mkv"));
		assertEquals(5, MizLib.getPartNumberFromFilepath("Avatar.part5.mkv"));
	}
}