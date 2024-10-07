/**
 * Encapsulates latitude and longitude coordinates in decimal degrees.
 * Normalizes the internal representation of latitude and longitude.
 * <p>
 * <strong>Normalized Latitude:</strong><br>
 * &minus;90&deg; &lt;= &phi; &lt;= 90&deg;
 * <p>
 * <strong>Normalized Longitude:</strong><br>
 * &minus;180&deg; &le; &lambda; &le; 180&deg;
 */

package devices.pos;

import java.io.*;

import wucore.utils.math.*;
import wucore.utils.string.*;

public class LLAPos extends GeoPos implements Serializable
{
	static public final double 				NORTH_POLE 		= 90.0;
	static public final double 				SOUTH_POLE 		= -NORTH_POLE;
	static public final double 				DATELINE 		= 180.0;

	static public final double				EQ_TOLERANCE	= 0.00001;

	static public final double				EQUATORIAL_RAD	= 6378000.0;		// Equatorial radius (m)
	static public final double				POLAR_RAD		= 6357000.0;		// Polar radius (m)
	static public final double				ECCENTRICITY	= Math.sqrt((Math.pow(EQUATORIAL_RAD,2)-Math.pow(POLAR_RAD,2))/Math.pow(EQUATORIAL_RAD,2));

	/**
	 * Used by the projection code for read-only quick access.
	 * This is meant for quick backdoor access by the projection library.
	 * Modify at your own risk!
	 */
	public transient double 				radlat_ 		= 0.0;
	public transient double 				radlon_ 		= 0.0;

	// Constructors
	/**
	 * Construct a LLAPos from raw float lat/lon in decimal degrees.
	 *
	 * @param lat latitude in decimal degrees
	 * @param lon longitude in decimal degrees
	 */
	public LLAPos ()
	{
		setLatLon (0.0, 0.0);
	}

	public LLAPos (double lat, double lon)
	{
		setLatLon (lat, lon);
	}

	public LLAPos (double lat, double lon, boolean isRadian)
	{
		setLatLon (lat, lon, isRadian);
	}

	public LLAPos (float lat, float lon)
	{
		setLatLon ((double) lat, (double) lon);
	}

	public LLAPos (LLAPos pt) 
	{
		set (pt);
	}

	// Class methods
	static public double getDegrees (double v)
	{ 
		return Math.floor (Math.abs (v)); 
	}

	static public double getMinutes (double v)
	{
		double			av, min;

		av		= Math.abs (v);
		min		= 60.0 * (av - Math.floor (av));
		return Math.round (min * 1000.0) / 1000.0; 
	}

	static public final String zeroFill (double v, int len1, int len2)
	{
		String			str;

		str		= Double.toString (v);
		while (str.indexOf ('.') < len1)
			str		= "0" + str;
		while (str.length () < len1+len2+1)
			str		+= "0";

		return str;
	}

	static public final String whiteFill (int v, int len)
	{
		String			str;

		str		= Integer.toString (v);
		while (str.length () < len)
			str		= " " + str;

		return str;
	}

	final public static double normalizeLatitude (double lat) 
	{
		if (lat > NORTH_POLE)
			lat = NORTH_POLE;
		if (lat < SOUTH_POLE)
			lat = SOUTH_POLE;

		return  lat;
	}

	public static boolean isInvalidLatitude (double lat) 
	{
		return  ((lat > NORTH_POLE) || (lat < SOUTH_POLE));
	}

	public static boolean isInvalidLongitude (double lon) 
	{
		return  ((lon < -DATELINE) || (lon > DATELINE));
	}

	// Accessors
	public final double		getLatitude ()					{ return y; }
	public final double		getLongitude ()					{ return x; }

	public final void		setLatitude (double lat)		{ y = normalizeLatitude (lat); radlat_ = (y * Angles.DTOR); }
	public final void		setLongitude (double lon)		{ x = Angles.degnorm_180 (lon); radlon_ = (x * Angles.DTOR); }

	// Instance methods
	public final void setLatLon (double lat, double lon) 
	{
		setLongitude (lon);
		setLatitude (lat);
	}

	public void setLatLon (double lat, double lon, boolean isRadian) 
	{
		if (isRadian) 
			setLatLon (lat * Angles.RTOD, lon * Angles.RTOD);
		else
			setLatLon (lat, lon);
	}

	public void set (LLAPos pos) 
	{
		setLatLon (pos.getLatitude (), pos.getLongitude ());
	}

