/*
 * (c) 2000-2001 Juan Pedro Canovas
 * (c) 2004 Humberto Martinez
 */

package devices.data;

import java.io.*;

import devices.pos.*;
import wucore.utils.geom.*;

public class GPSData implements Serializable, LoggableData
{
	static public final int			DATALOG			= 9;
	
	// Position fix definitions
	static public final int			FIX_ERROR		= 0;
	static public final int			FIX_INVALID		= 1;
	static public final int			FIX_2D			= 2;
	static public final int			FIX_3D			= 3;
	static public final int			FIX_WAAS_2D		= 4;
	static public final int			FIX_WAAS_3D		= 5;
	static public final int			FIX_VALID		= 6;

	// GPS data
	protected UTMPos				pos;		// GPS position		
	protected double				hgt;		// GPS height
	protected Point3				vel;		// GPS velocities (m/s)
	protected int					tow;		// Time of week (s)
	protected int					fix;		// Satellite fix
	protected int					num;		// Number of sattellites tracked

	// Utility variables
	private String					ellipsoid = "WGS-84";
	private GeoPos					utmt;	// LLA to UTM translator
	private double[]				data;	// Data storage for logging

	public GPSData ()
	{
		pos		= new UTMPos ();
		vel		= new Point3 ();
		data	= new double[DATALOG];
	}

	public GPSData (String ellipsoid)
	{
		this ();
		
		this.ellipsoid = ellipsoid;
	}

	// Class methods
  
	// ------------------------------
	 // GPS DATA UTILITIES
	 // ------------------------------
   
	 /** Returns the hour of the day. */
	 static public int getHours (int tow) 
	 {
		 int hour = tow;
		 hour = hour % (24 * 60 * 60);
		 hour = hour / 3600;
		 return hour;
	 }
	
	 /** Returns the minute of the hour. */
	 static public int getMinutes (int tow) 
	 {
		 int minute = tow;
		 minute = minute % (24 * 60 * 60);
		 minute = minute / 60;
		 minute = minute % 60;
		 return minute;
	 }
	
	 /** Returns the second of the minute. */
	static public int getSeconds (int tow) 
	{
		return tow % 60;
	}
	
	static public String fixToString (int fix)
 	{
 		String			text;
 		
		switch (fix)
		{
		case FIX_ERROR: 
			text	= "GPS error";
			break;

		case FIX_INVALID: 
			text	= "Invalid Position";
			break;

		case FIX_2D: 
			text	= "2D Position";
			break;

		case FIX_3D: 
			text	= "3D Position";
			break;

		case FIX_WAAS_2D: 
			text	= "2D WAAS Position";
			break;

		case FIX_WAAS_3D: 
			text	= "3D WAAS Position";
			break;

		case FIX_VALID: 
			text	= "Valid Position";
			break;

		default:
			text	= "Incorrect fix code <"+fix+">";
		}
		
		return text;
 	}
 
	// Accessors	
	public final UTMPos		getPos ()					{ return pos; }
	public final double		getAltitude ()				{ return hgt; }
	public final Point3		getVel ()					{ return vel; }
	public final int		getWeekSeconds ()			{ return tow; }
	public final int		getFix ()					{ return fix; }
	public final int		getNumSat ()				{ return num; }

	public final void		setPos (UTMPos pos)			{ this.pos.set (pos); }
	public final void		setAltitude (double hgt)	{ this.hgt = hgt; }
	public final void		setVel (Point3 vel)			{ this.vel.set (vel); }
	public final void		setWeekSeconds (int tow)	{ this.tow = tow; }
	public final void		setFix (int fix)			{ this.fix = fix; }
	public final void		setNumSat (int num)			{ this.num = num; }
	
	// Instance methods
	public void setVel (double vx, double vy, double vz)
	{
		vel.set (vx, vy, vz);	
	}
	
	public void setPos (LLAPos gpos)
	{
		pos.set (GeoPos.LLtoUTM (gpos, ellipsoid));	
	}
	
	public void set (GPSData data)
	{
		pos.set (data.pos);
		vel.set (data.vel);
		
		tow		= data.tow;
		fix		= data.fix;
		num		= data.fix;
	}
	
	public double[]	toDatalog ()
	{ 
		data[0]		= pos.getEast ();
		data[1]		= pos.getNorth ();
		data[2]		= hgt;
		data[3]		= vel.x ();
		data[4]		= vel.y ();
		data[5]		= vel.z ();
		data[6]		= tow;
		data[7]		= fix;
		data[8]		= num;
		
		return data;
	}
	
	public void fromDatalog (double[] datalog)
	{
		pos.setEast (datalog[0]);
		pos.setNorth (datalog[1]);
		pos.setZone ("30-S");					// TODO This doesn't work on other zones
		hgt		= datalog[2];
		vel.set (datalog[3], datalog[4], datalog[5]);
		tow		= (int) datalog[6];
		fix		= (int) datalog[7];
		num		= (int) datalog[8];
	}
}