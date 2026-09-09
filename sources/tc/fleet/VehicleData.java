/*
 * (c) 2001-2003 Humberto Martinez
 */
 
package tc.fleet;

import java.io.*;

import devices.pos.*;

public class VehicleData extends Object implements Serializable 
{
	// Vehicle corrected position
	public Position				cur;					// Current coordinates (m, m, rad)
	public Pose					pose;					// INS-based coordinates (rad, rad, rad)
	public double				qlty;					// Quality of current position
	
	// Vehicle payload data
	public PayloadData			payload;				// Payload data
	
	protected boolean			initialised;
	
	/* Constructors */
	public VehicleData ()
	{
		initialised	= false;							// TODO the initialisation is quite tricky
	}
	
	// Instance methods
	public void update (Position ncur, Pose npose, double nqlty)
	{
		if (!initialised)
		{
			cur				= new Position ();
			pose			= new Pose ();
			
			initialised		= true;
		}

		if (ncur != null)		cur.set (ncur);
		if (npose != null)		pose.set (npose);
		qlty	= nqlty;
	}
	
	public void update (Position ncur, Pose npose, double nqlty, PayloadData npayload)
	{
		if (!initialised)
			payload			= new PayloadData ();
		
		if (npayload != null)	payload.set (npayload);
		
		update (ncur, npose, nqlty);
	}
} 