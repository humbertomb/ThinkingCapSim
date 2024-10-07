/**
 * Title: UTMPos
 * Description: Posicion en coordenadas UTM
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Qui–onero
 * @author Humberto Martinez Barbera
 * @version 1.0
 */

package devices.pos;

import java.io.*;

public class UTMPos extends GeoPos implements Serializable
{
	protected int			znumber;		// Zone number
	protected String		zletter;		// Zone letter

	public UTMPos ()
	{
		setEastNorth (0.0, 0.0, "30-S");
	}

	public UTMPos (UTMPos other)
	{
		set (other);
	}

	public UTMPos (double east, double north, String zone)
	{
		setEastNorth (east, north, zone);
	}

	// Accessors
	public final double		getEast ()					{ return x; }
	public final double		getNorth ()					{ return y; }
	
	public String			getZone ()					{ return znumber+"-"+zletter; }
	public int				getZoneNumber ()			{ return znumber; }
	public String			getZoneLetter ()			{ return zletter; }	
	public double			getCentralMeridian ()		{ return (double) (znumber-1)*6.0-180.0 + 3.0; }

	public void				setEast (double east)		{ x = east; }
	public void				setNorth (double north)		{ y = north; }

	// Instance methods
	public void set (UTMPos pos)
	{
		setEast (pos.getEast ());
		setNorth (pos.getNorth ());
		setZone (pos.getZone ());
	}
	
	public void setEastNorth (double easting, double northing, String zone)
	{
		setEast (easting);
		setNorth (northing);
		setZone (zone);
	}
	
	public void setZone (String zone)
	{
		int			ndx;

		if (zone == null)			return;
		
		ndx		= zone.indexOf ("-");
		znumber	= Integer.valueOf (zone.substring (0, ndx)).intValue();
		zletter	= zone.substring (ndx+1, zone.length ());
	}
	
	public String toMapStr ()
	{
		return (int) getEast () +", "+ (int) getNorth () + ", " + getZone ();
	}
								
	public String toString ()
	{
		return "["+ getEast () +", "+ getNorth () +"] Zone: " + getZone ();
	}
}