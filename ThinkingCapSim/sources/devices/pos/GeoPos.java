/**
 * Title: GeoPos
 * Description: Clase para realizar transformaciones entre coordenadas LaLong y UTM
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.pos;

import wucore.utils.math.*;

public abstract class GeoPos
{		
	static private LLAPos			llapos = new LLAPos ();
	static private UTMPos			utmpos = new UTMPos ();
	
	protected double				x;
	protected double				y;

	// ****************************************
	// GENERAL METHODS
	// ****************************************
	public abstract String	toMapStr ();
	
	// ****************************************
	// STATIC METHODS
	// ****************************************
	
	static public final UTMPos LLtoUTM (LLAPos pos, String datum)
	{
		return LLtoUTM (pos.getLatitude(), pos.getLongitude(), datum);
	}
	
	static public final UTMPos LLtoUTM (double lat, double lon, String datum)
	{
		//converts lat/long to UTM coords.  Equations from USGS Bulletin 1532
		//East Longitudes are positive, West longitudes are negative.
		//North latitudes are positive, South latitudes are negative
		//Lat and Long are in decimal degrees
		//Written by Chuck Gantz- chuck.gantz@globalstar.com
		
		int ellipsoid = Ellipsoids.parseName (datum);
		double a = Ellipsoids.ellipsoid[ellipsoid].equatorialRadius;
		double eccSquared = Ellipsoids.ellipsoid[ellipsoid].eccentricitySquared;
		double k0 = 0.9996;
		double LongOrigin;
		double eccPrimeSquared;
		double N, T, C, A, M;
		
		//Make sure the longitude is between -180.00 .. 179.9
		double LongTemp = (lon+180)-(int)((lon+180)/360)*360-180; // -180.00 .. 179.9;
		
		double LatRad = lat*Angles.DTOR;
		double LongRad = LongTemp*Angles.DTOR;
		double LongOriginRad;
		int    number;
		double northing;
		double easting;
		String zone;
		
		number = (int)((LongTemp + 180)/6) + 1;
		
		if( lat >= 56.0 && lat < 64.0 && LongTemp >= 3.0 && LongTemp < 12.0 )
			number = 32;
		
		// Special zones for Svalbard
		if( lat >= 72.0 && lat < 84.0 )
		{
			if(      LongTemp >= 0.0  && LongTemp < 9.0 ) number = 31;
			else if( LongTemp >= 9.0  && LongTemp < 21.0 ) number = 33;
			else if( LongTemp >= 21.0 && LongTemp < 33.0 ) number = 35;
			else if( LongTemp >= 33.0 && LongTemp < 42.0 ) number = 37;
		}
		
		LongOrigin = (number - 1)*6 - 180 + 3;  //+3 puts origin in middle of zone
		LongOriginRad = LongOrigin * Angles.DTOR;
		
		//compute the UTM Zone from the latitude and longitude
		zone = String.valueOf(number)+"-"+UTMLetterDesignator(lat);
		
		//sprintf(UTMZone, "%d%c", ZoneNumber, UTMLetterDesignator(Lat));
		
		eccPrimeSquared = (eccSquared)/(1-eccSquared);
		
		N = a/Math.sqrt(1-eccSquared*Math.sin(LatRad)*Math.sin(LatRad));
		T = Math.tan(LatRad)*Math.tan(LatRad);
		C = eccPrimeSquared*Math.cos(LatRad)*Math.cos(LatRad);
		A = Math.cos(LatRad)*(LongRad-LongOriginRad);
		
		M = a*((1 - eccSquared/4 - 3*eccSquared*eccSquared/64 - 5*eccSquared*eccSquared*eccSquared/256)*LatRad
				- (3*eccSquared/8 + 3*eccSquared*eccSquared/32	+ 45*eccSquared*eccSquared*eccSquared/1024)*Math.sin(2*LatRad)
				+ (15*eccSquared*eccSquared/256 + 45*eccSquared*eccSquared*eccSquared/1024)*Math.sin(4*LatRad)
				- (35*eccSquared*eccSquared*eccSquared/3072)*Math.sin(6*LatRad));
		
		easting = (double)(k0*N*(A+(1-T+C)*A*A*A/6 + (5-18*T+T*T+72*C-58*eccPrimeSquared)*A*A*A*A*A/120)
				+ 500000.0);
		
		northing = (double)(k0*(M+N*Math.tan(LatRad)*(A*A/2+(5-T+9*C+4*C*C)*A*A*A*A/24
				+ (61-58*T+T*T+600*C-330*eccPrimeSquared)*A*A*A*A*A*A/720)));
		if(lat < 0)
			northing += 10000000.0; //10000000 meter offset for southern hemisphere
		
		utmpos.setEastNorth (easting, northing, zone);
		
		return utmpos;
	}
	
	static public final String UTMLetterDesignator (double lat)
	{
		//This routine determines the correct UTM letter designator for the given latitude
		//returns 'Z' if latitude is outside the UTM limits of 84N to 80S
		//Written by Chuck Gantz- chuck.gantz@globalstar.com
		char		letter;
		
		if((84 >= lat) && (lat >= 72)) letter = 'X';
		else if((72 > lat) && (lat >= 64)) letter = 'W';
		else if((64 > lat) && (lat >= 56)) letter = 'V';
		else if((56 > lat) && (lat >= 48)) letter = 'U';
		else if((48 > lat) && (lat >= 40)) letter = 'T';
		else if((40 > lat) && (lat >= 32)) letter = 'S';
		else if((32 > lat) && (lat >= 24)) letter = 'R';
		else if((24 > lat) && (lat >= 16)) letter = 'Q';
		else if((16 > lat) && (lat >= 8)) letter = 'P';
		else if(( 8 > lat) && (lat >= 0)) letter = 'N';
		else if(( 0 > lat) && (lat >= -8)) letter = 'M';
		else if((-8> lat) && (lat >= -16)) letter = 'L';
		else if((-16 > lat) && (lat >= -24)) letter = 'K';
		else if((-24 > lat) && (lat >= -32)) letter = 'J';
		else if((-32 > lat) && (lat >= -40)) letter = 'H';
		else if((-40 > lat) && (lat >= -48)) letter = 'G';
		else if((-48 > lat) && (lat >= -56)) letter = 'F';
		else if((-56 > lat) && (lat >= -64)) letter = 'E';
		else if((-64 > lat) && (lat >= -72)) letter = 'D';
		else if((-72 > lat) && (lat >= -80)) letter = 'C';
		else letter = 'Z'; //This is here as an error flag to show that the Latitude is outside the UTM limits
		
		return String.valueOf (letter);
	}
	
	static public final LLAPos UTMtoLL (UTMPos pos, String datum)
	{
		return UTMtoLL (pos.getEast(), pos.getNorth(), pos.getZoneNumber(), pos.getZoneLetter(), datum);
	}
	
	static public final LLAPos UTMtoLL (double east, double north, String zone, String datum)
	{
		int			ndx, znumber;
		String 		zletter;
		
		ndx		= zone.indexOf ("-");
		znumber	= Integer.valueOf (zone.substring (0, ndx)).intValue();
		zletter	= zone.substring (ndx+1, zone.length ());

		return UTMtoLL (east, north, znumber, zletter, datum);
	}
	
	static public final LLAPos UTMtoLL (double east, double north, int number, String letter, String datum)
	{
		//converts UTM coords to lat/long.  Equations from USGS Bulletin 1532
		//East Longitudes are positive, West longitudes are negative.
		//North latitudes are positive, South latitudes are negative
		//Lat and Long are in decimal degrees.
		//Written by Chuck Gantz- chuck.gantz@globalstar.com
		
		double k0 = 0.9996;
		int ellipsoid = Ellipsoids.parseName (datum);
		double a = Ellipsoids.ellipsoid[ellipsoid].equatorialRadius;
		double eccSquared = Ellipsoids.ellipsoid[ellipsoid].eccentricitySquared;
		double eccPrimeSquared;
		double e1 = (1-Math.sqrt(1-eccSquared))/(1+Math.sqrt(1-eccSquared));
		double N1, T1, C1, R1, D, M;
		double LongOrigin;
		double mu, phi1Rad;
		char zone = letter.charAt (0);		
		double lat, lon;
		
		east = east - 500000.0; //remove 500,000 meter offset for longitude
				
		//if((*ZoneLetter - 'N') >= 0)
		if (zone - 'N' < 0)
			north -= 10000000.0;//remove 10,000,000 meter offset used for southern hemisphere   }
		
		LongOrigin = (number - 1)*6 - 180 + 3;  //+3 puts origin in middle of zone
		
		eccPrimeSquared = (eccSquared)/(1-eccSquared);
		
		M = north / k0;
		mu = M/(a*(1-eccSquared/4-3*eccSquared*eccSquared/64-5*eccSquared*eccSquared*eccSquared/256));
		
		phi1Rad = mu + (3*e1/2-27*e1*e1*e1/32)*Math.sin(2*mu)
		+ (21*e1*e1/16-55*e1*e1*e1*e1/32)*Math.sin(4*mu)
		+(151*e1*e1*e1/96)*Math.sin(6*mu);
		
		N1 = a/Math.sqrt(1-eccSquared*Math.sin(phi1Rad)*Math.sin(phi1Rad));
		T1 = Math.tan(phi1Rad)*Math.tan(phi1Rad);
		C1 = eccPrimeSquared*Math.cos(phi1Rad)*Math.cos(phi1Rad);
		R1 = a*(1-eccSquared)/Math.pow(1-eccSquared*Math.sin(phi1Rad)*Math.sin(phi1Rad), 1.5);
		D = east/(N1*k0);
		
		lat = phi1Rad - (N1*Math.tan(phi1Rad)/R1)*(D*D/2-(5+3*T1+10*C1-4*C1*C1-9*eccPrimeSquared)*D*D*D*D/24
				+(61+90*T1+298*C1+45*T1*T1-252*eccPrimeSquared-3*C1*C1)*D*D*D*D*D*D/720);
		lat = lat * Angles.RTOD;
		
		lon = (D-(1+2*T1+C1)*D*D*D/6+(5-2*C1+28*T1-3*C1*C1+8*eccPrimeSquared+24*T1*T1)
				*D*D*D*D*D/120)/Math.cos(phi1Rad);
		lon = LongOrigin + lon * Angles.RTOD;
		
		llapos.setLatLon (lat, lon);
		return llapos;
	}
	
	static public final double UTMwidth (UTMPos pos)
	{		
		double lat, width;
		double lonm, lonw;
		
		lonm	= pos.getCentralMeridian ();
		lonw	= lonm - 3.0;	
		lat		= UTMtoLL (pos, "WGS-84").getLatitude ();
		width	= new LLAPos (lat, lonm).sphericalDistance (new LLAPos (lat, lonw));
		
		return Math.abs (width) * 2.0;
	}
}