/*
 * (c) 2000-2001 Juan Pedro Canovas
 * (c) 2004 Humberto Martinez
 */

package devices.data;

import java.io.*;

public class SatelliteData implements Serializable
{
	public int					prn			= -1;			// Satellite identification
	public boolean				valid		= false;
	public double				elevation;					// Elevation angle (deg)
	public double				azimuth;					// Azimuth angle (deg)
	public int					snr;						// Signal to noise ratio
	
  	// Constructors
	public SatelliteData ()
	{	
	}
	
	public SatelliteData (int prn, double elevation, double azimuth, int snr)
	{
		set (prn, elevation, azimuth, snr, true);
	}
	
	// Instance methods
	public void set (int prn, double elevation, double azimuth, int snr, boolean valid)
	{
		this.prn		= prn;
		this.elevation	= elevation;
		this.azimuth	= azimuth;
		this.snr		= snr;
		this.valid		= valid;
	}

	public String toString ()
	{
		return "[SAT] PRN: "+prn+", elev: "+elevation+", azimuth: "+azimuth+", snr: "+snr;
	}
}