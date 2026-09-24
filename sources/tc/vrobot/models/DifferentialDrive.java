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
public class DifferentialDrive extends RobotModel
{
	// Kynematics outputs
	public transient double		dVr, dVl;		// Differential drive model (right and left wheel speed)

	// Parameters
	public double		Dn;				// Wheel diameter (m)
	public double		Ce;				// Encoder resolution (pulses/revolution)
	public double		Gn;				// Gear ratio (a:b)
	public double		b;				// Wheel base (m)	Differential => Distance between wheels

	public transient double		Cm;				// Conversion factor (odometry)
	public transient double		Cf;				// Conversion factor (motor)
	public transient double		Va;				// Maximum angular speed (revolution/s)

	// Constructors
	public DifferentialDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public DifferentialDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);
	}
	
	// Instance methods
	public void update (Properties props)
	{
		super.update (props);
		
		try { Dn		= Double.valueOf (props.getProperty ("WHEEL")).doubleValue (); } 		catch (Exception e) 		{ }
		try { Ce		= Double.valueOf (props.getProperty ("PULSES")).doubleValue (); } 		catch (Exception e) 		{ }
		try { b		= Double.valueOf (props.getProperty ("BASE")).doubleValue (); } 		catch (Exception e) 		{ }
		try { Gn	 	= Double.valueOf (props.getProperty ("GEAR")).doubleValue (); } 		catch (Exception e) 		{ }

		// Robot kinematics parameters
		Cm		= (Math.PI * Dn) / (Ce * Gn);
		Va		= Vmax / (Math.PI * Dn);
		Cf		= Va *  (Ce * Gn) / Vmax;			// a wheel goes as fast as the platform does
	}
	
	public void kynematics_direct (double dVl, double dVr)
	{
		this.dVl		= dVl;
		this.dVr		= dVr;
		
		vr			= (dVr + dVl) / 2.0;
		wr			= (dVr - dVl) / b;
	}
	
	public void kynematics_inverse (double vlin, double vlat, double vrot)
	{
		// Set motion commands into the correct range: its wheels point where they are
		// built, so there is no going sideways
		vr	= Math.min (Math.max (vlin, -Vmax), Vmax);
		ur	= noLateral (vlat);
		wr	= Math.min (Math.max (vrot, -Rmax), Rmax);

		// Compute robot command (inverse kynematics)
		dVr	= (vr + b * wr * 0.5);
		dVl	= (vr - b * wr * 0.5);

		// Check for kynematics constraints
		dVr	= Math.min (Math.max (dVr, -Vmax), Vmax);
		dVl	= Math.min (Math.max (dVl, -Vmax), Vmax);
	}

	public void kynematics_simulation (double vlin, double vlat, double vrot)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (vlin, vlat, vrot);
		
		// Compute robot displacement (direct kynematics)
		kynematics_direct (dVl, dVr);
	}
}