	/**
	 * Determines whether two LLAPoss are equal.
	 * @param obj Object
	 * @return Whether the two points are equal up to a tolerance of
	 * 10<sup>-5</sup> degrees in latitude and longitude.
	 */
	public boolean equals (Object obj) 
	{
		if (obj instanceof LLAPos) 
		{
			LLAPos pt = (LLAPos) obj;
			return  (MoreMath.approximately_equal(y, pt.y, EQ_TOLERANCE) && MoreMath.approximately_equal (x, pt.x, EQ_TOLERANCE));
		}
		return  false;
	}

	public void translate (double dlat, double dlon)
	{
		setLatitude (y + dlat);
		setLongitude (x + dlon);
	}

	public final double rhumbDistance (LLAPos b) 
	{
		double		d, tc, dlon_W, dlon_E;

		dlon_W	= MoreMath.mod(b.radlon_ - radlon_, 2 * Math.PI);
		dlon_E	= MoreMath.mod(radlon_ - b.radlon_, 2 * Math.PI);
		tc		= rhumbAzimuth (b);
		if (Math.abs(radlat_ - b.radlat_) < Math.sqrt(0.00000000000001)) 
			d = Math.min(dlon_W, dlon_E) * Math.cos(radlat_); // distance along parallel
		else 
			d = Math.abs((b.radlat_ - radlat_) / Math.cos(tc));

		return d * 10800.0 / Math.PI * 1855.3;
	}

	/**
	 * Metoda pozwala obliczyæ azymut pomiêdzy punktami (namiar od
	 * pierwszego do drugiego)
	 */
	public final double rhumbAzimuth (LLAPos b) 
	{
		double		tc, dlon_W, dlon_E, dphi;

		dlon_W	= MoreMath.mod (b.radlon_ - radlon_, 2 * Math.PI);
		dlon_E	= MoreMath.mod (radlon_ - b.radlon_, 2 * Math.PI);
		dphi	= Math.log((1 + Math.sin(b.radlat_)) / Math.cos(b.radlat_)) - Math.log((1 + Math.sin(radlat_)) / Math.cos(radlat_));
		if (dlon_W < dlon_E)// West is the shortest
			tc = MoreMath.mod (Math.atan2(-dlon_W, dphi), 2 * Math.PI);
		else
			tc = MoreMath.mod (Math.atan2(dlon_E, dphi), 2 * Math.PI);

		return tc;
	}

	/**  Calculate distance (in m) between two points specified by latitude/longitude.
	 		from: Haversine formula - R. W. Sinnott, "Virtues of the Haversine",
	 		Sky and Telescope, vol 68, no 2, 1984
	 		http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1

	 	a = (sin(dlat/2))^2 + cos(lat1) * cos(lat2) * (sin(dlon/2))^2
	 	c = 2 * atan2( sqrt(a), sqrt(1-a) )
	 	d = r * c 

		r = eqr*sqrt(1-e^2)/(1-e^2*(sin(lat))^2)
	 */
	public final double sphericalDistance (LLAPos b)
	{
		double		dlong, dlat, alat;
		double		a, c, r;
//		double		ra, rb;

		dlong	= b.radlon_ - radlon_;
		dlat	= b.radlat_ - radlat_;
		a		= Math.pow(Math.sin(dlat/2),2) + Math.cos(radlat_) * Math.cos(b.radlat_) * Math.pow(Math.sin(dlong/2),2);
		c		= 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));

		alat	=  (getLatitude() + b.getLatitude()) * 0.5;		// Average latitude
		r		= EQUATORIAL_RAD * Math.sqrt (1.0 - Math.pow(ECCENTRICITY, 2)) / (1 - Math.pow(ECCENTRICITY, 2) * Math.pow (Math.sin (alat), 2));

