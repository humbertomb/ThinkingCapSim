/**
 * Title: GeoPos
 * Description: Clase para realizar transformaciones entre coordenadas LaLong y UTM
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.pos;

public class Ellipsoids extends Object
{
/*
# Common Ellipsoids
# Datum derived from file from US Army Corps of Engineers Topographic
# Engineering Center (TEC).  www.tec.army.mil

Airy                                    AA 10 6377563.396 6356256.9090 299.3249646
Modified Airy                           AM 11 6377340.189 6356034.4480 299.3249646
Australian National                     AN 08 6378160     6356774.7190 298.25
Bessel 1841                             BR 05 6377397.155 6356078.9630 299.1528128
Bessel 1841(Namibia)                    BN 16 6377483.865 6356165.383  299.1528128
Clarke 1866                             CC 01 6378206.4   6356583.800  294.9786982
Clarke 1880                             CD 03 6378249.145 6356514.870  293.465
Everest                                 EA 04 6377276.345 6356075.413  300.8017
Everest (E. Malasia, Brunei)            EB 27 6377298.556 6356097.55   300.8017
Everest 1956 (India)                    EC 29 6377301.243 6356100.228  300.8017
Everest 1969 (West Malasia)             ED 12 6377295.664 6356094.668  300.8017
Everest 1948(W.Mals. & Sing.)           EE 30 6377304.063 6356103.039  300.8017
Everest (Pakistan)                      EF 28 6377309.613 6356109.571  300.8017
Mod. Fischer 1960(South Asia)           FA 18 6378155     6356773.320  298.3
GRS 80                                  RF 21 6378137     6356752.3141 298.257222101
Helmert 1906                            HE 22 6378200     6356818.170  298.3
Hough                                   HO 14 6378270     6356794.343  297
Indonesian 1974                         ID 31 6378160     6356774.504  298.247
International                           IN 02 6378388     6356911.946  297
Krassovsky                              KA 23 6378245     6356863.019  298.3
South American 1969                     SA 24 6378160     6356774.719  298.25
WGS 72                                  WD 13 6378135     6356750.520  298.26
WGS 84                                  WE 09 6378137     6356752.3142 298.257223563

 */
	static public final Ellipsoid		ellipsoid[] =
	{
		new Ellipsoid ("Airy", 6377563, 0.00667054),
		new Ellipsoid ("Australian National", 6378160, 0.006694542),
		new Ellipsoid ("Bessel 1841", 6377397, 0.006674372),
	    new Ellipsoid ("Bessel 1841 (Namibia) ", 6377484, 0.006674372),
	    new Ellipsoid ("Clarke 1866", 6378206, 0.006768658),
	    new Ellipsoid ("Clarke 1880", 6378249, 0.006803511),
	    new Ellipsoid ("Everest", 6377276, 0.006637847),
	    new Ellipsoid ("Fischer 1960 (Mercury)", 6378166, 0.006693422),
	    new Ellipsoid ("Fischer 1968", 6378150, 0.006693422),
	    new Ellipsoid ("GRS 1967", 6378160, 0.006694605),
	    new Ellipsoid ("GRS 1980", 6378137, 0.00669438),
	    new Ellipsoid ("Helmert 1906", 6378200, 0.006693422),
	    new Ellipsoid ("Hough", 6378270, 0.00672267),
	    new Ellipsoid ("International", 6378388, 0.00672267),
	    new Ellipsoid ("Krassovsky", 6378245, 0.006693422),
	    new Ellipsoid ("Modified Airy", 6377340, 0.00667054),
	    new Ellipsoid ("Modified Everest", 6377304, 0.006637847),
	    new Ellipsoid ("Modified Fischer 1960", 6378155, 0.006693422),
	    new Ellipsoid ("South American 1969", 6378160, 0.006694542),
	    new Ellipsoid ("WGS-60", 6378165, 0.006693422),
	    new Ellipsoid ("WGS-66", 6378145, 0.006694542),
	    new Ellipsoid ("WGS-72", 6378135, 0.006694318),
	    new Ellipsoid ("WGS-84", 6378137, 0.00669438)
	};
	
	// Class methods
	static public int parseName (String name)
	{
		int			i;
		
		for (i = 0; i < ellipsoid.length; i++)
			if (name.equals (ellipsoid[i].ellipsoidName))
				return i;
				
		return 0;
	}

	static public String toString (int id)
	{
		if ((id >= 0) && (id < ellipsoid.length))
			return ellipsoid[id].ellipsoidName;
			
		return "Unknown ellipsoid";
	}
}