/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;

public class ItemCamera extends Item implements Serializable
{
	// Traction and steering control mode
	public static final int		 	CTRL_NONE		= 0;
	public static final int		 	CTRL_AUTO		= 1;
	public static final int		 	CTRL_MANUAL		= 2;	
	
	// Camera control
	public double					pan;				// Pan movement control
	public double					tilt;				// Tilt movement control
	public double					zoom;				// Zoom aperture control

	// Constructors
	public ItemCamera () 
	{
		this.set (0.0, 0.0, 0.0, 0);
	}	
	
	public ItemCamera (double pan, double tilt, double zoom, long tstamp) 
	{
		this.set (pan, tilt, zoom, tstamp);
	}	
	
	// Initialisers
	public void set (double pan, double tilt, double zoom, long tstamp)
	{
		set (tstamp);
		
		this.pan		= pan;
		this.tilt		= tilt;
		this.zoom		= zoom;
	}
	
	public String toString ()
	{
		return "pan=" + pan + ", tilt=" + tilt + ", zoom=" + zoom;
	}	
}