//		ra		= EQUATORIAL_RAD *(1 - Math.pow(ECCENTRICITY,2)) / Math.sqrt(Math.pow(1 - Math.pow(ECCENTRICITY,2)*Math.pow(Math.sin(alat),2),3));
//		rb		= EQUATORIAL_RAD /Math.sqrt(1-Math.pow(ECCENTRICITY,2)*Math.pow(Math.sin(alat),2));	
//		r		= ra * (dlat / (dlat + dlong)) + rb * (dlong / (dlat + dlong));

		return r * c;
	}

	/**
	 * Calculate spherical azimuth between two points.
	 * <p>
	 * Computes the azimuth `Az' east of north from phi1, lambda0
	 * bearing toward phi and lambda. (5-4b).  (-PI &lt;= Az &lt;= PI).
	 * <p>
	 *
	 */
	public final double sphericalAzimuth (LLAPos b)
	{
		double ldiff = radlon_ - b.radlon_;
		double cosphi = Math.cos (b.radlat_);

		return Math.atan2 (cosphi*Math.sin(ldiff), (Math.cos(radlat_)*Math.sin(b.radlat_) -  Math.sin(radlat_)*cosphi* Math.cos(ldiff)));
	}

	/**
	 * Computes a spherical translation. Distance MUST be given in
	 * nautical miles. Heading in radians
	 */
	public final void sphericalTranslation (double head, double dist)
	{
		double		d;
		double		lat, lon, dlon;

		d		= dist * Units.NMTOARC * Angles.DTOR;
		lat		= Math.asin(Math.sin(radlat_)*Math.cos(d)+Math.cos(radlat_)*Math.sin(d)*Math.cos(head));
		dlon	= Math.atan2(Math.sin(head)*Math.sin(d)*Math.cos(radlat_),Math.cos(d)-Math.sin(radlat_)*Math.sin(lat));
		lon		= ((radlon_ - dlon + Math.PI) % Angles.PI2) - Math.PI;
		setLatLon (lat*Angles.RTOD, lon*Angles.RTOD);
	}

	public final void sphericalTranslation (LLAPos b, double head, double dist)
	{
		set (b);
		sphericalTranslation (head, dist);
	}

	public double sphericalWidth (LLAPos b) 
	{
		return sphericalDistance (new LLAPos (getLatitude(), b.getLongitude())); 
	}

	public double sphericalHeight (LLAPos b) 
	{
		return sphericalDistance (new LLAPos (b.getLatitude(), getLongitude())); 
	}

	/**
	 * Calculate great circle between two points on the sphere.
	 * <p>
	 * Folds all computation (distance, azimuth, points between) into
	 * one function for optimization. returns n or n+1 pairs of lat,lon
	 * on great circle between lat-lon pairs.
	 * <p>
	 * @param n number of segments
	 * @param include_last return n or n+1 segments
	 * @return float[n] or float[n+1] radian lat,lon pairs
	 *
	 */
	public void greatCircle (LLAPos b, LLAPath path)
	{
		// calculate a bunch of stuff for later use
		double cosphi = Math.cos(b.radlat_);
		double cosphi1 = Math.cos(radlat_);
		double sinphi1 = Math.sin(radlat_);
		double ldiff = b.radlon_ - radlon_;
		double p2diff = Math.sin(((b.radlat_-radlat_)/2));
		double l2diff = Math.sin((ldiff)/2);

		// calculate spherical distance
		double c = 2.0 * Math.asin(Math.sqrt(p2diff*p2diff + cosphi1*cosphi*l2diff*l2diff));

		// calculate spherical azimuth
		double Az = Math.atan2(cosphi*Math.sin(ldiff),(cosphi1*Math.sin(b.radlat_) -sinphi1*cosphi*Math.cos(ldiff)));
		double cosAz = Math.cos(Az);
		double sinAz = Math.sin(Az);

		// generate the great circle line
		double inc = c/(path.num()-2);
		double len = inc;
		path.at (0, this);
		for (int i = 1; i < path.num()-1; i++, len+=inc) 
		{
			double sinc = Math.sin(len);
			double cosc = Math.cos(len);
			double lat = Math.asin(sinphi1*cosc + cosphi1*sinc*cosAz);
			double lon = Math.atan2(sinc*sinAz, cosphi1*cosc - sinphi1*sinc*cosAz) + radlon_;

			path.at (i).setLatLon (lat*Angles.RTOD, lon*Angles.RTOD);
		}
		path.at (path.num()-1, b);
	}

	public final String getLatStr ()
	{
		return whiteFill ((int) getDegrees (y), 2) + Symbols.DEGREE + zeroFill (getMinutes (y), 2, 3) + "'" + (y < 0.0 ? "S" : "N");
	}

	public final String getLonStr ()
	{
		return whiteFill ((int) getDegrees (x), 3) + Symbols.DEGREE + zeroFill (getMinutes (x), 2, 3) + "'" + (x < 0.0 ? "W" : "E");
	}

	public String toMapStr ()
	{
		return getLatStr () + ", " + getLonStr ();
	}

	public String toString ()
	{
		return "[" + getLatStr () + ", " + getLonStr () +  "]";
	}
}



