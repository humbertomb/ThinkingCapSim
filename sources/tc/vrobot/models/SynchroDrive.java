/*
 * Created on 04-sep-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SynchroDrive extends RobotModel
{
	// Constructors
	public SynchroDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public SynchroDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);
	}
	
	// Instance methods	
	public void kynematics_direct (double vr, double wr)
	{
		this.vr		= vr;
		this.wr		= wr;
	}
	
	public void kynematics_inverse (double speed, double turn)
	{
		// Set motion commands into the correct range
		vr	= Math.min (Math.max (speed, -Vmax), Vmax);
		wr	= Math.min (Math.max (turn, -Rmax), Rmax);
	}
	
	public void kynematics_simulation (double speed, double turn)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (speed, turn);
		
		// Compute robot displacement (direct kynematics)
		kynematics_direct (vr, wr);
	}
}
