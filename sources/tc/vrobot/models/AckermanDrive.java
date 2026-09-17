/*
 * Created on 04-sep-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;
import wucore.utils.math.Angles;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AckermanDrive extends RobotModel
{
	// Kynematics outputs
 	public transient double		vm, del;			// Ackerman model (forward speed and signus, wheel angle)

	// Dymanics simulation
	protected transient double	lvm, ldel;		// Ackerman model (previously issued control commands)

	// Parameters
	public double				l;				// Distance between wheel axles (m)

	// Maximum values of kynematics parameters
	private double				SAmax;			// Maximum steering-wheel angular velocity (rad/s)
	private transient double	STRmax; 			// Maximum steering angle (rad), from how fast it turns

	// Constructors
	public AckermanDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public AckermanDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);

		// Initialise simulation parameters
		lvm		= 0.0;
		ldel		= 0.0;
	}
	
	// Instance methdos
	public void update (Properties props)
	{
		super.update (props);
		
		try { SAmax	 = Double.valueOf (props.getProperty ("SAMAX")).doubleValue () * Angles.DTOR; } 	catch (Exception e) 		{ SAmax			= 360.0 * Angles.DTOR; }
		try { l		= Double.valueOf (props.getProperty ("LENGHT")).doubleValue (); } 				catch (Exception e) 		{ l				= 0.0; }

		// How far the wheel goes over is what turning as fast as it can asks of it:
		// Rmax = tan (STRmax) * Vmax / l.  Nothing to go on: hard over at a right angle.
		if ((Vmax > 0.0) && (l > 0.0))		STRmax	= Math.atan (Rmax * l / Vmax);
		else								STRmax	= 90.0 * Angles.DTOR;
	}
	
	public void kynematics_direct (double vm, double del)
	{
		double			dmax;
		
		this.vm		= vm;
		this.del		= del;
		
		// Check for dynamics constraints
		dmax		= (SAmax * rdesc.DTIME / 1000.0);
		if (del - ldel > dmax)
			del		= ldel + dmax;
		else if (del - ldel < -dmax)
			del		= ldel - dmax;

		// Compute direct kynematics
		wr		= Math.tan (del) * vm / l;
		vr		= vm;			
		
		ldel		= del;
		lvm		= vm;
	}
	
	public void kynematics_inverse (double speed, double turn)
	{
		// Set motion commands into the correct range
		vr	= Math.min (Math.max (speed, -Vmax), Vmax);
		wr	= Math.min (Math.max (turn, -Rmax), Rmax);

		// Compute robot command (inverse kynematics)
		if( (vr == 0.0))
		{
			vm = 0.0;
			del = ldel;
		}
		else 
		{
			vm	= vr;
			del = Math.atan (wr * l / vr);
		}

		// Check for kynematics constraints
		vm	= Math.min (Math.max (vm, -Vmax), Vmax);			// the motor takes it no faster than it goes
		del	= Math.min (Math.max (del, -STRmax), STRmax);
	}
	
	public void kynematics_simulation (double speed, double turn)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (speed, turn);
		
		// Compute robot displacement (direct kynematics)
		kynematics_direct (vm, del);
	}
}
