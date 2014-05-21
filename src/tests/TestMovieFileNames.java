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

package tests;

import com.miz.functions.DecryptedMovie;
import com.miz.functions.MizLib;

import junit.framework.Assert;
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
			"/Filme/Animiert/Drachenzähmen leicht gemacht - 2010 - Animation.avi", // 10
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
			"/Babycall [BluRay Screener][EspañolCastellano][2012][www.newpct.com].avi", // 31
			"/Batman Begins [DVDrip][AC3.5.1 Spanish][www.newpct.com].avi", // 32
			"/Berserk La Edad de Oro I El huevo del rey conquistador [BRrip][AC3 5.1 Español Castellano][2012][www.newpct.com].avi", // 33
			"/BlackThorn (Sin Destino) [DVDRIP][Spanish AC3 5.1][2011][www.newpct.com].avi", // 34
			"/Bunraku [BluRay Screener][Spanish HQ][2012].avi", // 35
			"/Chocolate [DVDrip][Español Castellano][2012][www.newpct.com].avi", // 36
			"/El Alucinante Mundo de Norman [BluRay Screener][Español Castellano SINCRONIZADO][2012][www.newpct.com].avi", // 37
			"/El Caballero Oscuro La Leyenda Renace [DVDRip][AC3 5.1 Español Castellano][2012][www.newpct.com].avi", // 38
			"/El Ultimo Gran Dia [BluRay RIP][Spanish_English AC3 5.1][www.newpct.com].avi", // 39
			"/Encontraras Dragones [DVDRIP][Spanish AC3 5.1][2011][www.newpct.com].avi", // 40
			"/Gantz 2 [DVDRIP][Spanish AC3 5.1][2011][www.descargaya.es].avi", // 41
			"/Infiltrado [DVDrip][Spanish][newpct.com].avi", // 42
			"/Intruders [DVD Screener][Spanish][2011].avi", // 43
			"/Largo Winch 2 [DVDRIP][Spanish AC3 5.1][2012].avi", // 44
			"/Los Idus De Marzo [ALTA DEFINICION 720p].avi", // 45
			"/Otra Tierra [DVDRIP][Spanish AC3 5.1][2012][www.pctestrenos.com].avi", // 46
			"/Prometheus [BRrip][AC3 5.1 Español Castellano][2012][www.newpct.com].avi", // 47
			"/Redención (Tyrannosaur) [BluRay Screener][Spaniish][2012].avi", // 48
			"/Somos Marshall [DVDrip][AC3.Spanish][www.newpct.com].avi", // 49
			"/The Amazing Spider-Man (Proper)[BluRayRIP][AC3 5.1 Español Castellano][2012][www.newpct.com].avi", // 50
			"/Todos los Días de mi Vida [DVD Screener][Spanish][2012].avi", // 51
			"/Verbo [BluRayRIP][Spanish AC3 5.1][2012][www.newpct.com].avi", // 52
			"/Valor De Ley [BluRay Screener][Spanish][2011].avi", // 53
			"/Avatar DVDRIP.avi", // 54
			"/El Deminio Bajo La Piel BRSCR.avi", // 55
			"/Futurama - El Gran Golpe De Bender - DVDRIP.avi", // 56
			"/la familia savages cd1.avi", // 57
			"/la familia savages cd2.avi", // 58
			"/Los Simpsons La Pelicula - BLURAY.mkv", // 59
			"/Mas Extraño Que La Ficción - BLURAY.mkv", // 60
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
			"/Le.Huitième.Jour.(1996).avi", // 82
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
			
	};
	private DecryptedMovie[] d;
	
	public TestMovieFileNames() {
		d = new DecryptedMovie[TEST_FILENAMES.length];
		for (int i = 0; i < TEST_FILENAMES.length; i++)
			d[i] = MizLib.decryptMovie(TEST_FILENAMES[i], "");
	}

	public void testDecryptedFilenames() {
		Assert.assertEquals("Avatar", d[0].getDecryptedFileName());
		Assert.assertEquals("Star Trek", d[1].getDecryptedFileName());
		Assert.assertEquals("The Thing", d[2].getDecryptedFileName());
		Assert.assertEquals("Green Lantern", d[3].getDecryptedFileName());
		Assert.assertEquals("Shaun of the Dead", d[4].getDecryptedFileName());
		Assert.assertEquals("Frontiers", d[5].getDecryptedFileName());
		Assert.assertEquals("Wargames", d[6].getDecryptedFileName());
		Assert.assertEquals("Fight Club", d[7].getDecryptedFileName());
		Assert.assertEquals("Hackers", d[8].getDecryptedFileName());
		Assert.assertEquals("Asterix und Obelix Mission Kleopatra", d[9].getDecryptedFileName());
		Assert.assertEquals("Drachenzähmen leicht gemacht", d[10].getDecryptedFileName());
		Assert.assertEquals("Jack the Giant Slayer", d[11].getDecryptedFileName());
		Assert.assertEquals("AE Apocalypse Earth", d[12].getDecryptedFileName());
		Assert.assertEquals("Amerrika", d[13].getDecryptedFileName());
		Assert.assertEquals("Atrapa El Fuego", d[14].getDecryptedFileName());
		Assert.assertEquals("Blade Runner The Final Cut", d[15].getDecryptedFileName());
		Assert.assertEquals("En Un Mundo Mejor", d[16].getDecryptedFileName());
		Assert.assertEquals("Matando Cabos", d[17].getDecryptedFileName());
		Assert.assertEquals("Matando Cabos", d[18].getDecryptedFileName());
		Assert.assertEquals("Star Wars I La Amenaza Fantasma", d[19].getDecryptedFileName());
		Assert.assertEquals("Star Wars II El Ataque De Los Clones", d[20].getDecryptedFileName());
		Assert.assertEquals("Star Wars III La Venganza De Los Shit", d[21].getDecryptedFileName());
		Assert.assertEquals("360 Juego de destinos", d[22].getDecryptedFileName());
		Assert.assertEquals("Cumbres borrascosas", d[23].getDecryptedFileName());
		Assert.assertEquals("Cenizas del cielo", d[24].getDecryptedFileName());
		Assert.assertEquals("Hansel y Gretel", d[25].getDecryptedFileName());
		Assert.assertEquals("El peso del agua", d[26].getDecryptedFileName());
		Assert.assertEquals("Iron Man 3", d[27].getDecryptedFileName());
		Assert.assertEquals("Albert Nobbs", d[28].getDecryptedFileName());
		Assert.assertEquals("Algo Prestado", d[29].getDecryptedFileName());
		Assert.assertEquals("Amenazados", d[30].getDecryptedFileName());
		Assert.assertEquals("Babycall", d[31].getDecryptedFileName());
		Assert.assertEquals("Batman Begins", d[32].getDecryptedFileName());
		Assert.assertEquals("Berserk La Edad de Oro I El huevo del rey conquistador", d[33].getDecryptedFileName());
		Assert.assertEquals("BlackThorn", d[34].getDecryptedFileName());
		Assert.assertEquals("Bunraku", d[35].getDecryptedFileName());
		Assert.assertEquals("Chocolate", d[36].getDecryptedFileName());
		Assert.assertEquals("El Alucinante Mundo de Norman", d[37].getDecryptedFileName());
		Assert.assertEquals("El Caballero Oscuro La Leyenda Renace", d[38].getDecryptedFileName());
		Assert.assertEquals("El Ultimo Gran Dia", d[39].getDecryptedFileName());
		Assert.assertEquals("Encontraras Dragones", d[40].getDecryptedFileName());
		Assert.assertEquals("Gantz 2", d[41].getDecryptedFileName());
		Assert.assertEquals("Infiltrado", d[42].getDecryptedFileName());
		Assert.assertEquals("Intruders", d[43].getDecryptedFileName());
		Assert.assertEquals("Largo Winch 2", d[44].getDecryptedFileName());
		Assert.assertEquals("Los Idus De Marzo", d[45].getDecryptedFileName());
		Assert.assertEquals("Otra Tierra", d[46].getDecryptedFileName());
		Assert.assertEquals("Prometheus", d[47].getDecryptedFileName());
		Assert.assertEquals("Redención", d[48].getDecryptedFileName());
		Assert.assertEquals("Somos Marshall", d[49].getDecryptedFileName());
		Assert.assertEquals("The Amazing SpiderMan", d[50].getDecryptedFileName());
		Assert.assertEquals("Todos los Días de mi Vida", d[51].getDecryptedFileName());
		Assert.assertEquals("Verbo", d[52].getDecryptedFileName());
		Assert.assertEquals("Valor De Ley", d[53].getDecryptedFileName());
		Assert.assertEquals("Avatar", d[54].getDecryptedFileName());
		Assert.assertEquals("El Deminio Bajo La Piel", d[55].getDecryptedFileName());
		Assert.assertEquals("Futurama El Gran Golpe De Bender", d[56].getDecryptedFileName());
		Assert.assertEquals("la familia savages", d[57].getDecryptedFileName());
		Assert.assertEquals("la familia savages", d[58].getDecryptedFileName());
		Assert.assertEquals("Los Simpsons La Pelicula", d[59].getDecryptedFileName());
		Assert.assertEquals("Mas Extraño Que La Ficción", d[60].getDecryptedFileName());
		Assert.assertEquals("Mi Ex Mi Novia y Yo", d[61].getDecryptedFileName());
		Assert.assertEquals("L'HOMME QUI RIT", d[62].getDecryptedFileName());
		Assert.assertEquals("Dr No", d[63].getDecryptedFileName());
		Assert.assertEquals("Never Say Never Again", d[64].getDecryptedFileName());
		Assert.assertEquals("Quantum Of Solace", d[65].getDecryptedFileName());
		Assert.assertEquals("The Assassin Next Door", d[66].getDecryptedFileName());
		Assert.assertEquals("Battle Royale", d[67].getDecryptedFileName());
		Assert.assertEquals("C'etait Un Rendez Vous", d[68].getDecryptedFileName());
		Assert.assertEquals("Death Race 2", d[69].getDecryptedFileName());
		Assert.assertEquals("Die Hard", d[70].getDecryptedFileName());
		Assert.assertEquals("Die Hard 3", d[71].getDecryptedFileName());
		Assert.assertEquals("Kill Bill Vol 1", d[72].getDecryptedFileName());
		Assert.assertEquals("Mission Impossible Ghost Protocol", d[73].getDecryptedFileName());
		Assert.assertEquals("Kill Bill Vol 1", d[74].getDecryptedFileName());
		Assert.assertEquals("La Proie", d[75].getDecryptedFileName());
		Assert.assertEquals("SWAT", d[76].getDecryptedFileName());
		Assert.assertEquals("xXx", d[77].getDecryptedFileName());
		Assert.assertEquals("eXistenz", d[78].getDecryptedFileName());
		Assert.assertEquals("2001 A Space Odyssey", d[79].getDecryptedFileName());
		Assert.assertEquals("The Fellowship of the Ring", d[80].getDecryptedFileName());
		Assert.assertEquals("The Sorcerers Apprentice", d[81].getDecryptedFileName());
		Assert.assertEquals("Le Huitième Jour", d[82].getDecryptedFileName());
		Assert.assertEquals("Hysteria The Def Leppard Story", d[83].getDecryptedFileName());
		Assert.assertEquals("Donnie Darko", d[84].getDecryptedFileName());
		Assert.assertEquals("Ne Le Dis A Personne", d[85].getDecryptedFileName());
		Assert.assertEquals("10 years", d[86].getDecryptedFileName());
		Assert.assertEquals("A Day at the Races", d[87].getDecryptedFileName());
		Assert.assertEquals("a late quartet", d[88].getDecryptedFileName());
		Assert.assertEquals("A Night at the Opera", d[89].getDecryptedFileName());
		Assert.assertEquals("close encounters of the third kind", d[90].getDecryptedFileName());
		Assert.assertEquals("a hijacking", d[91].getDecryptedFileName());
		Assert.assertEquals("2012", d[92].getDecryptedFileName());
		Assert.assertEquals("Home On The Range", d[93].getDecryptedFileName());
		Assert.assertEquals("Heat", d[94].getDecryptedFileName());
		Assert.assertEquals("Gnomeo and Juliet HD", d[95].getDecryptedFileName());
		Assert.assertEquals("Crazy Stupid Love", d[96].getDecryptedFileName());
		Assert.assertEquals("Dead Snow", d[97].getDecryptedFileName());
		Assert.assertEquals("And Soon the Darkness", d[98].getDecryptedFileName());
		Assert.assertEquals("Aftershock", d[99].getDecryptedFileName());
		Assert.assertEquals("Alpha and Omega", d[100].getDecryptedFileName());
		Assert.assertEquals("Battleground", d[101].getDecryptedFileName());
		Assert.assertEquals("Battle Force", d[102].getDecryptedFileName());
		Assert.assertEquals("Cloudy With A Chance Of Meatballs", d[103].getDecryptedFileName());
		Assert.assertEquals("Game of Death", d[104].getDecryptedFileName());
		Assert.assertEquals("Igor", d[105].getDecryptedFileName());
		Assert.assertEquals("Room In Rome", d[106].getDecryptedFileName());
		Assert.assertEquals("Look", d[107].getDecryptedFileName());
		Assert.assertEquals("Funny People", d[108].getDecryptedFileName());
		Assert.assertEquals("Frequently Asked Questions About Time Travel", d[109].getDecryptedFileName());
		Assert.assertEquals("Enter The Void", d[110].getDecryptedFileName());
		Assert.assertEquals("Test This Shit", d[111].getDecryptedFileName());
		Assert.assertEquals("Test", d[112].getDecryptedFileName());
		Assert.assertEquals("Hello", d[113].getDecryptedFileName());
		Assert.assertEquals("Hello Friend", d[114].getDecryptedFileName());
		Assert.assertEquals("Hello Friend", d[115].getDecryptedFileName());
	}
	
	public void testDecryptedFilenameYears() {
		Assert.assertEquals("2009", d[0].getFileNameYear());
		Assert.assertEquals("2009", d[1].getFileNameYear());
		Assert.assertEquals("2011", d[2].getFileNameYear());
		Assert.assertEquals("", d[3].getFileNameYear());
		Assert.assertEquals("", d[4].getFileNameYear());
		Assert.assertEquals("", d[5].getFileNameYear());
		Assert.assertEquals("1983", d[6].getFileNameYear());
		Assert.assertEquals("1999", d[7].getFileNameYear());
		Assert.assertEquals("1995", d[8].getFileNameYear());
		Assert.assertEquals("2002", d[9].getFileNameYear());
		Assert.assertEquals("2010", d[10].getFileNameYear());
		Assert.assertEquals("2013", d[11].getFileNameYear());
		Assert.assertEquals("", d[12].getFileNameYear());
		Assert.assertEquals("", d[13].getFileNameYear());
		Assert.assertEquals("", d[14].getFileNameYear());
		Assert.assertEquals("", d[15].getFileNameYear());
		Assert.assertEquals("2011", d[16].getFileNameYear());
		Assert.assertEquals("", d[17].getFileNameYear());
		Assert.assertEquals("", d[18].getFileNameYear());
		Assert.assertEquals("", d[19].getFileNameYear());
		Assert.assertEquals("", d[20].getFileNameYear());
		Assert.assertEquals("", d[21].getFileNameYear());
		Assert.assertEquals("", d[22].getFileNameYear());
		Assert.assertEquals("", d[23].getFileNameYear());
		Assert.assertEquals("", d[24].getFileNameYear());
		Assert.assertEquals("", d[25].getFileNameYear());
		Assert.assertEquals("", d[26].getFileNameYear());
		Assert.assertEquals("", d[27].getFileNameYear());
		Assert.assertEquals("2012", d[28].getFileNameYear());
		Assert.assertEquals("2011", d[29].getFileNameYear());
		Assert.assertEquals("2011", d[30].getFileNameYear());
		Assert.assertEquals("2012", d[31].getFileNameYear());
		Assert.assertEquals("", d[32].getFileNameYear());
		Assert.assertEquals("2012", d[33].getFileNameYear());
		Assert.assertEquals("2011", d[34].getFileNameYear());
		Assert.assertEquals("2012", d[35].getFileNameYear());
		Assert.assertEquals("2012", d[36].getFileNameYear());
		Assert.assertEquals("2012", d[37].getFileNameYear());
		Assert.assertEquals("2012", d[38].getFileNameYear());
		Assert.assertEquals("", d[39].getFileNameYear());
		Assert.assertEquals("2011", d[40].getFileNameYear());
		Assert.assertEquals("2011", d[41].getFileNameYear());
		Assert.assertEquals("", d[42].getFileNameYear());
		Assert.assertEquals("2011", d[43].getFileNameYear());
		Assert.assertEquals("2012", d[44].getFileNameYear());
		Assert.assertEquals("", d[45].getFileNameYear());
		Assert.assertEquals("2012", d[46].getFileNameYear());
		Assert.assertEquals("2012", d[47].getFileNameYear());
		Assert.assertEquals("2012", d[48].getFileNameYear());
		Assert.assertEquals("", d[49].getFileNameYear());
		Assert.assertEquals("2012", d[50].getFileNameYear());
		Assert.assertEquals("2012", d[51].getFileNameYear());
		Assert.assertEquals("2012", d[52].getFileNameYear());
		Assert.assertEquals("2011", d[53].getFileNameYear());
		Assert.assertEquals("", d[54].getFileNameYear());
		Assert.assertEquals("", d[55].getFileNameYear());
		Assert.assertEquals("", d[56].getFileNameYear());
		Assert.assertEquals("", d[57].getFileNameYear());
		Assert.assertEquals("", d[58].getFileNameYear());
		Assert.assertEquals("", d[59].getFileNameYear());
		Assert.assertEquals("", d[60].getFileNameYear());
		Assert.assertEquals("2011", d[61].getFileNameYear());
		Assert.assertEquals("", d[62].getFileNameYear());
		Assert.assertEquals("1962", d[63].getFileNameYear());
		Assert.assertEquals("1983", d[64].getFileNameYear());
		Assert.assertEquals("2008", d[65].getFileNameYear());
		Assert.assertEquals("2009", d[66].getFileNameYear());
		Assert.assertEquals("2000", d[67].getFileNameYear());
		Assert.assertEquals("1976", d[68].getFileNameYear());
		Assert.assertEquals("2010", d[69].getFileNameYear());
		Assert.assertEquals("1988", d[70].getFileNameYear());
		Assert.assertEquals("1995", d[71].getFileNameYear());
		Assert.assertEquals("2003", d[72].getFileNameYear());
		Assert.assertEquals("2011", d[73].getFileNameYear());
		Assert.assertEquals("2003", d[74].getFileNameYear());
		Assert.assertEquals("2011", d[75].getFileNameYear());
		Assert.assertEquals("2003", d[76].getFileNameYear());
		Assert.assertEquals("2002", d[77].getFileNameYear());
		Assert.assertEquals("1999", d[78].getFileNameYear());
		Assert.assertEquals("1968", d[79].getFileNameYear());
		Assert.assertEquals("2001", d[80].getFileNameYear());
		Assert.assertEquals("2010", d[81].getFileNameYear());
		Assert.assertEquals("1996", d[82].getFileNameYear());
		Assert.assertEquals("2001", d[83].getFileNameYear());
		Assert.assertEquals("2001", d[84].getFileNameYear());
		Assert.assertEquals("2006", d[85].getFileNameYear());
		Assert.assertEquals("2011", d[86].getFileNameYear());
		Assert.assertEquals("1937", d[87].getFileNameYear());
		Assert.assertEquals("2012", d[88].getFileNameYear());
		Assert.assertEquals("1935", d[89].getFileNameYear());
		Assert.assertEquals("1977", d[90].getFileNameYear());
		Assert.assertEquals("2012", d[91].getFileNameYear());
		Assert.assertEquals("2009", d[92].getFileNameYear());
		Assert.assertEquals("2004", d[93].getFileNameYear());
		Assert.assertEquals("1995", d[94].getFileNameYear());
		Assert.assertEquals("", d[95].getFileNameYear());
		Assert.assertEquals("2011", d[96].getFileNameYear());
		Assert.assertEquals("2009", d[97].getFileNameYear());
		Assert.assertEquals("2010", d[98].getFileNameYear());
		Assert.assertEquals("2010", d[99].getFileNameYear());
		Assert.assertEquals("2010", d[100].getFileNameYear());
		Assert.assertEquals("2012", d[101].getFileNameYear());
		Assert.assertEquals("2012", d[102].getFileNameYear());
		Assert.assertEquals("2009", d[103].getFileNameYear());
		Assert.assertEquals("2010", d[104].getFileNameYear());
		Assert.assertEquals("2008", d[105].getFileNameYear());
		Assert.assertEquals("2010", d[106].getFileNameYear());
		Assert.assertEquals("", d[107].getFileNameYear());
		Assert.assertEquals("2009", d[108].getFileNameYear());
		Assert.assertEquals("2009", d[109].getFileNameYear());
		Assert.assertEquals("", d[110].getFileNameYear());
	}
	
	public void testDecryptedParentNames() {
		Assert.assertEquals("", d[0].getDecryptedParentName());
		Assert.assertEquals("", d[1].getDecryptedParentName());
		Assert.assertEquals("", d[2].getDecryptedParentName());
		Assert.assertEquals("", d[3].getDecryptedParentName());
		Assert.assertEquals("Shaun of the Dead", d[4].getDecryptedParentName());
		Assert.assertEquals("Frontiers", d[5].getDecryptedParentName());
		Assert.assertEquals("Wargames", d[6].getDecryptedParentName());
		Assert.assertEquals("Fight Club", d[7].getDecryptedParentName());
		Assert.assertEquals("Hackers", d[8].getDecryptedParentName());
		Assert.assertEquals("Abenteuer", d[9].getDecryptedParentName());
		Assert.assertEquals("Animiert", d[10].getDecryptedParentName());
		Assert.assertEquals("movies", d[11].getDecryptedParentName());
		Assert.assertEquals("", d[12].getDecryptedParentName());
		Assert.assertEquals("", d[13].getDecryptedParentName());
		Assert.assertEquals("", d[14].getDecryptedParentName());
		Assert.assertEquals("", d[15].getDecryptedParentName());
		Assert.assertEquals("", d[16].getDecryptedParentName());
		Assert.assertEquals("", d[17].getDecryptedParentName());
		Assert.assertEquals("", d[18].getDecryptedParentName());
		Assert.assertEquals("", d[19].getDecryptedParentName());
		Assert.assertEquals("", d[20].getDecryptedParentName());
		Assert.assertEquals("", d[21].getDecryptedParentName());
		Assert.assertEquals("", d[22].getDecryptedParentName());
		Assert.assertEquals("", d[23].getDecryptedParentName());
		Assert.assertEquals("", d[24].getDecryptedParentName());
		Assert.assertEquals("", d[25].getDecryptedParentName());
		Assert.assertEquals("", d[26].getDecryptedParentName());
		Assert.assertEquals("", d[27].getDecryptedParentName());
		Assert.assertEquals("", d[28].getDecryptedParentName());
		Assert.assertEquals("", d[29].getDecryptedParentName());		
		Assert.assertEquals("", d[30].getDecryptedParentName());
		Assert.assertEquals("", d[31].getDecryptedParentName());
		Assert.assertEquals("", d[32].getDecryptedParentName());
		Assert.assertEquals("", d[33].getDecryptedParentName());
		Assert.assertEquals("", d[34].getDecryptedParentName());
		Assert.assertEquals("", d[35].getDecryptedParentName());
		Assert.assertEquals("", d[36].getDecryptedParentName());
		Assert.assertEquals("", d[37].getDecryptedParentName());
		Assert.assertEquals("", d[38].getDecryptedParentName());
		Assert.assertEquals("", d[39].getDecryptedParentName());
		Assert.assertEquals("", d[40].getDecryptedParentName());
		Assert.assertEquals("", d[41].getDecryptedParentName());
		Assert.assertEquals("", d[42].getDecryptedParentName());
		Assert.assertEquals("", d[43].getDecryptedParentName());
		Assert.assertEquals("", d[44].getDecryptedParentName());
		Assert.assertEquals("", d[45].getDecryptedParentName());
		Assert.assertEquals("", d[46].getDecryptedParentName());
		Assert.assertEquals("", d[47].getDecryptedParentName());
		Assert.assertEquals("", d[48].getDecryptedParentName());
		Assert.assertEquals("", d[49].getDecryptedParentName());
		Assert.assertEquals("", d[50].getDecryptedParentName());
		Assert.assertEquals("", d[51].getDecryptedParentName());
		Assert.assertEquals("", d[52].getDecryptedParentName());
		Assert.assertEquals("", d[53].getDecryptedParentName());
		Assert.assertEquals("", d[54].getDecryptedParentName());
		Assert.assertEquals("", d[55].getDecryptedParentName());		
		Assert.assertEquals("", d[56].getDecryptedParentName());
		Assert.assertEquals("", d[57].getDecryptedParentName());
		Assert.assertEquals("", d[58].getDecryptedParentName());
		Assert.assertEquals("", d[59].getDecryptedParentName());
		Assert.assertEquals("", d[60].getDecryptedParentName());
		Assert.assertEquals("", d[61].getDecryptedParentName());
		Assert.assertEquals("L'HOMME QUI RIT", d[62].getDecryptedParentName());
		Assert.assertEquals("", d[63].getDecryptedParentName());
		Assert.assertEquals("", d[64].getDecryptedParentName());
		Assert.assertEquals("", d[65].getDecryptedParentName());
		Assert.assertEquals("", d[66].getDecryptedParentName());
		Assert.assertEquals("", d[67].getDecryptedParentName());
		Assert.assertEquals("", d[68].getDecryptedParentName());
		Assert.assertEquals("", d[69].getDecryptedParentName());
		Assert.assertEquals("", d[70].getDecryptedParentName());
		Assert.assertEquals("", d[71].getDecryptedParentName());
		Assert.assertEquals("", d[72].getDecryptedParentName());
		Assert.assertEquals("", d[73].getDecryptedParentName());
		Assert.assertEquals("", d[74].getDecryptedParentName());
		Assert.assertEquals("", d[75].getDecryptedParentName());
		Assert.assertEquals("", d[76].getDecryptedParentName());
		Assert.assertEquals("", d[77].getDecryptedParentName());
		Assert.assertEquals("", d[78].getDecryptedParentName());
		Assert.assertEquals("", d[79].getDecryptedParentName());
		Assert.assertEquals("", d[80].getDecryptedParentName());
		Assert.assertEquals("", d[81].getDecryptedParentName());
		Assert.assertEquals("", d[82].getDecryptedParentName());
		Assert.assertEquals("", d[83].getDecryptedParentName());
		Assert.assertEquals("", d[84].getDecryptedParentName());
		Assert.assertEquals("", d[85].getDecryptedParentName());
		Assert.assertEquals("10 years", d[86].getDecryptedParentName());
		Assert.assertEquals("A Day at the Races", d[87].getDecryptedParentName());
		Assert.assertEquals("a late quartet", d[88].getDecryptedParentName());
		Assert.assertEquals("A Night at the Opera", d[89].getDecryptedParentName());
		Assert.assertEquals("close encounters of the third kind", d[90].getDecryptedParentName());
		Assert.assertEquals("Kapringen aka A Hijacking", d[91].getDecryptedParentName());
		Assert.assertEquals("", d[92].getDecryptedParentName());
		Assert.assertEquals("Home On The Range", d[93].getDecryptedParentName());
		Assert.assertEquals("Heat", d[94].getDecryptedParentName());
		Assert.assertEquals("Gnomeo and Juliet", d[95].getDecryptedParentName());
		Assert.assertEquals("Crazy Stupid Love", d[96].getDecryptedParentName());
		Assert.assertEquals("Dead Snow", d[97].getDecryptedParentName());
		Assert.assertEquals("And Soon the Darkness", d[98].getDecryptedParentName());
		Assert.assertEquals("Aftershock", d[99].getDecryptedParentName());
		Assert.assertEquals("Alpha and Omega", d[100].getDecryptedParentName());
		Assert.assertEquals("Battleground", d[101].getDecryptedParentName());
		Assert.assertEquals("Battle Force", d[102].getDecryptedParentName());
		Assert.assertEquals("Cloudy with a chance of meatballs", d[103].getDecryptedParentName());
		Assert.assertEquals("Game of Death", d[104].getDecryptedParentName());
		Assert.assertEquals("Igor", d[105].getDecryptedParentName());
		Assert.assertEquals("Room In Rome", d[106].getDecryptedParentName());
		Assert.assertEquals("", d[107].getDecryptedParentName());
		Assert.assertEquals("", d[108].getDecryptedParentName());
		Assert.assertEquals("", d[109].getDecryptedParentName());
		Assert.assertEquals("", d[110].getDecryptedParentName());
	}
	
	public void testDecryptedParentNameYears() {
		Assert.assertEquals("", d[0].getParentNameYear());
		Assert.assertEquals("", d[1].getParentNameYear());
		Assert.assertEquals("", d[2].getParentNameYear());
		Assert.assertEquals("", d[3].getParentNameYear());
		Assert.assertEquals("", d[4].getParentNameYear());
		Assert.assertEquals("", d[5].getParentNameYear());
		Assert.assertEquals("1983", d[6].getParentNameYear());
		Assert.assertEquals("1999", d[7].getParentNameYear());
		Assert.assertEquals("1995", d[8].getParentNameYear());
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
		Assert.assertEquals("", d[19].getParentNameYear());
		Assert.assertEquals("", d[20].getParentNameYear());
		Assert.assertEquals("", d[21].getParentNameYear());
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
		Assert.assertEquals("", d[38].getParentNameYear());
		Assert.assertEquals("", d[39].getParentNameYear());
		Assert.assertEquals("", d[40].getParentNameYear());
		Assert.assertEquals("", d[41].getParentNameYear());
		Assert.assertEquals("", d[42].getParentNameYear());
		Assert.assertEquals("", d[43].getParentNameYear());
		Assert.assertEquals("", d[44].getParentNameYear());
		Assert.assertEquals("", d[45].getParentNameYear());
		Assert.assertEquals("", d[46].getParentNameYear());
		Assert.assertEquals("", d[47].getParentNameYear());
		Assert.assertEquals("", d[48].getParentNameYear());
		Assert.assertEquals("", d[49].getParentNameYear());
		Assert.assertEquals("", d[50].getParentNameYear());
		Assert.assertEquals("", d[51].getParentNameYear());
		Assert.assertEquals("", d[52].getParentNameYear());
		Assert.assertEquals("", d[53].getParentNameYear());
		Assert.assertEquals("", d[54].getParentNameYear());
		Assert.assertEquals("", d[55].getParentNameYear());		
		Assert.assertEquals("", d[56].getParentNameYear());
		Assert.assertEquals("", d[57].getParentNameYear());
		Assert.assertEquals("", d[58].getParentNameYear());
		Assert.assertEquals("", d[59].getParentNameYear());
		Assert.assertEquals("", d[60].getParentNameYear());
		Assert.assertEquals("", d[61].getParentNameYear());
		Assert.assertEquals("", d[62].getParentNameYear());
		Assert.assertEquals("", d[63].getParentNameYear());
		Assert.assertEquals("", d[64].getParentNameYear());
		Assert.assertEquals("", d[65].getParentNameYear());
		Assert.assertEquals("", d[66].getParentNameYear());
		Assert.assertEquals("", d[67].getParentNameYear());
		Assert.assertEquals("", d[68].getParentNameYear());
		Assert.assertEquals("", d[69].getParentNameYear());
		Assert.assertEquals("", d[70].getParentNameYear());
		Assert.assertEquals("", d[71].getParentNameYear());
		Assert.assertEquals("", d[72].getParentNameYear());
		Assert.assertEquals("", d[73].getParentNameYear());
		Assert.assertEquals("", d[74].getParentNameYear());
		Assert.assertEquals("", d[75].getParentNameYear());
		Assert.assertEquals("", d[76].getParentNameYear());
		Assert.assertEquals("", d[77].getParentNameYear());
		Assert.assertEquals("", d[78].getParentNameYear());
		Assert.assertEquals("", d[79].getParentNameYear());
		Assert.assertEquals("", d[80].getParentNameYear());
		Assert.assertEquals("", d[81].getParentNameYear());
		Assert.assertEquals("", d[82].getParentNameYear());
		Assert.assertEquals("", d[83].getParentNameYear());
		Assert.assertEquals("", d[84].getParentNameYear());
		Assert.assertEquals("", d[85].getParentNameYear());
		Assert.assertEquals("2011", d[86].getParentNameYear());
		Assert.assertEquals("1937", d[87].getParentNameYear());
		Assert.assertEquals("2012", d[88].getParentNameYear());
		Assert.assertEquals("1935", d[89].getParentNameYear());
		Assert.assertEquals("1977", d[90].getParentNameYear());
		Assert.assertEquals("2012", d[91].getParentNameYear());
		Assert.assertEquals("", d[92].getParentNameYear());
		Assert.assertEquals("2004", d[93].getParentNameYear());
		Assert.assertEquals("1995", d[94].getParentNameYear());
		Assert.assertEquals("", d[95].getParentNameYear());
		Assert.assertEquals("2011", d[96].getParentNameYear());
		Assert.assertEquals("", d[97].getParentNameYear());
		Assert.assertEquals("2010", d[98].getParentNameYear());
		Assert.assertEquals("2010", d[99].getParentNameYear());
		Assert.assertEquals("2010", d[100].getParentNameYear());
		Assert.assertEquals("2012", d[101].getParentNameYear());
		Assert.assertEquals("2012", d[102].getParentNameYear());
		Assert.assertEquals("2009", d[103].getParentNameYear());
		Assert.assertEquals("2010", d[104].getParentNameYear());
		Assert.assertEquals("2008", d[105].getParentNameYear());
		Assert.assertEquals("2010", d[106].getParentNameYear());
		Assert.assertEquals("", d[107].getParentNameYear());
		Assert.assertEquals("", d[108].getParentNameYear());
		Assert.assertEquals("", d[109].getParentNameYear());
		Assert.assertEquals("", d[110].getParentNameYear());
	}
	
	public void testCustomTags() {
		DecryptedMovie mMovie = MizLib.decryptMovie(TEST_FILENAMES[0], "avatar");
		Assert.assertEquals("", mMovie.getDecryptedFileName());
		
		mMovie = MizLib.decryptMovie(TEST_FILENAMES[0], "tar");
		Assert.assertEquals("Ava", mMovie.getDecryptedFileName());
	}
	
	public void testPartNumbers() {
		Assert.assertEquals(1, MizLib.getPartNumberFromFilepath("Avatar part1.mkv"));
		Assert.assertEquals(2, MizLib.getPartNumberFromFilepath("Avatar part2.mkv"));
		Assert.assertEquals(1, MizLib.getPartNumberFromFilepath("Avatar cd1.mkv"));
		Assert.assertEquals(2, MizLib.getPartNumberFromFilepath("Avatar cd2.mkv"));
		Assert.assertEquals(5, MizLib.getPartNumberFromFilepath("Avatar.cd5.mkv"));
		Assert.assertEquals(5, MizLib.getPartNumberFromFilepath("Avatar.part5.mkv"));
	}
}