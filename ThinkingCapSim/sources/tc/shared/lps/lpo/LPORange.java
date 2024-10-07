/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

public abstract class LPORange extends LPO implements Serializable
{
	// Object range data
	protected SensorPos				spos;
	protected double					len;

	// Constructor
	protected LPORange ()
	{
	}
	
	protected LPORange (SensorPos spos, String label, int source)
	{
		super (0.0, 0.0, 0.0, label, source);
		
		this.spos	= spos;
	}
	
	// Accessors
	public void				len (double len)			{ this.len = len; }
	public double			len ()					{ return len; }
	
	// Instance methods
	public void update (double range, boolean valid)
	{
		double		cx, cy;
		
		cx	= spos.x () + range * Math.cos (spos.alpha ());
		cy	= spos.y () + range * Math.sin (spos.alpha ());
		
		locate (cx, cy, 0.0, range);
		active (valid);
	}
		
	public void locate (double x, double y, double alpha, double len)
	{
		locate (x, y, alpha);
		this.len	= len;
	}
}


