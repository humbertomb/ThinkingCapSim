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
	public double		dVr, dVl;		// Differential drive model (right and left wheel speed)

	// Parameters
	public double		Dn;				// Wheel diameter (m)
	public double		Ce;				// Encoder resolution (pulses/revolution)
	public double		Gn;				// Gear ratio (a:b)
	public double		b;				// Wheel base (m)	Differential => Distance between wheels

	public double		Cm;				// Conversion factor (odometry)
	public double		Cf;				// Conversion factor (motor)
	public double		Va;				// Maximum angular speed (revolution/s)

	public double		MOTmax; 			// Maximum traction speed (m/s)

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
		
		try { Dn		= new Double (props.getProperty ("WHEEL")).doubleValue (); } 		catch (Exception e) 		{ }
		try { Ce		= new Double (props.getProperty ("PULSES")).doubleValue (); } 		catch (Exception e) 		{ }
		try { b		= new Double (props.getProperty ("BASE")).doubleValue (); } 		catch (Exception e) 		{ }
		try { Gn	 	= new Double (props.getProperty ("GEAR")).doubleValue (); } 		catch (Exception e) 		{ }

		try { MOTmax = new Double (props.getProperty ("MAXMOTOR")).doubleValue (); } 	catch (Exception e) 		{ }

		// Robot kinematics parameters
		Cm		= (Math.PI * Dn) / (Ce * Gn);
		Va		= Vmax / (Math.PI * Dn);
		Cf		= Va *  (Ce * Gn) / MOTmax;
	}
	
	public void kynematics_direct (double dVl, double dVr)
	{
		this.dVl		= dVl;
		this.dVr		= dVr;
		
		vr			= (dVr + dVl) / 2.0;
		wr			= (dVr - dVl) / b;
	}
	
	public void kynematics_inverse (double speed, double turn)
	{
		// Set motion commands into the correct range
		vr	= Math.min (Math.max (speed, -Vmax), Vmax);
		wr	= Math.min (Math.max (turn, -Rmax), Rmax);

		// Compute robot command (inverse kynematics)
		dVr	= (vr + b * wr * 0.5);
		dVl	= (vr - b * wr * 0.5);

		// Check for kynematics constraints
		dVr	= Math.min (Math.max (dVr, -MOTmax), MOTmax);
		dVl	= Math.min (Math.max (dVl, -MOTmax), MOTmax);
	}

	public void kynematics_simulation (double speed, double turn)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (speed, turn);
		
		// Compute robot displacement (direct kynematics)
		kynematics_direct (dVl, dVr);
	}
}
