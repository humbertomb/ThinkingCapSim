/*
 * Created on 19-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.data;

import devices.pos.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TracklogPoint
{
	public LLAPos				pos;					// Trk point position (LLA)
	public double				height		= 0.0;		// Trk point height (m)
	public double				depth		= 0.0;		// Trk point depth (m)
	public long					time		= 0;		// Trk point acquisition time (s)
	public boolean				newseg;					// Trk point starts a new segment?
	
	// TODO convert time form seconds since UTC 12:00am 1989-12-31
	
	// Constructor
	public TracklogPoint ()
	{
		pos			= new LLAPos ();
	}

	// Instance methods
	public void set (double lat, double lon, double height, long time, boolean newseg)
	{
		pos.setLatLon (lat, lon);
		
		this.height = height;
		this.time	= time;
		this.newseg	= newseg;
	}
	
	public String toString ()
	{
		return "Time=<"+time+"> Pos="+pos;
	}
}
